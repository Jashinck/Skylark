package org.skylark.application.service.duplex;

import org.skylark.application.service.OrchestrationService.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Full-duplex Orchestration Service
 * 全双工编排服务
 *
 * <p>Replaces the half-duplex OrchestrationService with full-duplex capabilities:
 * <ol>
 *   <li>Uses DuplexSessionStateMachine instead of sessionSpeaking Map</li>
 *   <li>Uses streaming components instead of batch components</li>
 *   <li>Uplink and downlink channels work in parallel</li>
 *   <li>Supports barge-in interruption</li>
 * </ol></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class DuplexOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(DuplexOrchestrationService.class);

    private final TripleVADEngine vadEngine;
    private final StreamingASRService streamingASR;
    private final StreamingLLMService streamingLLM;
    private final StreamingTTSService streamingTTS;
    private final ServerAECProcessor aecProcessor;
    private final ModelRouter modelRouter;

    /** Per-session state machines (replaces sessionSpeaking + sessionBuffers) */
    private final Map<String, DuplexSessionStateMachine> sessions = new ConcurrentHashMap<>();

    /** Per-session response callbacks */
    private final Map<String, ResponseCallback> sessionCallbacks = new ConcurrentHashMap<>();

    /** Per-session playback reference audio for AEC */
    private final Map<String, float[]> playbackReferences = new ConcurrentHashMap<>();

    public DuplexOrchestrationService(
            TripleVADEngine vadEngine,
            StreamingASRService streamingASR,
            StreamingLLMService streamingLLM,
            StreamingTTSService streamingTTS,
            ServerAECProcessor aecProcessor,
            ModelRouter modelRouter) {
        this.vadEngine = vadEngine;
        this.streamingASR = streamingASR;
        this.streamingLLM = streamingLLM;
        this.streamingTTS = streamingTTS;
        this.aecProcessor = aecProcessor;
        this.modelRouter = modelRouter;
        logger.info("DuplexOrchestrationService initialized");
    }

    /**
     * Process continuous audio frame — full-duplex core entry point
     * 处理持续的音频帧 —— 全双工核心入口
     *
     * <p>Replaces OrchestrationService.processAudioStream().
     * Key difference: audio is processed at ALL times, including during system speech.</p>
     *
     * @param sessionId  session identifier
     * @param audioFrame raw PCM audio frame (16kHz, 16-bit, mono)
     * @param callback   response callback
     */
    public void processAudioFrame(String sessionId, byte[] audioFrame, ResponseCallback callback) {
        DuplexSessionStateMachine sm = sessions.computeIfAbsent(
                sessionId, k -> createStateMachine(k));
        sessionCallbacks.put(sessionId, callback);

        try {
            // Step 1: AEC echo cancellation (if system is playing TTS)
            float[] micAudio = ServerAECProcessor.pcmBytesToFloatArray(audioFrame);
            float[] refAudio = playbackReferences.get(sessionId);
            float[] cleanAudio = aecProcessor.process(micAudio, refAudio);

            // Step 2: VAD detection (always runs, never paused by state)
            VADResult vadResult = vadEngine.detect(cleanAudio);

            // Step 3: Feed VAD result to state machine
            DuplexSessionState previousState = sm.getState();
            if (vadResult.isSpeech()) {
                sm.onVADEvent(VADEvent.SPEECH_START);
            }

            // Step 4: Handle state-specific actions
            DuplexSessionState currentState = sm.getState();

            // Barge-in detected: notify client
            if (previousState == DuplexSessionState.SPEAKING &&
                    currentState == DuplexSessionState.LISTENING) {
                handleBargeIn(sessionId, callback);
            }

            // Feed audio to streaming ASR if listening
            if (currentState == DuplexSessionState.LISTENING) {
                if (!streamingASR.hasSession(sessionId)) {
                    startStreamingASR(sessionId, callback);
                }
                streamingASR.feedAudioChunk(sessionId, audioFrame);
            }

        } catch (Exception e) {
            logger.error("Error processing audio frame for session {}", sessionId, e);
            callback.send(sessionId, "error", Map.of("message", "Error processing audio: " + e.getMessage()));
        }
    }

    /**
     * Handle speech end event — finalize ASR and start LLM processing
     * 处理语音结束事件 —— 完成ASR并开始LLM处理
     */
    public void onSpeechEnd(String sessionId) {
        DuplexSessionStateMachine sm = sessions.get(sessionId);
        if (sm == null) return;

        sm.onVADEvent(VADEvent.SPEECH_END);

        if (sm.getState() == DuplexSessionState.PROCESSING) {
            // Finalize ASR and start LLM
            String transcript = streamingASR.finalizeSession(sessionId);
            if (transcript != null && !transcript.trim().isEmpty()) {
                ResponseCallback callback = sessionCallbacks.get(sessionId);
                if (callback != null) {
                    callback.send(sessionId, "asr_result", Map.of("text", transcript));
                    startStreamingLLM(sessionId, transcript, callback);
                }
            } else {
                // No speech detected, return to IDLE
                sm.onProcessingError();
            }
        }
    }

    /**
     * Clean up session resources
     * 清理会话资源
     */
    public void cleanupSession(String sessionId) {
        DuplexSessionStateMachine sm = sessions.remove(sessionId);
        if (sm != null) {
            sm.reset();
        }
        sessionCallbacks.remove(sessionId);
        playbackReferences.remove(sessionId);
        streamingASR.cancelSession(sessionId);
        streamingLLM.cancelStream(sessionId);
        streamingTTS.completeSession(sessionId);
        vadEngine.resetSession(sessionId);
        logger.info("Cleaned up duplex session: {}", sessionId);
    }

    /**
     * Get the state machine for a session
     */
    public DuplexSessionStateMachine getStateMachine(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Get all active session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Set playback reference audio for AEC
     */
    public void setPlaybackReference(String sessionId, float[] referenceAudio) {
        if (referenceAudio != null) {
            playbackReferences.put(sessionId, referenceAudio);
        } else {
            playbackReferences.remove(sessionId);
        }
    }

    private DuplexSessionStateMachine createStateMachine(String sessionId) {
        DuplexSessionStateMachine sm = new DuplexSessionStateMachine(sessionId);
        logger.info("Created duplex state machine for session: {}", sessionId);
        return sm;
    }

    private void handleBargeIn(String sessionId, ResponseCallback callback) {
        logger.info("Barge-in detected for session {}, stopping TTS", sessionId);

        // Stop TTS playback
        streamingTTS.stopImmediately(sessionId);
        playbackReferences.remove(sessionId);

        // Cancel LLM if running
        streamingLLM.cancelStream(sessionId);

        // Notify client to stop playback
        callback.send(sessionId, "barge_in", Map.of("action", "stop_playback"));
    }

    private void startStreamingASR(String sessionId, ResponseCallback callback) {
        streamingASR.startStreaming(sessionId, new StreamingASRService.ASRResultCallback() {
            @Override
            public void onPartialResult(String text) {
                callback.send(sessionId, "asr_partial", Map.of("text", text));
            }

            @Override
            public void onFinalResult(String text) {
                callback.send(sessionId, "asr_result", Map.of("text", text));
            }

            @Override
            public void onError(Exception e) {
                logger.error("ASR error for session {}", sessionId, e);
            }
        });
    }

    private void startStreamingLLM(String sessionId, String text, ResponseCallback callback) {
        DuplexSessionStateMachine sm = sessions.get(sessionId);
        if (sm == null) return;

        var future = streamingLLM.chatStream(sessionId, text, new StreamingLLMService.TokenStreamCallback() {
            private boolean firstChunkSent = false;

            @Override
            public void onToken(String token) {
                // Individual tokens can be used for UI text streaming
            }

            @Override
            public void onSentenceComplete(String sentence) {
                // Send sentence to TTS
                if (sm.getState() == DuplexSessionState.PROCESSING ||
                        sm.getState() == DuplexSessionState.SPEAKING) {

                    if (!firstChunkSent) {
                        sm.onFirstTTSChunk();
                        firstChunkSent = true;
                    }

                    streamingTTS.synthesizeSentence(sessionId, sentence,
                            new StreamingTTSService.AudioChunkCallback() {
                                @Override
                                public void onAudioChunk(byte[] audioChunk) {
                                    String audioBase64 = Base64.getEncoder().encodeToString(audioChunk);
                                    callback.send(sessionId, "tts_audio", Map.of("audio", audioBase64));
                                }

                                @Override
                                public void onComplete() {
                                    // Individual sentence complete
                                }

                                @Override
                                public void onError(Exception e) {
                                    logger.error("TTS error for session {}", sessionId, e);
                                }
                            });
                }
            }

            @Override
            public void onComplete(String fullResponse) {
                callback.send(sessionId, "llm_response", Map.of("text", fullResponse));
                sm.onTTSComplete();
                streamingTTS.completeSession(sessionId);
                playbackReferences.remove(sessionId);
            }

            @Override
            public void onError(Exception e) {
                logger.error("LLM error for session {}", sessionId, e);
                sm.onProcessingError();
                callback.send(sessionId, "error", Map.of("message", "LLM error: " + e.getMessage()));
            }
        });

        sm.setCurrentLLMTask(future);
    }
}
