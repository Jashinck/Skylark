package org.skylark.application.service.duplex;

import org.skylark.application.service.ASRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming ASR Service — replaces batch recognition with streaming
 * 流式ASR服务 —— 从批量识别升级为流式识别
 *
 * <p>Phase 1: Wraps existing ASRService with streaming interface.
 * Phase 2: Connects to FunASR Server via WebSocket for real-time streaming.</p>
 *
 * <p>Integration architecture:
 * <pre>
 *   Skylark Server ←──WebSocket──→ FunASR Server
 *   (PCM stream)                    (Paraformer-large real-time)
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class StreamingASRService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingASRService.class);

    private final ASRService asrService;
    private final Map<String, StreamingASRSession> sessions = new ConcurrentHashMap<>();

    /**
     * ASR result callback interface
     */
    public interface ASRResultCallback {
        /** Called with intermediate recognition results / 中间识别结果回调 */
        void onPartialResult(String text);
        /** Called with final recognition result / 最终识别结果回调 */
        void onFinalResult(String text);
        /** Called on error / 错误回调 */
        void onError(Exception e);
    }

    public StreamingASRService(ASRService asrService) {
        this.asrService = asrService;
        logger.info("StreamingASRService initialized (Phase 1: batch-mode wrapper)");
    }

    /**
     * Start a streaming ASR session
     * 开始流式ASR会话
     *
     * @param sessionId session identifier
     * @param callback result callback
     */
    public void startStreaming(String sessionId, ASRResultCallback callback) {
        StreamingASRSession session = new StreamingASRSession(sessionId, callback);
        sessions.put(sessionId, session);
        logger.info("Started streaming ASR session: {}", sessionId);
    }

    /**
     * Feed an audio chunk to the streaming session
     * 向流式会话送入音频块
     *
     * @param sessionId session identifier
     * @param audioChunk PCM audio chunk
     */
    public void feedAudioChunk(String sessionId, byte[] audioChunk) {
        StreamingASRSession session = sessions.get(sessionId);
        if (session != null && !session.isCancelled()) {
            session.addAudioChunk(audioChunk);
        }
    }

    /**
     * Finalize the streaming session and get the recognition result
     * 结束流式会话并获取识别结果
     *
     * @param sessionId session identifier
     * @return final recognized text, or null if no speech detected
     */
    public String finalizeSession(String sessionId) {
        StreamingASRSession session = sessions.remove(sessionId);
        if (session == null) {
            logger.warn("No streaming ASR session found for: {}", sessionId);
            return null;
        }

        try {
            byte[] allAudio = session.getAccumulatedAudio();
            if (allAudio.length == 0) {
                logger.info("No audio data accumulated for session: {}", sessionId);
                return null;
            }

            // Phase 1: Use existing batch ASR
            Map<String, String> result = asrService.recognize(allAudio);
            String text = result.get("text");

            if (text != null && !text.trim().isEmpty()) {
                session.getCallback().onFinalResult(text);
            }

            logger.info("Finalized streaming ASR for session {}: {}", sessionId, text);
            return text;
        } catch (Exception e) {
            logger.error("Error finalizing ASR session: {}", sessionId, e);
            session.getCallback().onError(e);
            return null;
        }
    }

    /**
     * Cancel a streaming session (barge-in)
     * 取消流式会话（打断时调用）
     *
     * @param sessionId session identifier
     */
    public void cancelSession(String sessionId) {
        StreamingASRSession session = sessions.remove(sessionId);
        if (session != null) {
            session.cancel();
            logger.info("Cancelled streaming ASR session: {}", sessionId);
        }
    }

    /**
     * Check if a session exists
     */
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * Internal streaming ASR session state
     */
    static class StreamingASRSession {
        private final String sessionId;
        private final ASRResultCallback callback;
        private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        private volatile boolean cancelled = false;

        StreamingASRSession(String sessionId, ASRResultCallback callback) {
            this.sessionId = sessionId;
            this.callback = callback;
        }

        void addAudioChunk(byte[] chunk) {
            if (!cancelled) {
                audioBuffer.write(chunk, 0, chunk.length);
            }
        }

        byte[] getAccumulatedAudio() {
            return audioBuffer.toByteArray();
        }

        void cancel() {
            this.cancelled = true;
        }

        boolean isCancelled() {
            return cancelled;
        }

        ASRResultCallback getCallback() {
            return callback;
        }

        String getSessionId() {
            return sessionId;
        }
    }
}
