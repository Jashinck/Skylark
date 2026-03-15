package org.skylark.application.service.duplex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VADResult
 */
class VADResultTest {

    @Test
    void testConstructor_ThreeArgs_DefaultsToSpeechEventType() {
        // Act
        VADResult result = new VADResult(true, 0.95f, 1000L);

        // Assert
        assertTrue(result.isSpeech());
        assertEquals(0.95f, result.getProbability(), 0.001f);
        assertEquals(1000L, result.getTimestamp());
        assertEquals(VADResult.AudioEventType.SPEECH, result.getEventType());
    }

    @Test
    void testConstructor_FourArgs_CustomEventType() {
        // Act
        VADResult result = new VADResult(false, 0.1f, 2000L, VADResult.AudioEventType.MUSIC);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.1f, result.getProbability(), 0.001f);
        assertEquals(2000L, result.getTimestamp());
        assertEquals(VADResult.AudioEventType.MUSIC, result.getEventType());
    }

    @Test
    void testSilence_ReturnsNonSpeechWithZeroProbability() {
        // Act
        VADResult result = VADResult.silence();

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.0f, result.getProbability(), 0.001f);
        assertEquals(VADResult.AudioEventType.SILENCE, result.getEventType());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void testNonSpeechEvent_WithMusicType() {
        // Act
        VADResult result = VADResult.nonSpeechEvent(VADResult.AudioEventType.MUSIC);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(0.0f, result.getProbability(), 0.001f);
        assertEquals(VADResult.AudioEventType.MUSIC, result.getEventType());
    }

    @Test
    void testNonSpeechEvent_WithEchoResidual() {
        // Act
        VADResult result = VADResult.nonSpeechEvent(VADResult.AudioEventType.ECHO_RESIDUAL);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(VADResult.AudioEventType.ECHO_RESIDUAL, result.getEventType());
    }

    @Test
    void testNonSpeechEvent_WithSinging() {
        // Act
        VADResult result = VADResult.nonSpeechEvent(VADResult.AudioEventType.SINGING);

        // Assert
        assertFalse(result.isSpeech());
        assertEquals(VADResult.AudioEventType.SINGING, result.getEventType());
    }

    @Test
    void testIsSpeech_TrueForSpeech() {
        VADResult result = new VADResult(true, 0.8f, 100L);
        assertTrue(result.isSpeech());
    }

    @Test
    void testIsSpeech_FalseForNonSpeech() {
        VADResult result = new VADResult(false, 0.2f, 100L);
        assertFalse(result.isSpeech());
    }

    @Test
    void testGetProbability_ReturnsCorrectValue() {
        VADResult result = new VADResult(true, 0.75f, 100L);
        assertEquals(0.75f, result.getProbability(), 0.001f);
    }

    @Test
    void testGetTimestamp_ReturnsCorrectValue() {
        VADResult result = new VADResult(true, 0.5f, 12345L);
        assertEquals(12345L, result.getTimestamp());
    }

    @Test
    void testToString_ContainsAllFields() {
        // Arrange
        VADResult result = new VADResult(true, 0.95f, 1000L, VADResult.AudioEventType.SPEECH);

        // Act
        String str = result.toString();

        // Assert
        assertTrue(str.contains("speech=true"));
        assertTrue(str.contains("0.950"));
        assertTrue(str.contains("SPEECH"));
        assertTrue(str.contains("1000"));
    }

    @Test
    void testToString_SilenceResult() {
        // Arrange
        VADResult result = VADResult.silence();

        // Act
        String str = result.toString();

        // Assert
        assertTrue(str.contains("speech=false"));
        assertTrue(str.contains("0.000"));
        assertTrue(str.contains("SILENCE"));
    }

    // --- AudioEventType enum tests ---

    @Test
    void testAudioEventType_AllValuesExist() {
        VADResult.AudioEventType[] types = VADResult.AudioEventType.values();
        assertEquals(6, types.length);
    }

    @Test
    void testAudioEventType_ValueOf() {
        assertEquals(VADResult.AudioEventType.SPEECH, VADResult.AudioEventType.valueOf("SPEECH"));
        assertEquals(VADResult.AudioEventType.SILENCE, VADResult.AudioEventType.valueOf("SILENCE"));
        assertEquals(VADResult.AudioEventType.MUSIC, VADResult.AudioEventType.valueOf("MUSIC"));
        assertEquals(VADResult.AudioEventType.SINGING, VADResult.AudioEventType.valueOf("SINGING"));
        assertEquals(VADResult.AudioEventType.ECHO_RESIDUAL, VADResult.AudioEventType.valueOf("ECHO_RESIDUAL"));
        assertEquals(VADResult.AudioEventType.UNKNOWN, VADResult.AudioEventType.valueOf("UNKNOWN"));
    }
}
