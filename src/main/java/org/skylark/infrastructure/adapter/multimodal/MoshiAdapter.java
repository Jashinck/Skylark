package org.skylark.infrastructure.adapter.multimodal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moshi Full-Duplex Voice LLM Adapter
 * Moshi全双工语音大模型适配器
 *
 * <p>Integrates with Moshi (Kyutai Labs) — the first open-source full-duplex voice LLM.
 * Moshi processes audio input/output end-to-end, bypassing traditional ASR/TTS pipeline.</p>
 *
 * <p>Key Moshi capabilities:
 * <ul>
 *   <li>Full-duplex: simultaneous speaking and listening</li>
 *   <li>Inner Monologue: text stream for understanding verification</li>
 *   <li>Mimi Codec: neural audio codec for end-to-end processing</li>
 *   <li>Dual-stream: independent user and system audio streams</li>
 * </ul></p>
 *
 * <p>Phase 3 component: WebSocket connection to Moshi inference server.
 * Current implementation is a placeholder with interface definition.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://github.com/kyutai-labs/moshi">Moshi GitHub</a>
 */
public class MoshiAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MoshiAdapter.class);

    private static final String DEFAULT_MOSHI_URL = "ws://localhost:8088/ws";

    private final String moshiServerUrl;
    private volatile boolean available = false;

    /**
     * Audio chunk callback for bidirectional streaming
     */
    public interface AudioChunkCallback {
        void onAudioChunk(byte[] audioChunk);
        void onTextToken(String token);
        void onError(Exception e);
    }

    public MoshiAdapter() {
        this(DEFAULT_MOSHI_URL);
    }

    public MoshiAdapter(String moshiServerUrl) {
        this.moshiServerUrl = moshiServerUrl;
        logger.info("MoshiAdapter initialized (Phase 3 placeholder). Server URL: {}", moshiServerUrl);
        // Phase 3: Attempt connection to verify server availability
    }

    /**
     * Create a full-duplex audio session with Moshi
     * 创建与Moshi的全双工音频会话
     *
     * <p>Establishes a bidirectional WebSocket connection:
     * - Uplink: continuously send user PCM audio
     * - Downlink: continuously receive system PCM audio
     * - Text stream: receive Inner Monologue text for understanding verification</p>
     *
     * @param sessionId session identifier
     * @param outputCallback callback for system audio output and text
     * @return MoshiSession handle (cancellable)
     */
    public MoshiSession createDuplexSession(String sessionId, AudioChunkCallback outputCallback) {
        logger.info("Creating Moshi duplex session: {} (placeholder)", sessionId);
        return new MoshiSession(sessionId);
    }

    /**
     * Send audio to an active Moshi session
     * 向活跃的Moshi会话发送音频
     *
     * @param session active Moshi session
     * @param audioChunk PCM audio chunk (16kHz, 16-bit, mono)
     */
    public void sendAudio(MoshiSession session, byte[] audioChunk) {
        if (session == null || session.isClosed()) {
            return;
        }
        // Phase 3: Send audio via WebSocket to Moshi server
        logger.debug("Moshi sendAudio: {} bytes to session {}", audioChunk.length, session.getSessionId());
    }

    /**
     * Close a Moshi session
     */
    public void closeSession(MoshiSession session) {
        if (session != null) {
            session.close();
            logger.info("Closed Moshi session: {}", session.getSessionId());
        }
    }

    /**
     * Check if Moshi server is available
     */
    public boolean isAvailable() {
        return available;
    }

    public String getMoshiServerUrl() {
        return moshiServerUrl;
    }

    /**
     * Moshi session handle
     * Moshi会话句柄
     */
    public static class MoshiSession {
        private final String sessionId;
        private volatile boolean closed = false;

        public MoshiSession(String sessionId) {
            this.sessionId = sessionId;
        }

        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}
