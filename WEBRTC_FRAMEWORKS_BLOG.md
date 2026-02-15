# 🐦 双剑合璧：云雀 Voice-Agent 引入 Kurento + LiveKit 双 WebRTC 框架的技术实践与商业价值

> **技术分享** | 作者：Skylark Team | 2026-02-15
>
> 📂 GitHub：[https://github.com/Jashinck/Skylark](https://github.com/Jashinck/Skylark)  
> 📜 协议：Apache License 2.0  
> ⭐ 欢迎 Star、Fork、Issue、PR，一起打造纯 Java 智能语音交互平台！

---

**License**: Apache 2.0 | **Java**: 17 | **Spring Boot**: 3.2.0 | **Kurento**: 6.18.0 | **LiveKit**: 0.12.0 | **PRs Welcome**

---

## 📋 目录

- [一、引言：为什么 Voice-Agent 需要双引擎？](#一引言为什么-voice-agent-需要双引擎)
- [二、云雀项目回顾](#二云雀项目回顾)
- [三、Kurento — 自托管媒体处理的重武器](#三kurento--自托管媒体处理的重武器)
- [四、LiveKit — 云原生时代的轻量级新星](#四livekit--云原生时代的轻量级新星)
- [五、策略模式：一个接口、三种实现、零感切换](#五策略模式一个接口三种实现零感切换)
- [六、双框架对比与协同](#六双框架对比与协同)
- [七、对云雀 Voice-Agent 的技术收益](#七对云雀-voice-agent-的技术收益)
- [八、给小微企业带来的商业价值](#八给小微企业带来的商业价值)
- [九、快速上手指南](#九快速上手指南)
- [十、后续规划与开源共建邀请](#十后续规划与开源共建邀请)
- [十一、总结](#十一总结)
- [附录：如何参与贡献](#附录如何参与贡献)

---

## 一、引言：为什么 Voice-Agent 需要双引擎？

在 AI 语音交互领域，**实时通信（RTC）** 是连接用户与智能体之间的"最后一公里"。一个优秀的 Voice-Agent 系统不仅需要具备 VAD（语音检测）→ ASR（语音识别）→ LLM（大语言模型）→ TTS（语音合成）的完整编排能力，更需要一个**高质量、低延迟、可靠的实时音频传输通道**。

然而，实际生产中，不同规模的企业对 WebRTC 方案的诉求截然不同：

- **需要服务端录制、转码、混音**：媒体流必须经过服务端 → 适配方案：**Kurento**（SFU/MCU）
- **追求轻量部署、云原生架构**：最少运维、弹性扩缩容 → 适配方案：**LiveKit**（SFU）
- **私有化部署、数据不出域**：全链路自托管 → 适配方案：**Kurento**
- **快速接入、Token 鉴权即用**：低学习成本、快速上线 → 适配方案：**LiveKit**

正是基于以上考量，云雀项目在已有 WebSocket 基础方案的基础上，先后引入了 **Kurento 6.18.0** 和 **LiveKit 0.12.0** 两大 WebRTC 框架，并通过**策略模式（Strategy Pattern）**实现了三种通道的无缝切换，让开发者和企业能够按需选择最适合自身场景的实时通信方案。

---

## 二、云雀项目回顾

**云雀（Skylark）** — *生于云端，鸣于指尖* — 是一个基于**纯 Java 生态**构建的智能语音交互代理系统（Voice-Agent）。

> 💡 **一句话介绍**：无需 Python 依赖，单一 JAR 包即可运行，集成 VAD + ASR + LLM + TTS + WebRTC 完整链路的 AI 语音交互平台。

### 核心能力全景

- **VAD**：Silero + ONNX Runtime 1.16.3 — 基于深度学习的语音活动检测
- **ASR**：Vosk 0.3.45 — 离线语音识别，支持中文
- **LLM**：可插拔后端（Ollama / OpenAI） — 大语言模型智能对话
- **TTS**：MaryTTS / 可扩展 — 文本转语音合成
- **RTC**：WebSocket / Kurento 6.18.0 / LiveKit 0.12.0 — 🆕 三种 WebRTC 通道策略

### 技术栈

**Java 17 + Spring Boot 3.2.0 + Maven**

- Vosk 0.3.45（离线 ASR）
- ONNX Runtime 1.16.3（Silero VAD）
- Kurento Client 6.18.0（WebRTC 媒体服务器）
- LiveKit Server 0.12.0（云原生 WebRTC）
- Spring Web / WebSocket / WebFlux

---

## 三、Kurento — 自托管媒体处理的重武器

### 3.1 为什么引入 Kurento？

在云雀项目的早期阶段，我们的音频传输依赖自定义 WebSocket 协议。虽然能满足基本的语音交互需求，但在以下方面存在明显短板：

- ❌ **非标准化**：无法利用浏览器原生 WebRTC 的 NAT 穿透、回声消除、降噪等能力
- ❌ **无服务端媒体处理**：无法在服务端录制、转码、混音
- ❌ **扩展性受限**：难以支持多方通话等复杂场景

**Kurento Media Server** 以开源、成熟、Java 原生的特性，成为了我们的首选：

- ✅ **Apache 2.0 协议**，与云雀项目一致
- ✅ **kurento-client Java SDK**，与 Spring Boot 无缝集成
- ✅ **MediaPipeline 模型**，音频流可在服务端任意编排
- ✅ **标准 WebRTC**，浏览器原生支持，ICE/STUN/TURN 内建
- ✅ **服务端媒体处理**，支持录制、混音、转码、滤镜

### 3.2 云雀中的 Kurento 架构

**🖥️ Browser (kurento-demo.html)**
→ REST API (SDP/ICE)

**RobotController (Kurento Endpoints)**
- POST /api/webrtc/kurento/session
- POST /session/{id}/offer
- POST /session/{id}/ice-candidate
- DELETE /session/{id}

→

**WebRTCService (应用层)**
- 会话管理 + VAD→ASR→LLM→TTS 编排

→

**KurentoChannelStrategy (策略实现)**
- KurentoClientAdapterImpl
- MediaPipeline 创建与缓存
- WebRtcEndpoint 配置 (STUN/TURN)
- 健康检查 + 指数退避自动重连
- 优雅关闭 + 资源释放

→ WebSocket (JSON-RPC)

**Kurento Media Server (Docker)**
- ws://localhost:8888/kurento

### 3.3 核心实现亮点

#### ① 自动健康检查与指数退避重连

```java
@Scheduled(fixedDelay = 30000, initialDelay = 30000)
public void healthCheck() {
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
```

#### ② 完整的会话生命周期管理

WebRTCSession 封装了单个 WebRTC 会话的全部状态，包括 MediaPipeline、WebRtcEndpoint、事件监听，以及资源的幂等释放。

#### ③ AudioProcessor 音频桥接

Kurento 的音频流通过 AudioProcessor 桥接到 VAD/ASR 管道，实现了 WebRTC 媒体流与 AI 语音处理的无缝衔接。

---

## 四、LiveKit — 云原生时代的轻量级新星

### 4.1 为什么还需要 LiveKit？

Kurento 是一把功能强大的"重武器"，但在某些场景下可能显得"大材小用"：

- Kurento Media Server 需要独立部署和运维
- 对于只需要标准音视频传输、不需要服务端媒体处理的场景，部署成本偏高
- 在云原生和 Kubernetes 环境下，LiveKit 的容器化友好度更高

**LiveKit** 作为新一代开源 WebRTC 框架，带来了截然不同的设计理念：

- ✅ **云原生架构**：Go 语言实现，天然适配 K8s，资源占用极低
- ✅ **Token 鉴权**：JWT Token 即可完成接入，客户端无需复杂的 SDP/ICE 手动协商
- ✅ **Room 模型**：基于房间的会话管理，天然支持多人场景
- ✅ **丰富的 SDK**：提供 JavaScript、Go、Python、Java 等多语言 SDK
- ✅ **完善的生态**：内建录制、Egress/Ingress 等高级功能

### 4.2 云雀中的 LiveKit 架构

**🖥️ Browser (livekit-demo.html)**
→ ① REST API 创建会话获取 Token

**RobotController (LiveKit Endpoints)**
- POST /api/webrtc/livekit/session
- DELETE /session/{sessionId}

→

**LiveKitChannelStrategy (策略实现)**
- LiveKitClientAdapterImpl
- Room 创建与管理
- JWT Token 生成
- 连接状态监控

→

**LiveKit Server (Docker/Cloud)**
- wss://livekit.example.com

→ ② 客户端直接使用 Token 连接 LiveKit Server

**🖥️ Browser (livekit-client SDK v2.6.4)**
- 自动 SDP/ICE 协商
- 自动重连（指数退避）
- 连接质量监控

### 4.3 核心实现亮点

#### ① 极简的接入流程

与 Kurento 需要手动 SDP/ICE 协商不同，LiveKit 的接入只需两步：

1. **服务端**：创建 Room + 生成 JWT Token
2. **客户端**：使用 Token 直接连接 LiveKit Server

```java
// 服务端 — LiveKitClientAdapterImpl
public String generateToken(String roomName, String participantIdentity) {
    AccessToken token = new AccessToken(apiKey, apiSecret);
    token.setName(participantIdentity);
    token.setIdentity(participantIdentity);
    token.addGrants(new RoomJoin(true), new RoomName(roomName));
    return token.toJwt();
}
```

```javascript
// 客户端 — livekit-webrtc.js
async connectToRoom(url, token) {
    this.room = new LivekitClient.Room();
    await this.room.connect(url, token);
    // 完成！无需手动处理 SDP/ICE
}
```

#### ② 自动重连与连接质量监控

```javascript
// 客户端内建断线重连
this.room.on(LivekitClient.RoomEvent.Reconnecting, () => {
    this.log('正在重新连接...');
});

this.room.on(LivekitClient.RoomEvent.ConnectionQualityChanged, (quality) => {
    this.log(`连接质量变化: ${quality}`);
});
```

#### ③ Room 模型天然支持多人

LiveKit 的 Room 模型使得从 1v1 扩展到多人场景几乎零成本，这对于未来多方语音会议、多 Agent 协作等场景具有重要价值。

---

## 五、策略模式：一个接口、三种实现、零感切换

### 5.1 设计理念

云雀采用**策略模式（Strategy Pattern）**统一管理三种 WebRTC 通道实现，这是本次双框架引入中最关键的架构设计：

**WebRTCChannelStrategy**（统一策略接口）

三种实现：

- **WebSocketChannelStrategy** — 基础方案
- **KurentoChannelStrategy** — 媒体处理方案
- **LiveKitChannelStrategy** — 云原生方案

### 5.2 统一接口定义

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

### 5.3 一行配置切换

```yaml
# application.yaml — 只需修改这一行即可切换 WebRTC 引擎
webrtc:
  strategy: websocket   # 可选: websocket / kurento / livekit
```

```java
// WebRTCStrategyConfig.java — 根据配置自动注入对应策略
@Bean
public WebRTCChannelStrategy webRTCChannelStrategy() {
    switch (strategyName.toLowerCase()) {
        case "kurento":  return new KurentoChannelStrategy(kurentoClientAdapter);
        case "livekit":  return new LiveKitChannelStrategy(liveKitClientAdapter);
        default:         return new WebSocketChannelStrategy();
    }
}
```

### 5.4 这个设计的价值

- **开闭原则**：新增 WebRTC 方案（如声网 Agora）只需实现接口，不改已有代码
- **运行时切换**：通过配置文件即可切换，无需修改代码或重新编译
- **独立测试**：每种策略可独立进行单元测试，互不干扰
- **渐进迁移**：企业可先用 WebSocket 验证，再平滑升级到 Kurento 或 LiveKit

---

## 六、双框架对比与协同

### 6.1 特性对比

**架构模型**
- Kurento：SFU / MCU（媒体管道）
- LiveKit：SFU（房间模型）

**核心语言**
- Kurento：Java (Client) + C++ (Server)
- LiveKit：Go (Server) + 多语言 SDK

**部署方式**
- Kurento：Docker / 独立进程
- LiveKit：Docker / K8s / 云服务

**媒体处理**
- Kurento：✅ 录制、混音、转码、滤镜
- LiveKit：✅ 录制（Egress）、导入（Ingress）

**SDP 协商**
- Kurento：手动 Offer/Answer + ICE
- LiveKit：自动（Token 即用）

**NAT 穿透**
- Kurento：内建 ICE/STUN/TURN
- LiveKit：内建 ICE/STUN/TURN

**接入复杂度**
- Kurento：中等（需理解 SDP/ICE 流程）
- LiveKit：低（Token 鉴权即可）

**资源占用**
- Kurento：较高（C++ 媒体服务器）
- LiveKit：较低（Go 运行时）

**社区活跃度**
- Kurento：成熟稳定，文档丰富
- LiveKit：快速增长，势头强劲

**Java 集成**
- Kurento：⭐ kurento-client 原生 SDK
- LiveKit：livekit-server SDK

**协议**
- Kurento：Apache 2.0
- LiveKit：Apache 2.0

### 6.2 适用场景矩阵

**LiveKit** 适合：低部署成本 + 低媒体处理需求的场景（快速接入、云原生）

**Kurento** 适合：高部署成本 + 高媒体处理需求的场景（媒体处理、私有化部署）

- **初创公司快速接入语音客服** → 推荐 **LiveKit**：部署简单，Token 鉴权，分钟级上线
- **金融/医疗行业的私有化部署** → 推荐 **Kurento**：全链路自托管，数据不出域
- **需要通话录制与质检分析** → 推荐 **Kurento**：服务端 MediaPipeline 原生支持
- **K8s 微服务架构的云原生企业** → 推荐 **LiveKit**：Go 运行时，容器友好，弹性伸缩
- **多人语音会议 + AI Agent** → 推荐 **LiveKit**：Room 模型天然支持多方
- **需要实时音频转码/混音** → 推荐 **Kurento**：MediaElement 可组合架构

### 6.3 协同价值

双框架并非互相替代，而是互为补充：

- **开发阶段**：使用 WebSocket 策略快速原型验证
- **测试阶段**：切换到 LiveKit 进行集成测试（部署简单）
- **生产阶段**：根据业务需求选择 Kurento（重媒体处理）或 LiveKit（轻量云原生）

这种"渐进式升级"的路径对于小微企业尤为友好——不需要一步到位，可以随着业务发展逐步升级实时通信能力。

---

## 七、对云雀 Voice-Agent 的技术收益

引入 Kurento 和 LiveKit 双框架后，云雀 Voice-Agent 获得了显著的技术能力提升：

### 7.1 从"能用"到"好用"

**音频质量**
- 引入前（WebSocket）：WebSocket 二进制传输
- 引入后（Kurento + LiveKit）：WebRTC 标准（回声消除、降噪、自动增益）

**NAT 穿透**
- 引入前：需自行处理
- 引入后：ICE/STUN/TURN 内建

**连接稳定性**
- 引入前：依赖 WebSocket
- 引入后：WebRTC + 自动重连 + 健康检查

**延迟**
- 引入前：受 WebSocket 帧限制
- 引入后：WebRTC UDP 实时传输

**浏览器兼容**
- 引入前：需自定义录音逻辑
- 引入后：浏览器原生 WebRTC API

### 7.2 架构优势

- **可插拔设计**：策略模式让 WebRTC 通道可替换，架构不与任何单一框架耦合
- **渐进迁移**：从 WebSocket → Kurento → LiveKit，每一步都是平滑过渡
- **独立演进**：Kurento 和 LiveKit 的实现互不干扰，可以独立升级和优化
- **测试友好**：130+ 单元测试覆盖，包含 Kurento 和 LiveKit 的专项测试

### 7.3 生态扩展能力

双框架的引入为云雀建立了一个可扩展的 WebRTC 适配层，未来接入声网 Agora、腾讯云 TRTC 等商业 RTC-PaaS 时，只需实现 `WebRTCChannelStrategy` 接口即可，无需修改上层业务逻辑。

**未来扩展路径：WebRTCChannelStrategy**

- ✅ WebSocketChannelStrategy — 已实现
- ✅ KurentoChannelStrategy — 已实现
- ✅ LiveKitChannelStrategy — 已实现
- 🔜 AgoraChannelStrategy — 规划中
- 🔜 TRTCChannelStrategy — 规划中
- 🔜 更多 ...

---

## 八、给小微企业带来的商业价值

对于小微企业而言，云雀搭载双 WebRTC 框架后，可以直接转化为实实在在的业务价值：

### 8.1 智能客服系统 — 降低 70% 人力成本

**智能语音客服方案**

客户来电 → WebRTC 实时连接（LiveKit / Kurento） → VAD 检测客户说话 → ASR："我想查一下上个月的账单" → LLM：理解意图，查询数据库，生成回复 → TTS："您上个月的账单金额为1280元..." → WebRTC 实时回传语音

💰 **收益**：7×24 无休，处理 80% 常见咨询
📉 **成本**：替代 3-5 名客服人力

**为什么双框架更有价值？**
- 使用 **LiveKit** 快速上线基础客服功能（1 周内部署完成）
- 业务稳定后切换 **Kurento** 实现通话录制、质检分析（监管合规）

### 8.2 AI 电话销售助手 — 提升转化率

- **实时语音对话**：WebRTC (Kurento/LiveKit) — 自然的人机交互体验
- **意图识别**：VAD + ASR + LLM — 精准判断客户需求
- **智能话术推荐**：LLM 实时生成 — 提升销售转化率 30%+
- **通话录音与分析**：Kurento MediaPipeline — 优化话术、培训新人

### 8.3 语音导航与预约系统

小微诊所、律师事务所、美容院等服务型企业：

- **语音导航**：客户拨入后，AI 语音引导选择服务项目
- **智能预约**：通过语音对话完成预约，自动记录到日程
- **候诊提醒**：TTS 语音主动回拨提醒客户

### 8.4 多语言客服（出海企业）

- ASR/LLM/TTS 支持多语言切换
- WebRTC 保障跨境通话质量
- 一套系统服务全球客户

### 8.5 成本对比分析

- **传统人工客服 (5人)**：月成本 ¥25,000+，8h 服务，能力有限
- **商业 AI 客服 SaaS**：月成本 ¥3,000-10,000，依赖厂商，数据外流
- **云雀 + LiveKit（自部署）**：月成本 **¥500-1,000**（云服务器），7×24 服务，数据自主
- **云雀 + Kurento（私有部署）**：月成本 **¥0**（自有服务器），完全私有，零 API 费用

> 💡 **核心优势**：开源免费 + 纯 Java 生态 + 单 JAR 部署 = 小微企业也能拥有企业级 AI 语音客服

---

## 九、快速上手指南

### 9.1 方案一：LiveKit 模式（推荐新手）

```bash
# 1. 启动 LiveKit Server (Docker)
docker run -d --name livekit \
  -p 7880:7880 -p 7881:7881 -p 7882:7882/udp \
  livekit/livekit-server \
  --dev --bind 0.0.0.0

# 2. 修改配置
# application.yaml
webrtc:
  strategy: livekit
  livekit:
    url: "ws://localhost:7880"
    api-key: "devkey"
    api-secret: "secret"

# 3. 构建并启动云雀
mvn clean package -DskipTests
java -jar target/skylark.jar

# 4. 访问演示页面
open http://localhost:8080/livekit-demo.html
```

### 9.2 方案二：Kurento 模式（需要媒体处理）

```bash
# 1. 启动 Kurento Media Server (Docker)
docker run -d --name kms \
  -p 8888:8888 \
  -e KMS_MIN_PORT=40000 -e KMS_MAX_PORT=57000 \
  -p 40000-57000:40000-57000/udp \
  kurento/kurento-media-server:latest

# 2. 修改配置
# application.yaml
webrtc:
  strategy: kurento
  kurento:
    ws-uri: ws://localhost:8888/kurento

# 3. 构建并启动云雀
mvn clean package -DskipTests
java -jar target/skylark.jar

# 4. 访问演示页面
open http://localhost:8080/kurento-demo.html
```

### 9.3 API 端点一览

**通用端点**
- POST `/api/webrtc/{strategy}/session` — 创建会话
- DELETE `/api/webrtc/{strategy}/session/{id}` — 关闭会话

**Kurento 专用端点**
- POST `/api/webrtc/kurento/session/{id}/offer` — SDP Offer
- POST `/api/webrtc/kurento/session/{id}/ice-candidate` — ICE Candidate

**LiveKit 专用端点**
- POST `/api/webrtc/livekit/session` — 创建会话（返回 Token + URL）

---

## 十、后续规划与开源共建邀请

### 10.1 技术路线图

**2026 Q1** ✅ Kurento 6.18.0 集成（已完成）
**2026 Q1** ✅ LiveKit 0.12.0 集成（已完成）
**2026 Q1** ✅ 策略模式统一 WebRTC 通道（已完成）
→
**2026 Q2** 声网 Agora SDK 适配
**2026 Q3** 腾讯云 TRTC 适配
**2026 Q4** 阿里云 RTC / 网易云信适配
→
**2027** 多 Agent 协作、语音会议、实时翻译

### 10.2 开源共建：我们需要你！

云雀项目的成功离不开每一位开发者的参与。以下是你可以贡献的方向：

- ⭐ **Star 支持**（零门槛）：让更多人看到这个项目
- 📝 **文档翻译**（⭐）：将中文文档翻译为英文
- 🐛 **Bug 修复**（⭐⭐）：发现问题，提 Issue 或 PR
- 🧪 **测试用例**（⭐⭐）：提高测试覆盖率
- 🔧 **新增 RTC 适配**（⭐⭐⭐）：实现 AgoraChannelStrategy 等
- 🎙️ **新增 ASR/TTS 引擎**（⭐⭐⭐）：接入 Whisper、Azure 等
- 🌐 **前端 UI 优化**（⭐⭐）：美化演示页面

### 10.3 贡献者福利

- ✅ **贡献者墙**：你的头像将出现在项目首页
- ✅ **技术成长**：深入理解 WebRTC、AI 语音交互、DDD 架构
- ✅ **优先支持**：贡献者享有问题优先响应
- ✅ **项目影响力**：参与技术选型和架构讨论
- ✅ **简历加分**：真实的企业级开源项目经验

---

## 十一、总结

通过引入 **Kurento** 和 **LiveKit** 双 WebRTC 框架，云雀 Voice-Agent 实现了从"基础 WebSocket 语音传输"到"专业级 WebRTC 实时通信"的跨越式升级。

### 技术层面

- **标准化**：从自定义 WebSocket 升级到标准 WebRTC
- **高质量**：浏览器原生回声消除、降噪、NAT 穿透
- **可选择**：策略模式支持 WebSocket / Kurento / LiveKit 无缝切换
- **可扩展**：未来接入更多 RTC 方案只需实现一个接口
- **生产级**：健康检查、自动重连、会话管理等企业级特性

### 商业层面

- **低成本**：开源免费，小微企业零授权费
- **快速上线**：LiveKit Token 鉴权，分钟级接入
- **数据自主**：Kurento 全链路自托管，数据不出域
- **灵活选择**：按需选择轻量或重型方案
- **渐进升级**：从基础到高级，每一步都平滑过渡

> 🐦 **云雀（Skylark）** — 生于云端，鸣于指尖。让每一个小微企业，都能拥有自己的 AI 语音助手。

---

## 附录：如何参与贡献

### 🌟 为什么要参与云雀开源项目？

#### 对个人的价值
- 💡 **技术成长**：深入理解 AI 语音交互、WebRTC 实时通信、企业级 DDD 架构
- 📚 **经验积累**：参与真实的企业级项目，积累宝贵的开源协作经验
- 🎯 **职业发展**：开源贡献可以成为简历上的亮点，提升职业竞争力
- 🤝 **技术交流**：结识志同道合的技术伙伴，拓展人脉圈子

#### 对技术社区的意义
- 🌍 **推动创新**：共同推进 AI 语音交互技术的发展
- 🔓 **知识共享**：让更多人能够接触和学习先进的技术实践
- 🚀 **降低门槛**：让小微企业也能构建专业的 AI 语音交互系统

### 🎯 参与方式

1️⃣ ⭐ **Star 支持** — [立即 Star](https://github.com/Jashinck/Skylark)  
2️⃣ 📢 **分享推广** — 在技术社区分享你的使用体验  
3️⃣ 🐛 **报告问题** — [提交 Issue](https://github.com/Jashinck/Skylark/issues)  
4️⃣ 📝 **完善文档** — 改进 README、教程、API 文档  
5️⃣ 🔧 **贡献代码** — [提交 Pull Request](https://github.com/Jashinck/Skylark/pulls)  
6️⃣ 💬 **参与讨论** — [加入 Discussion](https://github.com/Jashinck/Skylark/discussions)

### 📮 联系我们

- **GitHub 项目**：https://github.com/Jashinck/Skylark
- **提交 Issue**：https://github.com/Jashinck/Skylark/issues
- **Pull Request**：https://github.com/Jashinck/Skylark/pulls
- **技术讨论**：https://github.com/Jashinck/Skylark/discussions

---

## 📣 加入我们，一起让 AI 语音交互触手可及！

无论你是：
- 🎓 **在校学生** — 通过开源项目提升实战能力
- 💼 **职场人士** — 在工作之余贡献自己的力量
- 🏢 **企业开发者** — 将企业最佳实践回馈社区
- 🌟 **技术爱好者** — 纯粹热爱技术，享受编程乐趣

我们都欢迎你的加入！

👉 **现在就访问**：https://github.com/Jashinck/Skylark  
👉 **立即 Star**：https://github.com/Jashinck/Skylark/stargazers  
👉 **参与贡献**：https://github.com/Jashinck/Skylark/contribute

---

### 🐦 云雀 (Skylark)

**生于云端，鸣于指尖**

*让智能语音交互触手可及*

⭐ Star | 🍴 Fork | 👀 Watch — [GitHub](https://github.com/Jashinck/Skylark)

**🌟 如果这篇文章对你有帮助，欢迎 Star 支持！**

[⭐ Star 项目](https://github.com/Jashinck/Skylark) · [📖 阅读文档](https://github.com/Jashinck/Skylark#readme) · [💬 参与讨论](https://github.com/Jashinck/Skylark/discussions) · [🐛 报告问题](https://github.com/Jashinck/Skylark/issues)

---

**原创技术分享 | 开源项目推广 | 转载请注明出处并附上项目链接**

📍 **项目地址**：https://github.com/Jashinck/Skylark

---

## 🏷️ 标签

`#AI` `#语音识别` `#语音合成` `#实时互动` `#开源项目` `#Java` `#SpringBoot` `#WebRTC` `#Kurento` `#LiveKit` `#VoiceAgent` `#VAD` `#ASR` `#TTS` `#LLM` `#RTC` `#策略模式` `#DDD` `#小微企业` `#智能客服`

---

> 💡 **温馨提示**：
> - 本文介绍的云雀（Skylark）项目完全开源免费，欢迎 Star、Fork 和贡献
> - 项目持续更新中，欢迎关注项目动态
> - 如有任何问题或建议，欢迎在 GitHub 上提 Issue 或参与讨论
> - 期待与你一起，用开源的力量，让 AI 语音交互触手可及！
