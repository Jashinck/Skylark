# 云雀引入声网Agora RTC能力：PAAS RTC赋能智能语音交互新篇章

> **技术分享** | 作者：Skylark Team | 日期：2026-03-15 | 版本：1.0.0

> 从自建RTC到拥抱PAAS生态 —— 云雀(Skylark)如何通过声网Agora Linux SDK完成RTC能力的关键升级

---

## 📋 目录

1. [引言：为什么云雀需要PAAS RTC](#-引言为什么云雀需要paas-rtc)
2. [自建RTC vs PAAS RTC：技术选型思考](#-自建rtc-vs-paas-rtc技术选型思考)
3. [声网Agora SDK集成：技术方案与实现](#-声网agora-sdk集成技术方案与实现)
4. [引入Agora后的能力升级](#-引入agora后的能力升级)
5. [面向客户的PAAS RTC赋能规划](#-面向客户的paas-rtc赋能规划)
6. [总结与展望](#-总结与展望)

---

## 📖 引言：为什么云雀需要PAAS RTC

云雀(Skylark)作为一个智能语音交互代理系统，核心使命是让AI"听懂"用户并实时对话。在此之前，我们已经完成了 **VAD→ASR→AgentScope→TTS** 完整编排流水线的构建，并通过 WebSocket、Kurento Media Server、LiveKit Server 三种策略实现了WebRTC实时通信。

然而，当我们将云雀推向企业级客户场景时，一个关键问题浮出水面：

> **自建RTC方案在全球化覆盖、网络质量保障、运维复杂度上的短板，正在制约云雀走向更大规模的商用落地。**

具体来说，我们面临三大挑战：

| 挑战 | 自建RTC的痛点 | 企业客户的诉求 |
|------|--------------|---------------|
| 🌍 全球覆盖 | 需自建边缘节点、CDN | 开箱即用的全球加速网络 |
| 📡 网络质量 | 弱网抗性差，丢包重传需自研 | 99.9%+ 可用性SLA |
| 🔧 运维成本 | 媒体服务器需独立运维 | 免运维、按需付费 |

**PAAS RTC**（Platform as a Service 实时通信）正是解决这些问题的最佳答案。它提供了工业级的实时音视频传输能力，让开发者专注于业务逻辑，而非底层传输。

---

## 🎯 自建RTC vs PAAS RTC：技术选型思考

### 云雀现有RTC策略回顾

在引入PAAS RTC之前，云雀通过策略模式(Strategy Pattern)已支持三种RTC方案：

```
WebRTCChannelStrategy (接口)
  ├── WebSocketChannelStrategy  — 基础WebSocket信令，适合开发调试
  ├── KurentoChannelStrategy    — Kurento媒体服务器，支持SDP/ICE
  └── LiveKitChannelStrategy    — LiveKit云原生方案，Token鉴权
```

这三种方案各有适用场景，但共同的局限在于：

- **WebSocket方案**：仅做信令转发，不涉及真正的媒体服务器处理
- **Kurento方案**：需要独立部署和运维Kurento Media Server
- **LiveKit方案**：虽然支持云原生，但仍需自建或托管LiveKit Server

### 为什么选择PAAS RTC

| 维度 | 自建RTC (Kurento/LiveKit) | PAAS RTC (声网Agora) |
|------|--------------------------|---------------------|
| 部署方式 | 需自行部署媒体服务器 | 零部署，SDK即用 |
| 全球加速 | 无（或需自建CDN） | 全球250+数据中心，SD-RTN™ |
| 弱网优化 | 基础TCP/UDP | 智能路由、FEC前向纠错、带宽预测 |
| 音频处理 | 需自行实现 | AI降噪、回声消除、自动增益 |
| 并发能力 | 受限于服务器规模 | 弹性扩缩容，支持百万级并发 |
| SLA保障 | 自行承担 | 99.99%可用性SLA |
| 运维成本 | 高（人力+服务器） | 按量付费，免运维 |

**结论：PAAS RTC让云雀从"自建水电站"变为"接入国家电网"，大幅降低了RTC领域的技术门槛和运维成本。**

---

## 🔧 声网Agora SDK集成：技术方案与实现

### 整体架构

Agora RTC能力的引入，完美遵循了云雀的可插拔策略架构（开闭原则），新增了 `AgoraChannelStrategy` 策略实现：

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Skylark Architecture                         │
├──────────────────┬──────────────────────────────────────────────────┤
│   API Layer      │  RobotController                                  │
│   (Controller)   │  /api/webrtc/agora/session     ← NEW             │
│                  │  /api/webrtc/kurento/**  /api/webrtc/livekit/**   │
├──────────────────┼──────────────────────────────────────────────────┤
│   Application    │  WebRTCService  ←→  OrchestrationService          │
│   Layer          │        ↑                      ↑                   │
│                  │  (VAD→ASR→AgentScope→TTS Pipeline)                │
├──────────────────┼──────────────────────────────────────────────────┤
│   Infrastructure │  ┌──────────────────────────────────────────┐    │
│   Layer          │  │       WebRTCChannelStrategy (interface)   │    │
│                  │  ├──────────┬──────────┬──────────┬─────────┤    │
│                  │  │WebSocket │ Kurento  │ LiveKit  │  Agora  │    │
│                  │  │Strategy  │ Strategy │ Strategy │ Strategy│    │
│                  │  └──────────┴──────────┴──────────┴─────────┘    │
│                  │                                                    │
│                  │  Client Adapters:                                  │
│                  │  AgoraClientAdapterImpl       ← NEW               │
│                  │  KurentoClientAdapterImpl                          │
│                  │  LiveKitClientAdapterImpl                          │
│                  │                                                    │
│                  │  Config: WebRTCStrategyConfig (Bean 工厂)          │
└──────────────────┴──────────────────────────────────────────────────┘
```

### 核心实现详解

#### 1. Maven 依赖引入

```xml
<!-- Agora RTC Server SDK for Linux (声网 RTC 服务端 SDK) -->
<dependency>
    <groupId>io.agora.rtc</groupId>
    <artifactId>linux-java-sdk</artifactId>
    <version>4.4.31.4</version>
</dependency>
```

> Agora Linux Java SDK 发布在 Maven Central，使用 `io.agora.rtc:linux-java-sdk` 坐标。核心入口类为 `io.agora.rtc.AgoraService`。

#### 2. AgoraClientAdapterImpl — 核心适配器

适配器遵循云雀的 DDD 分层架构，位于基础设施层：

```java
@Component
public class AgoraClientAdapterImpl implements AgoraClientAdapter {

    // AgoraService 单例，@PostConstruct 初始化
    private AgoraService agoraService;

    // 每频道上下文：AgoraRtcConn + AgoraLocalUser + AudioSender + AudioTrack
    private final ConcurrentHashMap<String, ChannelContext> channels;

    @PostConstruct
    public void init() {
        // 初始化 AgoraService 单例
        // 优雅降级：当 native .so 不可用时，Token 生成仍正常
    }
}
```

**关键设计决策**：

- **单例 AgoraService**：全局共享一个 SDK 实例，节约资源
- **Per-Channel ChannelContext**：每个频道独立维护连接和音频发送器
- **优雅降级**：当 native .so 库不可用时，Token 生成功能仍然正常工作，频道操作变为 no-op

#### 3. Token 生成 — 纯 Java 实现

```java
// AgoraTokenBuilder — 纯 Java HMAC-SHA256，AccessToken2/007 格式
// 无需外部 Agora SDK，仅需 appId + appCertificate
public String generateToken(String channelName, String userId, int expireSeconds) {
    return AgoraTokenBuilder.buildToken(appId, appCertificate, channelName, userId, expireSeconds);
}
```

> Token 生成使用纯 Java 实现的 HMAC-SHA256 算法，遵循 Agora AccessToken2/007 格式规范，不依赖任何外部 SDK。

#### 4. 音频流水线 — 与 OrchestrationService 深度集成

```
用户说话 → Agora SDK 接收音频帧
         → DefaultAudioFrameObserver.onPlaybackAudioFrameBeforeMixing()
         → 转发至 OrchestrationService (VAD→ASR→AgentScope→TTS)
         → TTS 输出 → AgoraAudioPcmDataSender.send()
         → Agora SDK 发送给用户
```

这实现了**服务端音频处理闭环**：远端音频通过 Agora SDK 接收，经过云雀的 AI 流水线处理后，TTS 输出再通过 Agora SDK 发回给用户。

#### 5. 策略注册 — WebRTCStrategyConfig

```java
// WebRTCStrategyConfig.java — Bean 工厂
case "agora":
    strategy = new AgoraChannelStrategy(agoraClientAdapter, orchestrationService);
    break;
```

通过 `webrtc.strategy: agora` 配置即可启用，完全遵循开闭原则。

#### 6. REST API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/agora/session` | 创建 Agora 会话，返回 appId/channelName/token/uid |
| `DELETE` | `/api/webrtc/agora/session/{sessionId}` | 关闭 Agora 会话 |

#### 7. 配置示例

```yaml
webrtc:
  strategy: agora
  agora:
    app-id: "your-agora-app-id"
    app-certificate: "your-agora-app-certificate"
    region: "cn"              # 区域: cn, na, eu, as
    sample-rate: 16000        # 采样率: 16kHz
    channels: 1               # 单声道
    token-expire-seconds: 3600  # Token 有效期 1 小时
```

---

## ⚡ 引入Agora后的能力升级

### 1. 全球化实时通信能力

| 能力 | 引入前 | 引入后 |
|------|-------|--------|
| 网络覆盖 | 单点部署，延迟受限 | 声网 SD-RTN™ 全球 250+ 数据中心 |
| 弱网抗性 | 基础 UDP 传输 | 智能路由 + FEC 前向纠错 + 带宽预测 |
| 音频质量 | 原始 PCM 直传 | AI 降噪 + 回声消除 + 自动增益控制 |
| 延迟指标 | ~300ms (取决于部署位置) | <200ms (全球 p99) |

### 2. 架构可插拔性增强

引入前：3 种 WebRTC 策略  
引入后：**4 种 WebRTC 策略**

```
WebRTCChannelStrategy (接口)
  ├── WebSocketChannelStrategy   — 开发调试
  ├── KurentoChannelStrategy     — 专业媒体服务器
  ├── LiveKitChannelStrategy     — 云原生自建
  └── AgoraChannelStrategy       — PAAS 商用级 ✨ NEW
```

通过一行配置切换：`webrtc.strategy: agora`

### 3. 企业级 Token 安全体系

- **纯 Java Token 生成**：无需部署额外服务，纯 Java HMAC-SHA256 实现
- **时效控制**：Token 自带过期时间，默认 1 小时
- **频道隔离**：每个会话独立频道，数据完全隔离
- **零信任架构**：Token 一次性使用，动态生成

### 4. 服务端音频处理闭环

这是引入 Agora 带来的最大架构升级。通过 Agora Server SDK，云雀的服务端可以：

- **直接加入 Agora 频道**：作为"AI 参与者"加入实时通话
- **实时接收远端音频**：通过 `DefaultAudioFrameObserver` 监听用户语音
- **流水线处理**：VAD 检测 → ASR 识别 → AgentScope 推理 → TTS 合成
- **实时回送 TTS 音频**：通过 `AgoraAudioPcmDataSender` 将 AI 回复推送给用户

```
┌──────────────┐     Agora SD-RTN™      ┌──────────────────────┐
│  用户终端      │  ←───── 音频传输 ─────→  │  云雀服务端            │
│  (Web/App)   │                         │  ┌──────────────────┐│
│              │  ← TTS 音频回送          │  │ AgoraClientAdapter││
│  Agora SDK   │                         │  │     ↕            ││
│  (客户端)     │  → 用户语音             │  │ OrchestrationSvc  ││
│              │                         │  │ VAD→ASR→Agent→TTS ││
└──────────────┘                         │  └──────────────────┘│
                                         └──────────────────────┘
```

### 5. 降级容错能力

Agora 适配器设计了完善的降级机制：

- **无 native .so**：Token 生成正常，频道操作变为 no-op，系统不崩溃
- **无 appCertificate**：使用占位 Token，适合开发调试
- **网络异常**：自动重连 + 状态回调通知

---

## 🔮 面向客户的PAAS RTC赋能规划

### 第一阶段：声网Agora深化（当前 → Q3 2026）

当前阶段已完成 Agora Linux Server SDK 的核心集成，接下来将深化以下能力：

| 赋能方向 | 具体内容 | 目标客户 |
|---------|---------|---------|
| 🏥 智能客服 | AI 坐席实时接入 Agora 频道，VAD+ASR+LLM 实时响应 | 金融、电商、运营商 |
| 🎓 智能教育 | AI 教师/助教加入课堂频道，实时互动教学 | 在线教育平台 |
| 🏢 企业协作 | AI 会议助手，实时语音转写 + 智能摘要 | 企业办公 |
| 🤖 IoT 终端 | 嵌入式设备通过 Agora 连接云端 AI Agent | 智能硬件厂商 |

### 第二阶段：多PAAS厂商适配（Q3 2026 → Q4 2026）

利用云雀策略模式的可插拔架构，规划接入更多 PAAS RTC 厂商：

```
✅ 已完成：声网 Agora — 全球领先，SDK 成熟度高
  ↓
🔜 Q3 2026：腾讯云 TRTC — 深度集成腾讯云生态，国内市场覆盖
  ↓
🔜 Q4 2026：阿里云 RTC — 阿里云生态联动，网络优化能力强
  ↓
🔜 Q4 2026：网易云信 NERTC — 音频处理优秀，稳定可靠
  ↓
📋 2027+：更多 PAAS 厂商按需适配
```

### 第三阶段：客户赋能产品化（2027）

| 产品形态 | 说明 |
|---------|------|
| **AI Voice Agent SDK** | 将云雀 AI 流水线封装为可集成的 SDK，客户一键接入 |
| **多租户 SaaS** | 提供 SaaS 化服务，客户通过控制台配置 AI Agent |
| **私有化部署** | 面向金融、政务等安全敏感客户的私有化交付方案 |
| **行业解决方案** | 针对客服、教育、医疗等垂直行业的预置方案 |

### 面向亟需PAAS RTC能力客户的赋能路径

对于现阶段已有明确 PAAS RTC 需求的客户，我们提供以下赋能路径：

```
┌─────────────────────────────────────────────────────────────┐
│                    客户赋能路径                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: 需求评估                                            │
│  ├── 确认客户的 RTC 场景（1v1、1vN、NvN）                      │
│  ├── 确认音频参数需求（采样率、编码格式）                        │
│  └── 确认 PAAS 厂商偏好（声网/腾讯/阿里等）                    │
│                                                             │
│  Step 2: 环境部署                                            │
│  ├── 部署云雀服务端（JAR + native .so）                       │
│  ├── 配置 Agora AppId / AppCertificate                      │
│  └── 验证 Token 生成和频道连接                                │
│                                                             │
│  Step 3: 业务集成                                            │
│  ├── 客户端接入 Agora Web/Mobile SDK                         │
│  ├── 调用云雀 REST API 创建 AI 会话                           │
│  └── AI Agent 自动加入频道，实时语音交互                       │
│                                                             │
│  Step 4: 定制优化                                            │
│  ├── 定制 AI Agent 人设和知识库                               │
│  ├── 调优 VAD/ASR/TTS 参数                                   │
│  └── 对接客户业务系统（CRM、工单等）                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🌟 总结与展望

声网 Agora Linux SDK 的引入，标志着云雀从"自建 RTC 基础设施"到"拥抱 PAAS RTC 生态"的战略转变。这不仅仅是新增一个 WebRTC 策略实现，更是一次面向商用化的关键升级：

### 三大核心价值

1. **降低 RTC 门槛** — 客户无需关心底层传输，声网提供全球级 RTC 基础设施
2. **提升交互体验** — AI 降噪、弱网优化、低延迟传输，让 AI 语音交互更自然
3. **加速商业落地** — 从 PoC 到生产，PAAS RTC 让部署周期从月级缩短到天级

### 技术亮点回顾

- ✅ 完美遵循策略模式，零修改现有代码即可扩展
- ✅ 纯 Java Token 生成，无额外依赖
- ✅ 服务端音频处理闭环，AI 全流水线在线运行
- ✅ 优雅降级设计，无 native 库亦可开发调试
- ✅ 一行配置切换：`webrtc.strategy: agora`

### 实现路线图

```
✅ 已完成：基础 WebSocket WebRTC 方案
  ↓
✅ 已完成：Kurento Media Server 集成
  ↓
✅ 已完成：LiveKit Server 集成 + 可插拔策略架构
  ↓
✅ 已完成：声网 Agora Linux SDK 集成 ← 当前里程碑
  ↓
🔜 Q3 2026：腾讯云 TRTC 适配
  ↓
🔜 Q4 2026：阿里云 RTC、网易云信 NERTC 适配
  ↓
📋 2027：客户赋能产品化 + 多行业解决方案
```

> **🐦 云雀 (Skylark)** — 生于云端，鸣于指尖  
> *PAAS RTC 的引入，让云雀的声音传得更远、更清晰、更智能。*

---

**项目地址**: [GitHub - Skylark](https://github.com/Jashinck/Skylark)  
**开源协议**: Apache License 2.0  
**技术栈**: Java 17 | Spring Boot 3.2.0 | Agora Linux SDK 4.4.31.4 | AgentScope 1.0.9
