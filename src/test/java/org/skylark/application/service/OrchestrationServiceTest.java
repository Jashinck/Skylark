package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrchestrationService
 */
@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

    @Mock
    private VADService vadService;

    @Mock
    private ASRService asrService;

    @Mock
    private TTSService ttsService;

    @Mock
    private AgentService agentService;

    private OrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new OrchestrationService(vadService, asrService, ttsService, agentService);
    }

    @Test
    void testProcessTextInput_Success() throws Exception {
        // Arrange
        String sessionId = "test-session-1";
        String text = "Hello";
        String llmResponse = "Hi there!";
        byte[] ttsAudio = new byte[]{1, 2, 3, 4};
        
        // Mock AgentService response (backed by AgentScope ReActAgent)
        when(agentService.chat(eq(sessionId), eq(text))).thenReturn(llmResponse);
        
        // Mock TTS
        when(ttsService.synthesize(eq(llmResponse), isNull())).thenReturn(createTempFile(ttsAudio));
        
        List<Map<String, Object>> responses = new ArrayList<>();
        OrchestrationService.ResponseCallback callback = (sid, type, data) -> {
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sid);
            response.put("type", type);
            response.put("data", data);
            responses.add(response);
        };

        // Act
        orchestrationService.processTextInput(sessionId, text, callback);

        // Assert
        assertEquals(3, responses.size());
        
        // Check ASR result response
        assertEquals("asr_result", responses.get(0).get("type"));
        Map<String, Object> asrData = (Map<String, Object>) responses.get(0).get("data");
        assertEquals(text, asrData.get("text"));
        
        // Check LLM response
        assertEquals("llm_response", responses.get(1).get("type"));
        Map<String, Object> llmData = (Map<String, Object>) responses.get(1).get("data");
        assertEquals(llmResponse, llmData.get("text"));
        
        // Check TTS audio
        assertEquals("tts_audio", responses.get(2).get("type"));
        Map<String, Object> ttsData = (Map<String, Object>) responses.get(2).get("data");
        assertNotNull(ttsData.get("audio"));
        
        // Verify AgentService was called
        verify(agentService, times(1)).chat(eq(sessionId), eq(text));
    }

    @Test
    void testProcessAudioStream_DetectsVoiceActivity() throws Exception {
        // Arrange
        String sessionId = "test-session-2";
        byte[] audioData = new byte[1024];
        
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("isSpeaking", true);
        when(vadService.detect(anyString(), eq(sessionId))).thenReturn(vadResult);
        
        List<Map<String, Object>> responses = new ArrayList<>();
        OrchestrationService.ResponseCallback callback = (sid, type, data) -> {
            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            responses.add(response);
        };

        // Act
        orchestrationService.processAudioStream(sessionId, audioData, callback);

        // Assert
        verify(vadService, times(1)).detect(anyString(), eq(sessionId));
        // Should not process speech yet as it's still speaking
        assertEquals(0, responses.size());
    }

    @Test
    void testCleanupSession() {
        // Arrange
        String sessionId = "test-session-3";

        // Act
        orchestrationService.cleanupSession(sessionId);

        // Assert
        verify(agentService, times(1)).clearSession(sessionId);
        assertDoesNotThrow(() -> orchestrationService.cleanupSession(sessionId));
    }

    @Test
    void testProcessTextInput_WithAgentError() throws Exception {
        // Arrange
        String sessionId = "test-session-4";
        String text = "Hello";
        
        // Mock AgentService error (AgentScope agent call fails)
        when(agentService.chat(eq(sessionId), eq(text))).thenThrow(new RuntimeException("Agent error"));
        
        List<Map<String, Object>> responses = new ArrayList<>();
        OrchestrationService.ResponseCallback callback = (sid, type, data) -> {
            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            response.put("data", data);
            responses.add(response);
        };

        // Act
        orchestrationService.processTextInput(sessionId, text, callback);

        // Assert
        assertTrue(responses.stream().anyMatch(r -> "error".equals(r.get("type"))));
    }

    private java.io.File createTempFile(byte[] data) throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("test-audio", ".wav");
        tempFile.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(data);
        }
        return tempFile;
    }
}
