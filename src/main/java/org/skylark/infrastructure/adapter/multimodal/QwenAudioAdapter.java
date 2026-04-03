package org.skylark.infrastructure.adapter.multimodal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Qwen2-Audio End-to-End Voice Model Adapter
 * 通义千问2-Audio端到端语音大模型适配器
 *
 * <p>Integrates with Alibaba's Qwen2-Audio — an open-source multimodal large
 * language model that processes audio input directly, bypassing the traditional
 * ASR → LLM → TTS cascade pipeline.</p>
 *
 * <p>Key capabilities:
 * <ul>
 *   <li>Direct audio understanding without ASR transcription error accumulation</li>
 *   <li>Emotion and intent detection from speech prosody</li>
 *   <li>Audio event classification (AED): speech, music, noise, etc.</li>
 *   <li>Multilingual: 30+ languages supported natively</li>
 *   <li>Open-source: deployable on-premise or via DashScope API</li>
 * </ul></p>
 *
 * <p>Routing integration with ModelRouter:
 * <pre>
 *   Simple chat / emotional interaction → Qwen2-Audio (end-to-end)
 *   Tool calling / structured tasks    → Cascade (AgentScope ReAct)
 * </pre></p>
 *
 * <p>Phase 3 component ([C1] in the full-duplex upgrade roadmap).
 * Completes the ModelRouter END_TO_END routing path alongside
 * GLM4VoiceAdapter and MoshiAdapter.</p>
 *
 * <p>Example config.yaml usage (via ModelRouter in full duplex mode):
 * <pre>
 * duplex:
 *   mode: full
 *   endToEndModel: qwen-audio
 *   qwenAudioUrl: http://localhost:7860
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://github.com/QwenLM/Qwen2-Audio">Qwen2-Audio GitHub</a>
 * @see <a href="https://help.aliyun.com/zh/dashscope/developer-reference/qwen-audio-api">
 *      Qwen-Audio DashScope API</a>
 */
public class QwenAudioAdapter {

    private static final Logger logger = LoggerFactory.getLogger(QwenAudioAdapter.class);

    private static final String DEFAULT_SERVER_URL = "http://localhost:7860";
    private static final String DEFAULT_DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final String serverUrl;
    private final String apiKey;
    private volatile boolean available = false;

    /**
     * Audio+text output callback — Qwen2-Audio returns text understanding
     * simultaneously with optional TTS-ready text for downstream synthesis.
     * 双输出回调 —— Qwen2-Audio同时输出文本理解和语音合成文本
     */
    public interface QwenAudioCallback {
        /** Text response token for display / 文本响应token用于显示 */
        void onTextToken(String text);
        /** Audio event classification result / 音频事件分类结果 */
        void onAudioEvent(AudioEventType eventType, float confidence);
        /** Processing complete / 处理完成 */
        void onComplete();
        /** Error occurred / 发生错误 */
        void onError(Exception e);
    }

    /**
     * Audio event types detected by Qwen2-Audio's AED capability.
     * 音频事件类型 —— Qwen2-Audio的AED（音频事件检测）能力
     */
    public enum AudioEventType {
        /** Human speech / 人声 */
        SPEECH,
        /** Background music / 背景音乐 */
        MUSIC,
        /** Echo residue from AEC / AEC残留回声 */
        ECHO,
        /** Environmental noise / 环境噪声 */
        NOISE,
        /** Humming / singing / 哼唱 */
        SINGING,
        /** Silence / 静音 */
        SILENCE,
        /** Unknown audio event / 未知音频事件 */
        UNKNOWN
    }

    public QwenAudioAdapter() {
        this(DEFAULT_SERVER_URL, null);
    }

    /**
     * Creates a QwenAudioAdapter targeting a local inference server.
     *
     * @param serverUrl Local Qwen2-Audio inference server URL
     */
    public QwenAudioAdapter(String serverUrl) {
        this(serverUrl, null);
    }

    /**
     * Creates a QwenAudioAdapter using DashScope cloud API.
     *
     * @param serverUrl Server URL (use DEFAULT_DASHSCOPE_URL for cloud)
     * @param apiKey    DashScope API key (null for local inference)
     */
    public QwenAudioAdapter(String serverUrl, String apiKey) {
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        logger.info("QwenAudioAdapter initialized (Phase 3 placeholder). serverUrl={}, mode={}",
                serverUrl, apiKey != null ? "cloud/DashScope" : "local");
        // Phase 3: Attempt health check to verify server availability
    }

    /**
     * Process audio end-to-end with Qwen2-Audio.
     * 使用Qwen2-Audio进行端到端音频理解
     *
     * <p>Qwen2-Audio processes the raw audio directly, returning:
     * <ol>
     *   <li>Text response token stream (for display / downstream TTS)</li>
     *   <li>Audio event classification (speech / music / noise / echo)</li>
     * </ol></p>
     *
     * @param sessionId  Session identifier
     * @param inputAudio PCM audio input (16kHz, 16-bit, mono)
     * @param callback   Output callback
     */
    public void processAudio(String sessionId, byte[] inputAudio, QwenAudioCallback callback) {
        logger.info("[QwenAudio] Processing {} bytes for session: {} (Phase 3 placeholder)",
                inputAudio != null ? inputAudio.length : 0, sessionId);
        // Phase 3:
        // 1. Base64-encode inputAudio
        // 2. POST to serverUrl with {"model":"qwen-audio-chat","input":{"audio":base64,"text":"请回答"}}
        // 3. Stream token responses via SSE / WebSocket
        // 4. Invoke callback.onTextToken() for each token
        // 5. Invoke callback.onAudioEvent() with AED classification
        // 6. Invoke callback.onComplete() when done
    }

    /**
     * Classify audio event type without full language model processing.
     * 快速音频事件分类（不经过完整语言模型）
     *
     * <p>Lightweight AED (Audio Event Detection) mode that only returns
     * the event type classification, much faster than full audio processing.
     * Particularly valuable for the TripleVADEngine false barge-in reduction:</p>
     * <ul>
     *   <li>ECHO → filter out (AEC residue)</li>
     *   <li>MUSIC → ignore</li>
     *   <li>SPEECH → trigger ASR pipeline</li>
     * </ul>
     *
     * @param sessionId  Session identifier
     * @param audioChunk Audio chunk (typically 16-32ms frame)
     * @return Audio event type classification
     */
    public AudioEventType classifyAudioEvent(String sessionId, byte[] audioChunk) {
        logger.debug("[QwenAudio] Classifying audio event for session: {} (Phase 3 placeholder)", sessionId);
        // Phase 3: Lightweight AED inference via Qwen2-Audio or dedicated AED model
        return AudioEventType.UNKNOWN;
    }

    /**
     * Check if the Qwen2-Audio server is available.
     */
    public boolean isAvailable() {
        return available;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Returns true if configured to use DashScope cloud API.
     */
    public boolean isCloudMode() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
