package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ServerAECProcessor
 */
class ServerAECProcessorTest {

    private ServerAECProcessor aecProcessor;

    @BeforeEach
    void setUp() {
        aecProcessor = new ServerAECProcessor();
    }

    @Test
    void testProcess_NullRefAudio_ReturnsMicAudioUnchanged() {
        // Arrange
        float[] micAudio = {0.5f, -0.3f, 0.8f};

        // Act
        float[] result = aecProcessor.process(micAudio, null);

        // Assert
        assertSame(micAudio, result);
        assertArrayEquals(new float[]{0.5f, -0.3f, 0.8f}, result, 0.001f);
    }

    @Test
    void testProcess_EmptyRefAudio_ReturnsMicAudioUnchanged() {
        // Arrange
        float[] micAudio = {0.1f, 0.2f};
        float[] refAudio = new float[0];

        // Act
        float[] result = aecProcessor.process(micAudio, refAudio);

        // Assert
        assertSame(micAudio, result);
    }

    @Test
    void testProcess_WithBothMicAndRef_ReturnsMicAudio() {
        // Arrange - Phase 1 pass-through
        float[] micAudio = {0.5f, -0.3f, 0.8f, -0.1f};
        float[] refAudio = {0.2f, -0.1f, 0.3f, -0.05f};

        // Act
        float[] result = aecProcessor.process(micAudio, refAudio);

        // Assert - Phase 1: pass-through returns mic audio
        assertSame(micAudio, result);
        assertArrayEquals(micAudio, result, 0.001f);
    }

    @Test
    void testProcess_NullMicAudio_ReturnsEmptyArray() {
        // Act
        float[] result = aecProcessor.process(null, new float[]{0.1f});

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testProcess_EmptyMicAudio_ReturnsEmptyArray() {
        // Act
        float[] result = aecProcessor.process(new float[0], new float[]{0.1f});

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testProcess_BothNull_ReturnsEmptyArray() {
        // Act
        float[] result = aecProcessor.process(null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // --- pcmBytesToFloatArray tests ---

    @Test
    void testPcmBytesToFloatArray_ValidInput_CorrectConversion() {
        // Arrange - 16-bit little-endian PCM
        // Sample value 256 = 0x0100 in LE: bytes [0x00, 0x01]
        byte[] pcmBytes = {0x00, 0x01};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertEquals(256 / 32768.0f, result[0], 0.0001f);
    }

    @Test
    void testPcmBytesToFloatArray_ZeroSample() {
        // Arrange
        byte[] pcmBytes = {0x00, 0x00};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertEquals(0.0f, result[0], 0.0001f);
    }

    @Test
    void testPcmBytesToFloatArray_MaxPositive() {
        // Arrange - Max positive value: 32767 = 0x7FFF, LE: [0xFF, 0x7F]
        byte[] pcmBytes = {(byte) 0xFF, 0x7F};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertTrue(result[0] > 0.99f);
        assertTrue(result[0] <= 1.0f);
    }

    @Test
    void testPcmBytesToFloatArray_MaxNegative() {
        // Arrange - -32768 = 0x8000, LE: [0x00, 0x80]
        byte[] pcmBytes = {0x00, (byte) 0x80};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertEquals(-1.0f, result[0], 0.0001f);
    }

    @Test
    void testPcmBytesToFloatArray_MultipleSamples() {
        // Arrange - two samples: [0, 256]
        byte[] pcmBytes = {0x00, 0x00, 0x00, 0x01};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(2, result.length);
        assertEquals(0.0f, result[0], 0.0001f);
        assertEquals(256 / 32768.0f, result[1], 0.0001f);
    }

    @Test
    void testPcmBytesToFloatArray_NullInput_ReturnsEmpty() {
        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPcmBytesToFloatArray_EmptyInput_ReturnsEmpty() {
        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(new byte[0]);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPcmBytesToFloatArray_SingleByte_ReturnsEmpty() {
        // Arrange - less than 2 bytes, cannot form a sample
        byte[] pcmBytes = {0x42};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testPcmBytesToFloatArray_OddBytes_TruncatesLastByte() {
        // Arrange - 3 bytes, only 1 complete sample
        byte[] pcmBytes = {0x00, 0x01, 0x42};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertEquals(256 / 32768.0f, result[0], 0.0001f);
    }

    @Test
    void testPcmBytesToFloatArray_NegativeSample() {
        // Arrange - -256 = 0xFF00, LE: [0x00, 0xFF]
        byte[] pcmBytes = {0x00, (byte) 0xFF};

        // Act
        float[] result = ServerAECProcessor.pcmBytesToFloatArray(pcmBytes);

        // Assert
        assertEquals(1, result.length);
        assertTrue(result[0] < 0);
        assertEquals(-256 / 32768.0f, result[0], 0.0001f);
    }
}
