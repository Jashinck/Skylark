# 全双工 Voice Agent 技术研究报告
# Advanced Full-Duplex Voice Agent Technology Research Report

> **版本 / Version**: 1.0.0  
> **日期 / Date**: 2026-03-17  
> **作者 / Author**: Skylark Team  
> **状态 / Status**: 技术研究报告 / Technical Research Report  

---

## 📋 目录 / Table of Contents

1. [摘要](#1-摘要-abstract)
2. [背景与问题定义](#2-背景与问题定义-background--problem-definition)
3. [GitHub Top 10 开源 Voice Agent 项目调研](#3-github-top-10-开源-voice-agent-项目调研)
4. [全双工能力技术拆解](#4-全双工能力技术拆解-full-duplex-capability-analysis)
5. [具备全双工能力的项目深度对比](#5-具备全双工能力的项目深度对比-full-duplex-projects-comparison)
6. [通用全双工 Voice Agent 参考架构](#6-通用全双工-voice-agent-参考架构-reference-architecture)
7. [关键模块实现要点与伪代码](#7-关键模块实现要点与伪代码-key-module-implementation)
8. [技术选型建议](#8-技术选型建议-technology-selection-guide)
9. [风险与评估](#9-风险与评估-risks--evaluation)
10. [结论与建议](#10-结论与建议-conclusions--recommendations)
11. [参考链接](#11-参考链接-references)

---

## 1. 摘要 / Abstract

本报告系统梳理了截至 **2026 年 3 月** GitHub 上最受欢迎的 **Top 10 开源 Voice Agent 项目**，重点分析了全双工（Full-Duplex）语音交互的核心能力定义、关键技术组件及不同项目的实现差异，最终提出一套**通用的、可落地的全双工 Voice Agent 参考架构方案**。

**核心发现**：

- 在 Top 10 项目中，**6 个**具备不同程度的全双工能力，4 个仍为半双工模式。
- 全双工实现的核心差异在于：**VAD 精度**、**语义轮次检测（Turn Detection）**、**打断控制（Barge-in）** 三大能力的组合方式。
- Pipecat 和 LiveKit Agents 是当前**生产可用度最高**的全双工框架；Ultravox 以**音频原生 LLM** 实现了最低延迟（<300ms）。
- 一套完整的全双工架构需要包含：传输层（WebRTC/WS）、VAD、流式 STT、流式 LLM、流式 TTS、轮次检测器、打断控制器、上下文管理器八大核心组件。

---

## 2. 背景与问题定义 / Background & Problem Definition

### 2.1 语音 Agent 的交互范式演进

```
半双工 (Half-Duplex)          全双工 (Full-Duplex)
─────────────────────         ──────────────────────────
用户说话                       用户说话   ◄──── 同时 ────►   Agent 监听
    ↓ 等待静音 500ms              ↓                              ↓
Agent 处理                     Agent 回复  ◄──── 同时 ────►   用户可打断
    ↓
Agent 说话（用户等待）
    ↓
用户才能再说话
```

**核心差异**：半双工是严格的"你说我听、我说你等"的串行模式，类似对讲机；全双工则支持双方同时说话、随时打断，类似真实的面对面对话。

### 2.2 半双工模式的核心局限

| 问题 | 具体表现 | 用户感知 |
|------|----------|---------|
| 🚫 **不可打断** | TTS 播放期间用户无法插话 | "它说个不停，我插不上话" |
| ⏳ **端到端延迟高** | VAD→ASR→LLM→TTS 串行执行，E2E 延迟 3～10 秒 | "反应太慢了，不像在对话" |
| 🔇 **沉默等待长** | 必须检测到 500ms 静音才开始处理 | "我明明说完了，它还在等" |
| 🤖 **机械感强** | 严格轮替，无法实现自然对话的 overlap | "像跟对讲机说话" |

### 2.3 全双工的价值主张

全双工语音交互能够显著提升用户体验：
- **自然流畅**：对话节奏接近人类真实交流
- **高效率**：端到端延迟可降至 300～800ms
- **可控感强**：用户随时可以打断、纠正、补充
- **情感表达**：支持语气、情绪、重音等细节处理

---

## 3. GitHub Top 10 开源 Voice Agent 项目调研

> 数据截至 2026 年 3 月，Stars 数量为约数。

### 3.1 项目清单总览

| 排名 | 项目名称 | GitHub 地址 | ⭐ Stars（约） | 语言 | 核心定位 | 全双工 |
|:---:|---|---|:---:|:---:|---|:---:|
| 1 | **Pipecat** | [pipecat-ai/pipecat](https://github.com/pipecat-ai/pipecat) | ~10,700+ | Python | 实时语音/多模态对话框架 | ✅ |
| 2 | **LiveKit Agents** | [livekit/agents](https://github.com/livekit/agents) | ~9,700+ | Python/Go | 实时语音 AI Agent 框架（WebRTC） | ✅ |
| 3 | **Ultravox** | [fixie-ai/ultravox](https://github.com/fixie-ai/ultravox) | ~4,300+ | Python | 多模态语音 LLM（音频原生） | ✅ |
| 4 | **Vocode** | [vocodedev/vocode-core](https://github.com/vocodedev/vocode-core) | ~3,700+ | Python | 模块化语音 Agent 栈（电话/Web） | ✅ |
| 5 | **Bolna** | [bolna-ai/bolna](https://github.com/bolna-ai/bolna) | ~600+ | Python | 对话式语音 AI（电话自动化） | ⚠️ 部分 |
| 6 | **FireRedChat** | [FireRedTeam/FireRedChat](https://github.com/FireRedTeam/FireRedChat) | ~500+ | Python | 全双工自托管语音交互系统 | ✅ |
| 7 | **realtime-chatbot** | [AbrahamSanders/realtime-chatbot](https://github.com/AbrahamSanders/realtime-chatbot) | ~200+ | Python | 全双工开放域对话 Agent | ✅ |
| 8 | **Leon** | [leon-ai/leon](https://github.com/leon-ai/leon) | ~17,000+ | Node.js | 自托管个人语音助手 | ❌ |
| 9 | **AgenticSeek** | [Fosowl/agenticSeek](https://github.com/Fosowl/agenticSeek) | ~25,000+ | Python | 全本地自主语音 Agent | ❌ |
| 10 | **Project Alice** | [project-alice-assistant/ProjectAlice](https://github.com/project-alice-assistant/ProjectAlice) | ~700+ | Python | 模块化智能家居语音助手 | ❌ |

> **说明**：Leon 和 AgenticSeek 虽然 Stars 非常高，但偏向通用个人助手/自主 Agent，语音是其交互方式之一而非核心；按"Voice Agent 专业度"排序时整体靠后。

### 3.2 全双工能力分布

```
全双工项目（6个，60%）        非全双工项目（4个，40%）
─────────────────────         ──────────────────────────
✅ Pipecat                    ❌ Leon
✅ LiveKit Agents             ❌ AgenticSeek
✅ Ultravox                   ❌ Project Alice
✅ Vocode                     ⚠️ Bolna（部分支持）
✅ FireRedChat
✅ realtime-chatbot
```

---

## 4. 全双工能力技术拆解 / Full-Duplex Capability Analysis

### 4.1 全双工的定义

**全双工（Full-Duplex）语音交互** = 以下能力的完整组合：

| 编号 | 能力名称 | 英文术语 | 说明 |
|:---:|---|---|---|
| 1 | **同时说听** | Simultaneous Speaking & Listening | 系统输出语音的同时持续监听用户输入 |
| 2 | **随时打断** | Barge-in / Interruption | 用户可在 Agent 发言的任意时刻打断，系统立即停止并响应 |
| 3 | **流式处理** | Streaming Pipeline | STT/LLM/TTS 全链路流式处理，不等任一步骤完整完成 |
| 4 | **轮次检测** | Turn Detection / End-of-Turn (EoT) | 准确判断用户是否已说完，而非依赖固定静音超时 |
| 5 | **语义端点** | Semantic End-of-Utterance | 结合语义理解判断话语边界，而非仅靠 VAD |
| 6 | **上下文维护** | Context Management | 打断场景下准确记录双方实际说出的内容 |

### 4.2 关键技术组件

#### 语音活动检测（VAD）

VAD 是全双工的基础模块，负责实时判断用户是否在说话：

| VAD 方案 | 精度 | 延迟 | 使用项目 | 特点 |
|---|:---:|:---:|---|---|
| **Silero VAD** | 高（F1≈96%） | ~10ms | Pipecat, LiveKit | 轻量神经网络，精度高，开源 |
| **WebRTC VAD** | 中 | ~5ms | 浏览器原生 | 延迟极低，CPU 占用极低 |
| **个性化 pVAD** | 极高 | ~15ms | FireRedChat | 根据用户声纹定制，抗环境噪声 |
| **FireRedVAD** | 极高（F1=97.57%） | ~10ms | FireRedChat | 专为全双工优化，抗回声 |

#### 语义轮次检测（Turn Detector）

这是区分"全双工"与"半双工"的核心组件。传统方案仅靠 VAD 静音超时，会导致用户稍停顿就触发响应：

```
传统方案（纯 VAD 超时）：
用户说："我想买一张..."（停顿 0.5s）"飞机票"
                          ↑
                    Agent 抢话（错误触发）

全双工方案（VAD + 语义 EoT）：
用户说："我想买一张..."（停顿 0.5s）
                          ↑
                    语义检测：句子不完整（EoU概率 0.12）→ 继续等待
用户继续："飞机票"
                          ↑
                    语义检测：句子完整（EoU概率 0.93）→ 触发响应 ✅
```

#### 打断控制器（Interrupt Controller）

```
Agent 正在说话：
"今天北京天气晴朗，温度25度，适合出行——"
                              ↑
用户开始说话（VAD 检测到）
                              ↓
打断控制器执行：
① 立即停止 TTS 播放
② 取消正在进行的 LLM 推理（避免资源浪费）
③ 记录 Agent 已播放的内容（"今天北京天气晴朗，温度25度，"）
④ 重置流水线，开始处理用户新输入
```

### 4.3 端到端延迟对比

```
半双工级联流水线（传统）：
Audio → VAD(500ms静音等待) → ASR(批量) → LLM(同步) → TTS(整段) → 播放
                ↓                ↓            ↓           ↓
              ~500ms          ~800ms       ~2000ms     ~1000ms
总延迟: 4~10 秒

全双工流式流水线（现代）：
Audio → VAD(实时) → STT(流式) → LLM(流式) → TTS(分句) → 播放
           ↓           ↓            ↓           ↓
         ~10ms       ~200ms       ~100ms      ~200ms
总延迟: 300ms~800ms（首包）
```

---

## 5. 具备全双工能力的项目深度对比 / Full-Duplex Projects Comparison

### 5.1 横向对比矩阵

| 特性维度 | [Pipecat](https://github.com/pipecat-ai/pipecat) | [LiveKit Agents](https://github.com/livekit/agents) | [Ultravox](https://github.com/fixie-ai/ultravox) | [Vocode](https://github.com/vocodedev/vocode-core) | [FireRedChat](https://github.com/FireRedTeam/FireRedChat) | [realtime-chatbot](https://github.com/AbrahamSanders/realtime-chatbot) |
|---|---|---|---|---|---|---|
| **传输协议** | WebRTC/WebSocket | WebRTC | WebSocket | WebRTC/SIP/WS | LiveKit+WebRTC | WebSocket (Gradio) |
| **VAD 方案** | Silero VAD | Silero VAD + Turn Detector | 模型内置 | WebRTC VAD | 个性化 pVAD | Whisper 内置 |
| **轮次检测** | Frame 控制 + VAD | 语义 Turn Detector 模型 | 音频原生 LLM 自判断 | 超时 + VAD | 语义 EoT 检测器 | 连续轮次模型 |
| **打断支持** | ✅ 帧级打断 | ✅ 实时打断 | ✅ 流式中断 | ✅ 打断 | ✅ 打断 | ⚠️ 有限 |
| **首包延迟** | <800ms | <500ms | <300ms（无 ASR 步骤） | <1s | <1s | ~1~2s |
| **扩展性** | ⭐⭐⭐⭐⭐ 极高（插件化） | ⭐⭐⭐⭐ 高（LiveKit 云） | ⭐⭐⭐ 中（模型级） | ⭐⭐⭐⭐ 高 | ⭐⭐⭐ 中 | ⭐⭐ 低 |
| **生产可用** | ✅ 生产就绪 | ✅ 生产就绪 | ⚠️ 需要 Fixie 云 | ✅ 生产就绪 | ⚠️ 需要自部署 | ❌ 研究原型 |
| **核心创新** | Frame-based 流水线 | 语义 Turn Detector | 跳过 ASR，音频直入 LLM | 电话/SIP 全栈 | pVAD + 自托管全栈 | 无固定轮次 |

### 5.2 各项目全双工实现方式详解

#### 🔷 Pipecat — Frame-Based 流水线架构

**GitHub**: [pipecat-ai/pipecat](https://github.com/pipecat-ai/pipecat) ⭐ ~10,700+

**全双工实现方式**：

Pipecat 采用 **Frame-Based（帧驱动）流水线**架构，数据以"帧（Frame）"为单位在管道中异步流动：

```
AudioInputFrame → VADFrame → STTFrame → LLMTokenFrame → TTSAudioFrame → AudioOutputFrame
                                 ↕ InterruptFrame（任意帧可被打断帧取代）
```

**关键特性**：
- ✅ **帧级打断**：`InterruptFrame` 注入管道即可中断所有下游处理
- ✅ **Silero VAD**：16kHz、10ms 步长实时检测
- ✅ **并行流水线**：输入/输出管道完全异步并行
- ✅ **插件化设计**：STT/LLM/TTS 均可热插拔替换

**适用场景**：需要高度定制化的生产级全双工语音应用

---

#### 🔷 LiveKit Agents — WebRTC + 语义 Turn Detector

**GitHub**: [livekit/agents](https://github.com/livekit/agents) ⭐ ~9,700+

**全双工实现方式**：

LiveKit Agents 在 WebRTC 双向实时音频流的基础上，引入了**专门训练的语义轮次检测模型**：

```
WebRTC 双向流
    ↓
Silero VAD（物理层：是否在说话？）
    ↓
语义 Turn Detector 模型（语义层：话说完了吗？）
    ↓
两者均为 True → 触发 Agent 响应
```

**关键特性**：
- ✅ **语义 Turn Detector**：基于 Transformer 的专用模型（[livekit/turn-detector](https://github.com/livekit/turn-detector)），准确率远高于纯 VAD 超时
- ✅ **WebRTC 传输**：工业级音频传输，内置 AEC（回声消除）
- ✅ **多模态扩展**：支持视频、屏幕共享与语音的多模态交互
- ✅ **云原生**：与 LiveKit 服务端无缝集成

**适用场景**：需要可靠低延迟的生产级实时语音应用，尤其是 Web/移动端场景

---

#### 🔷 Ultravox — 音频原生多模态 LLM

**GitHub**: [fixie-ai/ultravox](https://github.com/fixie-ai/ultravox) ⭐ ~4,300+

**全双工实现方式**（颠覆性架构）：

```
传统架构：Audio → STT → Text → LLM → Text → TTS → Audio
                  ↑ 串行，延迟高
                  
Ultravox：  Audio ──────────────────► Audio-Native LLM → TTS → Audio
                  ↑ 完全跳过 STT！直接将音频 token 送入 LLM
```

**关键特性**：
- ✅ **音频原生 LLM**：将 Whisper 编码器与 Llama 系语言模型融合，LLM 直接理解音频 token
- ✅ **极低延迟**：端到端延迟 <300ms（省去 ASR 步骤）
- ✅ **自然打断**：LLM 层面感知用户说话，打断更自然
- ⚠️ **依赖 Fixie 云**：目前完整能力需要 Fixie.ai 的推理服务

**适用场景**：追求极致低延迟的场景

---

#### 🔷 Vocode — 电话/Web 全栈语音 Agent

**GitHub**: [vocodedev/vocode-core](https://github.com/vocodedev/vocode-core) ⭐ ~3,700+

**全双工实现方式**：

```
传输层（WebRTC/SIP/Twilio/WebSocket）
    ↓
异步音频流接收（持续监听，不阻塞）
    ↓
WebRTC VAD + 超时打断
    ↓
流式 STT（Deepgram 等）
    ↓
可取消的流式 LLM（打断时立即 cancel）
    ↓
流式 TTS + 分句播放
```

**关键特性**：
- ✅ **完整电话栈**：支持 SIP、Twilio、电话号码等真实电话场景
- ✅ **异步打断**：用户说话时立即取消当前 LLM/TTS 任务
- ✅ **丰富 STT/TTS 集成**：Deepgram、Azure、Google、ElevenLabs 等

**适用场景**：呼叫中心自动化、电话客服 AI

---

#### 🔷 FireRedChat — 个性化 VAD 全双工系统

**GitHub**: [FireRedTeam/FireRedChat](https://github.com/FireRedTeam/FireRedChat) ⭐ ~500+

**全双工实现方式**：

```
LiveKit RTC 传输层（WebRTC 双向流）
    ↓
个性化 VAD (pVAD) — 根据用户声纹定制的 VAD 模型
    ↓
语义 EoT 检测器（End-of-Turn Detector）
    ↓
可中断的 LLM + TTS 流水线
```

**关键特性**：
- ✅ **个性化 pVAD**：针对特定用户声纹训练，对该用户的语音检测精度极高，抗杂音能力强
- ✅ **语义 EoT 检测**：不依赖固定超时，通过语言模型判断话语语义完整性
- ✅ **完全自托管**：无外部服务依赖，适合私有化部署

**适用场景**：追求极致隐私保护的自托管全双工语音系统

---

#### 🔷 realtime-chatbot — 无固定轮次连续对话

**GitHub**: [AbrahamSanders/realtime-chatbot](https://github.com/AbrahamSanders/realtime-chatbot) ⭐ ~200+

**全双工实现方式**：

```
持续音频流（无固定轮次边界）
    ↓
Whisper 流式 ASR（持续识别，不等说完）
    ↓
连续轮次检测器（基于对话模型预测"是否该 Agent 说话"）
    ↓
Agent 可在任何时刻插话（不等用户说完）
```

**关键特性**：
- ✅ **无固定轮次**：Agent 可以在用户说话中途插入，更接近真实人类对话
- ✅ **连续轮次检测**：用概率模型预测最佳响应时机
- ⚠️ **研究原型**：完善程度不及 Pipecat/LiveKit，适合学术研究

**适用场景**：探索无固定轮次对话模式的学术研究

---

## 6. 通用全双工 Voice Agent 参考架构 / Reference Architecture

### 6.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Client (Browser / Phone / App)                      │
│  ┌───────────────┐                              ┌───────────────────┐   │
│  │  🎤 Mic 采集   │ ◄──── WebRTC / WebSocket ──► │  🔊 Speaker 播放   │  │
│  └───────┬───────┘        双向实时音频流           └─────────▲─────────┘  │
│          │                                                   │            │
└──────────┼───────────────────────────────────────────────────┼────────────┘
           │ 原始 PCM 音频流（16kHz）                           │ TTS 音频流
           ▼                                                   │
┌──────────────────────────────────────────────────────────────────────────┐
│                         Voice Agent Server                               │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                      ① 传输层 Transport Layer                      │  │
│  │              WebRTC (推荐) / WebSocket / SIP / Phone               │  │
│  │                  内置 AEC（回声消除）+ 降噪处理                      │  │
│  └──────────────────────────┬───────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                   ② VAD 语音活动检测层                               │  │
│  │            Silero VAD / WebRTC VAD / 个性化 pVAD                   │  │
│  │        → 输出: is_speech, speech_prob, event(start/end)            │  │
│  └──────────────────────────┬───────────────────────────────────────┘  │
│                              │                                          │
│         ┌────────────────────┼──────────────────────────┐              │
│         │                    │                          │              │
│         ▼                    ▼                          ▼              │
│  ┌─────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │ ③ 流式 STT  │  │  ⑤ 轮次检测器    │  │   ⑥ 打断控制器            │  │
│  │ Streaming   │  │  Turn Detector   │  │   Interrupt Controller   │  │
│  │ ASR         │  │  VAD + 语义 EoT  │  │   监控用户说话触发打断     │  │
│  └──────┬──────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
│         │                  │                          │              │
│         ▼                  │                          │              │
│  ┌─────────────┐            │                          │              │
│  │ ④ 流式 LLM  │ ◄──────────┘                          │              │
│  │ Streaming   │  (轮次检测确认用户说完后触发)            │              │
│  │ Language    │                                       │              │
│  │ Model       │ ◄─────────────────────────────────────┘              │
│  └──────┬──────┘  (打断控制器发起 cancel 请求)                          │
│         │                                                              │
│         ▼                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                        ⑦ 流式 TTS                               │  │
│  │         Streaming Text-to-Speech（分句合成，不等全文完成）          │  │
│  └──────────────────────────┬──────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    ⑧ 上下文管理器 Context Manager                │  │
│  │  对话历史 | 状态机（IDLE/LISTENING/PROCESSING/SPEAKING/INTERRUPT） │  │
│  │  打断恢复 | 已播放内容追踪 | 帧排序 | 会话状态                      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 核心组件职责说明

| 组件编号 | 组件名称 | 核心职责 | 推荐开源实现 |
|:---:|---|---|---|
| ① | **传输层** | 双向实时音频流传输 + AEC 回声消除 | WebRTC (LiveKit/Agora), WebSocket |
| ② | **VAD** | 实时语音活动检测，判断用户是否在说话 | Silero VAD, FireRedVAD |
| ③ | **流式 STT** | 将用户音频实时转录为文字（流式返回） | Deepgram, Whisper Streaming |
| ④ | **流式 LLM** | 流式生成 Agent 回复（逐 token 输出） | GPT-4o, Llama 3, Qwen2.5 |
| ⑤ | **轮次检测器** | 判断用户是否说完，决定何时 Agent 应答 | LiveKit turn-detector, 自定义 EoT |
| ⑥ | **打断控制器** | 检测用户打断，立即停止 TTS/LLM | 自定义实现（各框架均有） |
| ⑦ | **流式 TTS** | 将 LLM 输出实时合成语音（分句策略） | ElevenLabs, Cartesia, MaryTTS |
| ⑧ | **上下文管理器** | 维护对话历史、状态机、打断后准确上下文 | 自定义实现 |

### 6.3 数据流说明

```
【正常对话流程】
用户说话 → VAD 检测 speech_start → 
    STT 开始流式识别 →
    Turn Detector 持续评估"用户说完了吗" →
    VAD 检测 speech_end + EoT > 阈值 →
    STT 返回 final 转录 →
    Context Manager 添加 user message →
    LLM 开始流式生成 →
    每满一句话 → TTS 合成 → 传输层播放
    全部播放完毕 → Context Manager 添加 assistant message

【打断流程】
Agent 正在说话（TTS 播放中）→
    VAD 检测到用户语音 speech_start →
    Interrupt Controller 触发打断：
        1. 停止 TTS 播放
        2. 取消 LLM 推理
        3. 记录已播放的 TTS 内容到 Context
    流水线重置 →
    开始处理用户新输入（同正常对话流程）
```

---

## 7. 关键模块实现要点与伪代码 / Key Module Implementation

### 7.1 VAD 处理器

```python
"""
VAD 层 - 语音活动检测
核心职责：实时判断用户是否在说话，是全双工的基础
"""

class VADProcessor:
    def __init__(self, model="silero", threshold=0.5,
                 min_speech_ms=250, min_silence_ms=300):
        """
        threshold: 语音概率阈值
        min_speech_ms: 最小语音片段长度（防误触发）
        min_silence_ms: 最小静音长度（判断停顿）
        """
        self.model = load_vad_model(model)
        self.threshold = threshold
        self.min_speech_ms = min_speech_ms
        self.min_silence_ms = min_silence_ms

    def process_audio_chunk(self, audio_chunk: bytes) -> dict:
        """
        返回:
          is_speech: bool     是否有语音
          speech_prob: float  语音概率 0~1
          event: str          "speech_start" | "speech_end" | None
        """
        prob = self.model.predict(audio_chunk)
        return {
            "is_speech": prob > self.threshold,
            "speech_prob": prob,
            "event": self._detect_transition(prob)
        }
```

### 7.2 全双工流式流水线（Frame-Based 设计）

```python
"""
全双工流式流水线 - 参考 Pipecat 的 Frame-Based 设计
核心理念：数据以"帧（Frame）"为单位在管道中异步流动
所有组件必须是流式（Streaming）的，不能等一个环节完成再开始下一个
"""
import asyncio
from dataclasses import dataclass
from typing import AsyncIterator

# ===== Frame 定义 =====
@dataclass
class AudioFrame:
    data: bytes       # 16kHz PCM 音频数据
    timestamp: float  # 时间戳（ms）

@dataclass
class TranscriptFrame:
    text: str         # 转录文本
    is_final: bool    # 是否为最终结果（非中间 partial 结果）

@dataclass
class LLMTokenFrame:
    token: str        # LLM 输出的单个 token
    is_end: bool      # 是否为流式结束标记

@dataclass
class TTSAudioFrame:
    audio: bytes      # 合成的语音 PCM 数据

@dataclass
class InterruptFrame:
    pass              # 打断帧：注入管道时立即中断所有下游处理

# ===== 流式 STT =====
class StreamingSTT:
    async def process(
        self, audio_stream: AsyncIterator[AudioFrame]
    ) -> AsyncIterator[TranscriptFrame]:
        async for audio_frame in audio_stream:
            # Deepgram/Whisper 流式 API：持续接收音频，实时返回转录
            partial_result = await self.stt_client.stream(audio_frame.data)
            yield TranscriptFrame(
                text=partial_result.text,
                is_final=partial_result.is_final
            )

# ===== 流式 LLM =====
class StreamingLLM:
    async def process(
        self, text_stream: AsyncIterator[TranscriptFrame]
    ) -> AsyncIterator[LLMTokenFrame]:
        async for transcript in text_stream:
            if transcript.is_final:
                async for token in self.llm_client.stream_chat(transcript.text):
                    yield LLMTokenFrame(token=token, is_end=False)
                yield LLMTokenFrame(token="", is_end=True)

# ===== 流式 TTS =====
class StreamingTTS:
    async def process(
        self, token_stream: AsyncIterator[LLMTokenFrame]
    ) -> AsyncIterator[TTSAudioFrame]:
        sentence_buffer = ""
        async for token_frame in token_stream:
            sentence_buffer += token_frame.token
            # 策略：积累到一个完整句子/短语后立即合成，不等整段回复
            if self._is_sentence_boundary(sentence_buffer):
                audio = await self.tts_client.synthesize(sentence_buffer)
                yield TTSAudioFrame(audio=audio)
                sentence_buffer = ""
```

### 7.3 混合轮次检测器（VAD + 语义 EoT）

```python
"""
轮次检测器 - 结合 VAD + 语义分析
参考 LiveKit Turn Detector + FireRedChat EoT 设计

传统方案只靠 VAD 静音超时，会导致用户稍停顿 Agent 就抢话。
现代方案结合语义分析：只有 VAD + 语义 EoT 双重确认，才触发 Agent 响应。
"""

class HybridTurnDetector:
    def __init__(self):
        self.vad = SileroVAD()
        # LiveKit 开源的语义轮次检测模型（基于 Transformer 微调）
        self.semantic_model = load_model("livekit/turn-detector")
        self.eou_threshold = 0.85       # End-of-Utterance 语义置信度阈值
        self.silence_threshold_ms = 500  # 静音时长阈值（ms）

    async def should_agent_respond(
        self,
        audio_chunk: bytes,
        transcript_so_far: str,
        conversation_history: list
    ) -> dict:
        """
        返回:
          should_respond: bool   Agent 是否应该开始回复
          confidence: float      置信度 0~1
          reason: str            触发原因
        """
        # Step 1: VAD 检测 - 用户是否停止说话
        vad_result = self.vad.process(audio_chunk)
        silence_duration = self._get_silence_duration()

        # Step 2: 语义端点检测 - 用户的话语义上是否完整
        eou_probability = self.semantic_model.predict(
            transcript=transcript_so_far,
            history=conversation_history
        )

        # Step 3: 综合判断策略：
        #   短暂停顿 + 语义完整 → 回复（用户说完了）
        #   长时间静音 → 无论语义如何都回复（防止冷场）
        #   短暂停顿 + 语义不完整 → 等待（用户可能在思考）
        if silence_duration > 2000:   # 超长静音保底触发
            return {"should_respond": True, "confidence": 0.99, "reason": "long_silence"}

        if is_silent and silence_duration > self.silence_threshold_ms:
            if eou_probability > self.eou_threshold:
                return {"should_respond": True, "confidence": eou_probability, "reason": "vad+semantic_eot"}

        return {"should_respond": False, "confidence": eou_probability, "reason": "waiting"}
```

### 7.4 打断控制器

```python
"""
打断控制器 - 处理用户在 Agent 说话时插嘴的场景
参考 Pipecat + Vocode 的实现方案
"""

class InterruptController:
    """
    打断策略：
    1. 立即停止 TTS 播放（用户不再听到 Agent 的声音）
    2. 取消正在进行的 LLM 推理（避免继续消耗 GPU/token）
    3. 将已播放的内容保存到上下文（Agent 知道自己说到哪了）
    4. 重置流水线，开始处理用户新输入
    """

    def __init__(self, pipeline):
        self.pipeline = pipeline
        self.is_agent_speaking = False
        self.current_agent_text = ""   # Agent 计划说的完整文本
        self.played_agent_text = ""    # 已经播放给用户的部分

    async def on_user_speech_start(self):
        """用户开始说话时由 VAD 触发"""
        if self.is_agent_speaking:
            await self._handle_interrupt()

    async def _handle_interrupt(self):
        """执行打断操作"""
        # 1. 立即停止 TTS 音频输出
        await self.pipeline.tts.stop()
        # 2. 取消正在进行的 LLM 流式生成
        await self.pipeline.llm.cancel()
        # 3. 更新上下文：只记录 Agent 实际说出的部分
        self.pipeline.context.update_assistant_message(
            intended=self.current_agent_text,
            actually_spoken=self.played_agent_text
        )
        # 4. 发送打断帧，通知流水线各组件重置
        await self.pipeline.push_frame(InterruptFrame())
        self.is_agent_speaking = False

    async def on_tts_chunk_played(self, text_chunk: str):
        """TTS 每播放一段文本时回调，用于追踪实际播放内容"""
        self.played_agent_text += text_chunk
```

### 7.5 全双工上下文管理器

```python
"""
上下文管理器 - 在全双工场景下维护准确的对话历史

全双工场景的特殊挑战：
- Agent 可能被打断，实际说出的内容 ≠ 计划说的内容
- 用户的输入可能与 Agent 的输出在时间上重叠
- 需要精确追踪"用户实际听到了什么"
"""

class FullDuplexContextManager:
    def __init__(self):
        self.messages = []   # OpenAI 兼容格式的消息列表

    def add_user_message(self, text: str):
        self.messages.append({"role": "user", "content": text})

    def update_assistant_message(self, intended: str, actually_spoken: str):
        """
        打断场景：只记录 Agent 实际说出来的部分

        例：Agent 计划说 "今天北京天气晴朗，温度25度，适合出行"
            用户在 "温度25度" 后打断
            实际记录 "今天北京天气晴朗，温度25度，"
        """
        if actually_spoken.strip():
            self.messages.append({
                "role": "assistant",
                "content": actually_spoken.strip()
            })

    def get_context_for_llm(self, max_turns: int = 20) -> list:
        """获取用于 LLM 推理的上下文（最近 N 轮）"""
        return self.messages[-max_turns * 2:]
```

### 7.6 完整全双工 Voice Agent 主循环

```python
"""
完整的全双工 Voice Agent 编排
核心：输入循环（持续监听用户）和输出循环（持续播放回复）完全并行
这就是"全双工"的本质：输入和输出同时运行
"""
import asyncio

class FullDuplexVoiceAgent:
    def __init__(self, config):
        # 传输层
        self.transport = WebRTCTransport(config.webrtc)
        # 核心组件
        self.vad = SileroVAD()
        self.stt = StreamingSTT(provider="deepgram")
        self.turn_detector = HybridTurnDetector()
        self.llm = StreamingLLM(model="gpt-4o")
        self.tts = StreamingTTS(provider="elevenlabs")
        self.interrupt_ctrl = InterruptController(self)
        self.context = FullDuplexContextManager()

    async def run(self):
        """
        主循环：两个异步任务完全并行
        - _input_loop：持续监听用户
        - _output_loop：持续播放回复
        """
        await asyncio.gather(
            self._input_loop(),
            self._output_loop(),
        )

    async def _input_loop(self):
        """输入处理循环 - 永不停止监听"""
        async for audio_chunk in self.transport.receive_audio():
            vad_result = self.vad.process(audio_chunk)

            # 用户说话 + Agent 也在说话 → 触发打断判断
            if vad_result["is_speech"] and self.interrupt_ctrl.is_agent_speaking:
                await self.interrupt_ctrl.on_user_speech_start()

            if vad_result["is_speech"]:
                transcript = await self.stt.transcribe_chunk(audio_chunk)
                # 轮次检测：用户说完了吗？
                turn_result = await self.turn_detector.should_agent_respond(
                    audio_chunk, transcript, self.context.messages
                )
                if turn_result["should_respond"]:
                    self.context.add_user_message(transcript)
                    await self._generate_response(transcript)

    async def _generate_response(self, user_input: str):
        """生成并流式播放 Agent 回复"""
        self.interrupt_ctrl.is_agent_speaking = True
        full_response = ""
        async for token in self.llm.stream(self.context.get_context_for_llm()):
            full_response += token
            # 每积累一个句子就合成并播放（不等全文完成）
            if self._is_sentence_end(token):
                audio = await self.tts.synthesize(full_response)
                await self.transport.send_audio(audio)
                await self.interrupt_ctrl.on_tts_chunk_played(full_response)
                full_response = ""
        self.context.update_assistant_message(full_response, full_response)
        self.interrupt_ctrl.is_agent_speaking = False
```

---

## 8. 技术选型建议 / Technology Selection Guide

### 8.1 按场景推荐

| 使用场景 | 推荐方案 | 理由 |
|---|---|---|
| 🏢 **生产级全双工（通用）** | [Pipecat](https://github.com/pipecat-ai/pipecat) | 最成熟的框架级方案，插件化，社区活跃 |
| 📞 **电话/呼叫中心自动化** | [Vocode](https://github.com/vocodedev/vocode-core) 或 [Bolna](https://github.com/bolna-ai/bolna) | 完整电话栈，支持 SIP/Twilio |
| ⚡ **追求极致低延迟** | [Ultravox](https://github.com/fixie-ai/ultravox) | 音频原生 LLM，端到端 <300ms |
| 🔒 **完全私有化自托管** | [FireRedChat](https://github.com/FireRedTeam/FireRedChat) | 零外部依赖，完全自托管 |
| 🎮 **快速原型/Web 应用** | [LiveKit Agents](https://github.com/livekit/agents) | 生态完善，WebRTC 开箱即用 |
| 🧪 **学术研究/探索** | [realtime-chatbot](https://github.com/AbrahamSanders/realtime-chatbot) | 无固定轮次设计，适合研究 |

### 8.2 开源技术栈推荐（各组件）

| 组件 | 推荐开源方案 | 备注 |
|---|---|---|
| **传输层** | LiveKit (WebRTC) / Agora / Kurento | WebRTC 是首选，内置 AEC |
| **VAD** | Silero VAD / FireRedVAD | Silero 综合最佳，FireRedVAD 精度最高 |
| **流式 STT** | Deepgram (商用) / faster-whisper (开源) | faster-whisper 支持流式，可自托管 |
| **流式 LLM** | GPT-4o / Llama 3 / Qwen2.5 | 开源优先选 Llama 3 / Qwen2.5 |
| **Turn Detector** | LiveKit turn-detector | 开源 Transformer 模型，精度高 |
| **流式 TTS** | Cartesia (低延迟) / Coqui TTS (开源) | 开源优先选 Coqui TTS |

---

## 9. 风险与评估 / Risks & Evaluation

### 9.1 技术风险矩阵

| 风险项 | 风险等级 | 影响 | 缓解措施 |
|---|:---:|---|---|
| **回声消除（AEC）** | 🔴 高 | 全双工时 Agent 声音被 VAD 误检为用户语音 | 使用 WebRTC 内置 AEC；或硬件 AEC 设备 |
| **VAD 误触发** | 🟡 中 | 背景噪声/音乐触发不必要的响应 | 个性化 pVAD；提高 VAD 阈值；组合语义 EoT |
| **轮次检测精度** | 🟡 中 | Agent 抢话（用户未说完）或漏触发（用户说完不响应） | 引入语义 Turn Detector；可调阈值 |
| **流式延迟积累** | 🟡 中 | 分句策略导致 TTS 首包延迟不稳定 | 优化分句阈值（字数/标点/时间三重触发） |
| **打断上下文丢失** | 🟡 中 | 打断后上下文不准确影响 LLM 推理质量 | 精确追踪已播放文本；打断时同步 Context Manager |
| **并发性能** | 🟢 低 | 高并发时 GPU 资源不足 | LLM/TTS 异步队列；批处理优化 |

### 9.2 延迟分析

```
全双工流水线延迟分解（最优情况）：
┌─────────────────────────────────────────────────────────────────────┐
│                                                         首包延迟     │
│  VAD 检测        ~10ms    ████                               |       │
│  STT 首包        ~200ms   ████████████████████               |       │
│  LLM 首 Token    ~100ms   ████████████                       |       │
│  TTS 合成        ~200ms   ████████████████████               |       │
│  网络传输        ~50ms    ████████                           |       │
│                           ──────────────────────────────     |       │
│  合计            ~560ms                                      ↑       │
│                                                          目标 <800ms  │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.3 质量评估指标

| 指标 | 说明 | 目标值 |
|---|---|:---:|
| **首包延迟（TTFB）** | 从用户说完到听到 Agent 回复首字的时延 | <800ms |
| **打断响应时间** | 用户开始打断到 Agent 停止说话的时延 | <200ms |
| **轮次检测准确率** | 正确判断用户说完的比率 | >90% |
| **VAD 误触发率** | 背景噪声触发 VAD 的比率 | <5% |
| **打断上下文准确率** | 打断后对话历史与实际相符的比率 | >95% |

---

## 10. 结论与建议 / Conclusions & Recommendations

### 10.1 核心结论

1. **全双工是语音 Agent 的必然趋势**：半双工的"对讲机式"交互已无法满足用户对自然对话的预期，全双工将成为下一代语音 AI 的标准能力。

2. **技术成熟度已达生产可用门槛**：以 Pipecat 和 LiveKit Agents 为代表的开源框架已具备完整的生产部署能力，AEC、流式处理、语义轮次检测等关键技术均有成熟开源实现。

3. **"语义轮次检测"是核心竞争力**：区分优劣全双工系统的关键不在于是否支持打断，而在于**打断的准确性**——纯 VAD 超时方案仍会出现频繁抢话，只有语义 EoT 结合 VAD 才能实现接近人类水平的轮次感知。

4. **音频原生 LLM 代表未来方向**：Ultravox 验证了跳过 ASR 直接将音频 token 送入 LLM 的可行性，这一方向有望进一步将延迟降至 150ms 以内，并获得对语气、情感的原生理解能力。

### 10.2 对 Skylark 项目的建议

基于 Skylark 现有架构（Agora RTC + OrchestrationService + VAD + ASR + LLM + TTS），建议按以下路线推进全双工能力升级：

| 阶段 | 目标 | 关键实现 | 预计工作量 |
|:---:|---|---|:---:|
| **L1** | 可打断 | 并行 VAD 监听 + TTS 可取消 + 状态机 | 2 周 |
| **L2** | 流式处理 | 流式 STT/LLM/TTS + 分句策略 | 3 周 |
| **L3** | 真正全双工 | AEC 回声消除 + 语义 Turn Detector | 4 周 |
| **L4** | 自然对话 | 音频原生 LLM (GLM-4-Voice 等) | 规划中 |

> 详细升级方案请参阅：[FULL_DUPLEX_ARCHITECTURE.md](../share/FULL_DUPLEX_ARCHITECTURE.md)

---

## 11. 参考链接 / References

### 开源项目

| 项目 | 地址 | 说明 |
|---|---|---|
| Pipecat | [github.com/pipecat-ai/pipecat](https://github.com/pipecat-ai/pipecat) | Frame-based 全双工框架 |
| LiveKit Agents | [github.com/livekit/agents](https://github.com/livekit/agents) | WebRTC 语音 Agent 框架 |
| LiveKit Turn Detector | [github.com/livekit/turn-detector](https://github.com/livekit/turn-detector) | 语义轮次检测模型 |
| Ultravox | [github.com/fixie-ai/ultravox](https://github.com/fixie-ai/ultravox) | 音频原生多模态 LLM |
| Vocode | [github.com/vocodedev/vocode-core](https://github.com/vocodedev/vocode-core) | 电话/Web 全栈语音 Agent |
| Bolna | [github.com/bolna-ai/bolna](https://github.com/bolna-ai/bolna) | 对话式语音 AI 框架 |
| FireRedChat | [github.com/FireRedTeam/FireRedChat](https://github.com/FireRedTeam/FireRedChat) | 自托管全双工语音系统 |
| realtime-chatbot | [github.com/AbrahamSanders/realtime-chatbot](https://github.com/AbrahamSanders/realtime-chatbot) | 无固定轮次对话研究 |
| Leon | [github.com/leon-ai/leon](https://github.com/leon-ai/leon) | 自托管个人语音助手 |
| AgenticSeek | [github.com/Fosowl/agenticSeek](https://github.com/Fosowl/agenticSeek) | 全本地自主语音 Agent |
| Silero VAD | [github.com/snakers4/silero-vad](https://github.com/snakers4/silero-vad) | 轻量高精度 VAD 模型 |

### 相关技术文档（本仓库）

| 文档 | 路径 | 说明 |
|---|---|---|
| 全双工架构设计方案 | [share/FULL_DUPLEX_ARCHITECTURE.md](../share/FULL_DUPLEX_ARCHITECTURE.md) | Skylark 全双工升级详细设计 |
| 全双工技术分享 | [share/FULL_DUPLEX_TECH_SHARING.md](../share/FULL_DUPLEX_TECH_SHARING.md) | 从半双工到全双工的演进分析 |
| WebRTC 集成指南 | [share/WEBRTC_GUIDE.md](../share/WEBRTC_GUIDE.md) | WebRTC 传输层集成方案 |
| Agora RTC 技术分享 | [share/AGORA_RTC_TECH_SHARING.md](../share/AGORA_RTC_TECH_SHARING.md) | 声网 Agora 集成技术细节 |
| 总体架构文档 | [share/ARCHITECTURE.md](../share/ARCHITECTURE.md) | Skylark 整体架构设计 |

---

> **更新时间 / Last Updated**: 2026-03-17  
> **报告版本 / Report Version**: 1.0.0  
> **作者 / Author**: Skylark Team  
> **关联项目 / Related Project**: [Jashinck/Skylark](https://github.com/Jashinck/Skylark)
