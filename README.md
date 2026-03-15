<div align="center">

# 🐦 云雀 (Skylark)

### 生于云端，鸣于指尖

*Born in the Cloud, Singing at Your Fingertips*

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kurento](https://img.shields.io/badge/Kurento-6.18.0-blueviolet.svg)](https://kurento.openvidu.io/)
[![LiveKit](https://img.shields.io/badge/LiveKit-0.12.0-ff69b4.svg)](https://livekit.io/)
[![Agora](https://img.shields.io/badge/Agora_RTC-4.4.31.4-00C853.svg)](https://www.agora.io/)
[![AgentScope](https://img.shields.io/badge/AgentScope-1.0.9-cyan.svg)](https://github.com/modelscope/agentscope)

---

**云雀** 是一个基于 VAD、ASR、LLM、TTS、RTC 技术的智能语音交互代理系统，深度集成 **AgentScope** AI Agent 框架，实现自主推理与工具调用。

**Skylark** is an intelligent Voice Agent system based on VAD, ASR, LLM, TTS, and RTC technologies, deeply integrating the **AgentScope** AI Agent framework for autonomous reasoning and tool invocation.

</div>

---

## ✨ 项目特色 (Highlights)

🎯 **纯Java生态** - 全部使用Java实现，无需Python依赖  
🚀 **轻量部署** - 单一JAR包，一键启动  
🔧 **灵活配置** - 支持纯Java或混合模式部署  
🌐 **云原生友好** - 适配容器化和微服务架构  
🤖 **AgentScope AI Agent** - 深度集成 AgentScope 框架，ReAct 推理、工具调用、Per-Session 记忆管理，赋予系统自主任务执行能力  
🎙️ **WebRTC集成** - 实时语音通信，VAD→ASR→AgentScope→TTS完整编排  
📞 **Kurento 媒体服务** - 基于 Kurento Media Server 的专业 WebRTC 解决方案，提供服务端媒体处理、管道编排、会话管理与智能语音交互  
☁️ **LiveKit 云原生** - 基于 LiveKit 的轻量级云原生 WebRTC 方案，Token 鉴权即用，Room 模型天然支持多人场景  
📡 **声网 Agora PAAS RTC** - 集成声网 Agora Linux Server SDK，提供全球级实时音视频传输、AI 降噪、弱网优化，一行配置即可启用商用级 PAAS RTC 能力  

---

## 🎉 纯Java生态 (Pure Java Ecosystem)

本项目现已完全采用**纯Java实现**的Voice Agent系统！所有服务（ASR、TTS、VAD）都使用Java实现，无需Python依赖。核心 AI 推理能力由 **AgentScope** 框架提供。

This project now uses a **pure Java implementation** of the Voice Agent system! All services (ASR, TTS, VAD) are implemented in Java, with no Python dependencies. Core AI reasoning is powered by the **AgentScope** framework.

### 架构特点 (Architecture Features)

- **统一技术栈**: 全部使用Java实现，无需Python环境
- **简化部署**: 单一Java服务，易于部署和维护
- **直接调用**: 适配器直接调用服务，无需HTTP开销
- **Spring集成**: 使用Spring Boot进行依赖注入和管理

### 快速开始 (Quick Start)

#### 1. 下载模型 (Download Models)

在启动服务前，需要下载以下模型：

**Vosk ASR 模型 (中文小型模型，~42MB):**
```bash
mkdir -p models
cd models
wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
unzip vosk-model-small-cn-0.22.zip
cd ..
```

**Silero VAD 模型:**
```bash
mkdir -p models
wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx -O models/silero_vad.onnx
```

**MaryTTS 语音:**
MaryTTS 5.2.1 在 Maven Central 有依赖解析问题。要使用 MaryTTS:
1. 从 https://github.com/marytts/marytts/releases 下载 marytts-builder-5.2.1.zip
2. 解压并将 JAR 添加到项目依赖
3. 取消 TTSService.java 中 MaryTTS 代码的注释

目前 TTS 服务使用占位符实现（生成静音 WAV 文件）。

#### 2. 启动 Kurento Media Server (Start Kurento Media Server)

Kurento 通话功能依赖独立运行的 Kurento Media Server，推荐使用 Docker 快速启动：

```bash
docker pull kurento/kurento-media-server:latest

docker run -d --name kms \
  -p 8888:8888 \
  -e KMS_MIN_PORT=40000 \
  -e KMS_MAX_PORT=57000 \
  -p 40000-57000:40000-57000/udp \
  kurento/kurento-media-server:latest
```

#### 3. 构建和启动 (Build and Run)

```bash
# 1. 构建Java服务
mvn clean package -DskipTests

# 2. 启动服务（使用纯Java配置）
java -jar target/skylark.jar
```

### Docker部署

```bash
# 使用docker-compose启动服务
docker-compose up -d
```

### 技术栈 (Tech Stack)

- Spring Boot 3.2.0
- Spring Web (REST API)
- Spring WebFlux (异步HTTP客户端)
- Java 17
- **Vosk 0.3.45** - 离线语音识别
- **MaryTTS 5.2** - 文本转语音
- **ONNX Runtime 1.16.3** - Silero VAD 语音活动检测
- **AgentScope 1.0.9** - AI Agent 框架（ReAct 推理、工具调用、记忆管理）
- **OpenAI Java SDK 0.18.0** - OpenAI 兼容 API 客户端（支持 DeepSeek、千问等）
- **Kurento Client 6.18.0** - WebRTC 媒体服务器客户端
- **LiveKit Server SDK 0.12.0** - 云原生 WebRTC 服务端 SDK
- **kurento-utils (CDN)** - 前端 WebRTC Peer 管理
- **livekit-client (CDN v2.6.4)** - 前端 LiveKit 客户端 SDK

### 实现状态 (Implementation Status)

✅ **ASR (自动语音识别)** - 已集成 Vosk 离线语音识别  
⚠️ **TTS (文本转语音)** - 已准备 MaryTTS 集成（需手动安装）  
✅ **VAD (语音活动检测)** - 已集成 Silero VAD (ONNX Runtime)  
✅ **Agent (AI 智能体)** - 已集成 AgentScope 框架，支持 ReAct 推理、工具调用、记忆管理  
✅ **Kurento WebRTC** - 已集成 Kurento Media Server 实现 1v1 实时语音通话  
✅ **LiveKit WebRTC** - 已集成 LiveKit 实现云原生 WebRTC 实时通话  

所有服务均使用纯 Java 实现，无需 Python 依赖。

详见: [开发指南](./share/JAVA_SERVICES_README.md) | [AgentScope 技术博客](./share/AGENTSCOPE_INTEGRATION_BLOG.md)

## 🤖 AgentScope AI Agent 能力 (AgentScope AI Agent Capabilities)

云雀已深度集成 **AgentScope 1.0.9** 框架（阿里巴巴通义实验室出品），提供生产级 AI Agent 能力。

Skylark deeply integrates **AgentScope 1.0.9** framework (by Alibaba Tongyi Lab), providing production-grade AI Agent capabilities.

### 语音交互完整流水线 (Full Voice Interaction Pipeline)

```
用户语音输入
     │
     ▼
  VADService                ← Silero VAD，语音活动检测
     │ 语音片段
     ▼
  ASRService                ← Vosk 离线识别，语音转文字
     │ 转录文本
     ▼
 AgentService               ← AgentScope 1.0.9
  ├── ReActAgent            ← ReAct 推理引擎（Reasoning + Acting）
  │     ├── 思考(Thought)   ← 分析意图，制定策略
  │     ├── 行动(Action)    ← 调用工具（@Tool 注解）
  │     └── 观察(Observe)   ← 整合工具结果，继续推理
  ├── OpenAIChatModel       ← DeepSeek / GPT-4o / 千问 等
  ├── InMemoryMemory        ← Per-Session 对话历史
  └── Toolkit               ← 可扩展工具注册表
     │ AI 回复文本
     ▼
  TTSService                ← MaryTTS，文本转语音
     │
     ▼
用户语音输出
```

### 核心能力 (Core Capabilities)

🧠 **ReAct 推理** - Reasoning + Acting 自主任务执行，支持多步骤推理  
🛠️ **工具调用** - 基于 @Tool 注解的插件式工具生态，自动选择工具  
💾 **记忆管理** - Per-Session InMemoryMemory，自动维护对话历史  
🌐 **多模型支持** - OpenAI 兼容 API，支持 DeepSeek、GPT-4o、千问、智谱等  

### 快速开始 (Quick Start)

```bash
# 1. 配置 DeepSeek API Key
export DEEPSEEK_API_KEY=your_api_key_here

# 2. 启动服务
mvn spring-boot:run

# 3. 测试 Agent 对话
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "test-001", "userText": "你好"}'
```

### 技术特性 (Technical Features)

- ✅ **ReActAgent** - 标准 ReAct (Reasoning + Acting) 推理引擎
- ✅ **Toolkit** - 注解式工具注册，5 行代码添加新能力
- ✅ **InMemoryMemory** - 自动对话历史管理
- ✅ **OpenAIChatModel** - 支持任意 OpenAI 兼容模型
- ✅ **Session 管理** - Per-Session Agent 实例，并发安全

### 扩展工具 (Extending Tools)

通过 `@Tool` 注解，5 行代码即可为 Agent 添加新能力：

```java
public class MyTools {
    @Tool(name = "query_order", description = "查询订单状态")
    public String queryOrder(
        @ToolParam(name = "orderId", description = "订单ID") String orderId
    ) {
        return "订单 " + orderId + " 状态：已发货";
    }
}

// 注册到 AgentService
agentService.registerToolObject(new MyTools());
```

详细文档: [AgentScope 集成技术博客](./share/AGENTSCOPE_INTEGRATION_BLOG.md)

## 🎙️ WebRTC 实时语音交互 (WebRTC Real-time Voice Interaction)

云雀现已集成 WebRTC 实时语音通信能力，支持完整的 VAD→ASR→LLM→TTS 编排流程。

Skylark now integrates WebRTC real-time voice communication with complete VAD→ASR→LLM→TTS orchestration.

### 快速开始 (Quick Start)

```bash
# 启动服务
java -jar target/skylark.jar

# 访问 WebRTC 界面
http://localhost:8080/webrtc.html
```

### 功能特性 (Features)

**基础 WebRTC 能力 (Basic WebRTC):**

✅ **实时语音通信** - WebRTC实现的信令与WebSocket音频传输

✅ **VAD 语音检测** - 自动识别语音活动并分段  

✅ **ASR 语音识别** - Vosk 离线语音识别  

✅ **LLM 智能对话** - AgentScope ReActAgent 驱动，支持多步推理与工具调用  

✅ **TTS 语音合成** - 文本转语音输出  

✅ **完整测试覆盖** - 单元测试和集成测试

详细文档: [WebRTC 集成指南](./share/WEBRTC_GUIDE.md)

## 📞 Kurento 实时通话 (Kurento Real-time Voice Call)

云雀现已引入 **Kurento Media Server** 作为专业级 WebRTC 实时通话方案，实现用户与智能机器人的 1v1 实时语音交互。

Skylark now integrates **Kurento Media Server** as a professional WebRTC solution for 1v1 voice interaction between users and the intelligent robot.

### 核心特性

🎬 **服务端媒体处理** - 在服务端进行音频流处理，而非客户端  
🔄 **Media Pipeline 编排** - 灵活的媒体管道架构，支持复杂的音频处理流程  
🎙️ **WebRTC Endpoint 管理** - 专业的 WebRTC 端点创建、SDP 协商、ICE 处理  
🤖 **智能语音集成** - 无缝集成 VAD→ASR→**AgentScope ReActAgent**→TTS 完整管道  
⚡ **实时音频流处理** - AudioProcessor 实时处理音频数据，低延迟语音检测和识别  
🔧 **健康检查与重连** - 自动健康监测，连接断开时自动重连  
📊 **会话管理** - 完整的会话生命周期管理（创建、协商、维持、关闭）

### 架构优势

相比基础 WebRTC 方案，Kurento 提供：
- **专业媒体服务器**: 使用成熟的 Kurento Media Server 处理 WebRTC 连接
- **服务端处理**: 音频流在服务端处理，降低客户端复杂度
- **可扩展架构**: Media Pipeline 支持添加录制、转码、混音等功能
- **企业级稳定性**: 健康检查、自动重连、会话管理等生产级特性

### 架构概览

```
Browser (kurento-webrtc.js)
    │ REST API
    ↓
RobotController (Kurento Endpoints)
    │
    ↓
WebRTCService ←→ VAD / ASR / AgentScope(ReActAgent) / TTS
    │
    ↓
KurentoClientAdapter → Kurento Media Server (ws://localhost:8888/kurento)
```

### 快速开始

```bash
# 1. 启动 Kurento Media Server (Docker)
docker run -d --name kms -p 8888:8888 \
  -e KMS_MIN_PORT=40000 -e KMS_MAX_PORT=57000 \
  -p 40000-57000:40000-57000/udp \
  kurento/kurento-media-server:latest

# 2. 启动 Skylark 服务
mvn spring-boot:run

# 3. 访问 Kurento 演示页面
http://localhost:8080/kurento-demo.html
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/kurento/session` | 创建 Kurento WebRTC 会话 |
| `POST` | `/api/webrtc/kurento/session/{id}/offer` | 处理 SDP Offer |
| `POST` | `/api/webrtc/kurento/session/{id}/ice-candidate` | 添加 ICE Candidate |
| `DELETE` | `/api/webrtc/kurento/session/{id}` | 关闭会话 |

### 配置

```yaml
kurento:
  ws:
    uri: ws://localhost:8888/kurento
webrtc:
  stun:
    server: stun:stun.l.google.com:19302
```

详细文档: [Kurento 集成指南](./share/KURENTO_INTEGRATION.md)

## 🚀 LiveKit 云原生通话 (LiveKit Cloud-Native Voice Call)

云雀现已引入 **LiveKit** 作为云原生 WebRTC 方案，提供轻量级的实时语音接入能力。

Skylark now integrates **LiveKit** as a cloud-native WebRTC solution with lightweight real-time voice access.

### 核心特性

☁️ **云原生架构** - Go 运行时，容器友好，资源占用极低  
🔑 **Token 鉴权** - JWT Token 即可完成接入，无需复杂 SDP/ICE 手动协商  
🏠 **Room 模型** - 基于房间的会话管理，天然支持多人场景  
🔄 **自动重连** - 客户端内建断线重连和连接质量监控  

### 快速开始

```bash
# 1. 启动 LiveKit Server (Docker)
docker run -d --name livekit \
  -p 7880:7880 -p 7881:7881 -p 7882:7882/udp \
  livekit/livekit-server --dev --bind 0.0.0.0

# 2. 修改 application.yaml 中 webrtc.strategy 为 livekit

# 3. 启动 Skylark 服务
mvn spring-boot:run

# 4. 访问 LiveKit 演示页面
http://localhost:8080/livekit-demo.html
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/livekit/session` | 创建 LiveKit 会话（返回 Token + URL） |
| `DELETE` | `/api/webrtc/livekit/session/{id}` | 关闭会话 |

详细文档: [WebRTC 双框架技术博客](./share/WEBRTC_FRAMEWORKS_BLOG.md)

## 📁 项目结构 (Project Structure)

### 企业级DDD分层架构 (Enterprise DDD Layered Architecture)

本项目采用标准的企业级SpringBoot DDD（领域驱动设计）分层架构：

```
skylark/
├── ./                        # Java服务
│   ├── src/main/java/org/skylark/
│   │   ├── api/                        # API接口层
│   │   │   └── controller/             # REST控制器 (RobotController)
│   │   ├── application/                # 应用层
│   │   │   ├── dto/                    # 数据传输对象
│   │   │   └── service/                # 应用服务
│   │   │       ├── AgentService.java   # AgentScope ReActAgent 封装（核心）
│   │   │       ├── OrchestrationService.java  # VAD→ASR→Agent→TTS 编排
│   │   │       ├── ASRService.java     # 语音识别 (Vosk)
│   │   │       ├── TTSService.java     # 语音合成 (MaryTTS)
│   │   │       ├── VADService.java     # 语音活动检测 (Silero VAD)
│   │   │       └── WebRTCService.java  # WebRTC 实时通信
│   │   ├── domain/                     # 领域层
│   │   │   ├── model/                  # 领域模型 (Dialogue, Message)
│   │   │   └── service/                # 领域服务接口
│   │   ├── infrastructure/             # 基础设施层
│   │   │   ├── adapter/                # 适配器 (ASR, TTS, VAD, LLM, WebRTC/Kurento/LiveKit)
│   │   │   └── config/                 # Spring配置
│   │   └── common/                     # 公共层
│   │       ├── constant/               # 常量定义
│   │       ├── exception/              # 异常处理
│   │       └── util/                   # 工具类
│   ├── src/main/resources/config/       # 配置文件
│   │   └── config.yaml                 # 配置文件
│   └── pom.xml
├── web/                                 # Web前端
│   ├── js/kurento-webrtc.js           # Kurento WebRTC 客户端
│   ├── js/livekit-webrtc.js           # LiveKit WebRTC 客户端
│   ├── kurento-demo.html              # Kurento 演示页面
│   ├── livekit-demo.html              # LiveKit 演示页面
│   └── webrtc.html                    # WebRTC 交互页面
├── share/                               # 技术文档
│   ├── AGENTSCOPE_INTEGRATION_BLOG.md  # AgentScope 集成技术博客
│   ├── ARCHITECTURE.md                 # 架构设计文档
│   ├── KURENTO_INTEGRATION.md          # Kurento 集成指南
│   ├── WEBRTC_FRAMEWORKS_BLOG.md       # WebRTC 双框架技术博客
│   ├── WEBRTC_GUIDE.md                 # WebRTC 集成指南
│   └── DEPLOYMENT_GUIDE.md            # 部署指南
└── docker-compose.yml                   # Docker编排
```

### 架构说明 (Architecture Description)

- **API层** (`api`): REST API接口，提供对外服务（包含 Kurento 和 LiveKit WebRTC 端点）
- **应用层** (`application`): 业务逻辑编排，服务组合（包含 **AgentService**、OrchestrationService、WebRTCService）
- **领域层** (`domain`): 核心业务模型和规则
- **基础设施层** (`infrastructure`): 外部依赖适配，技术实现（包含 Kurento/LiveKit 适配器、WebRTCSession、AudioProcessor、策略模式）
- **公共层** (`common`): 通用工具和组件

> 📌 **AgentService** 是应用层的核心：它封装 AgentScope 的 `ReActAgent`，为每个会话维护独立的 `InMemoryMemory`，并通过 `Toolkit` 支持动态工具扩展。`OrchestrationService` 将 VAD→ASR→AgentService→TTS 串联为完整语音流水线。

---

## 🚀 部署与 Agora RTC Native 库加载

### 推荐启动方式（Linux x86_64，直接 java -jar）

```bash
# 1. 构建 JAR
mvn clean package -DskipTests

# 2.（可选）放置 Agora Linux SDK 的 .so 文件
#    将 SDK 提供的所有 .so 拷贝到以下路径：
mkdir -p native/agora/linux/x86_64
cp /path/to/agora-sdk/*.so native/agora/linux/x86_64/

# 3. 启动服务
./start.sh local
```

`./start.sh local` 会自动：
- 检测 `native/agora/linux/x86_64/` 是否存在 `.so`
- 若存在：`export LD_LIBRARY_PATH=native/agora/linux/x86_64` + 添加 JVM 参数 `-Djava.library.path`
- 若不存在：打印提示并以降级模式启动（Agora RTC join/send/recv 为 no-op，Token 仍正常）

也可通过环境变量覆盖默认路径：

```bash
AGORA_NATIVE_DIR=/custom/path/to/so \
JAR_PATH=/custom/skylark.jar \
CONFIG_PATH=/custom/config.yaml \
./start.sh local
```

### Agora native .so 放置说明

详见 [native/README.md](native/README.md)，包含：
- 需要放置的文件清单
- 文件权限设置
- 依赖校验命令（`ldd`）
- 常见 `UnsatisfiedLinkError` 排查手册

### 排查 .so 加载问题

```bash
# 检查 .so 依赖是否齐全（有输出则说明缺库）
ldd native/agora/linux/x86_64/*.so | grep "not found"

# 打开动态链接器加载日志
LD_DEBUG=libs ./start.sh local 2>&1 | grep agora

# 确认系统架构
uname -m  # 应输出 x86_64
```

若仍报 `UnsatisfiedLinkError`，查看 Java 日志中的提示，Agora 适配器会优雅降级而非崩溃。

---

## 📜 开源协议 (License)

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

---

<div align="center">

**🐦 云雀 (Skylark)** - 生于云端，鸣于指尖

*让智能语音交互触手可及*

</div>
