package org.skylark.application.service.duplex;

import org.skylark.application.service.TTSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming TTS Service — replaces batch synthesis with streaming chunks
 * 流式TTS服务 —— 从整段生成升级为流式分片合成
 *
 * <p>Phase 1: Wraps existing TTSService with cancellable session support.
 * Phase 2: Connects to CosyVoice 2 Server for real-time streaming synthesis.</p>
 *
 * <p>Key full-duplex enhancement: TTS sessions are cancellable for barge-in support.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class StreamingTTSService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingTTSService.class);

    private final TTSService ttsService;
    private final Map<String, StreamingTTSSession> sessions = new ConcurrentHashMap<>();

    /**
     * Audio chunk callback interface
     * 音频块回调接口
     */
    public interface AudioChunkCallback {
        /** Called with each audio chunk / 每个音频块的回调 */
        void onAudioChunk(byte[] audioChunk);
        /** Called when synthesis is complete / 合成完成回调 */
        void onComplete();
        /** Called on error / 错误回调 */
        void onError(Exception e);
    }

    public StreamingTTSService(TTSService ttsService) {
        this.ttsService = ttsService;
        logger.info("StreamingTTSService initialized (Phase 1: cancellable batch wrapper)");
    }

    /**
     * Synthesize a sentence and deliver audio via callback
     * 合成一个句子并通过回调传递音频
     *
     * @param sessionId session identifier
     * @param sentence text to synthesize
     * @param callback audio chunk callback
     * @return cancellable TTS session
     */
    public StreamingTTSSession synthesizeSentence(String sessionId, String sentence, AudioChunkCallback callback) {
        StreamingTTSSession session = sessions.computeIfAbsent(sessionId, k -> new StreamingTTSSession(sessionId));

        if (session.isCancelled()) {
            logger.info("TTS session {} is cancelled, skipping synthesis", sessionId);
            return session;
        }

        try {
            // Phase 1: Use existing batch TTS
            File audioFile = ttsService.synthesize(sentence, null);

            if (session.isCancelled()) {
                return session;
            }

            if (audioFile != null && audioFile.exists()) {
                byte[] audioData = Files.readAllBytes(audioFile.toPath());
                if (!session.isCancelled()) {
                    callback.onAudioChunk(audioData);
                }
            }
        } catch (Exception e) {
            if (!session.isCancelled()) {
                logger.error("Error synthesizing sentence for session {}: {}", sessionId, sentence, e);
                callback.onError(e);
            }
        }

        return session;
    }

    /**
     * Stop TTS immediately (barge-in)
     * 立即停止TTS（打断时调用）
     *
     * @param sessionId session identifier
     */
    public void stopImmediately(String sessionId) {
        StreamingTTSSession session = sessions.get(sessionId);
        if (session != null) {
            session.stopImmediately();
            logger.info("Stopped TTS immediately for session {}", sessionId);
        }
    }

    /**
     * Complete and clean up a TTS session
     */
    public void completeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Create a new TTS session
     */
    public StreamingTTSSession createSession(String sessionId) {
        StreamingTTSSession session = new StreamingTTSSession(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Check if a session exists and is not cancelled
     */
    public boolean isActive(String sessionId) {
        StreamingTTSSession session = sessions.get(sessionId);
        return session != null && !session.isCancelled();
    }

    /**
     * Cancellable TTS session — supports barge-in
     * 可取消的TTS会话 —— 支持打断
     */
    public static class StreamingTTSSession {
        private final String sessionId;
        private volatile boolean cancelled = false;

        public StreamingTTSSession(String sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * Stop immediately — called during barge-in
         * 立即停止 —— 打断时调用
         */
        public void stopImmediately() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}
