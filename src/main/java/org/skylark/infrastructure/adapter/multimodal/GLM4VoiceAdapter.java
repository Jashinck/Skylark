package org.skylark.infrastructure.adapter.multimodal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GLM-4-Voice Adapter
 * GLM-4-Voice适配器
 *
 * <p>Integrates with GLM-4-Voice (THUDM) — end-to-end voice LLM with
 * emotion control, speech rate adjustment, and excellent Chinese support.</p>
 *
 * <p>Key capabilities:
 * <ul>
 *   <li>End-to-end voice conversation</li>
 *   <li>Parallel text + audio token output (for subtitle display)</li>
 *   <li>Emotion and speech rate control</li>
 *   <li>Excellent Chinese language support</li>
 * </ul></p>
 *
 * <p>Phase 3 component: placeholder with interface definition.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://github.com/THUDM/GLM-4-Voice">GLM-4-Voice GitHub</a>
 */
public class GLM4VoiceAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GLM4VoiceAdapter.class);

    private static final String DEFAULT_GLM4_URL = "http://localhost:8089";

    private final String serverUrl;
    private volatile boolean available = false;

    /**
     * Dual output callback — GLM-4-Voice simultaneously outputs text and audio
     * 双输出回调 —— GLM-4-Voice同时输出文本和音频
     */
    public interface DualOutputCallback {
        /** Text token for subtitle display / 文本token用于字幕显示 */
        void onTextToken(String text);
        /** Audio chunk for playback / 音频块用于播放 */
        void onAudioChunk(byte[] audioChunk);
        /** Processing complete / 处理完成 */
        void onComplete();
        /** Error occurred / 发生错误 */
        void onError(Exception e);
    }

    public GLM4VoiceAdapter() {
        this(DEFAULT_GLM4_URL);
    }

    public GLM4VoiceAdapter(String serverUrl) {
        this.serverUrl = serverUrl;
        logger.info("GLM4VoiceAdapter initialized (Phase 3 placeholder). Server URL: {}", serverUrl);
    }

    /**
     * Process audio end-to-end with GLM-4-Voice
     * 使用GLM-4-Voice进行端到端音频处理
     *
     * <p>GLM-4-Voice simultaneously returns:
     * 1. Text token stream (for subtitle display)
     * 2. Audio token stream (for voice synthesis)</p>
     *
     * @param sessionId session identifier
     * @param inputAudio PCM audio input (16kHz, 16-bit, mono)
     * @param callback dual output callback
     */
    public void processAudio(String sessionId, byte[] inputAudio, DualOutputCallback callback) {
        logger.info("Processing audio with GLM-4-Voice for session: {} (placeholder)", sessionId);
        // Phase 3: Send audio to GLM-4-Voice server and process dual outputs
    }

    /**
     * Check if GLM-4-Voice server is available
     */
    public boolean isAvailable() {
        return available;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
