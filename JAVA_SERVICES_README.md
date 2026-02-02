# Java Service Direct Adapter Implementation

## 概述 (Overview)

本模块提供了Python服务的Java实现，以支持纯Java生态的Voice Agent系统。与传统的REST API架构不同，本实现采用**Direct Adapter模式**，通过直接调用服务类来提供ASR、TTS和VAD功能，无需HTTP网络开销。

This module provides Java implementations of the Python services to support a pure Java ecosystem for the Voice Agent system. Unlike traditional REST API architecture, this implementation uses a **Direct Adapter pattern** that directly calls service classes for ASR, TTS, and VAD functionality, eliminating HTTP network overhead.

## 架构设计 (Architecture Design)

### Direct Adapter模式

本实现采用Direct Adapter模式，核心组件包括：

**Adapter层** (适配器层):
- `DirectASRAdapter` - ASR适配器，直接调用ASRService
- `DirectTTSAdapter` - TTS适配器，直接调用TTSService  
- `DirectVADAdapter` - VAD适配器，直接调用VADService

**Service层** (服务层):
- `ASRService` - 语音识别服务实现（基于Vosk）
- `TTSService` - 语音合成服务实现（基于MaryTTS，可选）
- `VADService` - 语音活动检测服务实现（基于Silero VAD）

**调用流程**:
```
应用程序 → Direct Adapter → Service → ML库 (Vosk/MaryTTS/Silero VAD)
```

### 与HTTP Adapter的区别

本项目的架构设计支持两种集成模式：

1. **Direct Adapter模式**（✅ 已实现，推荐）:
   - 直接在进程内调用服务
   - 零网络延迟
   - 更好的性能和资源利用
   - 适合单体应用架构
   - **当前默认使用此模式**

2. **HTTP Adapter模式**（⚠️ 未实现，需要开发）:
   - 通过HTTP请求调用远程服务
   - 支持分布式部署
   - 服务可独立扩展
   - 需要配置外部服务URL
   - **需要自行开发REST API服务器**

在`config/config.yaml`中可以选择使用哪种模式：

```yaml
# Direct Adapter模式（当前实现）
asr:
  adapter: DirectASRAdapter

# HTTP Adapter模式（需要开发REST服务）
asr:
  adapter: HttpASRAdapter
  service_url: http://localhost:8080/asr/recognize
```

## 实现说明 (Implementation Notes)

### 当前状态 (Current Status)

本实现采用Direct Adapter模式，通过Spring依赖注入直接调用服务类，已集成实际的Java机器学习库：

✅ **ASR服务** - 已集成 **Vosk** 离线语音识别  
⚠️ **TTS服务** - 已准备 **MaryTTS** 集成（需手动安装，见下文）  
✅ **VAD服务** - 已集成 **Silero VAD** (ONNX Runtime) 语音活动检测

所有服务均使用纯 Java 实现，无需 Python 依赖，无需REST API服务器。

This implementation uses the Direct Adapter pattern, calling service classes directly through Spring dependency injection, and has integrated actual Java machine learning libraries:

✅ **ASR Service** - Integrated **Vosk** for offline speech recognition  
⚠️ **TTS Service** - Ready for **MaryTTS** integration (requires manual setup, see below)  
✅ **VAD Service** - Integrated **Silero VAD** (ONNX Runtime) for voice activity detection

All services are implemented in pure Java with no Python dependencies and no REST API server required.

### 已集成的库 (Integrated Libraries)

#### ASR服务 (ASR Service)
- **Vosk 0.3.45**: 离线语音识别，支持中文和多种语言
- 使用 16kHz, 16-bit, mono WAV 格式音频
- 模型路径可配置: `asr.model.path`
- 如模型文件不存在，服务启动时会给出明确的下载提示

#### TTS服务 (TTS Service)
- **MaryTTS 5.2.1**: Java原生文本转语音引擎（需手动安装）
- ⚠️ **Maven依赖问题**: MaryTTS及其传递依赖在Maven Central不完整
- **手动安装步骤**:
  1. 下载: https://github.com/marytts/marytts/releases/download/v5.2.1/marytts-builder-5.2.1.zip
  2. 解压并将所有JAR添加到项目classpath或本地Maven仓库
  3. 取消TTSService.java中MaryTTS代码的注释
- **当前状态**: 使用占位符实现（生成静音WAV文件）
- **替代方案**: 可考虑使用云服务API（Google Cloud TTS、Azure Speech等）

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

在`config/config.yaml`中配置服务参数和适配器选择:

**Direct Adapter模式配置（推荐，当前实现）**:

```yaml
# ASR Configuration
asr:
  adapter: DirectASRAdapter  # 使用Direct Adapter模式
  model:
    dir: models/vosk-model-small-cn-0.22
  sample:
    rate: 16000
  temp:
    dir: temp/asr

# TTS Configuration
tts:
  adapter: DirectTTSAdapter  # 使用Direct Adapter模式
  voice: cmu-slt-hsmm
  temp:
    dir: temp/tts

# VAD Configuration
vad:
  adapter: DirectVADAdapter  # 使用Direct Adapter模式
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

**HTTP Adapter模式配置（需要自行开发REST服务）**:

```yaml
# ⚠️ 注意：HTTP Adapter需要独立的REST API服务器，当前未实现
# 参见"部署"章节的"方式2"了解如何开发REST服务
asr:
  adapter: HttpASRAdapter
  service_url: http://localhost:8080/asr/recognize

tts:
  adapter: HttpTTSAdapter
  service_url: http://localhost:8080/tts

vad:
  adapter: HttpVADAdapter
  service_url: http://localhost:8080/vad
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
- ⚠️ 由于Maven依赖问题，MaryTTS需要手动安装
- 下载地址: https://github.com/marytts/marytts/releases
- 安装后取消TTSService中相关代码的注释
- 当前使用占位符实现（生成静音WAV）

## 部署 (Deployment)

### 方式1: Direct Adapter模式（推荐）

使用Direct Adapter模式，服务直接集成在应用程序中：

修改`config/config.yaml`，配置Direct Adapter:

```yaml
asr:
  adapter: DirectASRAdapter
  model:
    dir: models/vosk-model-small-cn-0.22

vad:
  adapter: DirectVADAdapter
  model:
    path: models/silero_vad.onnx

tts:
  adapter: DirectTTSAdapter
  voice: cmu-slt-hsmm
```

启动应用程序:
```bash
cd root
mvn clean package
java -jar target/skylark.jar config/config.yaml
```

### 方式2: HTTP Adapter模式（未实现，需要开发）

**⚠️ 重要提示**: HTTP Adapter模式当前**未实现**。当前代码库仅包含Direct Adapter实现，没有REST API端点。

如果您需要分布式微服务架构，需要进行以下开发工作：

1. **开发REST API服务器**:
   - 选项A: 使用现有的Python服务（如果有）
   - 选项B: 在Java项目中添加REST Controller（`@RestController`）来暴露服务端点
   - 开发端点如 `/asr/recognize`, `/tts`, `/vad` 等

2. **配置HTTP Adapter**:
   在`config/config.yaml`中配置HTTP Adapter和服务URL:
   ```yaml
   asr:
     adapter: HttpASRAdapter
     service_url: http://localhost:8080/asr/recognize
   
   tts:
     adapter: HttpTTSAdapter
     service_url: http://localhost:8080/tts
   
   vad:
     adapter: HttpVADAdapter
     service_url: http://localhost:8080/vad
   ```

**推荐**: 除非您有明确的分布式部署需求，否则建议使用Direct Adapter模式，它更简单、性能更好。

## 开发指南 (Development Guide)

### 核心组件 (Core Components)

本实现包含以下核心组件：

#### 1. Service层（服务层）

**ASRService** (`org.skylark.service.ASRService`)
- 使用Vosk进行离线语音识别
- 支持WAV格式音频（16kHz, 16-bit, mono）
- 线程安全，可处理并发请求
- Spring `@Service`注解，自动注入

**TTSService** (`org.skylark.service.TTSService`)
- 使用MaryTTS进行语音合成（需手动安装）
- 支持多种语音和语言
- 占位符模式：当MaryTTS不可用时生成静音WAV

**VADService** (`org.skylark.service.VADService`)
- 使用Silero VAD (ONNX Runtime)进行语音活动检测
- 支持会话状态管理
- 后备方案：简单的能量检测算法

#### 2. Adapter层（适配器层）

**DirectASRAdapter** (`org.skylark.asr.DirectASRAdapter`)
- 实现`ASR`接口
- 通过依赖注入直接调用`ASRService`
- 零网络延迟

**DirectTTSAdapter** (`org.skylark.tts.DirectTTSAdapter`)
- 实现`TTS`接口
- 通过依赖注入直接调用`TTSService`

**DirectVADAdapter** (`org.skylark.vad.DirectVADAdapter`)
- 实现`VAD`接口
- 通过依赖注入直接调用`VADService`

#### 3. HTTP Adapter（可选）

如需远程服务调用，可使用HTTP Adapter:
- `HttpASRAdapter` - 通过HTTP调用远程ASR服务
- `HttpTTSAdapter` - 通过HTTP调用远程TTS服务
- `HttpVADAdapter` - 通过HTTP调用远程VAD服务

### 扩展开发 (Extension Development)

#### 添加新的适配器

如需添加其他适配器（如调用云服务API），可以：

1. 实现相应的接口（`ASR`, `TTS`, 或 `VAD`）
2. 在`ComponentFactory`中注册新的适配器类型
3. 在配置文件中指定新的适配器名称

示例：添加Google Cloud ASR支持
```java
public class GoogleCloudASRAdapter implements ASR {
    @Override
    public String recognize(byte[] audioData) throws Exception {
        // 调用Google Cloud Speech-to-Text API
    }
}
```

配置:
```yaml
asr:
  adapter: GoogleCloudASRAdapter
  api_key: your-api-key
```

## 测试 (Testing)

### 前置准备

1. 确保模型文件已下载到正确位置:
   - `models/vosk-model-small-cn-0.22/` (Vosk ASR模型)
   - `models/silero_vad.onnx` (Silero VAD模型)

2. 配置`config/config.yaml`使用Direct Adapter:
   ```yaml
   asr:
     adapter: DirectASRAdapter
   tts:
     adapter: DirectTTSAdapter
   vad:
     adapter: DirectVADAdapter
   ```

3. 构建并运行应用程序:
   ```bash
   cd root
   mvn clean package
   java -jar target/skylark.jar config/config.yaml
   ```

### 单元测试

可以编写单元测试来验证各个服务：

```java
@SpringBootTest
public class ASRServiceTest {
    
    @Autowired
    private ASRService asrService;
    
    @Test
    public void testRecognize() throws Exception {
        byte[] audioData = loadTestAudio("test.wav");
        Map<String, String> result = asrService.recognize(audioData);
        assertNotNull(result.get("text"));
    }
}
```

### 集成测试

Direct Adapter模式的集成测试更简单，因为不需要启动HTTP服务器：

```java
@SpringBootTest
public class DirectASRAdapterTest {
    
    @Autowired
    private DirectASRAdapter asrAdapter;
    
    @Test
    public void testRecognizeWithDirectAdapter() throws Exception {
        byte[] audioData = loadTestAudio("test.wav");
        String text = asrAdapter.recognize(audioData);
        assertNotNull(text);
    }
}
```

## 优势 (Advantages)

### Direct Adapter模式的优势

1. **零网络延迟**: 直接方法调用，无HTTP开销
2. **更好的性能**: 避免序列化/反序列化和网络传输
3. **简化部署**: 无需启动独立的REST服务器
4. **易于调试**: 直接的方法调用栈，易于跟踪和调试
5. **类型安全**: Java强类型系统，编译时检查
6. **资源共享**: 服务实例可以共享资源（如模型加载）

### 纯Java生态的优势

1. **统一技术栈**: 所有服务使用相同的语言和框架
2. **离线运行**: Vosk和Silero VAD支持完全离线运行
3. **更好的类型安全**: Java的静态类型系统提供更好的安全性
4. **易于维护**: 统一的代码库和构建流程
5. **高性能**: 直接内存操作和ONNX Runtime优化
6. **Spring生态**: 利用Spring Boot的依赖注入、配置管理等特性

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
4. 系统会自动降级到能量检测后备方案

### Direct Adapter初始化失败
如果Direct Adapter报告服务未初始化:
1. 确保Spring Boot应用正确启动
2. 检查`@Service`注解是否存在于服务类上
3. 确认`ComponentFactoryConfig`正确配置
4. 查看Spring启动日志中的错误信息

## 下一步 (Next Steps)

1. ✅ ~~选择并集成实际的ASR库~~ (已完成 - Vosk with DirectASRAdapter)
2. ⚠️ ~~选择并集成实际的TTS库~~ (MaryTTS已准备，需手动安装)
3. ✅ ~~选择并集成实际的VAD库~~ (已完成 - Silero VAD with DirectVADAdapter)
4. ✅ ~~实现Direct Adapter模式~~ (已完成 - DirectASRAdapter, DirectTTSAdapter, DirectVADAdapter)
5. 添加单元测试和集成测试
6. 优化性能和资源使用
7. 添加监控和日志记录
8. 支持更多语言和模型
9. 考虑云服务API作为TTS替代方案（Google Cloud TTS、Azure Speech等）
10. （可选）开发REST API端点以支持HTTP Adapter模式

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
在 `config/config.yaml` 中设置模型路径:
```yaml
asr:
  adapter: DirectASRAdapter
  model:
    dir: models/vosk-model-small-cn-0.22
  sample:
    rate: 16000
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
  adapter: DirectTTSAdapter
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
  adapter: DirectVADAdapter
  model:
    path: models/silero_vad.onnx
  threshold: 0.5
  sampling:
    rate: 16000
  min:
    silence:
      duration:
        ms: 500
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
cd root
mvn clean package

# 运行服务 (使用Direct Adapter模式)
java -jar target/skylark.jar config/config.yaml
```

### 验证服务

Direct Adapter模式中，服务在应用程序启动时自动初始化。查看日志验证服务状态：

```bash
# 查看应用程序日志
tail -f logs/application.log

# 期望看到的日志输出:
# ✅ Vosk ASR模型初始化成功
# ✅ Silero VAD模型加载成功
# ✅ Initialized DirectASRAdapter
# ✅ Initialized DirectTTSAdapter
# ✅ Initialized DirectVADAdapter
```

如需测试服务功能，可以编写单元测试或使用应用程序的主要功能（语音对话）。

**注意**: Direct Adapter模式不提供REST API端点，因此无法使用curl直接测试。如需HTTP接口，需要自行开发REST API服务（参见"方式2: HTTP Adapter模式"部分）。

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
