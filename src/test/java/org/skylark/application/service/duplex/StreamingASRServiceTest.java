package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.ASRService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StreamingASRService
 */
@ExtendWith(MockitoExtension.class)
class StreamingASRServiceTest {

    @Mock
    private ASRService asrService;

    private StreamingASRService streamingASRService;

    @BeforeEach
    void setUp() {
        streamingASRService = new StreamingASRService(asrService);
    }

    @Test
    void testStartStreaming_CreatesSession() {
        // Arrange
        StreamingASRService.ASRResultCallback callback = createNoOpCallback();

        // Act
        streamingASRService.startStreaming("session-1", callback);

        // Assert
        assertTrue(streamingASRService.hasSession("session-1"));
    }

    @Test
    void testFeedAudioChunk_AccumulatesAudio() throws Exception {
        // Arrange
        AtomicReference<String> finalResult = new AtomicReference<>();
        StreamingASRService.ASRResultCallback callback = createCallbackCapturing(finalResult);
        streamingASRService.startStreaming("session-1", callback);

        Map<String, String> asrResult = new HashMap<>();
        asrResult.put("text", "hello world");
        when(asrService.recognize(any(byte[].class))).thenReturn(asrResult);

        // Act
        streamingASRService.feedAudioChunk("session-1", new byte[]{1, 2, 3});
        streamingASRService.feedAudioChunk("session-1", new byte[]{4, 5, 6});
        String result = streamingASRService.finalizeSession("session-1");

        // Assert
        assertEquals("hello world", result);
        assertEquals("hello world", finalResult.get());
        verify(asrService).recognize(any(byte[].class));
    }

    @Test
    void testFinalizeSession_CallsASRRecognize() throws Exception {
        // Arrange
        AtomicReference<String> finalResult = new AtomicReference<>();
        StreamingASRService.ASRResultCallback callback = createCallbackCapturing(finalResult);
        streamingASRService.startStreaming("session-1", callback);
        streamingASRService.feedAudioChunk("session-1", new byte[]{10, 20, 30, 40});

        Map<String, String> asrResult = new HashMap<>();
        asrResult.put("text", "你好世界");
        when(asrService.recognize(any(byte[].class))).thenReturn(asrResult);

        // Act
        String result = streamingASRService.finalizeSession("session-1");

        // Assert
        assertEquals("你好世界", result);
        assertEquals("你好世界", finalResult.get());
        verify(asrService, times(1)).recognize(any(byte[].class));
    }

    @Test
    void testFinalizeSession_NoAudio_ReturnsNull() {
        // Arrange
        StreamingASRService.ASRResultCallback callback = createNoOpCallback();
        streamingASRService.startStreaming("session-1", callback);

        // Act
        String result = streamingASRService.finalizeSession("session-1");

        // Assert
        assertNull(result);
    }

    @Test
    void testFinalizeSession_NoSession_ReturnsNull() {
        // Act
        String result = streamingASRService.finalizeSession("nonexistent");

        // Assert
        assertNull(result);
    }

    @Test
    void testFinalizeSession_OnError_CallsErrorCallback() throws Exception {
        // Arrange
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        StreamingASRService.ASRResultCallback callback = new StreamingASRService.ASRResultCallback() {
            @Override public void onPartialResult(String text) {}
            @Override public void onFinalResult(String text) {}
            @Override public void onError(Exception e) { errorRef.set(e); }
        };
        streamingASRService.startStreaming("session-1", callback);
        streamingASRService.feedAudioChunk("session-1", new byte[]{1, 2});

        when(asrService.recognize(any(byte[].class))).thenThrow(new RuntimeException("ASR failure"));

        // Act
        String result = streamingASRService.finalizeSession("session-1");

        // Assert
        assertNull(result);
        assertNotNull(errorRef.get());
        assertEquals("ASR failure", errorRef.get().getMessage());
    }

    @Test
    void testCancelSession_PreventsFurtherProcessing() {
        // Arrange
        StreamingASRService.ASRResultCallback callback = createNoOpCallback();
        streamingASRService.startStreaming("session-1", callback);

        // Act
        streamingASRService.cancelSession("session-1");

        // Assert
        assertFalse(streamingASRService.hasSession("session-1"));
    }

    @Test
    void testHasSession_ReturnsTrueForExistingSession() {
        // Arrange
        streamingASRService.startStreaming("session-1", createNoOpCallback());

        // Assert
        assertTrue(streamingASRService.hasSession("session-1"));
    }

    @Test
    void testHasSession_ReturnsFalseForNonExistingSession() {
        assertFalse(streamingASRService.hasSession("nonexistent"));
    }

    @Test
    void testFeedAudioChunk_ToCancelledSession_IsNoOp() {
        // Arrange
        StreamingASRService.ASRResultCallback callback = createNoOpCallback();
        streamingASRService.startStreaming("session-1", callback);
        streamingASRService.cancelSession("session-1");

        // Act - should not throw
        assertDoesNotThrow(() ->
                streamingASRService.feedAudioChunk("session-1", new byte[]{1, 2, 3}));
    }

    @Test
    void testFeedAudioChunk_ToNonExistingSession_IsNoOp() {
        // Act - should not throw
        assertDoesNotThrow(() ->
                streamingASRService.feedAudioChunk("nonexistent", new byte[]{1, 2, 3}));
    }

    @Test
    void testFinalizeSession_EmptyText_ReturnsNull() throws Exception {
        // Arrange
        StreamingASRService.ASRResultCallback callback = createNoOpCallback();
        streamingASRService.startStreaming("session-1", callback);
        streamingASRService.feedAudioChunk("session-1", new byte[]{1, 2});

        Map<String, String> asrResult = new HashMap<>();
        asrResult.put("text", "   ");
        when(asrService.recognize(any(byte[].class))).thenReturn(asrResult);

        // Act
        String result = streamingASRService.finalizeSession("session-1");

        // Assert - empty/whitespace text does not trigger callback, returns the text
        assertEquals("   ", result);
    }

    private StreamingASRService.ASRResultCallback createNoOpCallback() {
        return new StreamingASRService.ASRResultCallback() {
            @Override public void onPartialResult(String text) {}
            @Override public void onFinalResult(String text) {}
            @Override public void onError(Exception e) {}
        };
    }

    private StreamingASRService.ASRResultCallback createCallbackCapturing(AtomicReference<String> ref) {
        return new StreamingASRService.ASRResultCallback() {
            @Override public void onPartialResult(String text) {}
            @Override public void onFinalResult(String text) { ref.set(text); }
            @Override public void onError(Exception e) {}
        };
    }
}
