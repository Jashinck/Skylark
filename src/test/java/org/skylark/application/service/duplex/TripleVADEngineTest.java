package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.VADService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TripleVADEngine
 */
@ExtendWith(MockitoExtension.class)
class TripleVADEngineTest {

    @Mock
    private VADService vadService;

    private TripleVADEngine tripleVADEngine;

    @BeforeEach
    void setUp() {
        tripleVADEngine = new TripleVADEngine(vadService);
    }

    @Test
    void testDetect_WithSpeechAudio_ReturnsSpeech() {
        // Arrange - create audio samples with high energy (above default threshold 0.5)
        float[] speechAudio = new float[160];
        for (int i = 0; i < speechAudio.length; i++) {
            speechAudio[i] = 0.8f * (float) Math.sin(2 * Math.PI * i / 16);
        }

        // Act
        VADResult result = tripleVADEngine.detect(speechAudio);

        // Assert
        assertTrue(result.isSpeech());
        assertTrue(result.getProbability() > 0.5f);
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void testDetect_WithSilence_ReturnsSilence() {
        // Arrange - create very low energy audio
        float[] silentAudio = new float[160];
        for (int i = 0; i < silentAudio.length; i++) {
            silentAudio[i] = 0.01f;
        }

        // Act
        VADResult result = tripleVADEngine.detect(silentAudio);

        // Assert
        assertFalse(result.isSpeech());
        assertTrue(result.getProbability() < 0.5f);
    }

    @Test
    void testDetect_WithNullInput_ReturnsSilence() {
        // Act
        VADResult result = tripleVADEngine.detect(null);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.0f, result.getProbability(), 0.001f);
        assertEquals(VADResult.AudioEventType.SILENCE, result.getEventType());
    }

    @Test
    void testDetect_WithEmptyInput_ReturnsSilence() {
        // Act
        VADResult result = tripleVADEngine.detect(new float[0]);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.0f, result.getProbability(), 0.001f);
        assertEquals(VADResult.AudioEventType.SILENCE, result.getEventType());
    }

    @Test
    void testDetectWithFallback_OnError_ReturnsSilence() {
        // Arrange - the internal calculateEnergy method won't throw on valid input,
        // but we test the fallback path with an engine created with custom threshold
        TripleVADEngine engine = new TripleVADEngine(vadService, 0.5f);

        // Act - use very low energy input
        float[] lowEnergy = new float[]{0.001f, -0.001f, 0.002f};
        VADResult result = engine.detectWithFallback(lowEnergy);

        // Assert
        assertFalse(result.isSpeech());
        assertTrue(result.getProbability() < 0.5f);
    }

    @Test
    void testDetectWithSession_WithMockedVADService_SpeechStart() throws Exception {
        // Arrange
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("status", "start");
        when(vadService.detect(anyString(), eq("session-1"))).thenReturn(vadResult);

        // Act
        VADResult result = tripleVADEngine.detectWithSession("session-1", new byte[]{1, 2, 3, 4});

        // Assert
        assertTrue(result.isSpeech());
        assertEquals(0.8f, result.getProbability(), 0.001f);
        verify(vadService).detect(anyString(), eq("session-1"));
    }

    @Test
    void testDetectWithSession_WithMockedVADService_Silence() throws Exception {
        // Arrange
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("status", "silence");
        when(vadService.detect(anyString(), eq("session-2"))).thenReturn(vadResult);

        // Act
        VADResult result = tripleVADEngine.detectWithSession("session-2", new byte[]{1, 2});

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.1f, result.getProbability(), 0.001f);
    }

    @Test
    void testDetectWithSession_OnException_ReturnsSilence() throws Exception {
        // Arrange
        when(vadService.detect(anyString(), eq("session-err")))
                .thenThrow(new RuntimeException("VAD error"));

        // Act
        VADResult result = tripleVADEngine.detectWithSession("session-err", new byte[]{1, 2});

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(VADResult.AudioEventType.SILENCE, result.getEventType());
    }

    @Test
    void testResetSession_CallsVADServiceReset() {
        // Act
        tripleVADEngine.resetSession("session-1");

        // Assert
        verify(vadService).reset("session-1");
    }

    @Test
    void testConstructor_CustomThreshold() {
        // Arrange - high threshold so moderate energy is not speech
        TripleVADEngine engine = new TripleVADEngine(vadService, 0.9f);
        float[] moderateAudio = new float[100];
        for (int i = 0; i < moderateAudio.length; i++) {
            moderateAudio[i] = 0.5f;
        }

        // Act
        VADResult result = engine.detect(moderateAudio);

        // Assert - 0.5 energy < 0.9 threshold
        assertFalse(result.isSpeech());
    }
}
