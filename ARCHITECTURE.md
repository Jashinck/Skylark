# Skylark 架构文档 (Architecture Documentation)

## 概述 (Overview)

Skylark采用标准的企业级SpringBoot DDD（领域驱动设计）分层架构，实现了清晰的职责分离和高可维护性。

## 架构图 (Architecture Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│                      外部客户端 (Clients)                      │
│                   Web UI / Mobile Apps / APIs                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                    API层 (API Layer)                         │
│                 org.skylark.api.controller                   │
│              ┌──────────────────────────────┐                │
│              │  REST Controllers (未来添加)  │                │
│              │  - ASRController             │                │
│              │  - TTSController             │                │
│              │  - VADController             │                │
│              └──────────────────────────────┘                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                  应用层 (Application Layer)                   │
│              org.skylark.application.service                 │
│              org.skylark.application.dto                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ ASRService   │  │ TTSService   │  │ VADService   │      │
│  │ (语音识别)    │  │ (语音合成)    │  │ (语音检测)    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │                │
│         └─────────────────┴─────────────────┘                │
│                           │                                   │
└───────────────────────────┼───────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   领域层 (Domain Layer)                       │
│                 org.skylark.domain.model                     │
│                org.skylark.domain.service                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  领域模型 (Domain Models)                              │   │
│  │  - Dialogue (对话管理)                                 │   │
│  │  - Message  (消息实体)                                 │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┼───────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              基础设施层 (Infrastructure Layer)                │
│             org.skylark.infrastructure.adapter               │
│             org.skylark.infrastructure.config                │
│  ┌────────────────────────────────────────────────────┐     │
│  │  适配器 (Adapters)                                  │     │
│  │  - ASR Adapters (Direct/HTTP)                      │     │
│  │  - TTS Adapters (Direct/HTTP)                      │     │
│  │  - VAD Adapters (Direct/HTTP)                      │     │
│  │  - LLM Adapters (Ollama/OpenAI)                    │     │
│  │  - WebRTC Adapters (Kurento/LiveKit)               │     │
│  │  - WebRTC Strategy (WebSocket/Kurento/LiveKit)     │     │
│  └────────────────────────────────────────────────────┘     │
│  ┌────────────────────────────────────────────────────┐     │
│  │  配置 (Configuration)                               │     │
│  │  - ComponentFactoryConfig                          │     │
│  │  - WebRTCStrategyConfig                            │     │
│  │  - WebRTCProperties                                │     │
│  └────────────────────────────────────────────────────┘     │
└──────────────┬────────────────┬─────────────────┬───────────┘
               │                │                 │
               ↓                ↓                 ↓
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │    Vosk      │  │   MaryTTS    │  │ Silero VAD   │
      │   (ASR)      │  │   (TTS)      │  │   (VAD)      │
      └──────────────┘  └──────────────┘  └──────────────┘
               │                │
               ↓                ↓
      ┌──────────────┐  ┌──────────────┐
      │   Kurento    │  │   LiveKit    │
      │  Media Server│  │   Server     │
      └──────────────┘  └──────────────┘
```

## 分层说明 (Layer Description)

### 1. API层 (API Layer) - `org.skylark.api`

**职责**: 提供REST API接口，处理HTTP请求和响应

**组件**:
- `controller/`: REST控制器（当前为空，可扩展）
  - 未来可添加: `ASRController`, `TTSController`, `VADController`

**依赖**: 依赖应用层服务

**示例**:
```java
// 未来扩展示例
@RestController
@RequestMapping("/api/asr")
public class ASRController {
    @Autowired
    private ASRService asrService;
    
    @PostMapping("/recognize")
    public ResponseEntity<RecognitionResult> recognize(@RequestBody AudioData audio) {
        return ResponseEntity.ok(asrService.recognize(audio));
    }
}
```

### 2. 应用层 (Application Layer) - `org.skylark.application`

**职责**: 实现业务逻辑，协调领域对象和基础设施

**组件**:
- `service/`: 应用服务
  - `ASRService`: 自动语音识别服务（Vosk）
  - `TTSService`: 文本转语音服务（MaryTTS）
  - `VADService`: 语音活动检测服务（Silero VAD）
- `dto/`: 数据传输对象（当前为空，可扩展）

**特点**:
- 使用 `@Service` 注解
- 通过 `@Value` 注入配置
- 使用 `@PostConstruct` 初始化
- 调用基础设施层的适配器

### 3. 领域层 (Domain Layer) - `org.skylark.domain`

**职责**: 核心业务模型和业务规则

**组件**:
- `model/`: 领域模型
  - `Dialogue`: 对话管理（对话历史、持久化）
  - `Message`: 消息实体（角色、内容、时间戳）
- `service/`: 领域服务接口（当前为空，可扩展）

**特点**:
- 纯粹的业务逻辑
- 不依赖外部技术框架
- 可独立测试

### 4. 基础设施层 (Infrastructure Layer) - `org.skylark.infrastructure`

**职责**: 提供技术实现，对接外部系统

**组件**:
- `adapter/`: 适配器实现
  - `ASR`, `TTS`, `VAD`: 接口定义
  - `Direct*Adapter`: 直接调用实现
  - `Http*Adapter`: HTTP远程调用实现
  - `LLM`: 大语言模型接口
  - `OllamaLLM`, `OpenAILLM`: LLM实现
  - `webrtc/`: WebRTC 适配器
    - `KurentoClientAdapter`: Kurento 媒体服务器适配器
    - `LiveKitClientAdapter`: LiveKit 服务器适配器
    - `strategy/WebRTCChannelStrategy`: 可插拔 WebRTC 策略接口
    - `strategy/LiveKitChannelStrategy`: LiveKit 策略实现
    - `strategy/KurentoChannelStrategy`: Kurento 策略实现
    - `strategy/WebSocketChannelStrategy`: WebSocket 策略实现
- `config/`: 配置类
  - `ComponentFactoryConfig`: 组件工厂配置
  - `WebRTCStrategyConfig`: WebRTC 策略工厂配置
  - `WebRTCProperties`: WebRTC 配置属性 (Kurento/LiveKit/STUN/TURN)

**特点**:
- 实现技术细节
- 隔离外部依赖
- 可替换实现

### 5. 公共层 (Common Layer) - `org.skylark.common`

**职责**: 提供通用工具和组件

**组件**:
- `util/`: 工具类
  - `AudioUtils`: 音频处理工具
  - `ComponentFactory`: 组件工厂
  - `ConfigReader`: 配置读取器
- `constant/`: 常量定义（当前为空，可扩展）
- `exception/`: 异常定义（当前为空，可扩展）

## 设计模式 (Design Patterns)

### 1. 适配器模式 (Adapter Pattern)

用于统一不同的ASR/TTS/VAD实现：

```
ASR (接口)
  ├── DirectASRAdapter (直接调用)
  └── HttpASRAdapter (HTTP调用)
```

### 2. 策略模式 (Strategy Pattern)

用于可插拔的 WebRTC 通信方案切换：

```
WebRTCChannelStrategy (接口)
  ├── WebSocketChannelStrategy (WebSocket 方案)
  ├── KurentoChannelStrategy (Kurento 媒体服务器方案)
  └── LiveKitChannelStrategy (LiveKit 云原生方案)
```

通过 `webrtc.strategy` 配置项动态选择策略实现。

### 3. 工厂模式 (Factory Pattern)

`ComponentFactory` 负责根据配置动态创建组件。

### 4. 依赖注入 (Dependency Injection)

使用Spring的 `@Autowired` 实现松耦合。

## 依赖关系 (Dependencies)

```
API层 → 应用层 → 领域层
                ↓
           基础设施层
```

**依赖规则**:
- 上层可以依赖下层
- 下层不能依赖上层
- 基础设施层通过接口反向依赖领域层

## 扩展指南 (Extension Guide)

### 添加新的服务

1. 在 `application.service` 创建服务类
2. 在 `infrastructure.adapter` 创建适配器接口和实现
3. 在 `application.dto` 创建DTO（如需要）
4. 在 `api.controller` 创建控制器（如需要）

### 添加新的领域模型

1. 在 `domain.model` 创建领域实体
2. 在 `domain.service` 创建领域服务接口（如需要）
3. 在应用层使用领域模型

### 添加新的配置

1. 在 `infrastructure.config` 创建配置类
2. 使用 `@Configuration` 注解
3. 使用 `@Bean` 定义组件

## 技术栈 (Technology Stack)

- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **ASR**: Vosk 0.3.45
- **TTS**: MaryTTS 5.2 (可选)
- **VAD**: Silero VAD (ONNX Runtime 1.16.3)
- **LLM**: Ollama / OpenAI API
- **WebRTC**: Kurento Client 6.18.0 / LiveKit Server SDK 0.12.0
- **构建**: Maven

## 最佳实践 (Best Practices)

1. **单一职责**: 每个类只负责一个功能
2. **依赖倒置**: 依赖抽象而不是具体实现
3. **开闭原则**: 对扩展开放，对修改关闭
4. **接口隔离**: 使用细粒度的接口
5. **最少知识**: 减少类之间的耦合

## 相关文档 (Related Documentation)

- [README.md](README.md) - 项目介绍和快速开始
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - 部署指南
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - 项目总结

## 贡献指南 (Contributing)

遵循现有的架构模式和代码风格：
1. 新功能放在合适的层次
2. 使用依赖注入而不是硬编码
3. 编写单元测试
4. 更新相关文档

---

**更新时间**: 2026-02-15  
**架构版本**: 1.1.0 (DDD分层架构 + WebRTC 策略模式)
