package org.skylark.application.service.duplex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DuplexConfig and DuplexMode
 */
class DuplexConfigTest {

    // --- DuplexMode.fromString() tests ---

    @Test
    void testFromString_Half_ReturnsHalf() {
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString("half"));
    }

    @Test
    void testFromString_BargeIn_ReturnsBargeIn() {
        assertEquals(DuplexConfig.DuplexMode.BARGE_IN, DuplexConfig.DuplexMode.fromString("barge-in"));
    }

    @Test
    void testFromString_Streaming_ReturnsStreaming() {
        assertEquals(DuplexConfig.DuplexMode.STREAMING, DuplexConfig.DuplexMode.fromString("streaming"));
    }

    @Test
    void testFromString_Full_ReturnsFull() {
        assertEquals(DuplexConfig.DuplexMode.FULL, DuplexConfig.DuplexMode.fromString("full"));
    }

    @Test
    void testFromString_CaseInsensitive() {
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString("HALF"));
        assertEquals(DuplexConfig.DuplexMode.BARGE_IN, DuplexConfig.DuplexMode.fromString("Barge-In"));
        assertEquals(DuplexConfig.DuplexMode.STREAMING, DuplexConfig.DuplexMode.fromString("STREAMING"));
        assertEquals(DuplexConfig.DuplexMode.FULL, DuplexConfig.DuplexMode.fromString("Full"));
    }

    @Test
    void testFromString_Null_ReturnsHalf() {
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString(null));
    }

    @Test
    void testFromString_InvalidValue_ReturnsHalf() {
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString("invalid"));
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString(""));
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.fromString("duplex"));
    }

    // --- Feature flags for HALF mode ---

    @Test
    void testHalf_BargeInDisabled() {
        assertFalse(DuplexConfig.DuplexMode.HALF.isBargeInEnabled());
    }

    @Test
    void testHalf_StreamingDisabled() {
        assertFalse(DuplexConfig.DuplexMode.HALF.isStreamingEnabled());
    }

    @Test
    void testHalf_FullDuplexDisabled() {
        assertFalse(DuplexConfig.DuplexMode.HALF.isFullDuplexEnabled());
    }

    // --- Feature flags for BARGE_IN mode ---

    @Test
    void testBargeIn_BargeInEnabled() {
        assertTrue(DuplexConfig.DuplexMode.BARGE_IN.isBargeInEnabled());
    }

    @Test
    void testBargeIn_StreamingDisabled() {
        assertFalse(DuplexConfig.DuplexMode.BARGE_IN.isStreamingEnabled());
    }

    @Test
    void testBargeIn_FullDuplexDisabled() {
        assertFalse(DuplexConfig.DuplexMode.BARGE_IN.isFullDuplexEnabled());
    }

    // --- Feature flags for STREAMING mode ---

    @Test
    void testStreaming_BargeInEnabled() {
        assertTrue(DuplexConfig.DuplexMode.STREAMING.isBargeInEnabled());
    }

    @Test
    void testStreaming_StreamingEnabled() {
        assertTrue(DuplexConfig.DuplexMode.STREAMING.isStreamingEnabled());
    }

    @Test
    void testStreaming_FullDuplexDisabled() {
        assertFalse(DuplexConfig.DuplexMode.STREAMING.isFullDuplexEnabled());
    }

    // --- Feature flags for FULL mode ---

    @Test
    void testFull_BargeInEnabled() {
        assertTrue(DuplexConfig.DuplexMode.FULL.isBargeInEnabled());
    }

    @Test
    void testFull_StreamingEnabled() {
        assertTrue(DuplexConfig.DuplexMode.FULL.isStreamingEnabled());
    }

    @Test
    void testFull_FullDuplexEnabled() {
        assertTrue(DuplexConfig.DuplexMode.FULL.isFullDuplexEnabled());
    }

    // --- Getter tests ---

    @Test
    void testGetValue_AllModes() {
        assertEquals("half", DuplexConfig.DuplexMode.HALF.getValue());
        assertEquals("barge-in", DuplexConfig.DuplexMode.BARGE_IN.getValue());
        assertEquals("streaming", DuplexConfig.DuplexMode.STREAMING.getValue());
        assertEquals("full", DuplexConfig.DuplexMode.FULL.getValue());
    }

    @Test
    void testGetDescription_AllModes() {
        assertNotNull(DuplexConfig.DuplexMode.HALF.getDescription());
        assertNotNull(DuplexConfig.DuplexMode.BARGE_IN.getDescription());
        assertNotNull(DuplexConfig.DuplexMode.STREAMING.getDescription());
        assertNotNull(DuplexConfig.DuplexMode.FULL.getDescription());
        assertFalse(DuplexConfig.DuplexMode.HALF.getDescription().isEmpty());
    }

    // --- Enum values ---

    @Test
    void testDuplexMode_AllValuesExist() {
        DuplexConfig.DuplexMode[] modes = DuplexConfig.DuplexMode.values();
        assertEquals(4, modes.length);
    }

    @Test
    void testDuplexMode_ValueOf() {
        assertEquals(DuplexConfig.DuplexMode.HALF, DuplexConfig.DuplexMode.valueOf("HALF"));
        assertEquals(DuplexConfig.DuplexMode.BARGE_IN, DuplexConfig.DuplexMode.valueOf("BARGE_IN"));
        assertEquals(DuplexConfig.DuplexMode.STREAMING, DuplexConfig.DuplexMode.valueOf("STREAMING"));
        assertEquals(DuplexConfig.DuplexMode.FULL, DuplexConfig.DuplexMode.valueOf("FULL"));
    }

    // --- Constants ---

    @Test
    void testModeConstants() {
        assertEquals("half", DuplexConfig.MODE_HALF);
        assertEquals("barge-in", DuplexConfig.MODE_BARGE_IN);
        assertEquals("streaming", DuplexConfig.MODE_STREAMING);
        assertEquals("full", DuplexConfig.MODE_FULL);
    }
}
