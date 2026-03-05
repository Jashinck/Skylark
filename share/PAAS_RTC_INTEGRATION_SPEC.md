# Skylark PAAS RTC 集成规范
# Skylark PAAS RTC Integration Specification

> **版本 / Version**: 1.0.0  
> **日期 / Date**: 2026-03-05  
> **作者 / Author**: Skylark Team  
> **状态 / Status**: 草稿 / Draft  

本文档描述了将**声网（Agora/Shengwang）**和**阿里云（AliRTC/ARTC）**两家 PAAS 厂商 RTC 能力接入 Skylark 框架的完整技术规范，达到可直接交给 AI 智能体开发实现的精度。

This document provides a complete technical specification for integrating **Agora (Shengwang)** and **AliRTC (Alibaba Cloud ARTC)** PAAS RTC capabilities into the Skylark framework, at a precision level suitable for direct AI agent implementation.

---

## 目录 / Table of Contents

1. [总体架构设计](#1-总体架构设计)
2. [声网（Agora）后端接入方案](#2-声网agora后端接入方案)
3. [声网（Agora）前端接入方案](#3-声网agora前端接入方案)
4. [阿里云（AliRTC）后端接入方案](#4-阿里云alirtc后端接入方案)
5. [阿里云（AliRTC）前端接入方案](#5-阿里云alirtc前端接入方案)
6. [配置与部署](#6-配置与部署)
7. [API 端点设计](#7-api-端点设计)
8. [新增文件清单](#8-新增文件清单)
9. [接口定义与核心类伪代码](#9-接口定义与核心类伪代码)

---

## 1. 总体架构设计

### 1.1 现有策略模式回顾

Skylark 采用策略模式（Strategy Pattern）实现多 RTC 后端的可插拔切换。核心接口为 `WebRTCChannelStrategy`，当前有三个实现：

- `WebSocketChannelStrategy` — 基础 WebSocket 信令
- `KurentoChannelStrategy` — Kurento Media Server
- `LiveKitChannelStrategy` — LiveKit 云原生 WebRTC

**扩展方向**：新增两个实现 `AgoraChannelStrategy` 和 `AliRTCChannelStrategy`，无需修改现有代码（开闭原则）。

### 1.2 扩展后的架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Skylark Architecture                         │
├──────────────────┬──────────────────────────────────────────────────┤
│   API Layer      │  RobotController                                  │
│   (Controller)   │  /api/webrtc/livekit/**  /api/webrtc/kurento/**   │
│                  │  /api/webrtc/agora/**    /api/webrtc/alirtc/**    │
├──────────────────┼──────────────────────────────────────────────────┤
│   Application    │  WebRTCService  ←→  OrchestrationService          │
│   Layer          │        ↑                      ↑                   │
│                  │  (VAD→ASR→AgentService→TTS Pipeline)              │
├──────────────────┼──────────────────────────────────────────────────┤
│   Domain Layer   │  WebRTCSession  /  Dialogue  /  Message           │
├──────────────────┼──────────────────────────────────────────────────┤
│   Infrastructure │  ┌──────────────────────────────────────────┐    │
│   Layer          │  │       WebRTCChannelStrategy (interface)   │    │
│                  │  ├──────────┬──────────┬──────────┬─────────┤    │
│                  │  │WebSocket │ Kurento  │ LiveKit  │  Agora  │    │
│                  │  │Strategy  │ Strategy │ Strategy │ Strategy│    │
│                  │  ├──────────┴──────────┴──────────┼─────────┤    │
│                  │  │                                │AliRTC   │    │
│                  │  │                                │Strategy │    │
│                  │  └────────────────────────────────┴─────────┘    │
│                  │                                                    │
│                  │  Client Adapters:                                  │
│                  │  KurentoClientAdapterImpl                          │
│                  │  LiveKitClientAdapterImpl                          │
│                  │  AgoraClientAdapterImpl     ← NEW                 │
│                  │  AliRTCClientAdapterImpl    ← NEW                 │
│                  │                                                    │
│                  │  Config: WebRTCStrategyConfig (Bean 工厂)          │
│                  │  WebRTCProperties (YAML 配置绑定)                   │
└──────────────────┴──────────────────────────────────────────────────┘

Web Frontend:
  web/js/livekit-webrtc.js      (existing)
  web/js/agora-webrtc.js        ← NEW
  web/js/alirtc-webrtc.js       ← NEW
  web/livekit-demo.html         (existing)
  web/agora-demo.html           ← NEW
  web/alirtc-demo.html          ← NEW
```

### 1.3 策略切换机制

通过 `application.yaml` 中的 `webrtc.strategy` 属性在应用启动时选择策略：

```yaml
webrtc:
  strategy: agora      # 可选值: websocket | kurento | livekit | agora | alirtc
```

`WebRTCStrategyConfig` 在 `@Bean` 工厂方法中 `switch` 新增两个 case，注入对应 `ClientAdapter` Bean 并实例化对应 `ChannelStrategy`。

---

## 2. 声网（Agora）后端接入方案

### 2.1 Maven 依赖引入

声网 Server SDK for Java 支持 Maven 中央仓库直接引入：

```xml
<!-- pom.xml 新增依赖 -->
<dependency>
    <groupId>io.agora.rtc</groupId>
    <artifactId>linux-sdk</artifactId>
    <version>4.4.31.4</version>
</dependency>
```

> **说明**：`io.agora.rtc:linux-sdk` 包含 RTC 服务端 SDK 及 Token 生成工具。若 Maven 中央仓库无法获取，可从 [https://doc.shengwang.cn/doc/rtc-server-sdk/java/resources](https://doc.shengwang.cn/doc/rtc-server-sdk/java/resources) 下载本地 JAR 包，并通过 `system scope` 或本地 Maven 仓库引入：
> ```xml
> <dependency>
>     <groupId>io.agora.rtc</groupId>
>     <artifactId>linux-sdk</artifactId>
>     <version>4.4.31.4</version>
>     <scope>system</scope>
>     <systemPath>${project.basedir}/libs/agora-linux-sdk-4.4.31.4.jar</systemPath>
> </dependency>
> ```

Token 生成工具库（用于 RtcTokenBuilder2）：

```xml
<dependency>
    <groupId>io.agora</groupId>
    <artifactId>token-builder</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2.2 AgoraClientAdapter 接口设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/AgoraClientAdapter.java`

```java
package org.skylark.infrastructure.adapter.webrtc;

/**
 * Agora Client Adapter Interface
 * 声网客户端适配器接口
 */
public interface AgoraClientAdapter {

    /**
     * 生成 RTC Token（供客户端 Web SDK 使用）
     * Generate RTC Token for Web SDK client
     *
     * @param channelName 频道名
     * @param userId      用户 ID（整数或字符串 UID）
     * @param expireSeconds Token 有效期（秒）
     * @return RTC Token 字符串
     */
    String generateToken(String channelName, String userId, int expireSeconds);

    /**
     * 服务端加入频道（Server SDK join channel）
     * 用于服务端收发音频流
     *
     * @param channelName 频道名
     * @param userId      服务端 UID
     */
    void joinChannel(String channelName, String userId);

    /**
     * 服务端离开频道
     *
     * @param channelName 频道名
     */
    void leaveChannel(String channelName);

    /**
     * 向频道发送 PCM 音频帧（TTS 输出）
     * Send PCM audio frame to channel (for TTS output)
     *
     * @param channelName 频道名
     * @param pcmData     PCM 音频数据（16kHz, 16-bit, mono）
     * @param sampleRate  采样率（默认 16000）
     * @param channels    声道数（默认 1）
     */
    void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels);

    /**
     * 注册音频帧接收回调（VAD/ASR Pipeline 入口）
     * Register audio frame callback for received remote audio (for VAD/ASR pipeline)
     *
     * @param channelName 频道名
     * @param callback    音频帧回调
     */
    void registerAudioFrameCallback(String channelName, AudioFrameCallback callback);

    /**
     * 检查 SDK 是否已初始化并可用
     */
    boolean isAvailable();

    /**
     * 获取 Agora App ID（供前端使用）
     */
    String getAppId();

    /**
     * Functional interface for audio frame callback
     */
    @FunctionalInterface
    interface AudioFrameCallback {
        /**
         * @param channelName 来源频道名
         * @param userId      远端用户 UID
         * @param pcmData     PCM 原始音频数据
         * @param sampleRate  采样率
         * @param channels    声道数
         */
        void onAudioFrame(String channelName, String userId,
                          byte[] pcmData, int sampleRate, int channels);
    }
}
```

### 2.3 AgoraClientAdapterImpl 实现设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/AgoraClientAdapterImpl.java`

关键实现要点：

1. **初始化**（`@PostConstruct`）：
   - 读取 `webRTCProperties.getAgora()` 获取 `appId`、`appCertificate`
   - 调用 `AgoraRtcEngine.create(appId, handler)` 创建引擎实例
   - 设置频道场景：`AgoraRtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING)`
   - 设置客户端角色：`AgoraRtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER)`
   - 注册原始音频帧观测器：`AgoraRtcEngine.registerAudioFrameObserver(observer)`

2. **Token 生成**：使用 `RtcTokenBuilder2.buildTokenWithUserAccount(appId, appCertificate, channelName, userId, role, tokenExpireSeconds, privilegeExpireSeconds)` 生成 Token

3. **加入频道**：`AgoraRtcEngine.joinChannel(token, channelName, uid, options)` — 服务端以 `uid=0`（自动分配）或指定 UID 加入

4. **发送音频帧**：通过 `AgoraRtcEngine.pushExternalAudioFrame(frame)` 推送 PCM 数据，需先调用 `enableExternalAudioSource(true, sampleRate, channels)`

5. **接收音频帧**：实现 `IAudioFrameObserver.onPlaybackAudioFrameBeforeMixing()` 回调，将 PCM 数据传入 `OrchestrationService.processAudioStream()`

6. **资源清理**（`@PreDestroy`）：`leaveChannel()` → `AgoraRtcEngine.destroy()`

```java
package org.skylark.infrastructure.adapter.webrtc;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.RtcEngineConfig;
import io.agora.rtc.IAudioFrameObserver;
import io.agora.rtc.models.AudioFrame;
import io.agora.token.RtcTokenBuilder2;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgoraClientAdapterImpl implements AgoraClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgoraClientAdapterImpl.class);

    private final WebRTCProperties webRTCProperties;
    // channelName -> AudioFrameCallback
    private final ConcurrentHashMap<String, AudioFrameCallback> callbacks = new ConcurrentHashMap<>();

    private RtcEngine rtcEngine;
    private volatile boolean available = false;

    @Autowired
    public AgoraClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.Agora config = webRTCProperties.getAgora();
            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[Agora] appId not configured. Agora features will not be available.");
                return;
            }

            RtcEngineConfig engineConfig = new RtcEngineConfig();
            engineConfig.mAppId = config.getAppId();
            engineConfig.mEventHandler = new RtcEngine.RtcEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    logger.info("[Agora] Joined channel: {}, uid: {}", channel, uid);
                }
                @Override
                public void onUserOffline(int uid, int reason) {
                    logger.info("[Agora] User offline: uid={}, reason={}", uid, reason);
                }
                @Override
                public void onError(int err) {
                    logger.error("[Agora] Error: {}", err);
                }
            };

            rtcEngine = RtcEngine.create(engineConfig);
            // 外部音频源输入（TTS 推流）
            rtcEngine.enableExternalAudioSource(true,
                config.getSampleRate(), config.getChannels());
            // 注册原始音频帧观测器（ASR 收流）
            rtcEngine.registerAudioFrameObserver(buildAudioFrameObserver());

            available = true;
            logger.info("✅ Agora RTC Engine initialized successfully");
        } catch (Exception e) {
            logger.error("[Agora] Failed to initialize RTC Engine", e);
            available = false;
        }
    }

    @Override
    public String generateToken(String channelName, String userId, int expireSeconds) {
        WebRTCProperties.Agora config = webRTCProperties.getAgora();
        return RtcTokenBuilder2.buildTokenWithUserAccount(
            config.getAppId(),
            config.getAppCertificate(),
            channelName,
            userId,
            RtcTokenBuilder2.Role.ROLE_PUBLISHER,
            expireSeconds,
            expireSeconds
        );
    }

    @Override
    public void joinChannel(String channelName, String userId) {
        if (rtcEngine == null) throw new IllegalStateException("Agora Engine not initialized");
        String token = generateToken(channelName, userId, 3600);
        // uid=0 让 SDK 自动分配服务端 UID
        int ret = rtcEngine.joinChannel(token, channelName, null, 0);
        if (ret != 0) {
            throw new RuntimeException("[Agora] joinChannel failed, ret=" + ret);
        }
        logger.info("[Agora] Joining channel: {}", channelName);
    }

    @Override
    public void leaveChannel(String channelName) {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            callbacks.remove(channelName);
            logger.info("[Agora] Left channel: {}", channelName);
        }
    }

    @Override
    public void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels) {
        if (rtcEngine == null) return;
        AudioFrame frame = new AudioFrame();
        frame.type = AudioFrame.FRAME_TYPE_PCM16;
        frame.buffer = pcmData;
        frame.samples = pcmData.length / 2; // 16-bit = 2 bytes per sample
        frame.bytesPerSample = 2;
        frame.channels = channels;
        frame.samplesPerSec = sampleRate;
        rtcEngine.pushExternalAudioFrame(frame, System.currentTimeMillis());
    }

    @Override
    public void registerAudioFrameCallback(String channelName, AudioFrameCallback callback) {
        callbacks.put(channelName, callback);
    }

    @Override
    public boolean isAvailable() {
        return available && rtcEngine != null;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAgora().getAppId();
    }

    private IAudioFrameObserver buildAudioFrameObserver() {
        return new IAudioFrameObserver() {
            @Override
            public boolean onPlaybackAudioFrameBeforeMixing(String channelId, int uid, AudioFrame frame) {
                AudioFrameCallback cb = callbacks.get(channelId);
                if (cb != null) {
                    cb.onAudioFrame(channelId, String.valueOf(uid),
                        frame.buffer, frame.samplesPerSec, frame.channels);
                }
                return true;
            }
            // 其他回调方法返回 false（不处理）
            @Override public boolean onRecordAudioFrame(String channelId, AudioFrame frame) { return false; }
            @Override public boolean onPlaybackAudioFrame(String channelId, AudioFrame frame) { return false; }
            @Override public boolean onMixedAudioFrame(String channelId, AudioFrame frame) { return false; }
            @Override public int getObservedAudioFramePosition() { return 0x0020; } // POSITION_BEFORE_MIXING
            @Override public AudioParams getPlaybackAudioParams() { return new AudioParams(16000, 1, 2, 1024); }
            @Override public AudioParams getRecordAudioParams() { return new AudioParams(16000, 1, 2, 1024); }
            @Override public AudioParams getMixedAudioParams() { return new AudioParams(16000, 1, 2, 1024); }
            @Override public AudioParams getEarMonitoringAudioParams() { return new AudioParams(16000, 1, 2, 1024); }
        };
    }

    @PreDestroy
    public void destroy() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
            available = false;
            logger.info("[Agora] RTC Engine destroyed");
        }
    }
}
```

### 2.4 AgoraChannelStrategy 设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/strategy/AgoraChannelStrategy.java`

实现 `WebRTCChannelStrategy` 接口，对接 `AgoraClientAdapter`：

- `getStrategyName()` → `"agora"`
- `createSession(userId)`:
  1. 生成 `sessionId = UUID`
  2. 构建 `channelName = "skylark-" + sessionId`
  3. 调用 `agoraClient.joinChannel(channelName, "server-bot")`（服务端加入）
  4. 注册音频帧回调：`agoraClient.registerAudioFrameCallback(channelName, (ch, uid, pcm, rate, ch) -> orchestrationService.processAudioStream(sessionId, pcm, callback))`
  5. 将会话信息存入 `ConcurrentHashMap<String, AgoraSessionInfo> sessions`
  6. 返回 `sessionId`

- `processOffer(sessionId, sdpOffer)`:
  - 声网客户端直接使用 Token + ChannelName 连接，无需 SDP 协商
  - 生成并返回客户端连接信息 JSON：`{"token":"...", "channelName":"...", "appId":"...", "uid":"..."}`

- `addIceCandidate(...)` → no-op（声网内部处理 ICE）

- `closeSession(sessionId)`:
  1. 调用 `agoraClient.leaveChannel(channelName)`
  2. 从 `sessions` 移除

- `sessionExists(sessionId)` → `sessions.containsKey(sessionId)`
- `getActiveSessionCount()` → `sessions.size()`
- `isAvailable()` → `agoraClient.isAvailable()`

**内部类 `AgoraSessionInfo`**（参考 `LiveKitSessionInfo`）：

```java
static class AgoraSessionInfo {
    private final String sessionId;
    private final String userId;
    private final String channelName;
    private final String serverToken;
    // constructor + getters
}
```

### 2.5 与 OrchestrationService 的对接方式

声网服务端 SDK 通过 `IAudioFrameObserver.onPlaybackAudioFrameBeforeMixing()` 回调接收远端用户的 PCM 音频数据。

**对接流程**：

```
远端用户说话
    │
    ▼
Agora SDK 回调 onPlaybackAudioFrameBeforeMixing()
    │  (PCM 16kHz 16-bit mono)
    ▼
AgoraClientAdapterImpl.AudioFrameCallback.onAudioFrame()
    │
    ▼
OrchestrationService.processAudioStream(sessionId, pcmData, responseCallback)
    │
    ├── VADService.detect()  ──→ 是否有人声
    │
    ├── ASRService.recognize()  ──→ 文字转写
    │
    ├── AgentService.chat()  ──→ LLM 推理
    │
    └── TTSService.synthesize()  ──→ PCM 音频
          │
          ▼
    AgoraClientAdapterImpl.sendAudioFrame()  ──→ 推送给远端用户
```

**响应回调实现**（在 `AgoraChannelStrategy.createSession()` 内定义）：

```java
OrchestrationService.ResponseCallback responseCallback = (sid, type, data) -> {
    if ("tts_audio".equals(type) && data instanceof Map) {
        String audioBase64 = (String) ((Map<?,?>)data).get("audio");
        byte[] pcm = Base64.getDecoder().decode(audioBase64);
        agoraClient.sendAudioFrame(channelName, pcm, 16000, 1);
    }
    // 其他类型（asr_result, llm_response）可通过 REST 轮询或 WebSocket 推送给前端
};
```

### 2.6 配置项设计

在 `WebRTCProperties` 新增 `Agora` 静态内部类：

```java
public static class Agora {
    private String appId = "";
    private String appCertificate = "";
    private String region = "cn";        // cn | na | eu | ap
    private int sampleRate = 16000;      // 音频采样率
    private int channels = 1;            // 声道数
    private int tokenExpireSeconds = 3600; // Token 有效期（秒）
    // getters + setters
}
```

---

## 3. 声网（Agora）前端接入方案

### 3.1 agora-webrtc.js 设计

**文件路径**：`web/js/agora-webrtc.js`

基于声网 Web SDK `AgoraRTC`，类设计与 `LiveKitWebRTCClient` 保持一致的接口风格：

```javascript
/**
 * Agora WebRTC Client
 * 声网 WebRTC 客户端
 *
 * Implements real-time voice communication using Agora Web SDK
 * 基于声网 Web SDK 实现实时语音通信
 *
 * @author Skylark Team
 * @version 1.0.0
 */
class AgoraWebRTCClient {
    constructor() {
        this.client = null;           // AgoraRTC.createClient 实例
        this.localAudioTrack = null;  // 本地麦克风音频轨道
        this.sessionId = null;
        this.channelName = null;
        this.apiBaseUrl = this.getApiBaseUrl();
        this.statusCallback = null;
        this.messageCallback = null;
        this.connectionStateCallback = null;

        // Retry configuration (与 LiveKitWebRTCClient 保持一致)
        this.maxRetries = 3;
        this.retryDelay = 2000;
        this.retryBackoffMultiplier = 1.5;
        this.retryCount = 0;
        this.isReconnecting = false;
    }

    getApiBaseUrl() {
        const protocol = window.location.protocol;
        const host = window.location.host || 'localhost:8080';
        return `${protocol}//${host}/api/webrtc/agora`;
    }

    setStatusCallback(callback) { this.statusCallback = callback; }
    setMessageCallback(callback) { this.messageCallback = callback; }
    setConnectionStateCallback(callback) { this.connectionStateCallback = callback; }

    updateStatus(state, text) {
        console.log(`[AgoraWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) this.statusCallback(state, text);
    }

    notifyConnectionStateChange(state) {
        if (this.connectionStateCallback) this.connectionStateCallback(state);
    }

    sendMessage(type, data) {
        if (this.messageCallback) this.messageCallback(type, data);
    }

    /**
     * 启动声网 WebRTC 会话
     * Start Agora WebRTC session
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建声网会话...');

            // 1. 从后端获取 Token + ChannelName + AppId
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            if (!response.ok) throw new Error(`Failed to create session: ${response.status}`);

            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            const { token, channelName, appId, uid } = sessionData;
            this.channelName = channelName;

            this.updateStatus('connecting', '正在加入声网频道...');

            // 2. 创建 AgoraRTC 客户端
            this.client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
            this.setupClientEventListeners();

            // 3. 加入频道
            await this.client.join(appId, channelName, token, uid || null);

            // 4. 创建并发布本地麦克风音频轨道
            this.localAudioTrack = await AgoraRTC.createMicrophoneAudioTrack({
                encoderConfig: {
                    sampleRate: 16000,
                    stereo: false,
                    bitrate: 48
                },
                AEC: true,   // 回声消除
                ANS: true,   // 噪声抑制
                AGC: true    // 自动增益控制
            });
            await this.client.publish([this.localAudioTrack]);

            this.updateStatus('connected', '声网 WebRTC 通话已建立');
            this.sendMessage('success', '声网 WebRTC 连接成功！');
            this.notifyConnectionStateChange('connected');
            this.resetRetryCount();

        } catch (error) {
            console.error('[AgoraWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }

    /**
     * 设置客户端事件监听
     */
    setupClientEventListeners() {
        if (!this.client) return;

        // 收到远端用户发布的音频流
        this.client.on('user-published', async (user, mediaType) => {
            if (mediaType === 'audio') {
                await this.client.subscribe(user, mediaType);
                const remoteAudioTrack = user.audioTrack;
                remoteAudioTrack.play();  // 直接播放远端音频
                this.sendMessage('system', '收到远端音频流');
            }
        });

        // 远端用户取消发布
        this.client.on('user-unpublished', (user, mediaType) => {
            if (mediaType === 'audio') {
                this.sendMessage('system', '远端音频流已停止');
            }
        });

        // 连接状态变化
        this.client.on('connection-state-change', (curState, revState, reason) => {
            console.log(`[AgoraWebRTC] Connection: ${revState} -> ${curState}`, reason);
            this.notifyConnectionStateChange(curState.toLowerCase());

            if (curState === 'DISCONNECTED' && reason === 'NETWORK_ERROR') {
                this.handleConnectionFailure();
            }
        });

        // 用户离开
        this.client.on('user-left', (user) => {
            console.log('[AgoraWebRTC] User left:', user.uid);
        });
    }

    /**
     * 断线重连（与 LiveKitWebRTCClient 保持相同的重连策略）
     */
    async handleConnectionFailure() {
        if (this.isReconnecting || this.retryCount >= this.maxRetries) {
            if (this.retryCount >= this.maxRetries) {
                this.updateStatus('error', `连接失败，已重试 ${this.maxRetries} 次`);
            }
            return;
        }
        this.retryCount++;
        this.isReconnecting = true;
        const delay = this.retryDelay * Math.pow(this.retryBackoffMultiplier, this.retryCount - 1);
        this.updateStatus('reconnecting', `正在重连... (${this.retryCount}/${this.maxRetries})`);
        await this.sleep(delay);
        try {
            await this.stop();
            await this.start();
            this.isReconnecting = false;
        } catch (e) {
            this.isReconnecting = false;
            if (this.retryCount < this.maxRetries) await this.handleConnectionFailure();
        }
    }

    resetRetryCount() { this.retryCount = 0; }
    sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    /**
     * 停止会话
     */
    async stop() {
        try {
            this.retryCount = 0;
            this.isReconnecting = false;

            // 取消发布并关闭本地音频轨道
            if (this.localAudioTrack) {
                await this.client.unpublish([this.localAudioTrack]);
                this.localAudioTrack.close();
                this.localAudioTrack = null;
            }

            // 离开频道
            if (this.client) {
                await this.client.leave();
                this.client = null;
            }

            // 通知服务端关闭会话
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, { method: 'DELETE' });
                this.sessionId = null;
                this.channelName = null;
            }

            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', '声网 WebRTC 已断开');
        } catch (error) {
            console.error('[AgoraWebRTC] Failed to stop session:', error);
        }
    }

    isActive() {
        return this.sessionId !== null && this.client !== null;
    }
}

if (typeof window !== 'undefined') {
    window.AgoraWebRTCClient = AgoraWebRTCClient;
}
```

### 3.2 agora-demo.html 页面设计

**文件路径**：`web/agora-demo.html`

页面结构（参考 `livekit-demo.html` 样式风格）：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>云雀 (Skylark) - 声网(Agora) WebRTC 实时通话</title>
    <!-- 声网 Web SDK CDN 引入 -->
    <script src="https://download.agora.io/sdk/release/AgoraRTC_N-4.22.2.js"></script>
    <!-- 页面样式（与 livekit-demo.html 保持一致的设计语言） -->
    <style>/* ... 复用 livekit-demo.html 的 CSS，主题色改为声网橙色 #f5a623 ... */</style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎙️ 声网 (Agora) WebRTC</h1>
            <div class="subtitle">基于声网 PAAS 的实时语音通话</div>
        </div>
        <div class="status-bar">
            <div class="status-indicator">
                <div class="status-dot" id="statusDot"></div>
                <span id="statusText">未连接</span>
            </div>
            <div>策略: <strong>Agora RTC</strong></div>
        </div>
        <div class="messages" id="messages"></div>
        <div class="input-area">
            <input type="text" id="messageInput" placeholder="输入消息..." />
            <button id="sendBtn">发送</button>
        </div>
        <div class="controls">
            <button id="startBtn" class="btn-primary">开始通话</button>
            <button id="stopBtn" class="btn-danger" disabled>结束通话</button>
        </div>
    </div>
    <script src="js/agora-webrtc.js"></script>
    <script>
        // 页面交互逻辑（与 livekit-demo.html 保持一致的结构）
        const client = new AgoraWebRTCClient();
        // ... 按钮事件绑定、回调设置、消息显示等
    </script>
</body>
</html>
```

SDK CDN 地址：`https://download.agora.io/sdk/release/AgoraRTC_N-4.22.2.js`（最新版参考[声网文档](https://doc.shengwang.cn/doc/rtc/javascript/get-started/quick-start)）

---

## 4. 阿里云（AliRTC）后端接入方案

### 4.1 SDK 引入方式

阿里云 ARTC Linux SDK 目前**不支持 Maven 中央仓库**，需手动下载 SDK 包后以本地 JAR 引入。

**下载地址**：[阿里云 ARTC Linux SDK 下载](https://help.aliyun.com/zh/live/artc-download-the-sdk)

```xml
<!-- pom.xml 新增 —— 本地 JAR 方式 -->
<dependency>
    <groupId>com.aliyun.artc</groupId>
    <artifactId>alirtc-linux-sdk</artifactId>
    <version>2.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/alirtc-linux-sdk-2.0.0.jar</systemPath>
</dependency>
```

SDK 包中还包含本地共享库（`.so` 文件），需在 JVM 启动时加载：

```bash
# start.sh 或 Dockerfile 中添加 JVM 参数
-Djava.library.path=/path/to/artc/native/libs
```

或在代码中显式加载：

```java
System.loadLibrary("AliRtcEngine"); // 加载 libAliRtcEngine.so
```

### 4.2 AliRTCClientAdapter 接口设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/AliRTCClientAdapter.java`

```java
package org.skylark.infrastructure.adapter.webrtc;

/**
 * AliRTC Client Adapter Interface
 * 阿里云 ARTC 客户端适配器接口
 */
public interface AliRTCClientAdapter {

    /**
     * 生成加入频道所需的鉴权信息（Token/AuthInfo）
     * Generate authentication info for joining a channel
     *
     * @param channelId  频道 ID
     * @param userId     用户 ID
     * @return AuthInfo JSON 字符串（包含 token、nonce、timestamp 等）
     */
    String generateAuthInfo(String channelId, String userId);

    /**
     * 服务端加入频道
     *
     * @param channelId  频道 ID
     * @param userId     服务端用户 ID
     * @param authInfo   鉴权信息 JSON
     */
    void joinChannel(String channelId, String userId, String authInfo);

    /**
     * 服务端离开频道
     *
     * @param channelId 频道 ID
     */
    void leaveChannel(String channelId);

    /**
     * 向频道推送 PCM 音频帧（TTS 输出）
     *
     * @param channelId  频道 ID
     * @param pcmData    PCM 数据（16kHz, 16-bit, mono）
     * @param sampleRate 采样率
     * @param channels   声道数
     */
    void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels);

    /**
     * 注册远端音频数据回调（VAD/ASR Pipeline 入口）
     *
     * @param channelId 频道 ID
     * @param callback  音频帧回调
     */
    void registerAudioDataCallback(String channelId, AudioDataCallback callback);

    /**
     * 检查 SDK 是否可用
     */
    boolean isAvailable();

    /**
     * 获取阿里云 ARTC AppId（供前端使用）
     */
    String getAppId();

    /**
     * Audio data callback functional interface
     */
    @FunctionalInterface
    interface AudioDataCallback {
        void onAudioData(String channelId, String userId,
                         byte[] pcmData, int sampleRate, int channels);
    }
}
```

### 4.3 AliRTCClientAdapterImpl 实现设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/AliRTCClientAdapterImpl.java`

关键实现要点：

1. **JNI 本地库加载**（静态初始化块）：
   ```java
   static {
       try {
           System.loadLibrary("AliRtcEngine");
       } catch (UnsatisfiedLinkError e) {
           logger.error("[AliRTC] Failed to load native library AliRtcEngine", e);
       }
   }
   ```

2. **初始化**（`@PostConstruct`）：
   - 读取配置：`appId`、`appKey`、`appSecret`
   - 调用 `AliRtcEngine.create(context, extras)` 创建引擎实例（Linux SDK 无 Android Context，传空字符串或 null）
   - 注册原始音频数据观测器：`engine.registerAudioObserver(observer)`
   - 设置外部音频源（推流模式）：`engine.setExternalAudioSource(true, sampleRate, channels)`

3. **Token/AuthInfo 生成**：
   - 阿里云 ARTC 鉴权基于 HMAC-SHA256 签名：
     ```
     nonce = 随机字符串
     timestamp = 当前时间戳（秒）
     signature = HMAC-SHA256(appKey + channelId + userId + nonce + timestamp, appSecret)
     authInfo = JSON{appId, channelId, userId, nonce, timestamp, token: signature}
     ```
   - 或通过阿里云 OpenAPI（RTC 控制台）动态申请 Token

4. **加入频道**：`engine.joinChannel(authInfo, channelId, userId, options)`

5. **推送音频帧**：`engine.pushExternalAudioFrame(pcmData, sampleRate, channels, timestamp)`

6. **接收音频帧**：实现 `AliRtcAudioObserver.onRemoteAudioData(channelId, userId, data, type)` 回调

7. **资源清理**（`@PreDestroy`）：`engine.leaveChannel()` → `engine.destroy()`

```java
package org.skylark.infrastructure.adapter.webrtc;

import com.aliyun.artc.AliRtcEngine;
import com.aliyun.artc.AliRtcAudioObserver;
import com.aliyun.artc.AliRtcAudioData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AliRTCClientAdapterImpl implements AliRTCClientAdapter {

    static {
        try {
            System.loadLibrary("AliRtcEngine");
        } catch (UnsatisfiedLinkError e) {
            // 未安装 SDK 本地库时不阻断启动，isAvailable() 返回 false
            System.err.println("[AliRTC] Warning: Native library not found: " + e.getMessage());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(AliRTCClientAdapterImpl.class);

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioDataCallback> callbacks = new ConcurrentHashMap<>();

    private AliRtcEngine engine;
    private volatile boolean available = false;

    @Autowired
    public AliRTCClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.AliRTC config = webRTCProperties.getAlirtc();
            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[AliRTC] appId not configured. AliRTC features will not be available.");
                return;
            }

            // 创建引擎（Linux SDK extras 可传 "{}" 或配置 JSON）
            engine = AliRtcEngine.create("{}", new AliRtcEngine.AliRtcEventListener() {
                @Override
                public void onJoinChannelResult(int result, String channelId, String userId, int elapsed) {
                    logger.info("[AliRTC] Joined channel: {}, userId: {}, result: {}", channelId, userId, result);
                }
                @Override
                public void onLeaveChannelResult(int result) {
                    logger.info("[AliRTC] Left channel, result: {}", result);
                }
                @Override
                public void onRemoteUserOnLine(String channelId, String userId, int elapsed) {
                    logger.info("[AliRTC] Remote user online: {} in {}", userId, channelId);
                }
                @Override
                public void onRemoteUserOffLine(String channelId, String userId, int reason) {
                    logger.info("[AliRTC] Remote user offline: {} in {}, reason: {}", userId, channelId, reason);
                }
                @Override
                public void onOccurError(int error, String message) {
                    logger.error("[AliRTC] Error: {} - {}", error, message);
                }
            });

            // 注册音频观测器（接收远端 PCM 数据）
            engine.registerAudioObserver(new AliRtcAudioObserver() {
                @Override
                public boolean onRemoteAudioData(String channelId, String userId,
                                                 AliRtcAudioData audioData, int type) {
                    AudioDataCallback cb = callbacks.get(channelId);
                    if (cb != null) {
                        cb.onAudioData(channelId, userId, audioData.data,
                            audioData.sampleRate, audioData.channels);
                    }
                    return true;
                }
            });

            // 设置外部音频源（用于 TTS 推流）
            engine.setExternalAudioSource(true, config.getSampleRate(), config.getChannels());

            available = true;
            logger.info("✅ AliRTC Engine initialized successfully");
        } catch (Exception e) {
            logger.error("[AliRTC] Failed to initialize AliRTC Engine", e);
            available = false;
        }
    }

    @Override
    public String generateAuthInfo(String channelId, String userId) {
        try {
            WebRTCProperties.AliRTC config = webRTCProperties.getAlirtc();
            String nonce = UUID.randomUUID().toString().replace("-", "");
            long timestamp = System.currentTimeMillis() / 1000;
            String rawText = config.getAppKey() + channelId + userId + nonce + timestamp;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(rawText.getBytes(StandardCharsets.UTF_8)));

            return String.format(
                "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\","
                    + "\"nonce\":\"%s\",\"timestamp\":%d,\"token\":\"%s\"}",
                config.getAppId(), channelId, userId, nonce, timestamp, signature);
        } catch (Exception e) {
            throw new RuntimeException("[AliRTC] Failed to generate authInfo", e);
        }
    }

    @Override
    public void joinChannel(String channelId, String userId, String authInfo) {
        if (engine == null) throw new IllegalStateException("AliRTC Engine not initialized");
        int ret = engine.joinChannel(authInfo, channelId, userId, null);
        if (ret != 0) throw new RuntimeException("[AliRTC] joinChannel failed, ret=" + ret);
        logger.info("[AliRTC] Joining channel: {}", channelId);
    }

    @Override
    public void leaveChannel(String channelId) {
        if (engine != null) {
            engine.leaveChannel();
            callbacks.remove(channelId);
            logger.info("[AliRTC] Left channel: {}", channelId);
        }
    }

    @Override
    public void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels) {
        if (engine == null) return;
        AliRtcAudioData frame = new AliRtcAudioData();
        frame.data = pcmData;
        frame.sampleRate = sampleRate;
        frame.channels = channels;
        frame.samples = pcmData.length / (channels * 2); // 16-bit
        engine.pushExternalAudioFrame(frame, System.currentTimeMillis());
    }

    @Override
    public void registerAudioDataCallback(String channelId, AudioDataCallback callback) {
        callbacks.put(channelId, callback);
    }

    @Override
    public boolean isAvailable() {
        return available && engine != null;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAlirtc().getAppId();
    }

    @PreDestroy
    public void destroy() {
        if (engine != null) {
            engine.leaveChannel();
            AliRtcEngine.destroy();
            engine = null;
            available = false;
            logger.info("[AliRTC] Engine destroyed");
        }
    }
}
```

### 4.4 AliRTCChannelStrategy 设计

**文件路径**：`src/main/java/org/skylark/infrastructure/adapter/webrtc/strategy/AliRTCChannelStrategy.java`

实现 `WebRTCChannelStrategy` 接口，对接 `AliRTCClientAdapter`：

- `getStrategyName()` → `"alirtc"`
- `createSession(userId)`:
  1. 生成 `sessionId = UUID`
  2. 构建 `channelId = "skylark-" + sessionId`
  3. 生成 `authInfo = aliRTCClient.generateAuthInfo(channelId, "server-bot")`
  4. 调用 `aliRTCClient.joinChannel(channelId, "server-bot", authInfo)`
  5. 注册音频数据回调：将 PCM 数据路由至 `OrchestrationService.processAudioStream()`
  6. 存入 `ConcurrentHashMap<String, AliRTCSessionInfo> sessions`
  7. 返回 `sessionId`

- `processOffer(sessionId, sdpOffer)`:
  - 返回客户端连接信息 JSON：`{"appId":"...", "channelId":"...", "userId":"...", "authInfo":"..."}`
  - Web 前端使用此信息调用 ARTC Web SDK 加入频道

- `addIceCandidate(...)` → no-op
- `closeSession(sessionId)` → `aliRTCClient.leaveChannel(channelId)` + 移除会话
- `sessionExists / getActiveSessionCount / isAvailable` → 标准实现

**内部类 `AliRTCSessionInfo`**：

```java
static class AliRTCSessionInfo {
    private final String sessionId;
    private final String userId;
    private final String channelId;
    private final String authInfo;
    // constructor + getters
}
```

### 4.5 与 OrchestrationService 的对接方式

对接流程与声网相同，差异仅在于回调接口名：

```
远端用户说话
    │
    ▼
AliRTC SDK 回调 AliRtcAudioObserver.onRemoteAudioData()
    │  (PCM 16kHz 16-bit mono)
    ▼
AliRTCClientAdapterImpl.AudioDataCallback.onAudioData()
    │
    ▼
OrchestrationService.processAudioStream(sessionId, pcmData, responseCallback)
    │
    ├── VAD → ASR → AgentService → TTS Pipeline
    │
    └── TTS PCM 输出
          │
          ▼
    AliRTCClientAdapterImpl.pushAudioFrame()
```

### 4.6 配置项设计

在 `WebRTCProperties` 新增 `AliRTC` 静态内部类：

```java
public static class AliRTC {
    private String appId = "";
    private String appKey = "";        // 用于签名生成
    private String appSecret = "";     // 用于 HMAC-SHA256 签名
    private String region = "cn";      // 区域
    private int sampleRate = 16000;    // 音频采样率
    private int channels = 1;          // 声道数
    private int tokenExpireSeconds = 3600; // Token 有效期（秒）
    // getters + setters
}
```

---

## 5. 阿里云（AliRTC）前端接入方案

### 5.1 alirtc-webrtc.js 设计

**文件路径**：`web/js/alirtc-webrtc.js`

基于阿里云 ARTC Web SDK（`AliRtcEngine`），类设计与 `LiveKitWebRTCClient` 保持一致：

```javascript
/**
 * AliRTC WebRTC Client
 * 阿里云 ARTC WebRTC 客户端
 *
 * Implements real-time voice communication using AliRTC Web SDK
 * 基于阿里云 ARTC Web SDK 实现实时语音通信
 *
 * @author Skylark Team
 * @version 1.0.0
 */
class AliRTCWebRTCClient {
    constructor() {
        this.engine = null;           // AliRtcEngine 实例
        this.localStream = null;      // 本地媒体流
        this.sessionId = null;
        this.channelId = null;
        this.apiBaseUrl = this.getApiBaseUrl();
        this.statusCallback = null;
        this.messageCallback = null;
        this.connectionStateCallback = null;

        // Retry configuration
        this.maxRetries = 3;
        this.retryDelay = 2000;
        this.retryBackoffMultiplier = 1.5;
        this.retryCount = 0;
        this.isReconnecting = false;
    }

    getApiBaseUrl() {
        const protocol = window.location.protocol;
        const host = window.location.host || 'localhost:8080';
        return `${protocol}//${host}/api/webrtc/alirtc`;
    }

    setStatusCallback(callback) { this.statusCallback = callback; }
    setMessageCallback(callback) { this.messageCallback = callback; }
    setConnectionStateCallback(callback) { this.connectionStateCallback = callback; }

    updateStatus(state, text) {
        console.log(`[AliRTCWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) this.statusCallback(state, text);
    }

    notifyConnectionStateChange(state) {
        if (this.connectionStateCallback) this.connectionStateCallback(state);
    }

    sendMessage(type, data) {
        if (this.messageCallback) this.messageCallback(type, data);
    }

    /**
     * 启动阿里云 ARTC 会话
     * Start AliRTC session
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建阿里云 ARTC 会话...');

            // 1. 从后端获取 channelId + authInfo + appId
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            if (!response.ok) throw new Error(`Failed to create session: ${response.status}`);

            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            const { appId, channelId, userId, authInfo } = sessionData;
            this.channelId = channelId;

            this.updateStatus('connecting', '正在创建 ARTC 引擎实例...');

            // 2. 创建 AliRtcEngine 实例（配置参数根据 ARTC Web SDK 文档）
            this.engine = new AliRtcEngine({
                appId: appId,
                // 日志级别
                logLevel: 'info'
            });

            // 3. 注册事件回调
            this.setupEngineEventListeners();

            // 4. 加入频道
            const authInfoObj = JSON.parse(authInfo);
            await this.engine.joinChannel({
                channelId: channelId,
                userId: userId,
                token: authInfoObj.token,
                nonce: authInfoObj.nonce,
                timestamp: authInfoObj.timestamp
            });

            // 5. 获取麦克风并发布本地音频流
            this.localStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    sampleRate: 16000
                },
                video: false
            });
            await this.engine.publishLocalAudioStream(this.localStream);

            this.updateStatus('connected', '阿里云 ARTC 通话已建立');
            this.sendMessage('success', '阿里云 ARTC 连接成功！');
            this.notifyConnectionStateChange('connected');
            this.resetRetryCount();

        } catch (error) {
            console.error('[AliRTCWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }

    /**
     * 设置引擎事件监听
     */
    setupEngineEventListeners() {
        if (!this.engine) return;

        // 远端用户加入频道
        this.engine.on('onRemoteUserOnline', (channelId, userId) => {
            console.log(`[AliRTCWebRTC] Remote user online: ${userId} in ${channelId}`);
        });

        // 远端用户离开频道
        this.engine.on('onRemoteUserOffline', (channelId, userId, reason) => {
            console.log(`[AliRTCWebRTC] Remote user offline: ${userId}, reason: ${reason}`);
        });

        // 订阅远端音频流
        this.engine.on('onRemoteTrackAvailable', async (channelId, userId, mediaType) => {
            if (mediaType === 'audio') {
                const remoteStream = await this.engine.subscribeRemoteAudioStream(channelId, userId);
                // 播放远端音频
                const audioElement = new Audio();
                audioElement.srcObject = remoteStream;
                audioElement.id = `alirtc-remote-audio-${userId}`;
                audioElement.play().catch(e => console.warn('[AliRTCWebRTC] Auto-play blocked:', e));
                document.body.appendChild(audioElement);
                this.sendMessage('system', '收到远端音频流');
            }
        });

        // 连接状态变化
        this.engine.on('onConnectionStateChanged', (channelId, state, reason) => {
            console.log(`[AliRTCWebRTC] Connection state: ${state}, reason: ${reason}`);
            this.notifyConnectionStateChange(state.toLowerCase());

            if (state === 'DISCONNECTED') {
                this.handleConnectionFailure();
            }
        });

        // 错误事件
        this.engine.on('onOccurError', (error, message) => {
            console.error(`[AliRTCWebRTC] Error: ${error} - ${message}`);
            this.sendMessage('error', `ARTC 错误: ${message}`);
        });
    }

    /**
     * 断线重连
     */
    async handleConnectionFailure() {
        if (this.isReconnecting || this.retryCount >= this.maxRetries) {
            if (this.retryCount >= this.maxRetries) {
                this.updateStatus('error', `连接失败，已重试 ${this.maxRetries} 次`);
            }
            return;
        }
        this.retryCount++;
        this.isReconnecting = true;
        const delay = this.retryDelay * Math.pow(this.retryBackoffMultiplier, this.retryCount - 1);
        this.updateStatus('reconnecting', `正在重连... (${this.retryCount}/${this.maxRetries})`);
        await this.sleep(delay);
        try {
            await this.stop();
            await this.start();
            this.isReconnecting = false;
        } catch (e) {
            this.isReconnecting = false;
            if (this.retryCount < this.maxRetries) await this.handleConnectionFailure();
        }
    }

    resetRetryCount() { this.retryCount = 0; }
    sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    /**
     * 停止会话
     */
    async stop() {
        try {
            this.retryCount = 0;
            this.isReconnecting = false;

            // 停止本地音频流
            if (this.localStream) {
                this.localStream.getTracks().forEach(t => t.stop());
                this.localStream = null;
            }

            // 移除远端音频元素
            document.querySelectorAll('[id^="alirtc-remote-audio-"]').forEach(el => el.remove());

            // 离开频道并销毁引擎
            if (this.engine) {
                await this.engine.leaveChannel();
                this.engine.destroy();
                this.engine = null;
            }

            // 通知服务端
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, { method: 'DELETE' });
                this.sessionId = null;
                this.channelId = null;
            }

            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', '阿里云 ARTC 已断开');
        } catch (error) {
            console.error('[AliRTCWebRTC] Failed to stop session:', error);
        }
    }

    isActive() {
        return this.sessionId !== null && this.engine !== null;
    }
}

if (typeof window !== 'undefined') {
    window.AliRTCWebRTCClient = AliRTCWebRTCClient;
}
```

### 5.2 alirtc-demo.html 页面设计

**文件路径**：`web/alirtc-demo.html`

结构与 `agora-demo.html` 基本相同，差异：
- 标题：`云雀 (Skylark) - 阿里云(AliRTC) WebRTC 实时通话`
- 主题色改为阿里橙 `#FF6A00`
- SDK 引入：`<script src="https://g.alicdn.com/AliRTC/Web/latest/aliyun-webrtc-sdk.js"></script>`
- 使用 `AliRTCWebRTCClient` 替代 `AgoraWebRTCClient`

---

## 6. 配置与部署

### 6.1 WebRTCProperties 类扩展

**修改文件**：`src/main/java/org/skylark/infrastructure/config/WebRTCProperties.java`

```java
// 在现有字段后新增两个 final 字段
private final Agora agora = new Agora();
private final AliRTC alirtc = new AliRTC();

// 新增 getter 方法
public Agora getAgora() { return agora; }
public AliRTC getAlirtc() { return alirtc; }

// 新增两个静态内部类

/**
 * 声网 (Agora) 配置
 */
public static class Agora {
    private String appId = "";
    private String appCertificate = "";
    private String region = "cn";
    private int sampleRate = 16000;
    private int channels = 1;
    private int tokenExpireSeconds = 3600;
    // getters + setters for all fields
}

/**
 * 阿里云 ARTC 配置
 */
public static class AliRTC {
    private String appId = "";
    private String appKey = "";
    private String appSecret = "";
    private String region = "cn";
    private int sampleRate = 16000;
    private int channels = 1;
    private int tokenExpireSeconds = 3600;
    // getters + setters for all fields
}
```

### 6.2 WebRTCStrategyConfig 修改

**修改文件**：`src/main/java/org/skylark/infrastructure/config/WebRTCStrategyConfig.java`

```java
// 新增依赖注入
@Autowired
private AgoraClientAdapter agoraClientAdapter;

@Autowired
private AliRTCClientAdapter aliRTCClientAdapter;

@Autowired
private OrchestrationService orchestrationService;  // 用于音频回调对接

// 在 switch 语句新增两个 case
case "agora":
    strategy = new AgoraChannelStrategy(agoraClientAdapter, orchestrationService);
    logger.info("✅ Agora WebRTC strategy activated");
    break;
case "alirtc":
    strategy = new AliRTCChannelStrategy(aliRTCClientAdapter, orchestrationService);
    logger.info("✅ AliRTC WebRTC strategy activated");
    break;
```

> **注意**：`AgoraChannelStrategy` 和 `AliRTCChannelStrategy` 的构造函数需要额外接收 `OrchestrationService`（用于在创建会话时注册音频回调）。这是与 `LiveKitChannelStrategy` 的主要区别——声网和阿里云策略需要将接收到的音频实时路由到 Pipeline，而 LiveKit 的音频处理在服务端另有实现。

### 6.3 application.yaml 扩展

**修改文件**：`src/main/resources/application.yaml`

```yaml
# WebRTC Configuration (updated)
webrtc:
  # 可选值: websocket | kurento | livekit | agora | alirtc
  strategy: websocket
  kurento:
    ws-uri: ws://localhost:8888/kurento
  livekit:
    url: ""
    api-key: ""
    api-secret: ""
  # 声网 (Agora) 配置
  agora:
    app-id: ""                   # 声网 App ID（在声网控制台创建项目获取）
    app-certificate: ""          # 声网 App Certificate（用于 Token 生成）
    region: "cn"                 # 区域：cn | na | eu | ap
    sample-rate: 16000
    channels: 1
    token-expire-seconds: 3600
  # 阿里云 ARTC 配置
  alirtc:
    app-id: ""                   # 阿里云 ARTC AppId
    app-key: ""                  # 阿里云 ARTC AppKey（鉴权签名用）
    app-secret: ""               # 阿里云 ARTC AppSecret（HMAC 签名密钥）
    region: "cn"
    sample-rate: 16000
    channels: 1
    token-expire-seconds: 3600
  stun:
    server: stun:stun.l.google.com:19302
  turn:
    enabled: false
    server: ""
    username: ""
    password: ""
    transport: udp
```

### 6.4 RobotController 扩展

**修改文件**：`src/main/java/org/skylark/application/controller/RobotController.java`

新增两组 API 端点（详见第 7 节），遵循现有 Kurento/LiveKit 端点的命名模式：

```java
// ========== Agora WebRTC Endpoints ==========
@PostMapping("/agora/session")
public ResponseEntity<AgoraConnectionResponse> createAgoraSession(...)

@DeleteMapping("/agora/session/{sessionId}")
public ResponseEntity<Void> closeAgoraSession(...)

// ========== AliRTC WebRTC Endpoints ==========
@PostMapping("/alirtc/session")
public ResponseEntity<AliRTCConnectionResponse> createAliRTCSession(...)

@DeleteMapping("/alirtc/session/{sessionId}")
public ResponseEntity<Void> closeAliRTCSession(...)
```

### 6.5 Maven pom.xml 依赖添加

```xml
<!-- 声网 Server SDK for Java -->
<dependency>
    <groupId>io.agora.rtc</groupId>
    <artifactId>linux-sdk</artifactId>
    <version>4.4.31.4</version>
    <!-- 若 Maven 仓库不可达，改用 system scope + libs/ 目录 -->
</dependency>

<!-- 声网 Token Builder（用于生成 RTC Token） -->
<dependency>
    <groupId>io.agora</groupId>
    <artifactId>token-builder</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- 阿里云 ARTC Linux SDK（本地 JAR 方式） -->
<dependency>
    <groupId>com.aliyun.artc</groupId>
    <artifactId>alirtc-linux-sdk</artifactId>
    <version>2.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/alirtc-linux-sdk-2.0.0.jar</systemPath>
</dependency>
```

**本地 JAR 目录**：在项目根目录新建 `libs/` 目录，存放下载的 JAR 包和本地 `.so` 库。

### 6.6 Docker/部署说明

**阿里云 AliRTC 本地库加载**（需在容器中部署）：

```dockerfile
# Dockerfile 中复制本地 .so 库
COPY libs/*.so /usr/local/lib/
RUN ldconfig

# JVM 启动参数
ENV JAVA_OPTS="-Djava.library.path=/usr/local/lib"
```

**声网 SDK** 包含本地库（`libagora_rtc_sdk.so`），也需同样处理：

```dockerfile
COPY libs/agora-native/*.so /usr/local/lib/
RUN ldconfig
```

**策略切换**（通过环境变量覆盖 YAML 配置）：

```bash
# 使用声网策略
docker run -e WEBRTC_STRATEGY=agora \
           -e WEBRTC_AGORA_APP_ID=your_app_id \
           -e WEBRTC_AGORA_APP_CERTIFICATE=your_cert \
           skylark-app

# 使用阿里云策略
docker run -e WEBRTC_STRATEGY=alirtc \
           -e WEBRTC_ALIRTC_APP_ID=your_app_id \
           -e WEBRTC_ALIRTC_APP_KEY=your_app_key \
           -e WEBRTC_ALIRTC_APP_SECRET=your_secret \
           skylark-app
```

> Spring Boot 支持通过环境变量 `WEBRTC_STRATEGY`（对应 `webrtc.strategy`）覆盖 YAML 配置，属性名中的 `.` 转换为 `_`，并全大写。

---

## 7. API 端点设计

### 7.1 声网（Agora）REST API 端点

| 方法   | 路径                               | 描述                                      |
|------|------------------------------------|-------------------------------------------|
| POST | `/api/webrtc/agora/session`        | 创建声网会话，返回 Token + ChannelName + AppId  |
| DELETE | `/api/webrtc/agora/session/{id}` | 关闭声网会话，服务端离开频道                  |

**POST /api/webrtc/agora/session 请求体**（与 LiveKit 一致）：

```json
{
  "userId": "user-1712345678"
}
```

**POST /api/webrtc/agora/session 响应体**（`AgoraConnectionResponse`）：

```json
{
  "sessionId": "uuid-string",
  "appId": "声网 App ID",
  "channelName": "skylark-uuid-string",
  "token": "声网 RTC Token",
  "uid": "user-1712345678",
  "status": "created",
  "message": "Agora WebRTC session created successfully"
}
```

**新增 DTO 类**：`src/main/java/org/skylark/application/dto/webrtc/AgoraConnectionResponse.java`

```java
package org.skylark.application.dto.webrtc;

public class AgoraConnectionResponse {
    private String sessionId;
    private String appId;
    private String channelName;
    private String token;
    private String uid;
    private String status;
    private String message;
    // constructor + getters + setters
}
```

### 7.2 阿里云（AliRTC）REST API 端点

| 方法   | 路径                                 | 描述                                          |
|------|--------------------------------------|-----------------------------------------------|
| POST | `/api/webrtc/alirtc/session`         | 创建阿里云会话，返回 AppId + ChannelId + AuthInfo |
| DELETE | `/api/webrtc/alirtc/session/{id}`  | 关闭阿里云会话，服务端离开频道                      |

**POST /api/webrtc/alirtc/session 响应体**（`AliRTCConnectionResponse`）：

```json
{
  "sessionId": "uuid-string",
  "appId": "阿里云 ARTC App ID",
  "channelId": "skylark-uuid-string",
  "userId": "user-1712345678",
  "authInfo": "{\"token\":\"...\",\"nonce\":\"...\",\"timestamp\":1712345678}",
  "status": "created",
  "message": "AliRTC session created successfully"
}
```

**新增 DTO 类**：`src/main/java/org/skylark/application/dto/webrtc/AliRTCConnectionResponse.java`

```java
package org.skylark.application.dto.webrtc;

public class AliRTCConnectionResponse {
    private String sessionId;
    private String appId;
    private String channelId;
    private String userId;
    private String authInfo;
    private String status;
    private String message;
    // constructor + getters + setters
}
```

### 7.3 与现有 API 端点的一致性

| 策略      | Session 创建端点                  | Session 关闭端点                       | 响应类                         |
|---------|----------------------------------|--------------------------------------|-------------------------------|
| Kurento | `POST /kurento/session`          | `DELETE /kurento/session/{id}`       | `WebRTCSessionResponse`       |
| LiveKit | `POST /livekit/session`          | `DELETE /livekit/session/{id}`       | `LiveKitConnectionResponse`   |
| **Agora** | `POST /agora/session`          | `DELETE /agora/session/{id}`         | `AgoraConnectionResponse`     |
| **AliRTC** | `POST /alirtc/session`        | `DELETE /alirtc/session/{id}`        | `AliRTCConnectionResponse`    |

所有端点均遵循以下规范：
- 基础路径 `/api/webrtc/{strategy}/session`
- 创建用 `POST`，关闭用 `DELETE`
- 错误返回 `500` + JSON body `{status: "error", message: "..."}`

---

## 8. 新增文件清单

### 8.1 需新增的文件

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/AgoraClientAdapter.java` | 声网客户端适配器接口，定义 Token 生成、加入/离开频道、发送/接收音频帧等方法 |
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/AgoraClientAdapterImpl.java` | 声网客户端适配器实现，封装 Agora RTC Engine Java SDK |
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/strategy/AgoraChannelStrategy.java` | 声网通道策略，实现 `WebRTCChannelStrategy` 接口，管理 Agora 会话生命周期 |
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/AliRTCClientAdapter.java` | 阿里云 ARTC 客户端适配器接口 |
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/AliRTCClientAdapterImpl.java` | 阿里云 ARTC 客户端适配器实现，封装 AliRtcEngine Linux SDK（JNI） |
| `src/main/java/org/skylark/infrastructure/adapter/webrtc/strategy/AliRTCChannelStrategy.java` | 阿里云通道策略，实现 `WebRTCChannelStrategy` 接口 |
| `src/main/java/org/skylark/application/dto/webrtc/AgoraConnectionResponse.java` | 声网会话创建 REST 响应 DTO |
| `src/main/java/org/skylark/application/dto/webrtc/AliRTCConnectionResponse.java` | 阿里云会话创建 REST 响应 DTO |
| `web/js/agora-webrtc.js` | 声网 Web 端 JS 客户端，封装 AgoraRTC Web SDK，接口风格对齐 `LiveKitWebRTCClient` |
| `web/js/alirtc-webrtc.js` | 阿里云 Web 端 JS 客户端，封装 AliRTC Web SDK |
| `web/agora-demo.html` | 声网实时通话演示页面 |
| `web/alirtc-demo.html` | 阿里云实时通话演示页面 |

### 8.2 需修改的文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/java/org/skylark/infrastructure/config/WebRTCProperties.java` | 新增 `Agora` 和 `AliRTC` 静态内部类及对应 getter 方法 |
| `src/main/java/org/skylark/infrastructure/config/WebRTCStrategyConfig.java` | 注入 `AgoraClientAdapter`、`AliRTCClientAdapter`、`OrchestrationService`；在 `switch` 新增 `"agora"` 和 `"alirtc"` case |
| `src/main/java/org/skylark/application/controller/RobotController.java` | 新增 `/agora/**` 和 `/alirtc/**` 端点方法 |
| `src/main/resources/application.yaml` | 新增 `webrtc.agora.*` 和 `webrtc.alirtc.*` 配置项 |
| `pom.xml` | 新增声网 SDK 和阿里云 ARTC SDK 依赖 |

---

## 9. 接口定义与核心类伪代码

### 9.1 WebRTCChannelStrategy 接口（现有，无需修改）

```java
public interface WebRTCChannelStrategy {
    String getStrategyName();
    String createSession(String userId);
    String processOffer(String sessionId, String sdpOffer);
    void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex);
    void closeSession(String sessionId);
    boolean sessionExists(String sessionId);
    int getActiveSessionCount();
    boolean isAvailable();
}
```

### 9.2 AgoraChannelStrategy 完整伪代码

```java
package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.service.OrchestrationService;
import org.skylark.infrastructure.adapter.webrtc.AgoraClientAdapter;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AgoraChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AgoraChannelStrategy.class);

    private final AgoraClientAdapter agoraClient;
    private final OrchestrationService orchestrationService;
    private final ConcurrentHashMap<String, AgoraSessionInfo> sessions = new ConcurrentHashMap<>();

    public AgoraChannelStrategy(AgoraClientAdapter agoraClient,
                                 OrchestrationService orchestrationService) {
        this.agoraClient = agoraClient;
        this.orchestrationService = orchestrationService;
    }

    @Override
    public String getStrategyName() {
        return "agora";
    }

    @Override
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String channelName = "skylark-" + sessionId;
            logger.info("[Agora] Creating session for user: {}, channel: {}", userId, channelName);

            // 1. 服务端加入声网频道
            agoraClient.joinChannel(channelName, "skylark-server-bot");

            // 2. 注册音频帧回调，接收远端 PCM 数据并送入 OrchestrationService Pipeline
            agoraClient.registerAudioFrameCallback(channelName,
                (ch, uid, pcmData, sampleRate, channels) -> {
                    OrchestrationService.ResponseCallback responseCallback = (sid, type, data) -> {
                        if ("tts_audio".equals(type) && data instanceof Map) {
                            String audioBase64 = (String) ((Map<?, ?>) data).get("audio");
                            if (audioBase64 != null) {
                                byte[] ttsAudio = Base64.getDecoder().decode(audioBase64);
                                agoraClient.sendAudioFrame(ch, ttsAudio, sampleRate, channels);
                            }
                        }
                    };
                    orchestrationService.processAudioStream(sessionId, pcmData, responseCallback);
                });

            // 3. 生成客户端连接 Token
            String clientToken = agoraClient.generateToken(channelName, userId, 3600);

            AgoraSessionInfo sessionInfo = new AgoraSessionInfo(
                sessionId, userId, channelName, clientToken);
            sessions.put(sessionId, sessionInfo);

            logger.info("[Agora] Session created successfully: {}", sessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("[Agora] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create Agora WebRTC session", e);
        }
    }

    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        AgoraSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        // 声网客户端通过 Token + ChannelName 直接加入，无需 SDP 协商
        logger.debug("[Agora] Returning connection info for session: {}", sessionId);
        return String.format(
            "{\"token\":\"%s\",\"channelName\":\"%s\",\"appId\":\"%s\",\"uid\":\"%s\"}",
            session.getClientToken(),
            session.getChannelName(),
            agoraClient.getAppId(),
            session.getUserId());
    }

    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        // no-op — 声网内部处理 ICE
        logger.debug("[Agora] ICE handling delegated to Agora SDK for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        try {
            AgoraSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                agoraClient.leaveChannel(session.getChannelName());
                orchestrationService.cleanupSession(sessionId);
                logger.info("[Agora] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[Agora] Error closing session: {}", sessionId, e);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public int getActiveSessionCount() {
        return sessions.size();
    }

    @Override
    public boolean isAvailable() {
        return agoraClient.isAvailable();
    }

    /**
     * Internal session info for Agora strategy
     */
    static class AgoraSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelName;
        private final String clientToken;

        AgoraSessionInfo(String sessionId, String userId,
                          String channelName, String clientToken) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelName = channelName;
            this.clientToken = clientToken;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelName() { return channelName; }
        public String getClientToken() { return clientToken; }
    }
}
```

### 9.3 AliRTCChannelStrategy 完整伪代码

与 `AgoraChannelStrategy` 结构完全一致，差异如下：

```java
public class AliRTCChannelStrategy implements WebRTCChannelStrategy {

    private final AliRTCClientAdapter aliRTCClient;
    private final OrchestrationService orchestrationService;
    private final ConcurrentHashMap<String, AliRTCSessionInfo> sessions = new ConcurrentHashMap<>();

    @Override
    public String getStrategyName() { return "alirtc"; }

    @Override
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String channelId = "skylark-" + sessionId;

        // 生成鉴权信息
        String authInfo = aliRTCClient.generateAuthInfo(channelId, "skylark-server-bot");

        // 服务端加入频道
        aliRTCClient.joinChannel(channelId, "skylark-server-bot", authInfo);

        // 注册音频数据回调
        aliRTCClient.registerAudioDataCallback(channelId,
            (ch, uid, pcmData, sampleRate, channels) -> {
                OrchestrationService.ResponseCallback responseCallback = (sid, type, data) -> {
                    if ("tts_audio".equals(type) && data instanceof Map) {
                        String audioBase64 = (String) ((Map<?, ?>) data).get("audio");
                        if (audioBase64 != null) {
                            byte[] ttsAudio = Base64.getDecoder().decode(audioBase64);
                            aliRTCClient.pushAudioFrame(ch, ttsAudio, sampleRate, channels);
                        }
                    }
                };
                orchestrationService.processAudioStream(sessionId, pcmData, responseCallback);
            });

        // 生成客户端鉴权信息
        String clientAuthInfo = aliRTCClient.generateAuthInfo(channelId, userId);
        sessions.put(sessionId,
            new AliRTCSessionInfo(sessionId, userId, channelId, clientAuthInfo));
        return sessionId;
    }

    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        AliRTCSessionInfo session = sessions.get(sessionId);
        if (session == null) throw new IllegalArgumentException("Session not found: " + sessionId);
        return String.format(
            "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\",\"authInfo\":%s}",
            aliRTCClient.getAppId(),
            session.getChannelId(),
            session.getUserId(),
            session.getClientAuthInfo());
    }

    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        // no-op
    }

    @Override
    public void closeSession(String sessionId) {
        AliRTCSessionInfo session = sessions.remove(sessionId);
        if (session != null) {
            aliRTCClient.leaveChannel(session.getChannelId());
            orchestrationService.cleanupSession(sessionId);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) { return sessions.containsKey(sessionId); }

    @Override
    public int getActiveSessionCount() { return sessions.size(); }

    @Override
    public boolean isAvailable() { return aliRTCClient.isAvailable(); }

    static class AliRTCSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelId;
        private final String clientAuthInfo;
        // constructor + getters
        AliRTCSessionInfo(String sessionId, String userId,
                           String channelId, String clientAuthInfo) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelId = channelId;
            this.clientAuthInfo = clientAuthInfo;
        }
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public String getClientAuthInfo() { return clientAuthInfo; }
    }
}
```

### 9.4 RobotController 声网/阿里云端点伪代码

```java
// ========== Agora WebRTC Endpoints ==========

@PostMapping("/agora/session")
public ResponseEntity<AgoraConnectionResponse> createAgoraSession(
        @RequestBody CreateSessionRequest request) {
    try {
        String sessionId = webRTCService.createSession(request.getUserId());
        String connectionInfo = webRTCService.processOffer(sessionId, "");

        // 解析 JSON: {"token":"...", "channelName":"...", "appId":"...", "uid":"..."}
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(connectionInfo);

        AgoraConnectionResponse response = new AgoraConnectionResponse(
            sessionId,
            node.get("appId").asText(),
            node.get("channelName").asText(),
            node.get("token").asText(),
            node.get("uid").asText(),
            "created",
            "Agora WebRTC session created successfully"
        );
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(500)
            .body(new AgoraConnectionResponse(null, null, null, null, null,
                "error", "Failed to create Agora session"));
    }
}

@DeleteMapping("/agora/session/{sessionId}")
public ResponseEntity<Void> closeAgoraSession(@PathVariable String sessionId) {
    try {
        webRTCService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}

// ========== AliRTC WebRTC Endpoints ==========

@PostMapping("/alirtc/session")
public ResponseEntity<AliRTCConnectionResponse> createAliRTCSession(
        @RequestBody CreateSessionRequest request) {
    try {
        String sessionId = webRTCService.createSession(request.getUserId());
        String connectionInfo = webRTCService.processOffer(sessionId, "");

        // 解析 JSON: {"appId":"...", "channelId":"...", "userId":"...", "authInfo":"..."}
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(connectionInfo);

        AliRTCConnectionResponse response = new AliRTCConnectionResponse(
            sessionId,
            node.get("appId").asText(),
            node.get("channelId").asText(),
            node.get("userId").asText(),
            node.get("authInfo").asText(),
            "created",
            "AliRTC session created successfully"
        );
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(500)
            .body(new AliRTCConnectionResponse(null, null, null, null, null,
                "error", "Failed to create AliRTC session"));
    }
}

@DeleteMapping("/alirtc/session/{sessionId}")
public ResponseEntity<Void> closeAliRTCSession(@PathVariable String sessionId) {
    try {
        webRTCService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}
```

---

## 附录：注意事项与开发建议

### A. 线程安全

1. `AgoraClientAdapterImpl` 和 `AliRTCClientAdapterImpl` 使用 `ConcurrentHashMap` 管理回调，确保多会话并发安全
2. 音频帧回调在 SDK 内部线程中触发，`OrchestrationService.processAudioStream()` 内部有 `ConcurrentHashMap` 保护，线程安全
3. `AgoraChannelStrategy` / `AliRTCChannelStrategy` 的 `sessions` 字段使用 `ConcurrentHashMap`，与 `LiveKitChannelStrategy` 保持一致
4. `volatile boolean available/connected` 确保初始化状态的可见性

### B. SDK 未安装时的降级处理

两个 `ClientAdapterImpl` 在初始化失败时（SDK 未安装、配置缺失）不抛出异常，仅记录警告并设置 `available = false`。`isAvailable()` 返回 `false`，上层可据此给出友好错误提示。

### C. 声网 SDK 特殊说明

- 声网 Server SDK（Linux Java）版本随时更新，以[官网](https://doc.shengwang.cn/doc/rtc-server-sdk/java/resources)最新版为准
- `RtcEngine` 是**单例**，一个进程只能创建一个实例；多频道场景需使用 `AgoraRtcChannel` 或多线程隔离
- 服务端推流前需调用 `enableExternalAudioSource(true, ...)` 和 `setChannelProfile(LIVE_BROADCASTING)`

### D. 阿里云 ARTC SDK 特殊说明

- 阿里云 ARTC Linux SDK 目前处于**商务定制**阶段，需联系阿里云商务申请后才能下载完整 SDK 包
- SDK 依赖 `libAliRtcEngine.so` 等原生库，必须在运行环境中正确配置 `java.library.path`
- `AliRtcEngine.create()` 方法签名可能因 SDK 版本不同而有差异，以实际 SDK 包的 JavaDoc 为准

### E. 与现有架构的一致性检查

| 检查项 | 声网 | 阿里云 |
|--------|------|--------|
| 实现 `WebRTCChannelStrategy` | ✅ | ✅ |
| 使用 `ConcurrentHashMap` 管理会话 | ✅ | ✅ |
| 实现 `isAvailable()` 降级 | ✅ | ✅ |
| 日志格式 `[Agora/AliRTC] ...` | ✅ | ✅ |
| `@PostConstruct` / `@PreDestroy` 生命周期 | ✅ | ✅ |
| 接口 JS 类与 `LiveKitWebRTCClient` 对齐 | ✅ | ✅ |
| REST 端点命名规范 `/api/webrtc/{strategy}/session` | ✅ | ✅ |
| 配置属性 `webrtc.agora.*` / `webrtc.alirtc.*` | ✅ | ✅ |

---

🏷️ 标签

`#Agora` `#声网` `#AliRTC` `#阿里云` `#ARTC` `#PAAS` `#RTC` `#WebRTC` `#Java` `#SpringBoot` `#策略模式` `#DDD` `#音视频` `#实时通信` `#Skylark` `#VoiceAgent`
