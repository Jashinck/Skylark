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

本实现提供了与Python服务完全兼容的REST API接口，并已集成实际的Java机器学习库：

✅ **ASR服务** - 已集成 **Vosk** 离线语音识别  
✅ **TTS服务** - 已集成 **MaryTTS** 文本转语音  
✅ **VAD服务** - 已集成 **Silero VAD** (ONNX Runtime) 语音活动检测

所有服务均使用纯 Java 实现，无需 Python 依赖。

This implementation provides REST API interfaces fully compatible with the Python services and has integrated actual Java machine learning libraries:

✅ **ASR Service** - Integrated **Vosk** for offline speech recognition  
✅ **TTS Service** - Integrated **MaryTTS** for text-to-speech synthesis  
✅ **VAD Service** - Integrated **Silero VAD** (ONNX Runtime) for voice activity detection

All services are implemented in pure Java with no Python dependencies.

### 已集成的库 (Integrated Libraries)

#### ASR服务 (ASR Service)
- **Vosk 0.3.45**: 离线语音识别，支持中文和多种语言
- 使用 16kHz, 16-bit, mono WAV 格式音频
- 模型路径可配置: `asr.model.path`
- 如模型文件不存在，服务启动时会给出明确的下载提示

#### TTS服务 (TTS Service)
- **MaryTTS 5.2**: Java原生文本转语音引擎
- 支持多种语音和语言
- 默认语音可配置: `tts.voice`
- 首次启动时会自动下载所需的语音数据（需要网络连接）
- 提供 `/tts/voices` 端点列出所有可用语音

#### VAD服务 (VAD Service)
- **Silero VAD (ONNX Runtime 1.16.3)**: 高精度语音活动检测
- 使用预训练的 ONNX 模型
- 模型路径可配置: `vad.model.path`
- 支持会话状态管理 (线程安全)
- 包含能量检测作为后备方案

### 可选的集成方案 (Alternative Integration Options)

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

在`application.yaml`或命令行参数中配置服务参数:

```yaml
# ASR Configuration
asr:
  model:
    path: models/vosk-model-small-cn-0.22
  temp:
    dir: temp/asr

# TTS Configuration
tts:
  voice: cmu-slt-hsmm
  temp:
    dir: temp/tts

# VAD Configuration
vad:
  model:
    path: models/silero_vad.onnx
  sampling:
    rate: 16000
  threshold: 0.5
  min:
    silence:
      duration:
        ms: 500
```

### 模型文件要求 (Model File Requirements)

#### Vosk ASR 模型
- 需要预先下载并解压到 `models/vosk-model-small-cn-0.22/`
- 支持多种语言模型，详见: https://alphacephei.com/vosk/models
- 推荐中文模型: vosk-model-small-cn-0.22 (~42MB)

#### Silero VAD 模型
- 需要下载 ONNX 模型文件到 `models/silero_vad.onnx`
- 下载地址: https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx

#### MaryTTS 语音
- 首次运行时自动下载（需要网络连接）
- 可选安装中文语音包: `marytts-lang-zh`

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

### 服务已完成集成 (Services Integrated)

所有三个核心服务已完成实际ML库的集成：

1. ✅ **ASRService** - 使用 Vosk 进行语音识别
2. ✅ **TTSService** - 使用 MaryTTS 进行语音合成
3. ✅ **VADService** - 使用 Silero VAD (ONNX Runtime) 进行语音活动检测

### 扩展开发 (Extension Development)

如果需要集成其他ML库或云服务，可以：

#### 添加云服务ASR支持
编辑 `com.bailing.service.ASRService`，添加云服务提供商支持:
```java
// 例如: Google Cloud Speech, Azure Speech, AWS Transcribe
```

#### 添加其他TTS引擎
编辑 `com.bailing.service.TTSService`，支持更多TTS引擎:
```java
// 例如: Google Cloud TTS, Azure Speech, Amazon Polly
```

#### 优化VAD检测
编辑 `com.bailing.service.VADService`，优化检测算法:
```java
// 调整阈值、静音检测逻辑等
```

## 测试 (Testing)

### 前置准备
确保模型文件已下载到正确位置:
- `models/vosk-model-small-cn-0.22/` (Vosk ASR模型)
- `models/silero_vad.onnx` (Silero VAD模型)

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

# 列出可用语音
curl http://localhost:8080/tts/voices
```

## 优势 (Advantages)

1. **纯Java生态**: 无需Python依赖，简化部署
2. **统一技术栈**: 所有服务使用相同的语言和框架
3. **离线运行**: Vosk和Silero VAD支持完全离线运行
4. **更好的类型安全**: Java的静态类型系统提供更好的安全性
5. **易于维护**: 统一的代码库和构建流程
6. **云原生**: 更容易与Java生态的云服务集成
7. **高性能**: 直接内存操作和ONNX Runtime优化

## 故障排除 (Troubleshooting)

### 模型文件不存在错误
如果服务启动时报告模型文件不存在:
1. 检查模型文件路径配置是否正确
2. 按照README中的说明下载模型文件
3. 确保文件权限正确

### MaryTTS初始化失败
首次运行MaryTTS时需要网络连接下载语音数据:
1. 确保网络连接正常
2. 检查防火墙设置
3. 等待语音数据下载完成（可能需要几分钟）

### ONNX Runtime错误
如果VAD服务报告ONNX错误:
1. 确保 `silero_vad.onnx` 模型文件完整下载
2. 检查ONNX Runtime依赖是否正确安装
3. 查看详细错误日志

## 下一步 (Next Steps)

1. ✅ ~~选择并集成实际的ASR库~~ (已完成 - Vosk)
2. ✅ ~~选择并集成实际的TTS库~~ (已完成 - MaryTTS)
3. ✅ ~~选择并集成实际的VAD库~~ (已完成 - Silero VAD)
4. 添加单元测试和集成测试
5. 优化性能和资源使用
6. 添加监控和日志记录
7. 支持更多语言和模型
