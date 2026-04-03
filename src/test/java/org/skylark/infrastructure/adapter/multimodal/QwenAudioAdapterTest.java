package org.skylark.infrastructure.adapter.multimodal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QwenAudioAdapter
 */
class QwenAudioAdapterTest {

    // --- Constructor tests ---

    @Test
    void testConstructor_Default_CreatesAdapter() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();

        assertNotNull(adapter);
        assertFalse(adapter.isAvailable()); // Phase 3 placeholder
        assertFalse(adapter.isCloudMode()); // no apiKey → local mode
    }

    @Test
    void testConstructor_WithServerUrl_UsesCustomUrl() {
        QwenAudioAdapter adapter = new QwenAudioAdapter("http://custom:8000");

        assertEquals("http://custom:8000", adapter.getServerUrl());
        assertFalse(adapter.isCloudMode());
    }

    @Test
    void testConstructor_WithApiKey_EnablesCloudMode() {
        QwenAudioAdapter adapter = new QwenAudioAdapter("https://dashscope.aliyuncs.com/api", "sk-test");

        assertTrue(adapter.isCloudMode());
        assertEquals("https://dashscope.aliyuncs.com/api", adapter.getServerUrl());
    }

    @Test
    void testConstructor_WithNullApiKey_LocalMode() {
        QwenAudioAdapter adapter = new QwenAudioAdapter("http://localhost:7860", null);

        assertFalse(adapter.isCloudMode());
    }

    @Test
    void testConstructor_WithEmptyApiKey_LocalMode() {
        QwenAudioAdapter adapter = new QwenAudioAdapter("http://localhost:7860", "");

        assertFalse(adapter.isCloudMode());
    }

    // --- isAvailable() tests (Phase 3 placeholder: always false) ---

    @Test
    void testIsAvailable_PlaceholderReturnsFalse() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();
        assertFalse(adapter.isAvailable());
    }

    // --- classifyAudioEvent() tests (placeholder) ---

    @Test
    void testClassifyAudioEvent_ReturnsUnknown() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();
        byte[] audioChunk = new byte[320]; // 10ms at 16kHz 16-bit

        QwenAudioAdapter.AudioEventType result = adapter.classifyAudioEvent("session-1", audioChunk);

        assertEquals(QwenAudioAdapter.AudioEventType.UNKNOWN, result);
    }

    @Test
    void testClassifyAudioEvent_NullChunk_ReturnsUnknown() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();

        QwenAudioAdapter.AudioEventType result = adapter.classifyAudioEvent("session-1", null);

        assertEquals(QwenAudioAdapter.AudioEventType.UNKNOWN, result);
    }

    // --- processAudio() tests (placeholder, no-op) ---

    @Test
    void testProcessAudio_PlaceholderDoesNotThrow() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();
        byte[] audio = new byte[3200]; // 100ms at 16kHz 16-bit

        assertDoesNotThrow(() -> adapter.processAudio("session-1", audio, new QwenAudioAdapter.QwenAudioCallback() {
            @Override public void onTextToken(String text) {}
            @Override public void onAudioEvent(QwenAudioAdapter.AudioEventType type, float confidence) {}
            @Override public void onComplete() {}
            @Override public void onError(Exception e) {}
        }));
    }

    // --- AudioEventType enum tests ---

    @Test
    void testAudioEventType_AllValuesExist() {
        QwenAudioAdapter.AudioEventType[] types = QwenAudioAdapter.AudioEventType.values();
        assertTrue(types.length >= 7);
    }

    @Test
    void testAudioEventType_ContainsExpectedValues() {
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("SPEECH"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("MUSIC"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("ECHO"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("NOISE"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("SINGING"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("SILENCE"));
        assertNotNull(QwenAudioAdapter.AudioEventType.valueOf("UNKNOWN"));
    }
}
