package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.ASRService;
import org.skylark.application.service.VADService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AudioProcessor
 * AudioProcessor 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AudioProcessorTest {
    
    @Mock
    private VADService vadService;
    
    @Mock
    private ASRService asrService;
    
    private AudioProcessor audioProcessor;
    private static final String TEST_SESSION_ID = "test-session-123";
    
    @BeforeEach
    void setUp() {
        audioProcessor = new AudioProcessor(vadService, asrService, TEST_SESSION_ID);
    }
    
    @Test
    void testConstructor_NullVADService_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AudioProcessor(null, asrService, TEST_SESSION_ID)
        );
        assertEquals("VAD service cannot be null", exception.getMessage());
    }
    
    @Test
    void testConstructor_NullASRService_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AudioProcessor(vadService, null, TEST_SESSION_ID)
        );
        assertEquals("ASR service cannot be null", exception.getMessage());
    }
    
    @Test
    void testConstructor_NullSessionId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AudioProcessor(vadService, asrService, null)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testConstructor_EmptySessionId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AudioProcessor(vadService, asrService, "   ")
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testProcessAudioChunk_NullData_ReturnsNull() throws Exception {
        // Act
        String result = audioProcessor.processAudioChunk(null);
        
        // Assert
        assertNull(result);
        verify(vadService, never()).detect(anyString(), anyString());
    }
    
    @Test
    void testProcessAudioChunk_EmptyData_ReturnsNull() throws Exception {
        // Act
        String result = audioProcessor.processAudioChunk(new byte[0]);
        
        // Assert
        assertNull(result);
        verify(vadService, never()).detect(anyString(), anyString());
    }
    
    @Test
    void testProcessAudioChunk_VADReturnsStart_SetsSpeakingTrue() throws Exception {
        // Arrange
        byte[] audioData = new byte[1024];
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("status", "start");
        
        when(vadService.detect(anyString(), eq(TEST_SESSION_ID)))
            .thenReturn(vadResult);
        
        // Act
        String result = audioProcessor.processAudioChunk(audioData);
        
        // Assert
        assertEquals("start", result);
        assertTrue(audioProcessor.isSpeaking());
        verify(vadService, times(1)).detect(anyString(), eq(TEST_SESSION_ID));
    }
    
    @Test
    void testProcessAudioChunk_VADReturnsEnd_TriggersASRAndResetBuffer() throws Exception {
        // Arrange
        byte[] audioData1 = new byte[512];
        byte[] audioData2 = new byte[512];
        
        // First set speaking to true
        Map<String, Object> startResult = new HashMap<>();
        startResult.put("status", "start");
        when(vadService.detect(anyString(), eq(TEST_SESSION_ID)))
            .thenReturn(startResult);
        audioProcessor.processAudioChunk(audioData1);
        
        // Now simulate end
        Map<String, Object> endResult = new HashMap<>();
        endResult.put("status", "end");
        Map<String, String> asrResult = new HashMap<>();
        asrResult.put("text", "recognized text");
        
        when(vadService.detect(anyString(), eq(TEST_SESSION_ID)))
            .thenReturn(endResult);
        when(asrService.recognize(any(byte[].class)))
            .thenReturn(asrResult);
        
        // Act
        String result = audioProcessor.processAudioChunk(audioData2);
        
        // Assert
        assertEquals("end", result);
        assertFalse(audioProcessor.isSpeaking());
        verify(asrService, times(1)).recognize(any(byte[].class));
    }
    
    @Test
    void testReset_ResetsStateAndBuffer() throws Exception {
        // Arrange - set speaking state
        byte[] audioData = new byte[1024];
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("status", "start");
        when(vadService.detect(anyString(), eq(TEST_SESSION_ID)))
            .thenReturn(vadResult);
        audioProcessor.processAudioChunk(audioData);
        assertTrue(audioProcessor.isSpeaking());
        
        // Act
        audioProcessor.reset();
        
        // Assert
        assertFalse(audioProcessor.isSpeaking());
    }
    
    @Test
    void testRecognizeSpeech_NullData_ReturnsNull() throws Exception {
        // Act
        String result = audioProcessor.recognizeSpeech(null);
        
        // Assert
        assertNull(result);
        verify(asrService, never()).recognize(any(byte[].class));
    }
    
    @Test
    void testRecognizeSpeech_EmptyData_ReturnsNull() throws Exception {
        // Act
        String result = audioProcessor.recognizeSpeech(new byte[0]);
        
        // Assert
        assertNull(result);
        verify(asrService, never()).recognize(any(byte[].class));
    }
    
    @Test
    void testRecognizeSpeech_ValidData_ReturnsText() throws Exception {
        // Arrange
        byte[] audioData = new byte[1024];
        Map<String, String> asrResult = new HashMap<>();
        asrResult.put("text", "hello world");
        when(asrService.recognize(audioData)).thenReturn(asrResult);
        
        // Act
        String result = audioProcessor.recognizeSpeech(audioData);
        
        // Assert
        assertEquals("hello world", result);
        verify(asrService, times(1)).recognize(audioData);
    }
    
    @Test
    void testGetSessionId_ReturnsCorrectId() {
        // Act & Assert
        assertEquals(TEST_SESSION_ID, audioProcessor.getSessionId());
    }
}
