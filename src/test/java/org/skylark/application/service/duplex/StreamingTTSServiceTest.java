package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.TTSService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StreamingTTSService
 */
@ExtendWith(MockitoExtension.class)
class StreamingTTSServiceTest {

    @Mock
    private TTSService ttsService;

    private StreamingTTSService streamingTTSService;

    @BeforeEach
    void setUp() {
        streamingTTSService = new StreamingTTSService(ttsService);
    }

    @Test
    void testSynthesizeSentence_CallsTTSAndCallback() throws Exception {
        // Arrange
        File tempFile = createTempAudioFile(new byte[]{1, 2, 3, 4});
        when(ttsService.synthesize(eq("Hello"), isNull())).thenReturn(tempFile);

        AtomicReference<byte[]> audioRef = new AtomicReference<>();
        StreamingTTSService.AudioChunkCallback callback = createCallbackCapturing(audioRef);

        // Act
        streamingTTSService.synthesizeSentence("session-1", "Hello", callback);

        // Assert
        verify(ttsService).synthesize("Hello", null);
        assertNotNull(audioRef.get());
        assertEquals(4, audioRef.get().length);

        tempFile.delete();
    }

    @Test
    void testSynthesizeSentence_OnError_CallsErrorCallback() throws Exception {
        // Arrange
        when(ttsService.synthesize(anyString(), isNull()))
                .thenThrow(new RuntimeException("TTS failure"));

        AtomicReference<Exception> errorRef = new AtomicReference<>();
        StreamingTTSService.AudioChunkCallback callback = new StreamingTTSService.AudioChunkCallback() {
            @Override public void onAudioChunk(byte[] audioChunk) {}
            @Override public void onComplete() {}
            @Override public void onError(Exception e) { errorRef.set(e); }
        };

        // Act
        streamingTTSService.synthesizeSentence("session-1", "Hello", callback);

        // Assert
        assertNotNull(errorRef.get());
        assertEquals("TTS failure", errorRef.get().getMessage());
    }

    @Test
    void testStopImmediately_CancelsSession() {
        // Arrange
        streamingTTSService.createSession("session-1");
        assertTrue(streamingTTSService.isActive("session-1"));

        // Act
        streamingTTSService.stopImmediately("session-1");

        // Assert
        assertFalse(streamingTTSService.isActive("session-1"));
    }

    @Test
    void testStopImmediately_NonExistingSession_NoOp() {
        // Act & Assert
        assertDoesNotThrow(() -> streamingTTSService.stopImmediately("nonexistent"));
    }

    @Test
    void testCreateSession_AndIsActive() {
        // Act
        StreamingTTSService.StreamingTTSSession session =
                streamingTTSService.createSession("session-1");

        // Assert
        assertNotNull(session);
        assertEquals("session-1", session.getSessionId());
        assertTrue(streamingTTSService.isActive("session-1"));
    }

    @Test
    void testIsActive_NonExistingSession_ReturnsFalse() {
        assertFalse(streamingTTSService.isActive("nonexistent"));
    }

    @Test
    void testIsActive_CancelledSession_ReturnsFalse() {
        // Arrange
        streamingTTSService.createSession("session-1");
        streamingTTSService.stopImmediately("session-1");

        // Assert
        assertFalse(streamingTTSService.isActive("session-1"));
    }

    @Test
    void testCompleteSession_RemovesSession() {
        // Arrange
        streamingTTSService.createSession("session-1");
        assertTrue(streamingTTSService.isActive("session-1"));

        // Act
        streamingTTSService.completeSession("session-1");

        // Assert
        assertFalse(streamingTTSService.isActive("session-1"));
    }

    @Test
    void testSynthesizeSentence_CancelledSession_IsSkipped() throws Exception {
        // Arrange
        streamingTTSService.createSession("session-1");
        streamingTTSService.stopImmediately("session-1");

        AtomicBoolean chunkReceived = new AtomicBoolean(false);
        StreamingTTSService.AudioChunkCallback callback = new StreamingTTSService.AudioChunkCallback() {
            @Override public void onAudioChunk(byte[] audioChunk) { chunkReceived.set(true); }
            @Override public void onComplete() {}
            @Override public void onError(Exception e) {}
        };

        // Act
        streamingTTSService.synthesizeSentence("session-1", "Hello", callback);

        // Assert - TTS should not be called for cancelled session
        verify(ttsService, never()).synthesize(anyString(), any());
        assertFalse(chunkReceived.get());
    }

    @Test
    void testStreamingTTSSession_StopImmediately() {
        // Arrange
        StreamingTTSService.StreamingTTSSession session =
                new StreamingTTSService.StreamingTTSSession("test");

        // Assert initial state
        assertFalse(session.isCancelled());

        // Act
        session.stopImmediately();

        // Assert
        assertTrue(session.isCancelled());
    }

    @Test
    void testStreamingTTSSession_GetSessionId() {
        StreamingTTSService.StreamingTTSSession session =
                new StreamingTTSService.StreamingTTSSession("my-session");
        assertEquals("my-session", session.getSessionId());
    }

    @Test
    void testSynthesizeSentence_NullFile_NoCallback() throws Exception {
        // Arrange
        when(ttsService.synthesize(anyString(), isNull())).thenReturn(null);

        AtomicBoolean chunkReceived = new AtomicBoolean(false);
        StreamingTTSService.AudioChunkCallback callback = new StreamingTTSService.AudioChunkCallback() {
            @Override public void onAudioChunk(byte[] audioChunk) { chunkReceived.set(true); }
            @Override public void onComplete() {}
            @Override public void onError(Exception e) {}
        };

        // Act
        streamingTTSService.synthesizeSentence("session-1", "Hello", callback);

        // Assert
        assertFalse(chunkReceived.get());
    }

    private File createTempAudioFile(byte[] data) throws Exception {
        File tempFile = File.createTempFile("test-audio", ".wav");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
        return tempFile;
    }

    private StreamingTTSService.AudioChunkCallback createCallbackCapturing(AtomicReference<byte[]> ref) {
        return new StreamingTTSService.AudioChunkCallback() {
            @Override public void onAudioChunk(byte[] audioChunk) { ref.set(audioChunk); }
            @Override public void onComplete() {}
            @Override public void onError(Exception e) {}
        };
    }
}
