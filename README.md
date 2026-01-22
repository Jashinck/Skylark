# voice-agent
Voice Agent : Based on VAD, ASR, LLM, TTS, RTC

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

#### 2. 构建和启动 (Build and Run)

```bash
# 1. 构建Java服务
cd java-service
mvn clean package -DskipTests

# 2. 启动服务（使用纯Java配置）
java -jar target/bailing-java.jar config/config-java-only.yaml
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

### 实现状态 (Implementation Status)

✅ **ASR (自动语音识别)** - 已集成 Vosk 离线语音识别  
⚠️ **TTS (文本转语音)** - 已准备 MaryTTS 集成（需手动安装）  
✅ **VAD (语音活动检测)** - 已集成 Silero VAD (ONNX Runtime)

所有服务均使用纯 Java 实现，无需 Python 依赖。

详见: [开发指南](java-service/JAVA_SERVICES_README.md)

## 项目结构 (Project Structure)

```
voice-agent/
├── java-service/          # Java服务
│   ├── src/main/java/com/bailing/
│   │   ├── service/       # 服务实现 (ASR, TTS, VAD)
│   │   ├── asr/          # ASR适配器
│   │   ├── tts/          # TTS适配器
│   │   ├── vad/          # VAD适配器
│   │   ├── config/       # Spring配置
│   │   └── ...
│   └── pom.xml
├── config/                # 配置文件
│   ├── config-java-only.yaml  # 纯Java配置
│   └── config.yaml            # 备用配置
└── docker-compose.yml     # Docker编排
```
