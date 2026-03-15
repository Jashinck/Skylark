package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DuplexSessionStateMachine
 */
class DuplexSessionStateMachineTest {

    private DuplexSessionStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new DuplexSessionStateMachine("test-session");
    }

    // --- State transition tests ---

    @Test
    void testOnVADEvent_IdleToListening_OnSpeechStart() {
        // Arrange
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());

        // Act
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        assertEquals(DuplexSessionState.LISTENING, result);
        assertEquals(DuplexSessionState.LISTENING, stateMachine.getState());
    }

    @Test
    void testOnVADEvent_ListeningToProcessing_OnSpeechEnd() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        assertEquals(DuplexSessionState.LISTENING, stateMachine.getState());

        // Act
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SPEECH_END);

        // Assert
        assertEquals(DuplexSessionState.PROCESSING, result);
    }

    @Test
    void testOnVADEvent_ListeningToIdle_OnSilenceTimeout() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        assertEquals(DuplexSessionState.LISTENING, stateMachine.getState());

        // Act
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SILENCE_TIMEOUT);

        // Assert
        assertEquals(DuplexSessionState.IDLE, result);
    }

    @Test
    void testOnFirstTTSChunk_ProcessingToSpeaking() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        assertEquals(DuplexSessionState.PROCESSING, stateMachine.getState());

        // Act
        stateMachine.onFirstTTSChunk();

        // Assert
        assertEquals(DuplexSessionState.SPEAKING, stateMachine.getState());
    }

    @Test
    void testOnProcessingError_ProcessingToIdle() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        assertEquals(DuplexSessionState.PROCESSING, stateMachine.getState());

        // Act
        stateMachine.onProcessingError();

        // Assert
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testOnTTSComplete_SpeakingToIdle() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        assertEquals(DuplexSessionState.SPEAKING, stateMachine.getState());

        // Act
        stateMachine.onTTSComplete();

        // Assert
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testOnVADEvent_SpeakingBargeIn_ToListening() {
        // Arrange - get to SPEAKING state
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        assertEquals(DuplexSessionState.SPEAKING, stateMachine.getState());

        // Act - barge-in during TTS playback
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert - transitions through INTERRUPTING to LISTENING
        assertEquals(DuplexSessionState.LISTENING, result);
    }

    @Test
    void testOnVADEvent_ProcessingBargeIn_ToListening() {
        // Arrange - get to PROCESSING state
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        assertEquals(DuplexSessionState.PROCESSING, stateMachine.getState());

        // Act - barge-in during processing
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        assertEquals(DuplexSessionState.LISTENING, result);
    }

    // --- isBargeInPossible tests ---

    @Test
    void testIsBargeInPossible_Idle_ReturnsFalse() {
        assertFalse(stateMachine.isBargeInPossible());
    }

    @Test
    void testIsBargeInPossible_Listening_ReturnsFalse() {
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        assertFalse(stateMachine.isBargeInPossible());
    }

    @Test
    void testIsBargeInPossible_Processing_ReturnsTrue() {
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        assertTrue(stateMachine.isBargeInPossible());
    }

    @Test
    void testIsBargeInPossible_Speaking_ReturnsTrue() {
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        assertTrue(stateMachine.isBargeInPossible());
    }

    // --- reset() tests ---

    @Test
    void testReset_FromAnyState_ResetsToIdle() {
        // Arrange - move to LISTENING
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        assertEquals(DuplexSessionState.LISTENING, stateMachine.getState());

        // Act
        stateMachine.reset();

        // Assert
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testReset_CancelsLLMTask() {
        // Arrange
        CompletableFuture<String> mockTask = new CompletableFuture<>();
        stateMachine.setCurrentLLMTask(mockTask);

        // Act
        stateMachine.reset();

        // Assert
        assertTrue(mockTask.isCancelled());
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    // --- StateTransitionListener tests ---

    @Test
    void testStateTransitionListener_CalledOnTransition() {
        // Arrange
        List<String> transitions = new ArrayList<>();
        stateMachine.setStateTransitionListener(
                (sessionId, from, to) -> transitions.add(from + "->" + to));

        // Act
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        assertEquals(1, transitions.size());
        assertEquals("IDLE->LISTENING", transitions.get(0));
    }

    @Test
    void testStateTransitionListener_MultipleTransitions() {
        // Arrange
        List<String> transitions = new ArrayList<>();
        stateMachine.setStateTransitionListener(
                (sessionId, from, to) -> transitions.add(from + "->" + to));

        // Act
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        stateMachine.onTTSComplete();

        // Assert
        assertEquals(4, transitions.size());
        assertEquals("IDLE->LISTENING", transitions.get(0));
        assertEquals("LISTENING->PROCESSING", transitions.get(1));
        assertEquals("PROCESSING->SPEAKING", transitions.get(2));
        assertEquals("SPEAKING->IDLE", transitions.get(3));
    }

    @Test
    void testStateTransitionListener_BargeInTransitions() {
        // Arrange
        List<String> transitions = new ArrayList<>();
        stateMachine.setStateTransitionListener(
                (sessionId, from, to) -> transitions.add(from + "->" + to));

        // Get to SPEAKING state
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        transitions.clear();

        // Act - barge-in
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert - should see SPEAKING->INTERRUPTING and INTERRUPTING->LISTENING
        assertEquals(2, transitions.size());
        assertEquals("SPEAKING->INTERRUPTING", transitions.get(0));
        assertEquals("INTERRUPTING->LISTENING", transitions.get(1));
    }

    // --- LLM task cancellation on barge-in ---

    @Test
    void testBargeIn_CancelsLLMTask_DuringSpeaking() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();

        CompletableFuture<String> mockTask = new CompletableFuture<>();
        stateMachine.setCurrentLLMTask(mockTask);

        // Act - barge-in
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        assertTrue(mockTask.isCancelled());
    }

    @Test
    void testBargeIn_CancelsLLMTask_DuringProcessing() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);

        CompletableFuture<String> mockTask = new CompletableFuture<>();
        stateMachine.setCurrentLLMTask(mockTask);

        // Act - barge-in during processing
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        assertTrue(mockTask.isCancelled());
    }

    // --- Getter tests ---

    @Test
    void testGetSessionId_ReturnsCorrectId() {
        assertEquals("test-session", stateMachine.getSessionId());
    }

    @Test
    void testGetLastStateChangeTimestamp_UpdatesOnTransition() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        long after = System.currentTimeMillis();
        assertTrue(stateMachine.getLastStateChangeTimestamp() >= before);
        assertTrue(stateMachine.getLastStateChangeTimestamp() <= after);
    }

    @Test
    void testGetLastSpeechTimestamp_UpdatesOnSpeechStart() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        stateMachine.onVADEvent(VADEvent.SPEECH_START);

        // Assert
        long after = System.currentTimeMillis();
        assertTrue(stateMachine.getLastSpeechTimestamp() >= before);
        assertTrue(stateMachine.getLastSpeechTimestamp() <= after);
    }

    // --- No-op transitions ---

    @Test
    void testOnVADEvent_IdleSpeechEnd_NoTransition() {
        DuplexSessionState result = stateMachine.onVADEvent(VADEvent.SPEECH_END);
        assertEquals(DuplexSessionState.IDLE, result);
    }

    @Test
    void testOnFirstTTSChunk_NotInProcessing_NoTransition() {
        // In IDLE state
        stateMachine.onFirstTTSChunk();
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testOnTTSComplete_NotInSpeaking_NoTransition() {
        stateMachine.onTTSComplete();
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testOnProcessingError_FromSpeaking_TransitionsToIdle() {
        // Arrange
        stateMachine.onVADEvent(VADEvent.SPEECH_START);
        stateMachine.onVADEvent(VADEvent.SPEECH_END);
        stateMachine.onFirstTTSChunk();
        assertEquals(DuplexSessionState.SPEAKING, stateMachine.getState());

        // Act
        stateMachine.onProcessingError();

        // Assert
        assertEquals(DuplexSessionState.IDLE, stateMachine.getState());
    }

    @Test
    void testStateTransitionListener_ExceptionDoesNotBreakStateMachine() {
        // Arrange
        stateMachine.setStateTransitionListener((sessionId, from, to) -> {
            throw new RuntimeException("Listener error");
        });

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> stateMachine.onVADEvent(VADEvent.SPEECH_START));
        assertEquals(DuplexSessionState.LISTENING, stateMachine.getState());
    }
}
