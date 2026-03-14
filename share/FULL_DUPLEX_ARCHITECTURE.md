# Skylark 全双工语音交互架构设计方案
# Skylark Full-Duplex Voice Interaction Architecture Design

> **版本 / Version**: 1.0.0  
> **日期 / Date**: 2026-03-14  
> **作者 / Author**: Skylark Team  
> **状态 / Status**: 技术方案 / Technical Proposal  

---

## 目录 / Table of Contents

1. [背景与问题分析](#1-背景与问题分析)
2. [全双工核心能力定义](#2-全双工核心能力定义)
3. [开源技术选型](#3-开源技术选型)
4. [整体架构设计](#4-整体架构设计)
5. [全双工状态机设计](#5-全双工状态机设计)
6. [核心模块详细设计](#6-核心模块详细设计)
7. [多模态大模型集成方案](#7-多模态大模型集成方案)
8. [与 Skylark 现有架构的映射](#8-与-skylark-现有架构的映射)
9. [分阶段升级路线](#9-分阶段升级路线)
10. [性能指标与优化策略](#10-性能指标与优化策略)
11. [风险与挑战](#11-风险与挑战)
12. [附录：关键参考项目](#12-附录关键参考项目)

---

## 1. 背景与问题分析

### 1.1 当前级联模式架构

Skylark 当前采用经典的 **半双工级联流水线**（Half-Duplex Cascade Pipeline）：

```
用户语音输入 → VAD(Silero) → ASR(Vosk) → LLM(AgentScope/ReAct) → TTS(MaryTTS) → 语音输出
```

**当前代码实现路径**：

| 环节 | 实现类 | 工作模式 |
|------|--------|----------|
| 音频接入 | `WebRTCSignalingHandler.handleBinaryMessage()` | 接收 WebSocket 二进制帧 |
| 音频缓存 | `OrchestrationService.sessionBuffers` | ByteArrayOutputStream 累积 |
| 语音检测 | `VADService.detect()` → Silero ONNX / 能量检测 | 逐帧检测，等待静音 |
| 编排控制 | `OrchestrationService.processAudioStream()` | 等 VAD 返回 end 才触发后续 |
| 语音识别 | `ASRService.recognize()` → Vosk | **批量识别**，非流式 |
| 智能体 | `AgentService.chat()` → ReActAgent | **同步阻塞**，等完整回复 |
| 语音合成 | `TTSService.synthesize()` → MaryTTS | **整段生成**，非流式 |
| 音频回传 | `ResponseCallback.send("tts_audio", base64)` | 一次性 Base64 发送 |

### 1.2 半双工模式的核心限制

```
时间轴:
────────────────────────────────────────────────────────────────────────
用户说话 ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
VAD检测   ──────────▶end                                               
ASR识别              ████████                                          
LLM推理                      ████████████                              
TTS合成                                  ████████                      
系统播放                                         ████████████████      
用户等待  ░░░░░░░░░░░░░░░░████████████████████████████████████████░░░░░
```

**问题清单**：

| 问题 | 具体表现 | 代码定位 |
|------|----------|----------|
| **不可打断** | TTS 播放期间用户无法插话 | `OrchestrationService` 无打断机制 |
| **延迟高** | 必须等用户说完(500ms静音)才开始处理 | `VADService.minSilenceDurationMs=500` |
| **串行阻塞** | ASR→LLM→TTS 全链路串行 | `processCompleteSpeech()` 顺序执行 |
| **无流式处理** | 各环节均为批量处理 | ASR/LLM/TTS 全部一次性返回 |
| **单向监听** | 系统说话时不监听用户音频 | 无并行 VAD 监测 |

### 1.3 全双工的目标定义

**真正的全双工语音交互** = 以下能力的组合：

1. **同时说听（Simultaneous Speaking & Listening）**：系统在输出语音的同时持续监听用户输入
2. **随时打断（Barge-in）**：用户可在任意时刻打断系统发言，系统立即停止并响应
3. **流式处理（Streaming Pipeline）**：ASR/LLM/TTS 全链路流式，降低首字延迟
4. **自然轮转（Natural Turn-taking）**：无需严格的"你说完我再说"，支持重叠语音
5. **回声消除（AEC）**：区分用户语音与系统回放音，避免自激反馈

---

## 2. 全双工核心能力定义

### 2.1 能力分层模型

```
┌─────────────────────────────────────────────────────────────┐
│           Level 4: 自然对话 (Natural Conversation)            │
│  - 情感感知、语调理解、多人对话                                  │
│  - 需要：多模态大模型 (Audio-native LLM)                       │
├─────────────────────────────────────────────────────────────┤
│           Level 3: 全双工 (Full-Duplex)                       │
│  - 同时说听、重叠语音处理、自然轮转                               │
│  - 需要：双通道音频管理、回声消除、duplex状态机                     │
├─────────────────────────────────────────────────────────────┤
│           Level 2: 流式级联 (Streaming Cascade)               │
│  - 流式ASR→流式LLM→流式TTS、首字延迟<500ms                     │
│  - 需要：streaming ASR/LLM/TTS 组件                           │
├─────────────────────────────────────────────────────────────┤
│           Level 1: 可打断 (Barge-in)                          │
│  - TTS播放时可被用户打断、立即停止并切换                           │
│  - 需要：并行VAD监测、TTS中断控制                                │
├─────────────────────────────────────────────────────────────┤
│           Level 0: 半双工 (Half-Duplex) ← 当前状态              │
│  - 严格轮转、等说完再处理                                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 各层级关键技术需求

| 层级 | 关键技术 | 开源方案 |
|------|----------|----------|
| L1 打断 | 并行 VAD + TTS 中断 | Silero VAD（已有）+ 播放控制 |
| L2 流式 | Streaming ASR/LLM/TTS | Whisper Streaming / FunASR, OpenAI Streaming API, ChatTTS / CosyVoice |
| L3 全双工 | AEC + 双通道 + 状态机 | SpeexDSP AEC / WebRTC AEC3, Duplex State Machine |
| L4 自然对话 | Audio-native LLM | Moshi / GLM-4-Voice / Qwen2-Audio / Mini-Omni |

---

## 3. 开源技术选型

### 3.1 VAD（语音活动检测）

#### 现有能力：Silero VAD（ONNX Runtime）

当前 `VADService` 已集成 Silero VAD，支持 ONNX 推理和能量检测回退。**保留并增强**。

#### 增强方向

| 方案 | 说明 | 适用场景 |
|------|------|----------|
| **Silero VAD v5**（推荐升级） | 最新版本，支持 512/1024 采样窗口，准确率提升 | 实时流式 VAD |
| **WebRTC VAD** | Google WebRTC 内置 VAD，C++ 实现，极低延迟 | 作为前置快速过滤 |
| **双 VAD 策略** | WebRTC VAD（快速判定） + Silero VAD（精确确认） | 兼顾速度和精度 |

**全双工 VAD 增强要点**：
- 从当前"帧级检测 + 静音计数"升级为"**持续并行检测**"
- TTS 播放期间不停止 VAD，始终监听用户音频
- 引入 AEC 后的残留信号检测，区分用户语音与系统回声

### 3.2 流式 ASR（语音识别）

| 方案 | 语言 | 流式支持 | 中文能力 | 推荐度 |
|------|------|----------|----------|--------|
| **FunASR (Paraformer)** | Python/C++ | ✅ 实时流式 | ✅ 优秀 | ⭐⭐⭐⭐⭐ |
| **Whisper Streaming** | Python | ✅ LocalAgreement | ✅ 良好 | ⭐⭐⭐⭐ |
| **Vosk**（现有） | Java/Python | ⚠️ 部分流式 | ⚠️ 一般 | ⭐⭐⭐ |
| **SenseVoice** | Python | ✅ 流式 | ✅ 优秀 | ⭐⭐⭐⭐ |

**推荐方案：FunASR (阿里达摩院)**

```
选型理由：
1. 原生支持实时流式识别（Paraformer-large 实时版）
2. 中文识别准确率业界领先
3. 提供 WebSocket 服务端，可直接对接 Java 客户端
4. 支持热词、标点恢复、时间戳对齐
5. 部署资源需求合理（GPU 可选，CPU 可运行）
```

**集成方式**：通过 WebSocket 连接 FunASR Server，`ASRService` 升级为 `StreamingASRService`。

### 3.3 流式 TTS（语音合成）

| 方案 | 流式支持 | 中文能力 | 音色质量 | 推荐度 |
|------|----------|----------|----------|--------|
| **CosyVoice 2** | ✅ 流式分段 | ✅ 优秀 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **ChatTTS** | ✅ 流式 | ✅ 良好 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Edge-TTS** | ✅ 流式 | ✅ 良好 | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **MaryTTS**（现有） | ❌ 批量 | ❌ 无 | ⭐⭐ | ⭐⭐ |

**推荐方案：CosyVoice 2（阿里通义实验室）**

```
选型理由：
1. 支持流式合成（chunk-by-chunk 音频输出）
2. 中文发音自然度极高，支持多种音色
3. 支持零样本音色克隆
4. 延迟低（首包 < 150ms）
5. 可通过 gRPC/HTTP 接口集成
```

### 3.4 流式 LLM（大语言模型）

当前 `AgentService` 使用 `OpenAIChatModel` 调用 DeepSeek，**已支持 OpenAI-compatible Streaming API**，只需：

1. 启用 SSE (Server-Sent Events) 流式接口
2. 将 `chat()` 方法返回从 `String` 改为 `Flux<String>` / 回调式 token 流

### 3.5 多模态大模型（Audio-native LLM）

| 方案 | 能力 | 开源 | 成熟度 | 推荐度 |
|------|------|------|--------|--------|
| **Moshi (Kyutai)** | 全双工语音对话，直接音频输入/输出 | ✅ Apache-2.0 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **GLM-4-Voice** | 端到端语音对话，支持情感/语速控制 | ✅ 开源 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Qwen2-Audio** | 音频理解+生成，支持多语言 | ✅ 开源 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Mini-Omni 2** | 轻量级端到端语音模型 | ✅ MIT | ⭐⭐⭐ | ⭐⭐⭐ |
| **GPT-4o Realtime** | 全双工原生语音 | ❌ 闭源 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐（参考） |

**推荐方案：Moshi 作为长期目标 + 级联流式作为过渡**

```
Moshi 选型理由：
1. 业界首个开源全双工语音 LLM
2. 内置的 Inner Monologue 机制实现思考与说话分离
3. 原生音频 Codec（Mimi）实现端到端处理
4. 支持同时说话和倾听
5. Apache 2.0 许可证，可商用
```

### 3.6 回声消除（AEC）

| 方案 | 实现 | 适用场景 |
|------|------|----------|
| **WebRTC AEC3** | C++ (libwebrtc) | RTC 场景标配 |
| **SpeexDSP** | C | 轻量级，嵌入式友好 |
| **PAAS RTC 内置 AEC** | Agora/LiveKit SDK | 客户端侧，SDK 自带 |
| **3A 算法组合** | AEC + ANS + AGC | 完整的前处理链路 |

**推荐方案**：优先利用 **PAAS RTC SDK 内置 AEC**（Agora/LiveKit 客户端侧自带），服务端补充 SpeexDSP 进行二次处理。

---

## 4. 整体架构设计

### 4.1 全双工目标架构总览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Client (Web/Mobile/Device)                       │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │ Mic Capture  │  │ AEC (SDK内置) │  │  Speaker     │                   │
│  │ + WebRTC VAD │  │ + ANS + AGC  │  │  Playback    │                   │
│  └──────┬───────┘  └──────┬───────┘  └──────▲───────┘                   │
│         │                 │                 │                             │
│         └─────────┬───────┘                 │                             │
│                   │ RTC媒体流(上行)           │ RTC媒体流(下行)               │
└───────────────────┼─────────────────────────┼───────────────────────────┘
                    │                         │
              ┌─────┴─────────────────────────┴──────┐
              │       RTC Transport Layer             │
              │   (Agora / LiveKit / WebSocket)       │
              │   双向音频流持续传输                     │
              └─────┬─────────────────────────▲──────┘
                    │                         │
┌───────────────────┼─────────────────────────┼───────────────────────────┐
│                   │    Skylark Server        │                           │
│   ┌───────────────▼───────────────────┐     │                           │
│   │     Audio Input Pipeline           │     │                           │
│   │  ┌─────────┐  ┌────────────────┐  │     │                           │
│   │  │Server AEC│→│ Dual VAD Engine │  │     │                           │
│   │  │(SpeexDSP)│  │(WebRTC+Silero) │  │     │                           │
│   │  └─────────┘  └───────┬────────┘  │     │                           │
│   └───────────────────────┼───────────┘     │                           │
│                           │                 │                           │
│   ┌───────────────────────▼───────────┐     │                           │
│   │    Duplex State Machine            │     │                           │
│   │  (IDLE/LISTENING/PROCESSING/       │     │                           │
│   │   SPEAKING/INTERRUPTING)           │     │                           │
│   └───────┬──────────┬────────────────┘     │                           │
│           │          │                       │                           │
│   ┌───────▼──────┐   │   ┌─────────────────▼──┐                        │
│   │Streaming ASR │   │   │ Streaming TTS       │                        │
│   │  (FunASR)    │   │   │ (CosyVoice 2)       │                        │
│   └───────┬──────┘   │   └──────────▲──────────┘                        │
│           │          │              │                                    │
│   ┌───────▼──────────▼──────────────┴──┐                                │
│   │      Streaming LLM / Multimodal    │                                │
│   │  ┌────────────────────────────┐    │                                │
│   │  │ 级联模式: AgentScope ReAct  │    │                                │
│   │  │ + OpenAI Streaming API     │    │                                │
│   │  ├────────────────────────────┤    │                                │
│   │  │ 端到端模式: Moshi / GLM-4   │    │                                │
│   │  │ (直接 Audio→Audio)         │    │                                │
│   │  └────────────────────────────┘    │                                │
│   └────────────────────────────────────┘                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 双通道音频流管理

全双工的核心是**上行（用户→系统）和下行（系统→用户）音频流同时存在且互不干扰**：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Audio Stream Manager                          │
│                                                                  │
│  ┌─── 上行通道 (Uplink) ───────────────────────────────────┐    │
│  │                                                          │    │
│  │  RTC收音 → AEC处理 → VAD检测 → 流式ASR → 文本流          │    │
│  │                                                          │    │
│  │  ★ 始终活跃，不因系统发言而中断                            │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── 下行通道 (Downlink) ─────────────────────────────────┐    │
│  │                                                          │    │
│  │  文本流 → 流式TTS → PCM分片 → RTC播放                    │    │
│  │                                                          │    │
│  │  ★ 可被打断：收到 barge-in 信号后立即停止                   │    │
│  │  ★ 回声参考：将播放PCM送入 AEC 作为参考信号                 │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── 中断控制 (Interrupt Controller) ─────────────────────┐    │
│  │                                                          │    │
│  │  VAD持续监测上行音频                                      │    │
│  │  ├─ 检测到用户说话 + 当前正在播放 → 触发 BARGE-IN          │    │
│  │  ├─ 立即停止下行 TTS 播放                                 │    │
│  │  ├─ 取消进行中的 LLM/TTS 任务                             │    │
│  │  └─ 切换到 LISTENING 状态，开始新一轮处理                   │    │
│  └──────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 全双工状态机设计

### 5.1 状态定义

```
┌──────────────────────────────────────────────────────────────────────┐
│                      DuplexSessionState                               │
│                                                                       │
│  ┌──────────┐    user_speech_start     ┌───────────┐                 │
│  │          │ ──────────────────────▶  │           │                 │
│  │   IDLE   │                          │ LISTENING │                 │
│  │          │ ◀──────────────────────  │           │                 │
│  └──────────┘    silence_timeout       └─────┬─────┘                 │
│       ▲                                      │                       │
│       │                               user_speech_end                │
│       │                                      │                       │
│       │                                      ▼                       │
│       │                               ┌───────────┐                  │
│       │              timeout/error     │           │                  │
│       │ ◀───────────────────────────  │PROCESSING │                  │
│       │                               │           │                  │
│       │                               └─────┬─────┘                  │
│       │                                      │                       │
│       │                               first_tts_chunk                │
│       │                                      │                       │
│       │                                      ▼                       │
│       │                               ┌───────────┐                  │
│       │            tts_complete        │           │  user_speech     │
│       │ ◀───────────────────────────  │ SPEAKING  │ ──────────┐     │
│       │                               │           │            │     │
│       │                               └───────────┘            │     │
│       │                                                        ▼     │
│       │                               ┌──────────────┐              │
│       │            new_speech_ready    │              │              │
│       │ ◀──────────────────────────── │ INTERRUPTING │              │
│       │                               │  (Barge-in)  │              │
│       │                               └──────────────┘              │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.2 状态转换矩阵

| 当前状态 | 触发事件 | 目标状态 | 动作 |
|----------|----------|----------|------|
| `IDLE` | `user_speech_start` | `LISTENING` | 开始流式 ASR |
| `LISTENING` | `user_speech_end` | `PROCESSING` | 将 ASR 结果送入 LLM |
| `LISTENING` | `silence_timeout` | `IDLE` | 重置 ASR 缓存 |
| `PROCESSING` | `first_tts_chunk` | `SPEAKING` | 开始播放 TTS 音频 |
| `PROCESSING` | `timeout/error` | `IDLE` | 错误处理 |
| `SPEAKING` | `tts_complete` | `IDLE` | 播放完毕 |
| `SPEAKING` | `user_speech_start` | `INTERRUPTING` | 停止 TTS，取消 LLM |
| `INTERRUPTING` | `new_speech_ready` | `LISTENING` | 开始新一轮 ASR |

### 5.3 状态机伪代码

```java
/**
 * 全双工会话状态机
 * 替代当前 OrchestrationService 中的 sessionSpeaking Map
 */
public class DuplexSessionStateMachine {

    public enum State {
        IDLE,           // 空闲
        LISTENING,      // 正在听用户说话（流式 ASR 进行中）
        PROCESSING,     // ASR 完成，LLM 推理中
        SPEAKING,       // TTS 音频播放中（仍在监听 VAD）
        INTERRUPTING    // 用户打断，正在清理和切换
    }

    private State currentState = State.IDLE;
    private final StreamingASRSession asrSession;
    private final StreamingTTSSession ttsSession;
    private final StreamingLLMSession llmSession;

    /**
     * 处理 VAD 事件 —— 全双工核心：任何状态下都处理 VAD
     */
    public synchronized void onVADEvent(VADEvent event) {
        switch (currentState) {
            case IDLE:
                if (event == VADEvent.SPEECH_START) {
                    transitionTo(State.LISTENING);
                    asrSession.startStreaming();
                }
                break;

            case LISTENING:
                if (event == VADEvent.SPEECH_END) {
                    transitionTo(State.PROCESSING);
                    String transcript = asrSession.finalize();
                    llmSession.startStreaming(transcript);
                }
                break;

            case SPEAKING:
                if (event == VADEvent.SPEECH_START) {
                    // ★ 关键：用户打断
                    transitionTo(State.INTERRUPTING);
                    ttsSession.stopImmediately();    // 停止播放
                    llmSession.cancel();             // 取消 LLM
                    asrSession.startStreaming();      // 立即开始新识别
                    transitionTo(State.LISTENING);
                }
                break;

            case PROCESSING:
                if (event == VADEvent.SPEECH_START) {
                    // 用户在等待回复时又开始说话
                    transitionTo(State.INTERRUPTING);
                    llmSession.cancel();
                    asrSession.startStreaming();
                    transitionTo(State.LISTENING);
                }
                break;
        }
    }

    /**
     * LLM 流式输出回调 —— 逐句送入 TTS
     */
    public void onLLMToken(String token) {
        if (currentState == State.PROCESSING || currentState == State.SPEAKING) {
            ttsSession.feedText(token);  // 流式送文本给 TTS
            if (currentState == State.PROCESSING) {
                transitionTo(State.SPEAKING);
            }
        }
    }

    /**
     * TTS 播放完成回调
     */
    public void onTTSComplete() {
        if (currentState == State.SPEAKING) {
            transitionTo(State.IDLE);
        }
    }
}
```

---

## 6. 核心模块详细设计

### 6.1 增强型 VAD 模块

#### 6.1.1 双 VAD 引擎设计

```java
/**
 * DualVADEngine — 替代当前 VADService 的单一检测逻辑
 *
 * 采用两级 VAD 策略：
 * 1. 快速 VAD (WebRTC VAD): 极低延迟(< 1ms)，用于初步判定
 * 2. 精确 VAD (Silero VAD): 基于深度学习，用于确认判定
 *
 * 组合策略避免了误触发（用户咳嗽、背景噪音）和漏检
 */
public class DualVADEngine {

    private final WebRTCVAD quickVAD;       // 快速前置过滤
    private final SileroVAD preciseVAD;     // 精确确认（现有 VADService ONNX 部分）

    /**
     * 全双工 VAD 检测 —— 关键增强：
     * 1. 接收 AEC 处理后的音频（已消除回声）
     * 2. 始终运行，不因 SPEAKING 状态而暂停
     * 3. 返回带置信度的结果
     */
    public VADResult detect(float[] aecProcessedAudio) {
        // 第一级：WebRTC VAD 快速判定
        boolean quickResult = quickVAD.isSpeech(aecProcessedAudio);
        if (!quickResult) {
            return VADResult.silence();
        }

        // 第二级：Silero VAD 精确确认
        float probability = preciseVAD.getSpeechProbability(aecProcessedAudio);

        return new VADResult(
            probability > THRESHOLD,
            probability,
            System.currentTimeMillis()
        );
    }
}
```

#### 6.1.2 与现有 `VADService` 的映射

| 现有代码 | 全双工升级 |
|----------|-----------|
| `VADService.detect()` | 重构为 `DualVADEngine.detect()`，接收 AEC 后音频 |
| `VADService.performOnnxDetection()` | 保留为 `SileroVAD.getSpeechProbability()` |
| `VADService.performEnergyDetection()` | 替换为 `WebRTCVAD.isSpeech()` |
| `VADService.VADState` | 迁移到 `DuplexSessionStateMachine.State` |
| `VADService.sessionStates` | 由 `DuplexSessionManager` 统一管理 |

### 6.2 流式 ASR 模块

#### 6.2.1 StreamingASRService 设计

```java
/**
 * 流式 ASR 服务 —— 替代当前 ASRService 的批量识别
 *
 * 通过 WebSocket 连接 FunASR Server，实现边说边识别
 */
public class StreamingASRService {

    private final WebSocketClient funASRClient;

    /**
     * 流式识别接口（替代 ASRService.recognize()）
     *
     * @param audioChunkStream 持续的 PCM 音频块流
     * @param callback         识别结果回调（中间结果 + 最终结果）
     */
    public void recognizeStream(
            Flux<byte[]> audioChunkStream,
            ASRResultCallback callback) {

        // 将音频块持续发送到 FunASR WebSocket
        audioChunkStream.subscribe(chunk -> {
            funASRClient.sendBinary(chunk);
        });

        // 接收识别结果
        funASRClient.onMessage(message -> {
            ASRResult result = parseResult(message);
            if (result.isPartial()) {
                callback.onPartialResult(result.getText());  // 中间结果
            } else {
                callback.onFinalResult(result.getText());    // 最终结果
            }
        });
    }
}
```

#### 6.2.2 FunASR 集成架构

```
┌──────────────────┐    WebSocket    ┌─────────────────────────┐
│  Skylark Server  │ ◀────────────▶ │    FunASR Server         │
│                  │    (PCM流)      │                          │
│ StreamingASR     │    (JSON结果)   │  Paraformer-large 实时版  │
│ Service          │                 │  + 标点恢复               │
│                  │                 │  + 热词                   │
└──────────────────┘                 └─────────────────────────┘
                                      部署方式: Docker
                                      docker run -p 10095:10095 \
                                        registry.cn-hangzhou.aliyuncs.com/\
                                        funasr/funasr:latest
```

### 6.3 流式 LLM 模块

#### 6.3.1 StreamingLLMService 设计

```java
/**
 * 流式 LLM 服务 —— 增强当前 AgentService
 *
 * 在现有 AgentScope ReActAgent 基础上，增加流式输出能力
 * 通过 OpenAI-compatible Streaming API 实现逐 token 输出
 */
public class StreamingLLMService {

    private final AgentService agentService;  // 保留现有 Agent 能力

    /**
     * 流式对话（增强 AgentService.chat()）
     *
     * @param sessionId  会话 ID
     * @param text       用户文本
     * @param callback   Token 流回调
     * @return           可取消的 Future
     */
    public CompletableFuture<Void> chatStream(
            String sessionId,
            String text,
            TokenStreamCallback callback) {

        return CompletableFuture.runAsync(() -> {
            // 使用 OpenAI Streaming API
            // model.streamChat() → 逐 token 回调
            //
            // 分句策略：累积到句号/问号/感叹号时，
            // 将完整句子送入 Streaming TTS
            StringBuilder sentenceBuffer = new StringBuilder();

            streamTokens(sessionId, text, token -> {
                callback.onToken(token);
                sentenceBuffer.append(token);

                // 句子边界检测
                if (isSentenceEnd(sentenceBuffer.toString())) {
                    callback.onSentenceComplete(sentenceBuffer.toString());
                    sentenceBuffer.setLength(0);
                }
            });
        });
    }

    /**
     * 取消当前流式推理（barge-in 时调用）
     */
    public void cancelStream(String sessionId) {
        // 关闭 SSE 连接，中断推理
    }
}
```

### 6.4 流式 TTS 模块

#### 6.4.1 StreamingTTSService 设计

```java
/**
 * 流式 TTS 服务 —— 替代当前 TTSService 的整段生成
 *
 * 连接 CosyVoice 2 服务，实现边生成边播放
 */
public class StreamingTTSService {

    private final CosyVoiceClient cosyVoiceClient;

    /**
     * 流式合成（替代 TTSService.synthesize()）
     *
     * @param textStream  文本流（LLM 逐句输出）
     * @param callback    音频块回调
     * @return            可取消的合成会话
     */
    public StreamingTTSSession synthesizeStream(
            Flux<String> textStream,
            AudioChunkCallback callback) {

        StreamingTTSSession session = new StreamingTTSSession();

        textStream.subscribe(sentence -> {
            // 将每个完整句子送入 CosyVoice
            cosyVoiceClient.synthesize(sentence, audioChunk -> {
                if (!session.isCancelled()) {
                    callback.onAudioChunk(audioChunk);  // PCM 块 → RTC 发送
                }
            });
        });

        return session;
    }
}

/**
 * 可取消的 TTS 会话 —— 支持 barge-in
 */
public class StreamingTTSSession {
    private volatile boolean cancelled = false;

    public void stopImmediately() {
        this.cancelled = true;
        // 通知 CosyVoice 停止生成
        // 清空待发送音频队列
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
```

#### 6.4.2 CosyVoice 2 集成架构

```
┌──────────────────┐   gRPC/HTTP     ┌─────────────────────────┐
│  Skylark Server  │ ◀────────────▶ │   CosyVoice 2 Server     │
│                  │   (文本分句)     │                          │
│ StreamingTTS     │   (PCM音频块)   │  流式合成引擎              │
│ Service          │                 │  + 音色选择/克隆           │
│                  │                 │  + 韵律控制               │
└──────────────────┘                 └─────────────────────────┘
                                      部署方式: Docker
                                      模型: CosyVoice2-0.5B
                                      首包延迟: < 150ms
```

### 6.5 AEC（回声消除）模块

#### 6.5.1 服务端 AEC 设计

```java
/**
 * 服务端回声消除 —— 在 VAD 检测前处理音频
 *
 * 消除系统 TTS 播放音在用户麦克风中的回声，
 * 使 VAD 只检测到用户的真实语音
 */
public class ServerAECProcessor {

    /**
     * 处理回声消除
     *
     * @param micAudio   上行麦克风音频（可能包含回声）
     * @param refAudio   下行参考音频（TTS 正在播放的内容）
     * @return           消除回声后的纯净音频
     */
    public float[] process(float[] micAudio, float[] refAudio) {
        if (refAudio == null) {
            // 系统未在播放，无需 AEC
            return micAudio;
        }
        // SpeexDSP AEC 处理
        return speexAEC.cancelEcho(micAudio, refAudio);
    }
}
```

#### 6.5.2 AEC 在全双工中的位置

```
RTC 上行音频 ──┐
              ├──▶ AEC处理 ──▶ 纯净音频 ──▶ VAD检测 ──▶ ASR
RTC 下行音频 ──┘   (参考信号)
(TTS播放内容)
```

### 6.6 全双工编排服务

#### 6.6.1 DuplexOrchestrationService 设计

```java
/**
 * 全双工编排服务 —— 替代当前 OrchestrationService
 *
 * 核心变化：
 * 1. 用 DuplexSessionStateMachine 替代 sessionSpeaking Map
 * 2. 用流式组件替代批量组件
 * 3. 上行和下行通道并行工作
 * 4. 支持 barge-in 打断
 */
@Service
public class DuplexOrchestrationService {

    private final DualVADEngine vadEngine;
    private final StreamingASRService streamingASR;
    private final StreamingLLMService streamingLLM;
    private final StreamingTTSService streamingTTS;
    private final ServerAECProcessor aecProcessor;

    // 每个会话一个状态机（替代 sessionSpeaking + sessionBuffers）
    private final Map<String, DuplexSessionStateMachine> sessions
        = new ConcurrentHashMap<>();

    /**
     * 处理持续的音频流 —— 全双工核心入口
     *
     * 替代当前 OrchestrationService.processAudioStream()
     * 关键区别：任何时候都在处理音频，包括系统说话期间
     */
    public void processAudioFrame(String sessionId, byte[] audioFrame) {
        DuplexSessionStateMachine sm = sessions.computeIfAbsent(
            sessionId, k -> new DuplexSessionStateMachine(/*...*/));

        // Step 1: AEC 回声消除（如果系统正在播放 TTS）
        float[] cleanAudio = aecProcessor.process(
            toFloatArray(audioFrame),
            sm.getCurrentPlaybackReference()
        );

        // Step 2: VAD 检测（始终执行，不受状态限制）
        VADResult vadResult = vadEngine.detect(cleanAudio);

        // Step 3: 将 VAD 结果送入状态机
        if (vadResult.isSpeech()) {
            sm.onVADEvent(VADEvent.SPEECH_START);
        } else {
            sm.onVADEvent(VADEvent.SPEECH_CONTINUE_SILENCE);
        }

        // Step 4: 如果正在 LISTENING 状态，将音频送入流式 ASR
        if (sm.getState() == State.LISTENING) {
            streamingASR.feedAudioChunk(sessionId, cleanAudio);
        }
    }
}
```

---

## 7. 多模态大模型集成方案

### 7.1 级联模式 vs 端到端模式

```
┌────────────────────────────────────────────────────────────────────┐
│              方案 A: 增强级联模式 (Enhanced Cascade)                 │
│                                                                    │
│  音频 → VAD → ASR → [文本] → LLM → [文本] → TTS → 音频            │
│              (FunASR)   (AgentScope)   (CosyVoice)                 │
│                                                                    │
│  优点: 各模块可独立优化、可复用现有工具链、成熟稳定                     │
│  缺点: 级联延迟累积、文本中间态损失副语言信息                          │
├────────────────────────────────────────────────────────────────────┤
│              方案 B: 端到端模式 (End-to-End)                        │
│                                                                    │
│  音频 ──────────▶ Audio LLM ──────────▶ 音频                       │
│              (Moshi / GLM-4-Voice)                                  │
│                                                                    │
│  优点: 延迟最低、保留语调/情感、天然全双工                             │
│  缺点: 模型大、部署要求高、工具调用能力弱                              │
├────────────────────────────────────────────────────────────────────┤
│              方案 C: 混合模式 (Hybrid) ← 推荐                       │
│                                                                    │
│  音频 → VAD → ┬→ ASR → LLM(+Tools) → TTS    (复杂任务)             │
│               └→ Audio LLM                    (简单对话)             │
│                                                                    │
│  路由策略：简单闲聊走端到端，需要工具调用走级联                         │
│  优点: 兼顾延迟和能力                                               │
└────────────────────────────────────────────────────────────────────┘
```

### 7.2 Moshi 集成方案

#### 7.2.1 Moshi 架构概述

```
┌─────────────────────────────────────────────────────────────────┐
│                         Moshi Architecture                        │
│                                                                   │
│  ┌──────────┐     ┌──────────────────┐     ┌──────────────┐     │
│  │  Mimi    │     │    Helium LLM     │     │    Mimi      │     │
│  │ Encoder  │────▶│  (Dual-Stream)    │────▶│   Decoder    │     │
│  │(Audio→   │     │                   │     │ (Tokens→     │     │
│  │ Tokens)  │     │ Inner Monologue   │     │  Audio)      │     │
│  └──────────┘     │ (Text Stream)     │     └──────────────┘     │
│                   │                   │                           │
│                   │ Audio Output      │                           │
│                   │ (Audio Stream)    │                           │
│                   └──────────────────┘                            │
│                                                                   │
│  ★ 全双工实现原理：                                                 │
│    - Mimi Encoder 持续将用户音频编码为 token                        │
│    - Helium LLM 同时接收用户 token 和生成回复 token                 │
│    - Inner Monologue 用文本指导语音生成（可理解性保障）               │
│    - Mimi Decoder 将生成的 token 解码为音频                         │
│    - 用户和系统的音频流完全独立并行                                   │
└─────────────────────────────────────────────────────────────────┘
```

#### 7.2.2 Moshi 集成接口

```java
/**
 * Moshi 集成适配器 —— 端到端全双工语音模型
 *
 * 直接处理音频输入/输出，绕过传统 ASR/TTS 环节
 */
public class MoshiAdapter {

    /**
     * 建立全双工音频会话
     *
     * @param sessionId 会话标识
     * @param inputAudioStream  用户音频输入流
     * @param outputCallback    系统音频输出回调
     * @return 可取消的会话句柄
     */
    public MoshiSession createDuplexSession(
            String sessionId,
            Flux<byte[]> inputAudioStream,
            AudioChunkCallback outputCallback) {

        // 通过 WebSocket 连接 Moshi 推理服务
        // 上行：持续发送用户 PCM 音频
        // 下行：持续接收系统 PCM 音频
        // 天然全双工，无需状态机管理轮转
        return new MoshiSession(sessionId, inputAudioStream, outputCallback);
    }
}
```

### 7.3 GLM-4-Voice 集成方案

```java
/**
 * GLM-4-Voice 适配器
 *
 * 特点：支持情感控制、语速调节、中文效果好
 * 模式：流式端到端，可并行生成文本和音频
 */
public class GLM4VoiceAdapter {

    /**
     * 端到端语音对话
     * GLM-4-Voice 同时输出文本和音频 token，
     * 可用文本流驱动 UI 显示字幕
     */
    public void processAudio(
            String sessionId,
            byte[] inputAudio,
            DualOutputCallback callback) {

        // GLM-4-Voice 同时返回：
        // 1. 文本 token 流（用于字幕显示）
        // 2. 音频 token 流（用于语音合成）
        //
        // callback.onTextToken(text);
        // callback.onAudioChunk(audio);
    }
}
```

### 7.4 模型路由策略

```java
/**
 * 智能模型路由 —— 根据对话场景选择最佳模型
 */
public class ModelRouter {

    private final StreamingLLMService cascadeLLM;   // 级联模式（AgentScope）
    private final MoshiAdapter moshiAdapter;         // 端到端模式（Moshi）

    /**
     * 路由策略：
     * 1. 检测到需要工具调用 → 级联模式（AgentScope ReAct）
     * 2. 简单闲聊/情感交互 → 端到端模式（Moshi）
     * 3. 用户主动切换 → 按用户偏好
     */
    public ModelType route(String sessionId, String context) {
        if (requiresToolCalling(context)) {
            return ModelType.CASCADE;  // 使用 AgentScope ReAct + Streaming
        }
        return ModelType.END_TO_END;   // 使用 Moshi / GLM-4-Voice
    }
}
```

---

## 8. 与 Skylark 现有架构的映射

### 8.1 代码演进映射表

| 现有组件 | 文件路径 | 全双工升级 | 变化说明 |
|----------|----------|-----------|----------|
| `OrchestrationService` | `application/service/` | `DuplexOrchestrationService` | 重构为全双工编排，引入状态机 |
| `VADService` | `application/service/` | `DualVADEngine` | 增强为双 VAD 引擎 |
| `ASRService` | `application/service/` | `StreamingASRService` | 从批量改为 WebSocket 流式 |
| `TTSService` | `application/service/` | `StreamingTTSService` | 从整段生成改为流式分片 |
| `AgentService` | `application/service/` | `StreamingLLMService` | 增加流式 token 输出 |
| `WebRTCChannelStrategy` | `infrastructure/adapter/webrtc/strategy/` | 保持不变 | 复用现有策略模式 |
| `AgoraChannelStrategy` | `infrastructure/adapter/webrtc/strategy/` | 增强 `createSession()` | 注册双向持续音频回调 |
| `ResponseCallback` | `OrchestrationService` 内部接口 | `DuplexResponseCallback` | 支持流式音频块发送 |
| `sessionBuffers` (Map) | `OrchestrationService` | 移除 | 被流式 ASR 取代 |
| `sessionSpeaking` (Map) | `OrchestrationService` | 移除 | 被 `DuplexSessionStateMachine` 取代 |

### 8.2 新增组件清单

| 新增组件 | 包路径 | 职责 |
|----------|--------|------|
| `DuplexSessionStateMachine` | `application/service/duplex/` | 全双工会话状态管理 |
| `DuplexOrchestrationService` | `application/service/duplex/` | 全双工编排服务 |
| `DualVADEngine` | `application/service/duplex/` | 双 VAD 引擎 |
| `StreamingASRService` | `application/service/duplex/` | 流式 ASR 服务 |
| `StreamingTTSService` | `application/service/duplex/` | 流式 TTS 服务 |
| `StreamingLLMService` | `application/service/duplex/` | 流式 LLM 服务 |
| `ServerAECProcessor` | `application/service/duplex/` | 服务端回声消除 |
| `ModelRouter` | `application/service/duplex/` | 多模态模型路由 |
| `MoshiAdapter` | `infrastructure/adapter/multimodal/` | Moshi 全双工集成 |
| `GLM4VoiceAdapter` | `infrastructure/adapter/multimodal/` | GLM-4-Voice 集成 |
| `FunASRClient` | `infrastructure/adapter/asr/` | FunASR WebSocket 客户端 |
| `CosyVoiceClient` | `infrastructure/adapter/tts/` | CosyVoice gRPC 客户端 |

### 8.3 WebRTC 策略层的全双工适配

```java
/**
 * 全双工增强的 Agora 通道策略
 *
 * 现有 AgoraChannelStrategy.createSession() 注册音频回调时，
 * 需要改为双向持续模式
 */
public class DuplexAgoraChannelStrategy extends AgoraChannelStrategy {

    @Override
    public String createSession(String userId) {
        String sessionId = super.createSession(userId);

        // 增强：注册持续双向音频回调
        agoraClientAdapter.registerContinuousAudioCallback(
            sessionId,
            // 上行：用户音频持续送入全双工编排
            (audioFrame) -> duplexOrchestration.processAudioFrame(sessionId, audioFrame),
            // 下行：全双工编排持续输出音频
            () -> duplexOrchestration.getOutputAudioQueue(sessionId)
        );

        return sessionId;
    }
}
```

---

## 9. 分阶段升级路线

### Phase 1: 可打断能力（Barge-in）—— 2-3 周

**目标**：实现 Level 1，用户可在系统说话时打断

```
当前状态:
用户说话 ████████ → 系统处理 → 系统说话 ████████████
                                        (不可打断)

Phase 1 目标:
用户说话 ████████ → 系统处理 → 系统说话 ████░░░ STOP!
                                        用户打断 ████
```

**改造范围**：

| 改造项 | 说明 | 影响文件 |
|--------|------|----------|
| TTS 播放时持续 VAD | 系统说话期间不停止 VAD 检测 | `OrchestrationService` |
| TTS 中断机制 | 收到 barge-in 时立即停止音频发送 | `TTSService`, `ResponseCallback` |
| 会话状态枚举 | 引入 IDLE/LISTENING/SPEAKING 状态 | 新增 `SessionState` |
| LLM 取消机制 | barge-in 时取消进行中的 LLM 请求 | `AgentService` |

**代码改动示意**：

```java
// OrchestrationService 改动：在 processAudioStream 中增加打断检测
public void processAudioStream(String sessionId, byte[] audioData, ResponseCallback callback) {
    // ... 现有逻辑 ...

    // ★ 新增：如果当前正在播放 TTS 且检测到用户说话，触发打断
    SessionState state = sessionStates.get(sessionId);
    if (state == SessionState.SPEAKING && isSpeaking) {
        logger.info("Barge-in detected for session {}, stopping TTS", sessionId);
        callback.send(sessionId, "barge_in", Map.of("action", "stop_playback"));
        cancelCurrentProcessing(sessionId);  // 取消 LLM/TTS
        sessionStates.put(sessionId, SessionState.LISTENING);
    }
}
```

### Phase 2: 流式级联管线（Streaming Pipeline）—— 4-6 周

**目标**：实现 Level 2，全链路流式处理，首字延迟 < 500ms

**改造范围**：

| 改造项 | 说明 | 新增依赖 |
|--------|------|----------|
| 流式 ASR | 部署 FunASR，实现 WebSocket 对接 | FunASR Docker |
| 流式 LLM | AgentService 增加 SSE streaming | OpenAI Java SDK（已有） |
| 流式 TTS | 部署 CosyVoice 2，实现 gRPC 对接 | CosyVoice Docker |
| 分句策略 | LLM 输出按句号/逗号分段送 TTS | 新增 `SentenceSplitter` |

**延迟对比**：

```
当前(批量):
  VAD等待(500ms) + ASR(800ms) + LLM(2000ms) + TTS(1000ms) = ~4300ms

Phase 2(流式):
  VAD(100ms) + 首个ASR结果(200ms) + LLM首token(300ms) + TTS首包(150ms) = ~750ms
```

### Phase 3: 全双工 + 多模态（Full-Duplex）—— 6-8 周

**目标**：实现 Level 3-4，真正的全双工对话

**改造范围**：

| 改造项 | 说明 |
|--------|------|
| 回声消除 | 集成 SpeexDSP / WebRTC AEC |
| 双通道管理 | 上行/下行独立管道 |
| 全双工状态机 | `DuplexSessionStateMachine` |
| Moshi 集成 | 端到端全双工语音模型 |
| 模型路由 | 级联/端到端智能切换 |
| 全双工编排 | `DuplexOrchestrationService` |

### 升级路线图

```
2026 Q1-Q2 升级路线图
═══════════════════════════════════════════════════════════════

Phase 1 (2-3周)     Phase 2 (4-6周)          Phase 3 (6-8周)
可打断能力           流式级联管线               全双工+多模态
────────────        ───────────────           ─────────────────
• 并行VAD监测        • FunASR流式ASR集成        • AEC回声消除
• TTS中断机制        • LLM流式输出              • 双通道音频管理
• 基础状态枚举        • CosyVoice流式TTS         • 全双工状态机
• LLM取消           • 分句策略                 • Moshi/GLM-4集成
                    • 首字延迟<500ms           • 模型路由
                                              • 自然轮转

当前                                                    目标
Level 0 ──────▶ Level 1 ──────▶ Level 2 ──────▶ Level 3/4
半双工            可打断           流式级联          全双工
```

---

## 10. 性能指标与优化策略

### 10.1 目标指标

| 指标 | 当前值 | Phase 1 | Phase 2 | Phase 3 |
|------|--------|---------|---------|---------|
| 首字延迟 (FRT) | ~4.3s | ~3.5s | <500ms | <300ms |
| 打断响应时间 | ∞ (不支持) | <300ms | <200ms | <100ms |
| 端到端延迟 | ~5s | ~4s | <1.5s | <800ms |
| 并发会话数 | ~50 | ~50 | ~100 | ~100 |
| VAD 检测延迟 | ~50ms | ~30ms | ~20ms | ~10ms |

### 10.2 关键优化策略

#### 10.2.1 LLM 预热 (Speculative Prefill)

```java
/**
 * 在 ASR 尚未完成时，利用已识别的部分文本预热 LLM
 * 减少 LLM 首 token 延迟
 */
public class SpeculativePrefill {
    public void onPartialASR(String partialText) {
        // 基于部分文本构建 prompt
        // 预热 KV-cache（如果 LLM 支持）
        llmService.warmUp(partialText);
    }
}
```

#### 10.2.2 音频缓冲策略

```java
/**
 * 自适应音频缓冲 —— 平衡延迟与音质
 *
 * 短缓冲 = 低延迟但可能卡顿
 * 长缓冲 = 高延迟但平滑播放
 */
public class AdaptiveAudioBuffer {
    private int bufferSizeMs = 100;  // 初始 100ms

    public void adjustBuffer(NetworkCondition condition) {
        if (condition.jitter > 50) {
            bufferSizeMs = Math.min(bufferSizeMs + 20, 300);
        } else {
            bufferSizeMs = Math.max(bufferSizeMs - 10, 60);
        }
    }
}
```

#### 10.2.3 TTS 预合成（Sentence Pipelining）

```
时间线(流式优化后):
─────────────────────────────────────────────────────
LLM输出:  [句1] ──── [句2] ──── [句3] ────
TTS合成:       [句1合成] [句2合成] [句3合成]
音频播放:           [播放句1] [播放句2] [播放句3]

LLM 输出句1时，TTS 立即开始合成句1；
LLM 输出句2时，TTS 已完成句1合成并开始播放，同时合成句2。
→ 流水线并行，延迟仅取决于首句。
```

---

## 11. 风险与挑战

### 11.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| AEC 效果不佳导致 VAD 误触发 | 频繁假打断 | 双 VAD 确认 + 能量阈值调整 |
| 流式 ASR 中间结果不稳定 | 发送错误文本到 LLM | 延迟确认策略（等待 200ms 稳定窗口） |
| LLM 流式输出断续 | TTS 播放卡顿 | 自适应缓冲 + 句级缓存 |
| Moshi 模型 GPU 显存需求大 | 部署成本高 | 使用量化版本 (INT8/INT4) |
| 多模态模型中文效果有限 | 中文场景体验差 | 级联模式作为回退方案 |

### 11.2 兼容性保障

```java
/**
 * 全双工特性开关 —— 保证向后兼容
 *
 * 配置项: duplex.mode = half | barge-in | streaming | full
 * 默认值: half（保持现有行为不变）
 */
@Configuration
public class DuplexConfig {

    @Value("${duplex.mode:half}")
    private String duplexMode;

    @Bean
    public OrchestrationService orchestrationService() {
        switch (duplexMode) {
            case "full":
                return new DuplexOrchestrationService(/*...*/);
            case "streaming":
                return new StreamingOrchestrationService(/*...*/);
            case "barge-in":
                return new BargeInOrchestrationService(/*...*/);
            case "half":
            default:
                return new OrchestrationService(/*...*/);  // 现有实现
        }
    }
}
```

---

## 12. 附录：关键参考项目

### 12.1 开源项目参考

| 项目 | 地址 | 贡献 |
|------|------|------|
| **Moshi** | github.com/kyutai-labs/moshi | 全双工语音 LLM |
| **FunASR** | github.com/modelscope/FunASR | 流式 ASR |
| **CosyVoice** | github.com/FunAudioLLM/CosyVoice | 流式 TTS |
| **Silero VAD** | github.com/snakers4/silero-vad | VAD 模型 |
| **GLM-4-Voice** | github.com/THUDM/GLM-4-Voice | 端到端语音 LLM |
| **Qwen2-Audio** | github.com/QwenLM/Qwen2-Audio | 多模态音频理解 |
| **ChatTTS** | github.com/2noise/ChatTTS | 流式 TTS |
| **Mini-Omni** | github.com/gpt-omni/mini-omni | 轻量端到端模型 |
| **SpeexDSP** | github.com/xiph/speexdsp | 回声消除 |
| **WebRTC VAD** | webrtc.googlesource.com/src | 快速 VAD |
| **Open Voice Agent (LiveKit)** | github.com/livekit/agents | 语音 Agent 框架 |
| **Pipecat (Daily.co)** | github.com/pipecat-ai/pipecat | 语音 AI 管线 |

### 12.2 相关技术论文

| 论文 | 主题 | 关键贡献 |
|------|------|----------|
| Moshi (Kyutai, 2024) | 全双工语音 LLM | Inner Monologue, Mimi Codec |
| LSLM (2024) | 端到端全双工 | 自我监听机制 |
| Freeze-Omni (2024) | 低延迟端到端 | 冻结 LLM 的多模态训练策略 |
| Mini-Omni (2024) | 轻量端到端 | "Any Model Can Talk" 方法论 |
| SenseVoice (2024) | 多语言 ASR/AED | 高精度低延迟语音理解 |

### 12.3 部署参考

```yaml
# docker-compose.yml 全双工组件扩展

services:
  # 现有 Skylark 服务
  skylark:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DUPLEX_MODE=streaming
      - FUNASR_WS_URL=ws://funasr:10095
      - COSYVOICE_GRPC_URL=cosyvoice:50000

  # FunASR 流式 ASR 服务
  funasr:
    image: registry.cn-hangzhou.aliyuncs.com/funasr/funasr:latest
    ports:
      - "10095:10095"
    command: >
      funasr-wss-server
      --model-dir damo/speech_paraformer-large_asr_nat-zh-cn-16k-common-vocab8404-online
      --vad-dir damo/speech_fsmn_vad_zh-cn-16k-common-onnx
      --punc-dir damo/punc_ct-transformer_zh-cn-common-vocab272727-onnx

  # CosyVoice 流式 TTS 服务
  cosyvoice:
    image: cosyvoice:latest
    ports:
      - "50000:50000"
    deploy:
      resources:
        reservations:
          devices:
            - capabilities: [gpu]

  # Moshi 全双工推理服务（Phase 3）
  moshi:
    image: moshi-server:latest
    ports:
      - "8088:8088"
    deploy:
      resources:
        reservations:
          devices:
            - capabilities: [gpu]
```

---

> **文档结语**：本方案设计了从当前 Level 0（半双工级联）到 Level 3/4（全双工 + 多模态）的完整升级路径。通过分阶段实施（打断 → 流式 → 全双工），在保障现有功能稳定的前提下，逐步实现真正的全双工语音交互能力。推荐优先实施 Phase 1（可打断），以最小改动获得最大体验提升。
