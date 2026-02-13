# 🐦 云雀 × Kurento：为Voice-Agent引入生产级别WebRTC实时通话能力

> **技术分享** | 作者：Skylark Team | 2026-02-13
>
> 📂 GitHub：[https://github.com/Jashinck/Skylark](https://github.com/Jashinck/Skylark)  
> 📜 协议：Apache License 2.0  
> ⭐ 欢迎 Star、Fork、Issue、PR，一起打造纯 Java 智能语音交互平台！

---

<div align="center">

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/Jashinck/Skylark/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kurento](https://img.shields.io/badge/Kurento-6.18.0-blueviolet.svg)](https://kurento.openvidu.io/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Jashinck/Skylark/pulls)

</div>

---

## 📋 目录

- [一、背景与动机](#一背景与动机)
- [二、项目速览](#二项目速览)
- [三、架构设计](#三架构设计)
- [四、核心组件深度解析](#四核心组件深度解析)
- [五、技术优势分析](#五技术优势分析)
- [六、通话流程详解](#六通话流程详解)
- [七、快速上手](#七快速上手)
- [八、Kurento 生态与行业趋势](#八kurento-生态与行业趋势)
- [九、后续规划与共建邀请](#九后续规划与共建邀请)
- [十、总结](#十总结)
- [附录：如何参与贡献](#附录如何参与贡献)

---

## 一、背景与动机

### 1.1 云雀项目简介

**云雀 (Skylark)** — *生于云端，鸣于指尖* — 是一个基于**纯 Java 生态**构建的 Voice-Agent 智能语音交互系统。

> 💡 **一句话介绍**：云雀是一个无需 Python 依赖，单一 JAR 包即可运行的 AI 语音交互平台，集成 VAD + ASR + LLM + TTS + WebRTC 完整链路。

核心能力包括：

| 模块 | 技术 | 说明 |
|------|------|------|
| **VAD** (Voice Activity Detection) | Silero + ONNX Runtime 1.16.3 | 基于深度学习的语音活动检测 |
| **ASR** (Automatic Speech Recognition) | Vosk 0.3.45 | 离线语音识别，支持中文 |
| **LLM** (Large Language Model) | 可插拔 LLM 后端 | 大语言模型智能对话 |
| **TTS** (Text-to-Speech) | 可扩展 TTS 引擎 | 文本转语音合成 |
| **RTC** (Real-Time Communication) | Kurento 6.18.0 + WebRTC | 🆕 标准 WebRTC 实时通话 |

### 1.2 此前的痛点

此前，云雀已经具备基于 WebSocket 的音频流传输方案，通过浏览器录音 → WebSocket 上传 → 服务端 VAD/ASR 处理的方式实现了基本的语音交互。然而，这种方案存在以下不足：

| 痛点 | 描述 |
|------|------|
| 🔴 非标准化 | 基于自定义 WebSocket 协议，非 WebRTC 标准，NAT 穿透能力弱 |
| 🔴 缺乏媒体处理能力 | 服务端无法对媒体流进行录制、混音、转码等操作 |
| 🟡 扩展性有限 | 难以扩展到多方通话、媒体录制等高级场景 |
| 🟡 音频质量受限 | 缺少标准的回声消除、降噪等 WebRTC 内建能力 |

### 1.3 为什么选择 Kurento？

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

## 二、项目速览

### 2.1 技术栈全景

```
┌───────────────────────────────────────────────────────────────┐
│                       云雀 (Skylark) v1.0.0                    │
├───────────────────────────────────────────────────────────────┤
│  语言: Java 17          框架: Spring Boot 3.2.0               │
│  构建: Maven             协议: Apache 2.0                      │
├───────────────────────────────────────────────────────────────┤
│  核心依赖:                                                     │
│  ├── spring-boot-starter-web        (REST API)                │
│  ├── spring-boot-starter-websocket  (WebSocket 支持)          │
│  ├── spring-boot-starter-webflux    (异步 HTTP 客户端)        │
│  ├── vosk 0.3.45                    (离线语音识别 ASR)        │
│  ├── onnxruntime 1.16.3             (Silero VAD 推理)         │
│  ├── kurento-client 6.18.0          (WebRTC 媒体服务器)       │
│  ├── jackson-databind / yaml        (JSON/YAML 解析)          │
│  ├── logback-classic                (日志框架)                │
│  └── lombok                         (代码简化)                │
├───────────────────────────────────────────────────────────────┤
│  外部服务:                                                     │
│  └── Kurento Media Server (Docker / Native)                   │
│      ws://localhost:8888/kurento                              │
└───────────────────────────────────────────────────────────────┘
```

### 2.2 项目结构 (DDD 分层架构)

```
skylark/
├── src/main/java/org/skylark/
│   ├── api/                            # 🌐 API 接口层
│   │   └── controller/                 #     RobotController (含 Kurento 端点)
│   ├── application/                    # 📋 应用层
│   │   ├── dto/                        #     数据传输对象
│   │   └── service/                    #     WebRTCService, VADService, ASRService, TTSService
│   ├── domain/                         # 🏛️ 领域层
│   │   ├── model/                      #     Dialogue, Message
│   │   └── service/                    #     领域服务接口
│   ├── infrastructure/                 # ⚙️ 基础设施层
│   │   ├── adapter/                    #     KurentoClientAdapter, WebRTCSession, AudioProcessor
│   │   └── config/                     #     WebRTCProperties, Spring 配置
│   └── common/                         # 🔧 公共层
│       ├── constant/                   #     常量定义
│       ├── exception/                  #     异常处理
│       └── util/                       #     工具类
├── src/test/java/org/skylark/          # 🧪 测试
│   ├── application/
│   │   ├── controller/                 #     RobotControllerKurentoTest
│   │   └── service/                    #     WebRTCServiceTest
│   └── infrastructure/
│       ├── adapter/webrtc/             #     KurentoClientAdapterImplTest, WebRTCSessionTest
│       └── config/                     #     WebRTCPropertiesTest
├── web/                                # 🖥️ 前端
│   ├── js/kurento-webrtc.js           #     Kurento WebRTC 客户端 (418 行)
│   ├── kurento-demo.html              #     Kurento 演示页面
│   └── webrtc.html                    #     WebSocket WebRTC 页面
├── config/                             # ⚙️ 配置
│   ├── config-java-only.yaml          #     纯 Java 模式配置
│   └── config.yaml                    #     默认配置
├── pom.xml                            #     Maven 构建配置
├── docker-compose.yml                 #     Docker 编排
├── KURENTO_INTEGRATION.md             #     Kurento 集成指南
├── WEBRTC_GUIDE.md                    #     WebRTC 集成指南
└── LICENSE                            #     Apache 2.0
```

### 2.3 API 端点一览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/kurento/session` | 创建 Kurento WebRTC 会话 |
| `POST` | `/api/webrtc/kurento/session/{id}/offer` | 处理 SDP Offer，返回 SDP Answer |
| `POST` | `/api/webrtc/kurento/session/{id}/ice-candidate` | 添加 ICE Candidate |
| `DELETE` | `/api/webrtc/kurento/session/{id}` | 关闭会话，释放资源 |

---

## 三、架构设计

### 3.1 整体架构

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
┌──────────────────────────────────────────────────────┐
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
┌──────────────────────────────────────────────────────┐
│            Kurento Media Server                       │
│   MediaPipeline → WebRtcEndpoint                     │
│   ws://localhost:8888/kurento                        │
└──────────────────────────────────────────────────────┘
```

### 3.2 数据流全景

```
🎤 用户麦克风
     ↓ (浏览器 WebRTC API)
KurentoWebRTCClient (kurento-webrtc.js)
     ↓ (REST: SDP Offer)
RobotController
     ↓
WebRTCService.processOffer()
     ↓
WebRTCSession.processOffer() ←→ Kurento Media Server
     ↓ (SDP Answer + ICE)
KurentoWebRTCClient
     ↓
═══════ WebRTC Media Stream ═══════
     ↓ (音频流)
AudioProcessor.processAudioChunk()
     ↓ (Base64 编码)
VADService.detect()           ← 语音活动检测 (Silero ONNX)
     ↓ (语音段: start → buffer → end)
ASRService.recognize()        ← 语音识别 (Vosk)
     ↓ (文本)
LLMService                    ← 大模型对话
     ↓ (回复文本)
TTSService                    ← 语音合成
     ↓ (音频)
WebRtcEndpoint                → 🔊 回传用户
```

---

## 四、核心组件深度解析

### 4.1 KurentoClientAdapter — Kurento 连接适配器

> **源码路径**: [`KurentoClientAdapter.java`](https://github.com/Jashinck/Skylark/blob/main/src/main/java/org/skylark/infrastructure/adapter/webrtc/KurentoClientAdapter.java) / [`KurentoClientAdapterImpl.java`](https://github.com/Jashinck/Skylark/blob/main/src/main/java/org/skylark/infrastructure/adapter/webrtc/KurentoClientAdapterImpl.java)

面向接口编程，解耦 Kurento 客户端的具体实现：

```java
public interface KurentoClientAdapter {
    MediaPipeline createMediaPipeline();
    void releaseMediaPipeline(String pipelineId);
    WebRtcEndpoint createWebRTCEndpoint(MediaPipeline pipeline);
    boolean isConnected();
}
```

实现亮点：

**① 生命周期管理 + 自动重连**

```java
@Component
public class KurentoClientAdapterImpl implements KurentoClientAdapter {
    
    private static final int MAX_RECONNECT_DELAY_MS = 60000;   // 最大重连间隔 60s
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000; // 初始重连间隔 1s
    
    private KurentoClient kurentoClient;
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private int reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
    
    @PostConstruct
    public void init() {
        connectToKurento();  // 应用启动时自动连接
    }
    
    @PreDestroy
    public void destroy() {
        // 释放所有 Pipeline → 销毁 KurentoClient
        pipelines.values().forEach(pipeline -> {
            try { pipeline.release(); } catch (Exception e) { /* log */ }
        });
        pipelines.clear();
        kurentoClient.destroy();
    }
}
```

**② 健康检查 + 指数退避重连**

```java
@Scheduled(fixedDelay = 30000, initialDelay = 30000)
public void healthCheck() {
    if (kurentoClient != null) {
        try {
            kurentoClient.getServerManager().getInfo(); // 心跳探测
            if (!connected) {
                connected = true;
                reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS; // 恢复后重置
            }
        } catch (Exception e) {
            connected = false;
            attemptReconnect(); // 指数退避: 1s → 2s → 4s → ... → 60s
        }
    }
}
```

**③ STUN/TURN 服务器配置**

```java
@Override
public WebRtcEndpoint createWebRTCEndpoint(MediaPipeline pipeline) {
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
    
    // 配置 STUN（必选，解决 NAT 穿透）
    String stunServer = webRTCProperties.getStun().getServer();
    if (stunServer != null && !stunServer.trim().isEmpty()) {
        webRtcEndpoint.setStunServerAddress(stunServer);
    }
    
    // 配置 TURN（可选，对称型 NAT 场景）
    if (webRTCProperties.getTurn().isEnabled()) {
        webRtcEndpoint.setTurnUrl(webRTCProperties.getTurn().getTurnUrl());
    }
    
    return webRtcEndpoint;
}
```

### 4.2 WebRTCSession — 会话封装

> **源码路径**: [`WebRTCSession.java`](https://github.com/Jashinck/Skylark/blob/main/src/main/java/org/skylark/infrastructure/adapter/webrtc/WebRTCSession.java)

封装单个 WebRTC 会话的完整生命周期（共 188 行，职责清晰）：

```java
public class WebRTCSession {
    private final String sessionId;
    private final MediaPipeline pipeline;
    private final WebRtcEndpoint webRtcEndpoint;
    private volatile boolean active;
    
    public WebRTCSession(String sessionId, MediaPipeline pipeline, WebRtcEndpoint webRtcEndpoint) {
        // 参数校验（防御性编程）
        if (sessionId == null || sessionId.trim().isEmpty())
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        if (pipeline == null)
            throw new IllegalArgumentException("Media pipeline cannot be null");
        if (webRtcEndpoint == null)
            throw new IllegalArgumentException("WebRTC endpoint cannot be null");
        
        this.sessionId = sessionId;
        this.pipeline = pipeline;
        this.webRtcEndpoint = webRtcEndpoint;
        this.active = true;
        setupEventListeners(); // 注册 4 类事件
    }
}
```

**丰富的事件监听**：

```java
private void setupEventListeners() {
    webRtcEndpoint.addMediaSessionStartedListener(event -> 
        logger.info("Media session started for session: {}", sessionId));
    webRtcEndpoint.addMediaSessionTerminatedListener(event -> 
        logger.info("Media session terminated for session: {}", sessionId));
    webRtcEndpoint.addIceCandidateFoundListener(event -> 
        logger.debug("ICE candidate found: {}", event.getCandidate().getCandidate()));
    webRtcEndpoint.addIceComponentStateChangeListener(event -> 
        logger.debug("ICE state changed: {}", event.getState()));
}
```

**资源安全释放**（先 Endpoint 后 Pipeline，幂等操作）：

```java
public void release() {
    if (!active) return;  // 幂等：多次调用安全
    active = false;
    try { webRtcEndpoint.release(); } catch (Exception e) { /* log */ }
    try { pipeline.release(); }       catch (Exception e) { /* log */ }
    logger.info("WebRTC session released: {}", sessionId);
}
```

### 4.3 AudioProcessor — VAD/ASR 音频桥接器

> **源码路径**: [`AudioProcessor.java`](https://github.com/Jashinck/Skylark/blob/main/src/main/java/org/skylark/infrastructure/adapter/webrtc/AudioProcessor.java)

作为 WebRTC 音频流与 VAD/ASR 服务之间的桥梁，实现了**语音分段累积 + 端点检测触发识别**的核心策略：

```java
public class AudioProcessor {
    private final VADService vadService;
    private final ASRService asrService;
    private final String sessionId;
    private final ByteArrayOutputStream audioBuffer;
    private volatile boolean isSpeaking;
    
    /**
     * 处理音频块 — 核心方法
     * @param audioData Raw PCM audio data (16kHz, 16-bit, mono)
     */
    public String processAudioChunk(byte[] audioData) {
        // 1. Base64 编码后送入 VAD
        String audioBase64 = Base64.getEncoder().encodeToString(audioData);
        Map<String, Object> vadResult = vadService.detect(audioBase64, sessionId);
        String status = (String) vadResult.get("status");
        
        // 2. 状态机处理
        if ("start".equals(status)) {
            isSpeaking = true;
            audioBuffer.reset();           // 新语音段开始，重置缓冲
            audioBuffer.write(audioData);
        } else if ("end".equals(status)) {
            isSpeaking = false;
            audioBuffer.write(audioData);
            byte[] completeAudio = audioBuffer.toByteArray();
            recognizeSpeech(completeAudio); // → ASR → LLM → TTS
            audioBuffer.reset();
        } else if (isSpeaking) {
            audioBuffer.write(audioData);  // 持续缓冲
        }
        return status;
    }
}
```

> 💡 **设计要点**：使用 `ByteArrayOutputStream` 缓冲音频数据，在 VAD 检测到语音结束时一次性送入 ASR，避免碎片化识别请求，提升识别准确率。

### 4.4 WebRTCService — 核心编排服务

> **源码路径**: [`WebRTCService.java`](https://github.com/Jashinck/Skylark/blob/main/src/main/java/org/skylark/application/service/WebRTCService.java)

应用层编排服务（共 254 行），统一协调所有组件：

```java
@Service
public class WebRTCService {
    private final KurentoClientAdapter kurentoClient;
    private final VADService vadService;
    private final ASRService asrService;
    private final TTSService ttsService;
    
    // 线程安全的会话存储
    private final ConcurrentHashMap<String, WebRTCSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AudioProcessor> audioProcessors = new ConcurrentHashMap<>();
    
    /** 创建会话 — 一站式编排 */
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        MediaPipeline pipeline = kurentoClient.createMediaPipeline();
        WebRtcEndpoint webRtcEndpoint = kurentoClient.createWebRTCEndpoint(pipeline);
        AudioProcessor audioProcessor = new AudioProcessor(vadService, asrService, sessionId);
        audioProcessors.put(sessionId, audioProcessor);
        WebRTCSession session = new WebRTCSession(sessionId, pipeline, webRtcEndpoint);
        sessions.put(sessionId, session);
        return sessionId;
    }
    
    /** 关闭会话 — 三层资源清理 */
    public void closeSession(String sessionId) {
        // 1️⃣ 释放 WebRTC 会话（Endpoint + Pipeline）
        WebRTCSession session = sessions.remove(sessionId);
        if (session != null) session.release();
        // 2️⃣ 清理音频处理器
        AudioProcessor processor = audioProcessors.remove(sessionId);
        if (processor != null) processor.reset();
        // 3️⃣ 清理 VAD 状态
        vadService.reset(sessionId);
    }
}
```

### 4.5 KurentoWebRTCClient — 前端客户端

> **源码路径**: [`web/js/kurento-webrtc.js`](https://github.com/Jashinck/Skylark/blob/main/web/js/kurento-webrtc.js)

前端使用 `kurento-utils` 库简化 WebRTC Peer 管理（共 418 行），具备完整的状态机和自动重连能力：

```javascript
class KurentoWebRTCClient {
    constructor() {
        this.webRtcPeer = null;
        this.sessionId = null;
        // 自动重连配置
        this.maxRetries = 3;
        this.retryDelay = 2000;               // 初始 2 秒
        this.retryBackoffMultiplier = 1.5;     // 指数退避因子
    }
    
    /** 一键启动 */
    async start() {
        // 1. POST /session → 获取 sessionId
        // 2. 创建 WebRtcPeerSendrecv（纯音频 + 回声消除 + 降噪 + AGC）
        // 3. 生成 SDP Offer → POST /session/{id}/offer → 处理 Answer
        // 4. ICE Candidate 自动收集 & 上报
    }
    
    /** 纯音频配置 */
    async createWebRtcPeer() {
        const options = {
            localVideo: null,
            remoteVideo: null,
            mediaConstraints: {
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true
                },
                video: false
            }
        };
        this.webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, callback);
    }
    
    /** ICE 连接状态监控 + 自动重连 */
    setupIceConnectionMonitoring() {
        pc.oniceconnectionstatechange = () => {
            if (state === 'connected')    // → resetRetryCount()
            if (state === 'failed')       // → handleConnectionFailure() 自动重连
        };
    }
}
```

---

## 五、技术优势分析

### 5.1 对云雀工程的直接价值

#### ✅ 标准化 WebRTC 通信

引入 Kurento 后，云雀的实时通信从自定义 WebSocket 协议升级为**标准 WebRTC**：
- 浏览器原生支持，无需额外插件
- 内建的 ICE/STUN/TURN 机制，**自动解决 NAT 穿透**
- 标准的 SDP 协商流程，兼容性极佳

#### ✅ 服务端媒体处理能力

Kurento 的 MediaPipeline 模型让服务端能够：

```
用户音频 → WebRtcEndpoint → [录制] → [转码] → [混音] → [滤镜] → 输出
```

- **录制通话** — RecorderEndpoint 录制完整通话
- **实时转码** — 不同音频编码之间转换
- **音频增强** — GStreamer 滤镜降噪
- **媒体混合** — 多方通话基础

#### ✅ 与 VAD→ASR→LLM→TTS 管道无缝集成

AudioProcessor 使用 `ByteArrayOutputStream` 缓冲音频数据，在 VAD 检测到语音结束时一次性送入 ASR，避免碎片化识别请求。

#### ✅ 架构合规性

严格遵循 DDD 分层架构：
- **基础设施层**：`KurentoClientAdapter` 面向接口编程，可替换实现
- **应用层**：`WebRTCService` 业务逻辑编排
- **API 层**：`RobotController` RESTful 扩展

#### ✅ 生产级健壮性

| 特性 | 实现方式 |
|------|---------|
| 生命周期管理 | `@PostConstruct` / `@PreDestroy` |
| 线程安全 | `ConcurrentHashMap` + `volatile` |
| 健康检查 | `@Scheduled` 每 30 秒心跳探测 |
| 自动重连 | 指数退避 (1s → 2s → 4s → ... → 60s) |
| 优雅降级 | Kurento 连接失败不影响其他功能 |
| 防御性编程 | 所有构造函数参数校验 |
| 多层资源清理 | Session → AudioProcessor → VAD 状态 |
| 前端自动重连 | 最多 3 次 × 1.5 倍退避 |

### 5.2 性能特征

| 指标 | 说明 |
|------|------|
| **延迟** | WebRTC 点对点连接，端到端延迟通常 < 150ms |
| **并发** | Kurento 单实例支持数百路并发媒体流 |
| **编解码** | 支持 Opus（高质量、低延迟）、VP8/VP9 等 |
| **内存** | 每个 MediaPipeline 约占 10-20MB |
| **CPU** | 纯音频场景下 CPU 占用极低（无视频编解码开销） |

---

## 六、通话流程详解

### 6.1 完整时序

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
  │                              │─────────────────────────────>│
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

### 6.2 SDP 协商关键细节

前端使用 `kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv` 创建全双工连接，配置纯音频约束：

```javascript
const options = {
    localVideo: null,
    remoteVideo: null,
    mediaConstraints: {
        audio: {
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true
        },
        video: false
    }
};
```

服务端通过 `WebRtcEndpoint.processOffer()` 处理 SDP Offer 并生成 Answer，随后调用 `gatherCandidates()` 启动 ICE 收集：

```java
public String processOffer(String sessionId, String sdpOffer) {
    WebRTCSession session = sessions.get(sessionId);
    String sdpAnswer = session.processOffer(sdpOffer);
    session.gatherCandidates(); // 启动 ICE 收集
    return sdpAnswer;
}
```

---

## 七、快速上手

### 7.1 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 必须 |
| Maven | 3.8+ | 构建 |
| Docker | 20.10+ | 运行 Kurento Media Server |

### 7.2 三步启动

```bash
# ① 启动 Kurento Media Server
docker run -d --name kms \
  -p 8888:8888 \
  -e KMS_MIN_PORT=40000 -e KMS_MAX_PORT=57000 \
  -p 40000-57000:40000-57000/udp \
  kurento/kurento-media-server:latest

# ② 克隆并构建
git clone https://github.com/Jashinck/Skylark.git
cd Skylark
mvn clean package -DskipTests

# ③ 启动云雀
java -jar target/skylark.jar
```

### 7.3 访问 Demo

```
🌐 Kurento 演示页面: http://localhost:8080/kurento-demo.html
🌐 WebSocket 页面:   http://localhost:8080/webrtc.html
```

### 7.4 配置文件

```yaml
# application.yml 关键配置
kurento:
  ws:
    uri: ws://localhost:8888/kurento    # Kurento Media Server 地址
webrtc:
  stun:
    server: stun:stun.l.google.com:19302 # Google 公共 STUN
  turn:
    enabled: false                       # 按需开启 TURN
    server: ""                           # TURN 服务器地址
    username: ""
    password: ""
    transport: udp                       # 传输协议 (udp/tcp)
```

---

## 八、Kurento 生态与行业趋势

### 8.1 Kurento 版本现状

> 截至 2026 年初，Kurento Media Server 最新版本为 **7.3.0**。我们项目使用的 `kurento-client 6.18.0` 是稳定的生产版本，与最新服务端完全兼容。

### 8.2 Kurento vs OpenVidu vs LiveKit

| 维度 | Kurento | OpenVidu v3 | LiveKit |
|------|---------|-------------|---------|
| 定位 | 底层媒体服务器 | 平台级封装 | 现代 SFU |
| 媒体处理 | ✅ Pipeline 模型 | ⚠️ 依赖 Kurento/LiveKit | ❌ 纯路由 |
| Java SDK | ✅ 原生支持 | ✅ 支持 | ⚠️ Go/JS 为主 |
| 滤镜/转码 | ✅ GStreamer | ⚠️ 有限 | ❌ 无 |
| 适合场景 | AI 语音/视频管道 | 视频会议室 | 大规模直播 |
| **云雀选择理由** | **✅ 服务端音频处理 + Java 原生 + Pipeline 自由组合** | | |

> 💡 OpenVidu v3 已从 Kurento 底层迁移至 LiveKit，但 Kurento 在**服务端媒体处理**（滤镜、转码、AI 管道）领域仍然是最佳选择。这正是云雀选择 Kurento 的核心原因 — 我们需要在服务端对音频流做 VAD/ASR 处理，而非简单的媒体路由。

---

## 九、后续规划与共建邀请

### 🔜 Phase 1：功能增强（近期）

| 方向 | 描述 | 优先级 | 难度 |
|------|------|--------|------|
| **通话录制** | 利用 Kurento `RecorderEndpoint` 录制通话音频 | 🔴 高 | ⭐⭐ |
| **实时字幕** | ASR 结果通过 WebSocket 实时推送前端 | 🔴 高 | ⭐⭐ |
| **打断机制 (Barge-in)** | TTS 播放中用户说话时自动打断 | 🔴 高 | ⭐⭐⭐ |
| **会话超时** | 自动检测和关闭超时会话 | 🟡 中 | ⭐ |

### 🔜 Phase 2：架构升级（中期）

| 方向 | 描述 | 优先级 | 难度 |
|------|------|--------|------|
| **WebSocket 信令** | 从 REST 升级为 WebSocket 双向信令 | 🔴 高 | ⭐⭐ |
| **Kurento 集群** | 水平扩展和高可用 | 🟡 中 | ⭐⭐⭐ |
| **多方通话** | Composite/Dispatcher 多人语音会议 | 🟡 中 | ⭐⭐⭐ |
| **GStreamer 滤镜** | 服务端音频增强（降噪、AGC） | 🟡 中 | ⭐⭐ |

### 🔜 Phase 3：智能化演进（远期）

| 方向 | 描述 | 优先级 | 难度 |
|------|------|--------|------|
| **流式 ASR** | 整段识别 → 流式识别，减少首次响应延迟 | 🔴 高 | ⭐⭐⭐ |
| **流式 TTS** | LLM 生成一段即播一段 | 🔴 高 | ⭐⭐⭐ |
| **情感语音分析** | 根据语气调整 LLM 回复风格 | 🟡 中 | ⭐⭐⭐⭐ |
| **多语言支持** | 自动语言检测和切换 | 🟡 中 | ⭐⭐⭐ |
| **端到端延迟优化** | 目标：用户说完→开始播放 < 500ms | 🔴 高 | ⭐⭐⭐⭐ |
| **视频通话** | 带画面的智能语音交互 | 🟢 低 | ⭐⭐⭐ |

### 📋 技术债务清理 (Good First Issues)

> 🙋 以下任务非常适合首次贡献者！

**已完成 ✅**

- [x] Kurento 相关的单元测试和集成测试（已覆盖 5 个测试类：`WebRTCServiceTest`、`KurentoClientAdapterImplTest`、`WebRTCSessionTest`、`RobotControllerKurentoTest`、`WebRTCPropertiesTest`）
- [x] TURN 服务器集成的完整配置化（`WebRTCProperties.Turn` 支持 enabled/server/username/password/transport，自动拼装 TURN URL）
- [x] Kurento 连接健康检查和自动重连（`@Scheduled` 每 30 秒心跳探测 + 指数退避重连 1s → 60s）
- [x] 前端 WebRTC 连接断开重试机制（`KurentoWebRTCClient.handleConnectionFailure()` 最多 3 次 × 1.5 倍退避自动重连）

**待完成 🔧**

- [ ] 🟢 **Metrics 暴露** — 将 Kurento 连接状态、活跃会话数、重连次数等指标暴露到 Spring Boot Actuator / Prometheus（难度 ⭐）
- [ ] 🟢 **docker-compose 完善** — 当前仅编排了 Java 服务，需增加 Kurento Media Server 容器，实现真正的一键部署（难度 ⭐）
- [ ] 🟢 **GitHub Actions CI/CD** — 添加自动构建、测试、Docker 镜像推送流水线（难度 ⭐⭐）
- [ ] 🟡 **性能基准测试** — 编写并发会话压测脚本，输出延迟/吞吐量/资源占用报告（难度 ⭐⭐）
- [ ] 🟡 **英文文档补充** — 技术分享文档、集成指南等补充英文版本（难度 ⭐）
- [ ] 🟡 **AudioProcessor 测试增强** — AudioProcessor 内部 VAD 状态机转换需更细粒度的单元测试覆盖（难度 ⭐⭐）
- [ ] 🟡 **WebSocket 信令替换 REST** — ICE Candidate 当前通过 REST 单向上报，需改为 WebSocket 实现服务端主动推送（难度 ⭐⭐⭐）

---

## 十、总结

Kurento Media Server 的引入为云雀项目带来了**质的飞跃**：

| # | 价值 | 说明 |
|---|------|------|
| 1 | **通信标准化** | 自定义协议 → 标准 WebRTC，获得浏览器原生音频处理能力 |
| 2 | **服务端媒体处理** | MediaPipeline 模型解锁录制、转码、混音等操作 |
| 3 | **架构扩展性** | 为多方通话、视频通话等高级场景奠定基础 |
| 4 | **工程质量** | DDD 分层 + 面向接口编程 + Spring 生态深度集成 |
| 5 | **生产级健壮性** | 自动重连、健康检查、优雅降级、多层资源清理 |

Kurento 不仅是一个技术组件的引入，更是云雀从"语音交互 Demo"向"**生产级智能语音平台**"演进的关键一步。

---

## 附录：如何参与贡献

我们热忱欢迎每一位开源爱好者加入云雀共建！🤝

### 🌟 贡献方式

| 方式 | 说明 |
|------|------|
| ⭐ **Star** | [给项目点个 Star](https://github.com/Jashinck/Skylark)，是最简单的支持 |
| 🐛 **Issue** | 发现 Bug？提一个 [Issue](https://github.com/Jashinck/Skylark/issues) |
| 💡 **Feature Request** | 有好点子？提一个 [Feature Request](https://github.com/Jashinck/Skylark/issues) |
| 🔧 **Pull Request** | 直接贡献代码！查看上方 Roadmap 和技术债务列表 |
| 📖 **文档** | 帮助完善中英文文档 |
| 🧪 **测试** | 补充测试用例，提升代码覆盖率 |

### 🚀 贡献流程

```bash
# 1. Fork 项目
# 2. 创建特性分支
git checkout -b feature/your-awesome-feature

# 3. 提交修改
git commit -m "feat: add your awesome feature"

# 4. 推送并创建 PR
git push origin feature/your-awesome-feature
```

### 💬 联系我们

- **GitHub Discussions**：[项目讨论区](https://github.com/Jashinck/Skylark/discussions)
- **Issue 反馈**：[提交 Issue](https://github.com/Jashinck/Skylark/issues)

---

<div align="center">

**🐦 云雀 (Skylark)** — *生于云端，鸣于指尖*

让智能语音交互触手可及

**如果这篇文章对你有帮助，请给 [Skylark](https://github.com/Jashinck/Skylark) 点个 ⭐ Star！**

</div>

---

*本文基于 [PR #18](https://github.com/Jashinck/Skylark/pull/18) 的实际代码编写，所有代码示例均来自仓库真实实现。