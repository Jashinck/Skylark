# Java Service REST API Implementation

## 概述 (Overview)

本模块提供了Python服务的Java实现，以支持纯Java生态的Voice Agent系统。

This module provides Java implementations of the Python services to support a pure Java ecosystem for the Voice Agent system.

## 服务端点 (Service Endpoints)

### ASR Service (自动语音识别)

**基础URL:** `http://localhost:8080/asr`

#### 健康检查
```
GET /asr/health
```
响应: `{"status": "healthy", "service": "asr"}`

#### 语音识别
```
POST /asr/recognize
Content-Type: multipart/form-data

参数:
- file: 音频文件 (WAV格式)

响应:
{
  "text": "识别的文本",
  "language": "zh"
}
```

### TTS Service (文本转语音)

**基础URL:** `http://localhost:8080/tts`

#### 健康检查
```
GET /tts/health
```
响应: `{"status": "healthy", "service": "tts"}`

#### 语音合成
```
POST /tts
Content-Type: application/json

请求体:
{
  "text": "要合成的文本",
  "voice": "zh-CN-XiaoxiaoNeural" (可选)
}

响应: WAV音频文件
```

#### 语音列表
```
GET /tts/voices
```
响应:
```json
{
  "voices": [
    {
      "name": "zh-CN-XiaoxiaoNeural",
      "gender": "Female",
      "locale": "zh-CN"
    }
  ]
}
```

### VAD Service (语音活动检测)

**基础URL:** `http://localhost:8080/vad`

#### 健康检查
```
GET /vad/health
```
响应: `{"status": "healthy", "service": "vad"}`

#### 语音活动检测
```
POST /vad
Content-Type: application/json

请求体:
{
  "audio_data": "base64编码的音频数据",
  "session_id": "会话ID" (可选, 默认为"default")
}

响应:
{
  "status": "start" | "end" | null,
  "timestamp": 1234567890 (可选)
}
```

#### 重置会话状态
```
POST /vad/reset
Content-Type: application/json

请求体:
{
  "session_id": "会话ID" (可选)
}

响应: {"status": "reset"}
```

#### 清除所有会话
```
POST /vad/clear

响应: {"status": "cleared"}
```

## 实现说明 (Implementation Notes)

### 当前状态 (Current Status)

本实现提供了与Python服务完全兼容的REST API接口。目前的实现包含占位符代码，需要集成实际的Java机器学习库。

This implementation provides REST API interfaces fully compatible with the Python services. The current implementation contains placeholder code that needs to be integrated with actual Java machine learning libraries.

### 建议的集成方案 (Recommended Integration)

#### ASR服务
- **Vosk**: 离线语音识别，支持多种语言
- **Google Cloud Speech-to-Text**: 高质量云服务
- **Azure Speech Services**: 微软语音服务
- **Whisper (ONNX Runtime)**: OpenAI的Whisper模型

#### TTS服务
- **MaryTTS**: Java原生TTS引擎
- **FreeTTS**: 轻量级Java语音合成
- **Google Cloud Text-to-Speech**: 高质量云服务
- **Azure Speech Services**: 微软语音服务
- **Amazon Polly**: AWS语音服务

#### VAD服务
- **WebRTC VAD (via JNI)**: 通过JNI调用WebRTC VAD
- **Silero VAD (via ONNX Runtime)**: 使用ONNX Runtime运行Silero VAD模型
- **Custom Energy-based VAD**: 自定义能量检测算法

### 配置 (Configuration)

在`application.properties`或`application.yml`中配置服务参数:

```yaml
asr:
  model:
    dir: "iic/SenseVoiceSmall"
  device: "cpu"
  temp:
    dir: "temp/asr"

tts:
  voice: "zh-CN-XiaoxiaoNeural"
  temp:
    dir: "temp/tts"

vad:
  sampling:
    rate: 16000
  threshold: 0.5
  min:
    silence:
      duration:
        ms: 500
```

## 部署 (Deployment)

### 方式1: 仅Java服务 (Pure Java)

修改`config/config.yaml`，将服务URL指向Java实现:

```yaml
asr:
  service_url: http://localhost:8080/asr/recognize

vad:
  service_url: http://localhost:8080/vad

tts:
  service_url: http://localhost:8080/tts
```

### 方式2: 混合模式 (Hybrid Mode)

保留现有的Python服务配置，继续使用独立的Python微服务。

## 开发指南 (Development Guide)

### 添加实际ASR实现

编辑`com.bailing.service.ASRService`中的`performRecognition`方法:

```java
private String performRecognition(File audioFile) {
    // 集成实际的ASR库
    // 例如: Vosk, Cloud API等
    return recognizedText;
}
```

### 添加实际TTS实现

编辑`com.bailing.service.TTSService`中的`performSynthesis`方法:

```java
private void performSynthesis(String text, String voice, File outputFile) throws Exception {
    // 集成实际的TTS库
    // 例如: MaryTTS, Cloud API等
}
```

### 添加实际VAD实现

编辑`com.bailing.service.VADService`中的`performDetection`方法:

```java
private String performDetection(byte[] audioBytes, VADState state) {
    // 集成实际的VAD库
    // 例如: WebRTC VAD, Silero VAD等
    return status;
}
```

## 测试 (Testing)

启动服务后，可以使用curl测试各个端点:

```bash
# 测试ASR健康检查
curl http://localhost:8080/asr/health

# 测试TTS健康检查
curl http://localhost:8080/tts/health

# 测试VAD健康检查
curl http://localhost:8080/vad/health

# 测试TTS合成
curl -X POST http://localhost:8080/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"你好世界"}' \
  --output test.wav
```

## 优势 (Advantages)

1. **纯Java生态**: 无需Python依赖，简化部署
2. **统一技术栈**: 所有服务使用相同的语言和框架
3. **更好的类型安全**: Java的静态类型系统提供更好的安全性
4. **易于维护**: 统一的代码库和构建流程
5. **云原生**: 更容易与Java生态的云服务集成

## 下一步 (Next Steps)

1. 选择并集成实际的ASR库
2. 选择并集成实际的TTS库
3. 选择并集成实际的VAD库
4. 添加单元测试和集成测试
5. 优化性能和资源使用
6. 添加监控和日志记录

## 模型下载和配置 (Model Download and Configuration)

### ASR - Vosk 模型

本实现使用 Vosk 进行离线语音识别。

**下载模型:**
1. 访问 https://alphacephei.com/vosk/models
2. 下载适合您语言的模型，推荐:
   - 中文: `vosk-model-small-cn-0.22` (小型模型, ~42MB)
   - 中文: `vosk-model-cn-0.22` (完整模型, ~1.3GB)
   - 英文: `vosk-model-small-en-us-0.15` (小型模型, ~40MB)
   - 英文: `vosk-model-en-us-0.22` (完整模型, ~1.8GB)

**安装步骤:**
```bash
# 创建模型目录
mkdir -p models

# 下载并解压中文小型模型 (示例)
cd models
wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
unzip vosk-model-small-cn-0.22.zip
cd ..
```

**配置:**
在 `config/config-java-only.yaml` 或 `application.properties` 中设置模型路径:
```yaml
asr:
  model_dir: models/vosk-model-small-cn-0.22
  sample_rate: 16000
```

### TTS - MaryTTS 声音包 (可选 / Optional)

本实现支持使用 MaryTTS 进行离线语音合成，但由于 MaryTTS 的依赖复杂性，默认已注释。

**可选安装 (如需使用):**

由于 MaryTTS 的某些依赖在 Maven Central 不可用，推荐使用以下替代方案：

1. **云服务API (推荐):**
   - Google Cloud Text-to-Speech: https://cloud.google.com/text-to-speech
   - Azure Speech Services: https://azure.microsoft.com/en-us/services/cognitive-services/text-to-speech/
   - AWS Polly: https://aws.amazon.com/polly/

2. **本地MaryTTS (高级用户):**
   如果需要使用 MaryTTS，需要手动编译和安装依赖：
   ```bash
   # 从源码编译 MaryTTS
   git clone https://github.com/marytts/marytts.git
   cd marytts
   ./gradlew build
   # 按照官方文档安装到本地 Maven 仓库
   ```

**当前行为:**
- TTS 服务会在启动时尝试加载 MaryTTS
- 如果 MaryTTS 不可用，会自动使用占位符模式
- 占位符模式生成静音 WAV 文件

**配置:**
```yaml
tts:
  voice: cmu-slt-hsmm
```

**注意:** 
- MaryTTS 主要支持英语和德语
- 对于中文 TTS，建议使用云服务 API (如 Azure Speech, Google Cloud TTS)
- 或者考虑其他支持中文的 TTS 库

### VAD - Silero VAD (ONNX)

本实现支持使用 Silero VAD ONNX 模型进行高精度语音活动检测。

**下载模型:**
```bash
# 创建模型目录
mkdir -p models

# 下载 Silero VAD ONNX 模型
cd models
wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
cd ..
```

**或者手动下载:**
1. 访问 https://github.com/snakers4/silero-vad
2. 下载 `silero_vad.onnx` 文件
3. 放置到 `models/silero_vad.onnx`

**配置:**
```yaml
vad:
  model_path: models/silero_vad.onnx
  threshold: 0.5
  sampling_rate: 16000
  min_silence_duration_ms: 500
```

**后备方案:**
如果 ONNX 模型不可用，系统会自动使用简单的能量检测算法作为后备方案。

### 快速安装脚本

创建 `download_models.sh` 脚本快速下载所有模型:

```bash
#!/bin/bash

echo "正在创建模型目录..."
mkdir -p models

echo "下载 Vosk ASR 模型..."
cd models
wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
unzip vosk-model-small-cn-0.22.zip
rm vosk-model-small-cn-0.22.zip

echo "下载 Silero VAD 模型..."
wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx

cd ..
echo "✅ 所有模型下载完成!"
echo "MaryTTS 将使用内置声音包，无需额外下载。"
```

### Maven 依赖

确保 `pom.xml` 包含以下依赖:

```xml
<!-- Vosk for ASR -->
<dependency>
    <groupId>com.alphacephei</groupId>
    <artifactId>vosk</artifactId>
    <version>0.3.45</version>
</dependency>

<!-- ONNX Runtime for VAD -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.0</version>
</dependency>

<!-- MaryTTS for TTS (可选，默认已注释) -->
<!-- 如需使用，请取消注释并解决依赖问题 -->
```

**注意:** MaryTTS 依赖默认已注释，因为部分依赖在 Maven Central 不可用。建议使用云服务 API 进行语音合成。

### 构建和运行

```bash
# 构建项目
cd java-service
mvn clean package

# 运行服务
java -jar target/bailing-java.jar
```

### 验证服务

```bash
# 检查服务健康状态
curl http://localhost:8080/asr/health
curl http://localhost:8080/tts/health
curl http://localhost:8080/vad/health

# 测试 TTS (文本转语音)
curl -X POST http://localhost:8080/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world"}' \
  --output test.wav

# 播放生成的音频
# Linux: aplay test.wav
# macOS: afplay test.wav
# Windows: start test.wav
```

### 故障排除

**Vosk 模型未找到:**
- 确保模型目录路径正确
- 检查模型文件是否完整解压
- 查看日志中的具体路径

**MaryTTS 初始化失败:**
- 确保依赖正确安装
- 检查 Java 版本 (需要 Java 17+)
- 查看详细错误日志

**ONNX Runtime 错误:**
- 确保 ONNX 模型文件存在
- 检查文件权限
- 如果失败，系统会自动使用能量检测作为后备

### 性能优化

1. **模型预加载:** 所有模型在服务启动时加载，避免首次请求延迟
2. **线程安全:** 所有服务实现都是线程安全的，支持并发请求
3. **资源管理:** 使用 `@PreDestroy` 确保资源正确释放
4. **错误处理:** 完善的异常处理和日志记录
