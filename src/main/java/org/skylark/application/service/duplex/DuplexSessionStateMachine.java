package org.skylark.application.service.duplex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Full-duplex session state machine
 * 全双工会话状态机
 *
 * <p>Manages state transitions for full-duplex voice interaction sessions.
 * Replaces the simple sessionSpeaking Map in OrchestrationService.</p>
 *
 * <p>Key design principle: VAD events are processed in ANY state (the core of full-duplex).
 * During SPEAKING state, VAD continues to monitor for user barge-in.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class DuplexSessionStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(DuplexSessionStateMachine.class);

    private final String sessionId;
    private final AtomicReference<DuplexSessionState> currentState = new AtomicReference<>(DuplexSessionState.IDLE);
    private volatile CompletableFuture<?> currentLLMTask;
    private volatile long lastStateChangeTimestamp;
    private volatile long lastSpeechTimestamp;

    /**
     * State transition callback interface
     * 状态转换回调接口
     */
    public interface StateTransitionListener {
        void onStateChanged(String sessionId, DuplexSessionState fromState, DuplexSessionState toState);
    }

    private volatile StateTransitionListener stateTransitionListener;

    public DuplexSessionStateMachine(String sessionId) {
        this.sessionId = sessionId;
        this.lastStateChangeTimestamp = System.currentTimeMillis();
    }

    /**
     * Process a VAD event — full-duplex core: VAD events are handled in ANY state
     * 处理VAD事件 —— 全双工核心：任何状态下都处理VAD
     *
     * @param event the VAD event to process
     * @return the resulting state after processing the event
     */
    public synchronized DuplexSessionState onVADEvent(VADEvent event) {
        DuplexSessionState state = currentState.get();
        logger.debug("Session {} received VAD event {} in state {}", sessionId, event, state);

        switch (state) {
            case IDLE:
                if (event == VADEvent.SPEECH_START) {
                    transitionTo(DuplexSessionState.LISTENING);
                }
                break;

            case LISTENING:
                if (event == VADEvent.SPEECH_END) {
                    transitionTo(DuplexSessionState.PROCESSING);
                } else if (event == VADEvent.SILENCE_TIMEOUT) {
                    transitionTo(DuplexSessionState.IDLE);
                }
                break;

            case PROCESSING:
                if (event == VADEvent.SPEECH_START) {
                    // User started speaking while waiting for LLM response — barge-in
                    cancelCurrentLLMTask();
                    transitionTo(DuplexSessionState.INTERRUPTING);
                    transitionTo(DuplexSessionState.LISTENING);
                }
                break;

            case SPEAKING:
                if (event == VADEvent.SPEECH_START) {
                    // ★ Key: user barge-in during TTS playback
                    // ★ 关键：用户在TTS播放期间打断
                    cancelCurrentLLMTask();
                    transitionTo(DuplexSessionState.INTERRUPTING);
                    transitionTo(DuplexSessionState.LISTENING);
                }
                break;

            case INTERRUPTING:
                // Transitional state, will be moved to LISTENING
                break;
        }

        if (event == VADEvent.SPEECH_START) {
            lastSpeechTimestamp = System.currentTimeMillis();
        }

        return currentState.get();
    }

    /**
     * Notify that the first TTS chunk is ready — transition to SPEAKING
     * 通知首个TTS音频块就绪 —— 转换到SPEAKING状态
     */
    public synchronized void onFirstTTSChunk() {
        if (currentState.get() == DuplexSessionState.PROCESSING) {
            transitionTo(DuplexSessionState.SPEAKING);
        }
    }

    /**
     * Notify that TTS playback is complete — transition to IDLE
     * 通知TTS播放完成 —— 转换到IDLE状态
     */
    public synchronized void onTTSComplete() {
        if (currentState.get() == DuplexSessionState.SPEAKING) {
            transitionTo(DuplexSessionState.IDLE);
        }
    }

    /**
     * Notify that processing encountered an error or timeout — transition to IDLE
     * 通知处理遇到错误或超时 —— 转换到IDLE状态
     */
    public synchronized void onProcessingError() {
        DuplexSessionState state = currentState.get();
        if (state == DuplexSessionState.PROCESSING || state == DuplexSessionState.SPEAKING) {
            transitionTo(DuplexSessionState.IDLE);
        }
    }

    /**
     * Force reset to IDLE state (for cleanup)
     * 强制重置到IDLE状态（用于清理）
     */
    public synchronized void reset() {
        cancelCurrentLLMTask();
        transitionTo(DuplexSessionState.IDLE);
        logger.info("Session {} state machine reset to IDLE", sessionId);
    }

    public DuplexSessionState getState() {
        return currentState.get();
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getLastStateChangeTimestamp() {
        return lastStateChangeTimestamp;
    }

    public long getLastSpeechTimestamp() {
        return lastSpeechTimestamp;
    }

    public void setCurrentLLMTask(CompletableFuture<?> task) {
        this.currentLLMTask = task;
    }

    public void setStateTransitionListener(StateTransitionListener listener) {
        this.stateTransitionListener = listener;
    }

    /**
     * Check if barge-in is possible in current state
     * 检查当前状态是否可以执行打断
     */
    public boolean isBargeInPossible() {
        DuplexSessionState state = currentState.get();
        return state == DuplexSessionState.SPEAKING || state == DuplexSessionState.PROCESSING;
    }

    private void transitionTo(DuplexSessionState newState) {
        DuplexSessionState oldState = currentState.getAndSet(newState);
        lastStateChangeTimestamp = System.currentTimeMillis();
        logger.info("Session {} state transition: {} -> {}", sessionId, oldState, newState);

        if (stateTransitionListener != null) {
            try {
                stateTransitionListener.onStateChanged(sessionId, oldState, newState);
            } catch (Exception e) {
                logger.error("Error in state transition listener for session {}", sessionId, e);
            }
        }
    }

    private void cancelCurrentLLMTask() {
        CompletableFuture<?> task = this.currentLLMTask;
        if (task != null && !task.isDone()) {
            task.cancel(true);
            logger.info("Cancelled current LLM task for session {}", sessionId);
        }
        this.currentLLMTask = null;
    }
}
