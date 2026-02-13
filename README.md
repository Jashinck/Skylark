<div align="center">

# 🐦 云雀 (Skylark)

### 生于云端，鸣于指尖

*Born in the Cloud, Singing at Your Fingertips*

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kurento](https://img.shields.io/badge/Kurento-6.18.0-blueviolet.svg)](https://kurento.openvidu.io/)

---

**云雀** 是一个基于 VAD、ASR、LLM、TTS、RTC 技术的智能语音交互代理系统。

**Skylark** is an intelligent Voice Agent system based on VAD, ASR, LLM, TTS, and RTC technologies.

</div>

---

## ✨ 项目特色 (Highlights)

🎯 **纯Java生态** - 全部使用Java实现，无需Python依赖  
🚀 **轻量部署** - 单一JAR包，一键启动  
🔧 **灵活配置** - 支持纯Java或混合模式部署  
🌐 **云原生友好** - 适配容器化和微服务架构  
🎙️ **WebRTC集成** - 实时语音通信，VAD→ASR→LLM→TTS完整编排  
📞 **Kurento 媒体服务** - 基于 Kurento Media Server 的专业 WebRTC 解决方案，提供服务端媒体处理、管道编排、会话管理与智能语音交互  

---

## 🎉 纯Java生态 (Pure Java Ecosystem)

本项目现已完全采用**纯Java实现**的Voice Agent系统！所有服务（ASR、TTS、VAD）都使用Java实现，无需Python依赖。

This project now uses a **pure Java implementation** of the Voice Agent system! All services (ASR, TTS, VAD) are implemented in Java, with no Python dependencies.

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
cd root
mvn clean package -DskipTests

# 2. 启动服务（使用纯Java配置）
java -jar target/skylark.jar config/config-java-only.yaml
```

### Docker部署

```bash
# 使用docker-compose启动服务
docker-compose up -d
```

### 技术栈 (Tech Stack)

- Spring Boot 3.2.0
- Spring Web (REST API)
- Spring WebFlux (异步HTTP客户��)
- Java 17
- **Vosk 0.3.45** - 离线语音识别
- **MaryTTS 5.2** - 文本转语音
- **ONNX Runtime 1.16.3** - Silero VAD 语音活动检测
- **Kurento Client 6.18.0** - WebRTC 媒体服务器客户端
- **kurento-utils (CDN)** - 前端 WebRTC Peer 管理

### 实现状态 (Implementation Status)

✅ **ASR (自动语音识别)** - 已集成 Vosk 离线语音识别  
⚠️ **TTS (文本转语音)** - 已准备 MaryTTS 集成（需手动安装）  
✅ **VAD (语音活动检测)** - 已集成 Silero VAD (ONNX Runtime)  
✅ **Kurento WebRTC** - 已集成 Kurento Media Server 实现 1v1 实时语音通话  

所有服务均使用纯 Java 实现，无需 Python 依赖。

详见: [开发指南](./JAVA_SERVICES_README.md)

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
✅ **LLM 智能对话** - 支持多种 LLM 后端  
✅ **TTS 语音合成** - 文本转语音输出  
✅ **完整测试覆盖** - 单元测试和集成测试

详细文档: [WebRTC 集成指南](./WEBRTC_GUIDE.md)

## 📞 Kurento 实时通话 (Kurento Real-time Voice Call)

云雀现已引入 **Kurento Media Server** 作为专业级 WebRTC 实时通话方案，实现用户与智能机器人的 1v1 实时语音交互。

Skylark now integrates **Kurento Media Server** as a professional WebRTC solution for 1v1 voice interaction between users and the intelligent robot.

### 核心特性

🎬 **服务端媒体处理** - 在服务端进行音频流处理，而非客户端  
🔄 **Media Pipeline 编排** - 灵活的媒体管道架构，支持复杂的音频处理流程  
🎙️ **WebRTC Endpoint 管理** - 专业的 WebRTC 端点创建、SDP 协商、ICE 处理  
🤖 **智能语音集成** - 无缝集成 VAD→ASR→LLM→TTS 完整管道  
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
WebRTCService ←→ VAD / ASR / LLM / TTS
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

详细文档: [Kurento 集成指南](./KURENTO_INTEGRATION.md)

## 📁 项目结构 (Project Structure)

### 企业级DDD分层架构 (Enterprise DDD Layered Architecture)

本项目采用标准的企业级SpringBoot DDD（领域驱动设计）分层架构：

```
skylark/
├── ./                        # Java服务
│   ├── src/main/java/org/skylark/
│   │   ├── api/                        # API接口层
│   │   │   └── controller/             # REST控制器
│   │   ├── application/                # 应用层
│   │   │   ├── dto/                    # 数据传输对象
│   │   │   └── service/                # 应用服务 (ASR, TTS, VAD, WebRTC)
│   │   ├── domain/                     # 领域层
│   │   │   ├── model/                  # 领域模型 (Dialogue, Message)
│   │   │   └── service/                # 领域服务接口
│   │   ├── infrastructure/             # 基础设施层
│   │   │   ├── adapter/                # 适配器 (ASR, TTS, VAD, LLM, WebRTC/Kurento)
│   │   │   └── config/                 # Spring配置
│   │   └── common/                     # 公共层
│   │       ├── constant/               # 常量定义
│   │       ├── exception/              # 异常处理
│   │       └── util/                   # 工具类
│   └── pom.xml
├── config/                              # 配置文件
│   ├── config-java-only.yaml          # 纯Java配置
│   └── config.yaml                     # 备用配置
├── web/                                 # Web前端
│   ├── js/kurento-webrtc.js           # Kurento WebRTC 客户端
│   ├── kurento-demo.html              # Kurento 演示页面
│   └── webrtc.html                    # WebRTC 交互页面
├── KURENTO_INTEGRATION.md              # Kurento 集成指南
├── WEBRTC_GUIDE.md                     # WebRTC 集成指南
└── docker-compose.yml                   # Docker编排
```

### 架构说明 (Architecture Description)

- **API层** (`api`): REST API接口，提供对外服务（包含 Kurento WebRTC 端点）
- **应用层** (`application`): 业务逻辑编排，服务组合（包含 WebRTCService）
- **领域层** (`domain`): 核心业务模型和规则
- **基础设施层** (`infrastructure`): 外部依赖适配，技术实现（包含 Kurento 适配器、WebRTCSession、AudioProcessor）
- **公共层** (`common`): 通用工具和组件

---

## 📜 开源协议 (License)

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

---

<div align="center">

**🐦 云雀 (Skylark)** - 生于云端，鸣于指尖

*让智能语音交互触手可及*

</div>
