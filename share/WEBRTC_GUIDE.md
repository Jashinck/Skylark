# WebRTC 集成指南 (WebRTC Integration Guide)

## 概述 (Overview)

云雀 (Skylark) 现已集成多种 WebRTC 实时语音通信方案，支持完整的 VAD→ASR→LLM→TTS 编排流程。通过可插拔的 **WebRTCChannelStrategy** 策略模式，支持以下四种 WebRTC 方案：

| 策略 | 配置值 | 说明 |
|------|--------|------|
| WebSocket | `websocket` | 基于 WebSocket 的基础音频传输方案 |
| Kurento | `kurento` | 基于 Kurento Media Server 的专业媒体服务器方案 |
| LiveKit | `livekit` | 基于 LiveKit Server 的云原生实时通信方案 |
| Agora | `agora` | 基于声网 Agora Linux SDK 的 PAAS 商用级 RTC 方案 |

Skylark now integrates multiple WebRTC real-time voice communication solutions, supporting the complete VAD→ASR→LLM→TTS orchestration pipeline. Through the pluggable **WebRTCChannelStrategy** pattern, four WebRTC strategies are supported: WebSocket, Kurento, LiveKit, and Agora.

## 架构 (Architecture)

```
Web Browser (WebRTC Client)
        ↓
    WebSocket (/ws/webrtc)
        ↓
WebRTCSignalingHandler
        ↓
OrchestrationService
        ↓
VAD → ASR → LLM → TTS
```

## 核心组件 (Core Components)

### 1. WebSocketConfig
WebSocket 配置类，注册 WebRTC 信令端点。

WebSocket configuration class that registers WebRTC signaling endpoints.

**端点 (Endpoint)**: `/ws/webrtc`

### 2. WebRTCSignalingHandler  
处理 WebRTC 信令消息和音频流。

Handles WebRTC signaling messages and audio streams.

**支持的消息类型 (Supported Message Types)**:
- `offer`: WebRTC 连接请求 (WebRTC connection request)
- `answer`: WebRTC 连接应答 (WebRTC connection answer)
- `ice-candidate`: ICE 候选交换 (ICE candidate exchange)
- `text`: 文本输入 (Text input)
- 二进制消息: PCM 音频数据 (Binary message: PCM audio data)

### 3. OrchestrationService
编排服务，协调 VAD、ASR、LLM、TTS 各个组件。

Orchestration service that coordinates VAD, ASR, LLM, and TTS components.

**工作流程 (Workflow)**:
1. **语音输入 (Voice Input)**: 
   - 接收实时 PCM 音频流 (16kHz, 16-bit, mono)
   - VAD 检测语音活动
   - 语音结束时触发处理

2. **VAD (Voice Activity Detection)**:
   - 实时检测语音活动
   - 缓存语音片段
   - 识别语音结束点

3. **ASR (Automatic Speech Recognition)**:
   - 将语音转换为文本
   - 使用 Vosk 离线识别

4. **LLM (Large Language Model)**:
   - 生成智能响应
   - 支持流式输出

5. **TTS (Text-to-Speech)**:
   - 将响应转换为语音
   - 返回音频数据给客户端

## Web 客户端 (Web Client)

### 使用 webrtc.html

新的 WebRTC 客户端提供完整的实时语音交互界面。

The new WebRTC client provides a complete real-time voice interaction interface.

**功能特性 (Features)**:
- ✅ 实时语音通信 (Real-time voice communication)
- ✅ 音量指示器 (Volume indicator)  
- ✅ 文本输入支持 (Text input support)
- ✅ 消息历史显示 (Message history display)
- ✅ 自动重连 (Auto-reconnection)

**访问地址 (Access URL)**: 
```
http://localhost:8080/webrtc.html
```

### API 消息格式 (API Message Format)

#### 客户端→服务器 (Client → Server)

**文本消息 (Text Message)**:
```json
{
  "type": "text",
  "content": "你好"
}
```

**二进制消息 (Binary Message)**:
- 原始 PCM 音频数据 (Raw PCM audio data)
- 16kHz, 16-bit, mono

#### 服务器→客户端 (Server → Client)

**ASR 结果 (ASR Result)**:
```json
{
  "type": "asr_result",
  "data": {
    "text": "你好"
  }
}
```

**LLM 响应 (LLM Response)**:
```json
{
  "type": "llm_response",
  "data": {
    "text": "你好！我是云雀，有什么可以帮助你的吗？"
  }
}
```

**TTS 音频 (TTS Audio)**:
```json
{
  "type": "tts_audio",
  "data": {
    "audio": "base64-encoded-wav-data"
  }
}
```

**错误消息 (Error Message)**:
```json
{
  "type": "error",
  "data": {
    "message": "Error description"
  }
}
```

## 快速开始 (Quick Start)

### 1. 启动服务 (Start Server)

```bash
# 构建项目 (Build project)
mvn clean package -DskipTests

# 启动服务 (Start server)
java -jar target/skylark.jar
```

### 2. 访问 Web 界面 (Access Web Interface)

打开浏览器访问 (Open browser and visit):
```
http://localhost:8080/webrtc.html
```

### 3. 开始对话 (Start Conversation)

1. 点击"开始对话"按钮 (Click "Start Conversation" button)
2. 允许麦克风权限 (Allow microphone permission)
3. 开始语音交互 (Start voice interaction)

或者输入文本并点击"发送" (Or type text and click "Send")

## 配置 (Configuration)

### application.yaml

```yaml
server:
  port: 8080

# VAD 配置
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

# ASR 配置  
asr:
  model:
    path: models/vosk-model-small-cn-0.22
  temp:
    dir: temp/asr

# TTS 配置
tts:
  voice: cmu-slt-hsmm
  temp:
    dir: temp/tts
```

## 测试 (Testing)

### 运行单元测试 (Run Unit Tests)

```bash
# 运行所有测试 (Run all tests)
mvn test

# 运行 WebRTC 相关测试 (Run WebRTC related tests)
mvn test -Dtest=OrchestrationServiceTest
mvn test -Dtest=WebRTCSignalingHandlerTest
```

### 手动测试 (Manual Testing)

1. **文本输入测试 (Text Input Test)**:
   - 在文本框中输入消息
   - 点击"发送"按钮
   - 验证 LLM 响应和 TTS 音频播放

2. **语音输入测试 (Voice Input Test)**:
   - 点击"开始对话"
   - 对着麦克风说话
   - 验证 ASR 识别、LLM 响应和 TTS 音频

3. **连接测试 (Connection Test)**:
   - 检查状态指示器显示"已连接"
   - 验证自动重连功能

## 故障排除 (Troubleshooting)

### 无法连接服务器 (Cannot Connect to Server)

**问题 (Problem)**: WebSocket 连接失败

**解决方案 (Solution)**:
1. 确保服务器正在运行 (Ensure server is running)
2. 检查端口 8080 是否被占用 (Check if port 8080 is in use)
3. 验证防火墙设置 (Verify firewall settings)

### 麦克风无法访问 (Cannot Access Microphone)

**问题 (Problem)**: 浏览器无法获取麦克风权限

**解决方案 (Solution)**:
1. 使用 HTTPS 或 localhost (Use HTTPS or localhost)
2. 检查浏览器麦克风权限设置 (Check browser microphone permission settings)
3. 重新加载页面并允许权限 (Reload page and allow permission)

### 没有识别结果 (No Recognition Result)

**问题 (Problem)**: 语音输入后没有ASR结果

**解决方案 (Solution)**:
1. 确保 VAD 模型已正确加载 (Ensure VAD model is loaded correctly)
2. 检查 ASR 模型路径配置 (Check ASR model path configuration)
3. 验证音频格式 (16kHz, 16-bit, mono) (Verify audio format)
4. 查看服务器日志 (Check server logs)

### TTS 没有声音 (No TTS Audio)

**问题 (Problem)**: 没有听到 TTS 响应

**解决方案 (Solution)**:
1. 检查浏览器音量设置 (Check browser volume settings)
2. 验证 TTS 服务配置 (Verify TTS service configuration)
3. 查看浏览器控制台错误 (Check browser console for errors)

## 性能优化 (Performance Optimization)

### 1. 音频缓冲 (Audio Buffering)
- 使用适当的音频块大小 (4096 samples)
- 减少网络传输延迟

### 2. VAD 阈值调整 (VAD Threshold Tuning)
- 根据环境噪音调整阈值 (0.3 - 0.7)
- 优化语音检测准确性

### 3. 会话管理 (Session Management)
- 自动清理无效会话
- 内存使用优化

## 安全考虑 (Security Considerations)

1. **使用 HTTPS**: 在生产环境中使用 HTTPS 加密通信
2. **身份验证**: 添加用户身份验证机制
3. **速率限制**: 实施 API 速率限制
4. **数据隐私**: 不存储敏感语音数据

## 扩展开发 (Extension Development)

### 添加自定义处理 (Add Custom Processing)

在 OrchestrationService 中扩展处理逻辑:

```java
@Service
public class CustomOrchestrationService extends OrchestrationService {
    
    @Override
    protected void processCompleteSpeech(String sessionId, byte[] audioData, ResponseCallback callback) {
        // 添加自定义预处理
        // Add custom preprocessing
        
        super.processCompleteSpeech(sessionId, audioData, callback);
        
        // 添加自定义后处理
        // Add custom postprocessing
    }
}
```

### 集成其他 AI 服务 (Integrate Other AI Services)

实现新的适配器:

```java
@Component
public class CustomASRAdapter implements ASR {
    @Override
    public Map<String, String> recognize(byte[] audioData) throws Exception {
        // 实现自定义 ASR 逻辑
        // Implement custom ASR logic
    }
}
```

## API 参考 (API Reference)

### OrchestrationService

```java
public void processAudioStream(String sessionId, byte[] audioData, ResponseCallback callback)
public void processTextInput(String sessionId, String text, ResponseCallback callback)
public void cleanupSession(String sessionId)
```

### WebRTCSignalingHandler

```java
protected void handleTextMessage(WebSocketSession session, TextMessage message)
protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
public void afterConnectionEstablished(WebSocketSession session)
public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
```

## LiveKit 集成 (LiveKit Integration)

### 概述

LiveKit 是一个基于 Go 语言构建的云原生开源 WebRTC 媒体服务器，云雀通过 `LiveKitChannelStrategy` 实现了 LiveKit 的集成。

### 配置

```yaml
webrtc:
  strategy: livekit
  livekit:
    url: ws://localhost:7880
    api-key: your-api-key
    api-secret: your-api-secret
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/livekit/session` | 创建 LiveKit 会话（返回 Token + URL） |
| `DELETE` | `/api/webrtc/livekit/session/{id}` | 关闭会话，删除房间 |

### 客户端

访问 `http://localhost:8080/livekit-demo.html` 使用 LiveKit 演示页面。

客户端使用 `livekit-client 2.6.4` SDK，支持：
- 自动重连 (指数退避，最多 3 次重试)
- 回声消除、噪声抑制、自动增益控制
- 远程音频自动播放
- 连接状态回调

### 快速启动 LiveKit Server

```bash
docker run -d --name livekit \
  -p 7880:7880 -p 7881:7881 -p 7882:7882/udp \
  livekit/livekit-server --dev --bind 0.0.0.0
```

详细文档请参考：[LiveKit 官方文档](https://docs.livekit.io/) | [WebRTC 双框架技术博客](./WEBRTC_FRAMEWORKS_BLOG.md)

## 声网 Agora 集成 (Agora Integration)

### 概述

声网 Agora 是全球领先的 PAAS RTC 服务商，云雀通过 `AgoraChannelStrategy` 实现了 Agora Linux Server SDK 的集成，提供商用级实时音视频传输能力。

Agora is a leading global PAAS RTC provider. Skylark integrates Agora Linux Server SDK through `AgoraChannelStrategy`, providing enterprise-grade real-time audio/video communication.

### 配置

```yaml
webrtc:
  strategy: agora
  agora:
    app-id: "your-agora-app-id"
    app-certificate: "your-agora-app-certificate"
    region: "cn"              # 区域: cn, na, eu, as
    sample-rate: 16000
    channels: 1
    token-expire-seconds: 3600
```

### Native 库部署

Agora SDK 需要 native .so 文件：

```bash
# 放置 .so 文件
mkdir -p native/agora/linux/x86_64
cp /path/to/agora-sdk/*.so native/agora/linux/x86_64/

# 使用 start.sh 自动检测并加载
./start.sh local
```

> 若 native .so 不可用，适配器将优雅降级：Token 生成正常，频道操作变为 no-op。

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/webrtc/agora/session` | 创建 Agora 会话（返回 appId + channelName + Token + uid） |
| `DELETE` | `/api/webrtc/agora/session/{id}` | 关闭会话 |

### 核心特性

- **服务端音频处理闭环**：AI Agent 直接加入 Agora 频道，实时接收/发送音频
- **纯 Java Token 生成**：HMAC-SHA256 实现，无需额外 SDK
- **全球加速**：声网 SD-RTN™ 全球 250+ 数据中心
- **优雅降级**：无 native 库时自动降级，不影响开发调试

详细技术方案请参考：[Agora RTC 技术分享](./AGORA_RTC_TECH_SHARING.md) | [PAAS RTC 集成规范](./PAAS_RTC_INTEGRATION_SPEC.md)

## 策略切换 (Strategy Switching)

通过修改 `webrtc.strategy` 配置值，可在 WebSocket、Kurento、LiveKit、Agora 四种方案间自由切换：

```yaml
webrtc:
  strategy: livekit  # 可选: websocket, kurento, livekit, agora
```

## 贡献 (Contributing)

欢迎提交 Issue 和 Pull Request！

Welcome to submit Issues and Pull Requests!

## 许可证 (License)

Apache License 2.0

---

**🐦 云雀 (Skylark)** - 生于云端，鸣于指尖

*让智能语音交互触手可及*
