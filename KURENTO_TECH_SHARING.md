# 🐦 云雀 × Kurento：为智能语音代理引入服务端 WebRTC 实时通话能力

> **技术分享** | 作者：Skylark Team | 2026-02-13

---

## 一、背景与动机

### 1.1 云雀项目简介

**云雀 (Skylark)** 是一个基于纯 Java 生态构建的智能语音交互代理系统，核心能力包括：

- **VAD (Voice Activity Detection)** — 基于 Silero + ONNX Runtime 的语音活动检测
- **ASR (Automatic Speech Recognition)** — 基于 Vosk 的离线语音识别
- **LLM (Large Language Model)** — 大语言模型智能对话
- **TTS (Text-to-Speech)** — 文本转语音合成

此前，云雀已经具备基于 WebSocket 的音频流传输方案，通过浏览器录音 → WebSocket 上传 → 服务端 VAD/ASR 处理的方式实现了基本的语音交互。然而，这种方案存在以下不足：

| 痛点 | 描述 |
|------|------|
| 非标准化 | 基于自定义 WebSocket 协议，非 WebRTC 标准，NAT 穿透能力弱 |
| 缺乏媒体处理能力 | 服务端无法对媒体流进行录���、混音、转码等操作 |
| 扩展性有限 | 难以扩展到多方通话、媒体录制等高级场景 |
| 音频质量受限 | 缺少标准的回声消除、降噪等 WebRTC 内建能力 |

### 1.2 为什么选择 Kurento？

在调研了多种 WebRTC 解决方案后，我们选择了 **Kurento Media Server** 作为实时通话的媒体服务器：

| 对比维度 | 纯 WebSocket 方案 | 纯浏览器 P2P WebRTC | **Kurento (SFU/MCU)** |
|----------|-------------------|---------------------|----------------------|
| NAT 穿透 | ❌ 需自行实现 | ⚠️ 依赖 STUN/TURN | ✅ 内建 ICE/STUN/TURN |
| 服务端媒体处理 | ❌ 无 | ❌ 无 | ✅ 录制、转码、混音、滤镜 |
| 扩展到多方 | ❌ 困难 | ⚠️ 网状拓扑性能差 | ✅ SFU/MCU 架构 |
| Java 生态集成 | ✅ 简单 | ❌ 无服务端 | ✅ kurento-client Java SDK |
| 标准化程度 | ❌ 自定义协议 | ✅ 标准 WebRTC | ✅ 标准 WebRTC |
| 音频质量 | ⚠️ 一般 | ✅ 浏览器 WebRTC | ✅ WebRTC + 服务端增强 |

**Kurento** 的核心优势在于：
1. **开源免费** — Apache 2.0 协议，与云雀项目协议一致
2. **Java 原生支持** — 提供 `kurento-client` Java SDK，与 Spring Boot 无缝集成
3. **服务端媒体管道** — 提供 MediaPipeline 模型，音频流可在服务端进行任意处理
4. **标准 WebRTC** — 完全遵循 WebRTC 标准，浏览器原生支持
5. **可组合架构** — MediaElement 可自由连接，构建复杂的媒体处理管道

---

## 二、架构设计

### 2.1 整体架构

本次 Kurento 集成严格遵循云雀项目的 **DDD 分层架构**，在每一层添加相应的组件：

```
┌──────────────────────────────────────────────────────┐
│                   Frontend (Browser)                  │
│   ┌────────────────────┐  ┌────────────────────────┐ │
│   │  kurento-webrtc.js │  │  kurento-demo.html     │ │
│   │  (WebRTC Client)   │  │  (Demo UI)             │ │
│   └────────┬───────────┘  └────────────────────────┘ │
└────────────┼─────────────────────────────────────────┘
             │ REST API (SDP/ICE)
             ↓
┌──────────────��───────────────────────────────────────┐
│               API Layer (RobotController)             │
│   POST   /api/webrtc/kurento/session                 │
│   POST   /api/webrtc/kurento/session/{id}/offer      │
│   POST   /api/webrtc/kurento/session/{id}/ice-candidate│
│   DELETE /api/webrtc/kurento/session/{id}            │
└────────────┬─────────────────────────────────────────┘
             │
             ↓
┌──────────────────────────────────────────────────────┐
│            Application Layer (WebRTCService)          │
│   - 会话生命周期管理                                    │
│   - SDP Offer/Answer 协商编排                          │
│   - VAD → ASR → LLM → TTS 管道集成                    │
└────────────┬─────────────────────────────────────────┘
             │
             ↓
┌──────────────────────────────────────────────────────┐
│             Infrastructure Layer                      │
│   ┌───────────────────┐  ┌──────────────────┐        │
│   │KurentoClientAdapter│  │  WebRTCSession   │        │
│   │ (Kurento连接管理)   │  │  (会话状态管理)   │        │
│   └───────────────────┘  └──────────────────┘        │
│   ┌───────────────────┐                              │
│   │  AudioProcessor   │                              │
│   │ (VAD/ASR音频桥接) │                              │
│   └───────────────────┘                              │
└────────────┬─────────────────────────────────────────┘
             │ WebSocket (JSON-RPC)
             ↓
┌────────────────────────────────────────────────────���─┐
│            Kurento Media Server                       │
│   MediaPipeline → WebRtcEndpoint                     │
│   ws://localhost:8888/kurento                        │
└──────────────────────────────────────────────────────┘
```

### 2.2 核心组件

#### 基础设施层 (Infrastructure)

**`KurentoClientAdapter` / `KurentoClientAdapterImpl`**

```java
// 适配器接口 — 解耦 Kurento 客户端
public interface KurentoClientAdapter {
    MediaPipeline createMediaPipeline();
    void releaseMediaPipeline(String pipelineId);
    WebRtcEndpoint createWebRTCEndpoint(MediaPipeline pipeline);
}
```

适配器实现通过 `@PostConstruct` 初始化 Kurento 连接，通过 `@PreDestroy` 优雅释放所有媒体管道资源。使用 `ConcurrentHashMap` 管理多个 MediaPipeline 实例，支持并发会话。

**`WebRTCSession`**

封装单个 WebRTC 会话的完整生命周期：
- MediaPipeline 和 WebRtcEndpoint 的持有与管理
- SDP Offer/Answer 协商
- ICE Candidate 的收集与添加
- 丰富的事件监听（MediaSessionStarted、MediaSessionTerminated、IceCandidateFound、IceComponentStateChange）

**`AudioProcessor`**

作为 WebRTC 音频流与 VAD/ASR 服务之间的桥梁：
- 实时接收音频块并送入 VAD 检测
- 语音开始时缓冲音频数据
- 语音结束时将累积音频送入 ASR 识别
- 识别结果进入 LLM → TTS 管道

#### 应用层 (Application)

**`WebRTCService`**

核心编排服务，职责包括：
- 创建和管理 WebRTC 会话（`ConcurrentHashMap` 线程安全存储）
- 协调 SDP 协商流程
- 将音频数据路由到 VAD → ASR → LLM → TTS 管道
- 会话关闭时的资源清理（WebRTCSession 释放、AudioProcessor 重置、VAD 状态清理）

#### 前端 (Frontend)

**`KurentoWebRTCClient` (kurento-webrtc.js)**

```javascript
const client = new KurentoWebRTCClient();
client.setStatusCallback((state, text) => { /* 状态更新 */ });
client.setMessageCallback((type, data) => { /* 消息处理 */ });
await client.start();  // 创建会话 → SDP 协商 → ICE 处理
await client.stop();   // 优雅关闭
```

特性：
- 使用 `kurento-utils` 库简化 WebRTC Peer 管理
- 纯音频模式（`video: false`），开启回声消除、降噪、自动增益
- 自动 ICE Candidate 收集与上报
- 完整的状态机和回调机制

---

## 三、技术优势分析

### 3.1 对云雀工程的直接价值

#### ✅ 标准化 WebRTC 通信

引入 Kurento 后，云雀的实时通信从自定义 WebSocket 协议升级为**标准 WebRTC**。这意味着：
- 浏览器原生支持，无需额外插件
- 内建的 ICE/STUN/TURN 机制，**自动解决 NAT 穿透**问题
- 标准的 SDP 协商流程，兼容性极佳

#### ✅ 服务端媒体处理能力

这是 Kurento 带来的**最核心能力增强**。Kurento 的 MediaPipeline 模型让服务端能够：

```
用户音频 → WebRtcEndpoint → [录制] → [转码] → [混音] → [滤镜] → 输出
```

对比之前的 WebSocket 方案，现在服务端可以：
- **录制通话** — 使用 RecorderEndpoint 录制完整的通话音频
- **实时转码** — 在不同音频编码之间转换
- **音频增强** — 通过 GStreamer 滤镜进行降噪等处理
- **媒体混合** — 为未来多方通话打下基础

#### ✅ 与现有管道的无缝集成

Kurento 的引入完美融入了云雀已有的 VAD→ASR→LLM→TTS 编排管道：

```
Kurento WebRtcEndpoint
     ↓ (音频流)
AudioProcessor
     ↓
VADService.detect()     ← 语音活动检测
     ↓ (语音段)
ASRService.recognize()  ← 语音识别
     ↓ (文本)
LLMService              ← 大模型对话
     ↓ (回复文本)
TTSService              ← 语音合成
     ↓ (音频)
WebRtcEndpoint          → 回传用户
```

AudioProcessor 使用 `ByteArrayOutputStream` 缓冲音频数据，在 VAD 检测到语音结束时一次性送入 ASR，避免了碎片化的识别请求。

#### ✅ 架构合规性

严格遵循项目 DDD 分层架构：
- **基础设施层**：KurentoClientAdapter（面向接口编程，可替换实现）
- **应用层**：WebRTCService（业务逻辑编排，不关心底层实现）
- **API 层**：RobotController 扩展（RESTful 接口，职责单一）

#### ✅ 生产级健壮性

- `@PostConstruct` / `@PreDestroy` 生命周期管理
- `ConcurrentHashMap` 线程安全的会话存储
- 完善的异常处理和日志记录
- Kurento 初始化失败时优雅降级（不影响其他功能）
- 会话关闭时的多层资源清理

### 3.2 性能特征

| 指标 | 说明 |
|------|------|
| **延迟** | WebRTC 点对点连接，端到端延迟通常 < 150ms |
| **并发** | Kurento 单实例支持数百路并发媒体流 |
| **编解码** | 支持 Opus（高质量、低延迟）、VP8/VP9 等 |
| **内存** | 每个 MediaPipeline 约占 10-20MB |
| **CPU** | 纯音频场景下 CPU 占用极低（无视频编解码开销） |

---

## 四、通话流程详解

### 4.1 完整时序

```
浏览器                      Skylark Server              Kurento Media Server
  │                              │                              │
  │ 1. POST /session             │                              │
  │─────────────────────────────>│                              │
  │                              │ 2. createMediaPipeline()     │
  │                              │─────────────────────────────>│
  │                              │<─────────────────────────────│
  │                              │ 3. createWebRtcEndpoint()    │
  │                              │─────────────────────────────>│
  │                              │<─────────────────────────────│
  │ 4. sessionId                 │                              │
  │<─────────────────────────────│                              │
  │                              │                              │
  │ 5. getUserMedia(audio)       │                              │
  │ 6. createOffer (SDP)         │                              │
  │                              │                              │
  │ 7. POST /session/{id}/offer  │                              │
  │─────────────────────────────>│                              │
  │                              │ 8. processOffer(sdp)         │
  │                              │─────────────────────────────>│
  │                              │ 9. SDP Answer                │
  │                              │<─────────────────────────────│
  │ 10. SDP Answer               │                              │
  │<─────────────────────────────│                              │
  │                              │                              │
  │ 11. setRemoteDescription     │                              │
  │                              │                              │
  │ 12. ICE Candidate            │                              │
  │─────────────────────────────>│ 13. addIceCandidate()       │
  │                              ��─────────────────────────────>│
  │                              │                              │
  │ ═══════════════ WebRTC Media Stream Established ═══════════ │
  │                              │                              │
  │ 🎤 Audio ═══════════════════>│ 14. VAD → ASR → LLM → TTS  │
  │ 🔊 Audio <═══════════════════│ 15. Response Audio           │
  │                              │                              │
  │ 16. DELETE /session/{id}     │                              │
  │─────────────────────────────>│ 17. release()               │
  │                              │─────────────────────────────>│
```

### 4.2 SDP 协商关键细节

前端使用 `kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv` 创建全双工连接，配置纯音频约束：

```javascript
const options = {
    localVideo: null,   // 无视频
    remoteVideo: null,  // 无视频
    mediaConstraints: {
        audio: {
            echoCancellation: true,   // 回声消除
            noiseSuppression: true,   // 降噪
            autoGainControl: true     // 自动增益
        },
        video: false
    }
};
```

服务端通过 `WebRtcEndpoint.processOffer()` 处理 SDP Offer 并生成 Answer，随后调用 `gatherCandidates()` 启动 ICE 收集。

---

## 五、关键代码解析

### 5.1 会话创建与管道编排

```java
// WebRTCService.createSession()
public String createSession(String userId) {
    String sessionId = UUID.randomUUID().toString();
    
    // 1. 创建 Kurento 媒体管道
    MediaPipeline pipeline = kurentoClient.createMediaPipeline();
    
    // 2. 创建 WebRTC 端点
    WebRtcEndpoint webRtcEndpoint = kurentoClient.createWebRTCEndpoint(pipeline);
    
    // 3. 创建音频处理器，桥接 VAD/ASR
    AudioProcessor audioProcessor = new AudioProcessor(vadService, asrService, sessionId);
    
    // 4. 封装为会话对象
    WebRTCSession session = new WebRTCSession(sessionId, pipeline, webRtcEndpoint);
    sessions.put(sessionId, session);
    
    return sessionId;
}
```

### 5.2 音频流处理管道

```java
// AudioProcessor.processAudioChunk()
public String processAudioChunk(byte[] audioData) {
    // VAD 检测
    Map<String, Object> vadResult = vadService.detect(audioBase64, sessionId);
    String status = (String) vadResult.get("status");
    
    if ("start".equals(status)) {
        // 语音开始：重置缓冲，开始累积
        isSpeaking = true;
        audioBuffer.reset();
        audioBuffer.write(audioData);
    } else if ("end".equals(status)) {
        // 语音结束：送入 ASR 识别
        isSpeaking = false;
        audioBuffer.write(audioData);
        byte[] completeAudio = audioBuffer.toByteArray();
        recognizeSpeech(completeAudio);  // → ASR → LLM → TTS
        audioBuffer.reset();
    } else if (isSpeaking) {
        // 语音中：持续缓冲
        audioBuffer.write(audioData);
    }
    
    return status;
}
```

### 5.3 资源优雅释放

```java
// WebRTCService.closeSession()
public void closeSession(String sessionId) {
    // 1. 释放 WebRTC 会话（Endpoint + Pipeline）
    WebRTCSession session = sessions.remove(sessionId);
    if (session != null) session.release();
    
    // 2. 清理音频处理器
    AudioProcessor processor = audioProcessors.remove(sessionId);
    if (processor != null) processor.reset();
    
    // 3. 清理 VAD 状态
    vadService.reset(sessionId);
}
```

三层清理确保无资源泄漏。

---

## 六、后续规划

### 🔜 Phase 1：功能增强（近期）

| 方向 | 描述 | 优先级 |
|------|------|--------|
| **通话录制** | 利用 Kurento `RecorderEndpoint` 录制通话音频，用于质量回顾和模型训练 | 🔴 高 |
| **实时字幕** | 将 ASR 识别结果通过 WebSocket 实时推送到前端，显示实时字幕 | 🔴 高 |
| **打断机制 (Barge-in)** | 用户在 TTS 播放过程中说话时自动打断并响应新问题 | 🔴 高 |
| **会话超时** | 自动检测和关闭超时会话，防止资源泄漏 | 🟡 中 |

### 🔜 Phase 2：架构升级（中期）

| 方向 | 描述 | 优先级 |
|------|------|--------|
| **WebSocket 信令** | 从 REST 轮询升级为 WebSocket 信令，实现双向实时通信（ICE Candidate 服务端推送） | 🔴 高 |
| **Kurento 集群** | 引入 Kurento 集群方案，支持水平扩展和高可用 | 🟡 中 |
| **多方通话** | 基于 Kurento Composite/Dispatcher 实现多人语音会议室 | 🟡 中 |
| **GStreamer 滤镜** | 利用 Kurento 的 GStreamer 后端实现服务端音频增强（降噪、AGC） | 🟡 中 |
| **OpenVidu 集成评估** | 评估升级到 OpenVidu（Kurento 上层封装）以获得更丰富的房间管理能力 | 🟢 低 |

### 🔜 Phase 3：智能化演进（远期）

| 方向 | 描述 | 优先级 |
|------|------|--------|
| **情感语音分析** | 在 AudioProcessor 中增加情感检测模块，让 LLM 根据用户语气调整回复风格 | 🟡 中 |
| **多语言支持** | 引入多语言 ASR 模型，实现自动语言检测和切换 | 🟡 中 |
| **流式 ASR** | 从整段识别升级为流式识别，减少首次响应延迟 | 🔴 高 |
| **流式 TTS** | 采用流式 TTS 方案，LLM 生成一段即播一段 | 🔴 高 |
| **端到端延迟优化** | 目标将完整链路延迟（用户说完→开始播放回复）控制在 500ms 以内 | 🔴 高 |
| **视频通话** | 基于 Kurento 的视频能力，支持带画面的智能语音交互 | 🟢 低 |

### 技术债务清理

- [ ] 补充 Kurento 相关的单元测试和集成测试
- [ ] 实现 TURN 服务器集成的完整配置化
- [ ] 添加 Kurento 连接健康检查和自动重连
- [ ] 完善前端 WebRTC 连接断开重试机制
- [ ] 性能基准测试和调优

---

## 七、总结

Kurento Media Server 的引入为云雀项目带来了**质的飞跃**：

1. **通信标准化** — 从自定义协议升级为标准 WebRTC，获得浏览器原生的音频处理能力
2. **服务端媒体处理** — MediaPipeline 模型解锁了录制、转码、混音等服务端媒体操作
3. **架构扩展性** — 为多方通话、视频通话、媒体处理等高级场景奠定了坚实基础
4. **工程质量** — 严格遵循 DDD 分层架构，面向接口编程，Spring 生态深度集成

Kurento 不仅是一个技术组件的引入，更是云雀从"语音交互 Demo"向"生产级智能语音平台"演进的关键一步。

---

*本文基于 [PR #18](https://github.com/Jashinck/Skylark/pull/18) 的实际代码编写，所有代码示例均来自仓库真实实现。*