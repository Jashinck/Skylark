package org.skylark.infrastructure.adapter.asr;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QwenASRAdapter
 */
class QwenASRAdapterTest {

    private static final String DEFAULT_MODEL = "paraformer-realtime-v2";
    private static final String DEFAULT_URL = "https://dashscope.aliyuncs.com";

    // --- Constructor / configuration tests ---

    @Test
    void testConstructor_ValidConfig_CreatesAdapter() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-api-key");

        QwenASRAdapter adapter = new QwenASRAdapter(config);

        assertNotNull(adapter);
        assertEquals(DEFAULT_MODEL, adapter.getModel());
    }

    @Test
    void testConstructor_WithCustomModel_UsesCustomModel() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-key");
        config.put("model", "paraformer-realtime-8k-v2");

        QwenASRAdapter adapter = new QwenASRAdapter(config);

        assertEquals("paraformer-realtime-8k-v2", adapter.getModel());
    }

    @Test
    void testConstructor_WithCustomServiceUrl_UsesCustomUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-key");
        config.put("serviceUrl", "https://custom.asr.endpoint/v1");

        QwenASRAdapter adapter = new QwenASRAdapter(config);

        assertEquals("https://custom.asr.endpoint/v1", adapter.getServiceUrl());
    }

    @Test
    void testConstructor_MissingApiKey_ThrowsIllegalArgumentException() {
        Map<String, Object> config = new HashMap<>();
        config.put("model", "paraformer-realtime-v2");

        assertThrows(IllegalArgumentException.class, () -> new QwenASRAdapter(config));
    }

    @Test
    void testConstructor_NullConfig_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new QwenASRAdapter(null));
    }

    @Test
    void testConstructor_EmptyConfig_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new QwenASRAdapter(new HashMap<>()));
    }

    // --- recognize() input validation tests (no network calls) ---

    @Test
    void testRecognize_NullAudio_ThrowsException() {
        QwenASRAdapter adapter = buildAdapter();

        Exception ex = assertThrows(Exception.class, () -> adapter.recognize(null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void testRecognize_EmptyAudio_ThrowsException() {
        QwenASRAdapter adapter = buildAdapter();

        Exception ex = assertThrows(Exception.class, () -> adapter.recognize(new byte[0]));
        assertNotNull(ex.getMessage());
    }

    // --- Getter tests ---

    @Test
    void testGetModel_DefaultModel() {
        QwenASRAdapter adapter = buildAdapter();

        assertEquals(DEFAULT_MODEL, adapter.getModel());
    }

    @Test
    void testGetServiceUrl_DefaultUrl_StartsWithHttps() {
        QwenASRAdapter adapter = buildAdapter();

        assertNotNull(adapter.getServiceUrl());
        assertTrue(adapter.getServiceUrl().startsWith("https://"));
    }

    @Test
    void testGetModel_CustomModel() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "key");
        config.put("model", "custom-model-v3");

        QwenASRAdapter adapter = new QwenASRAdapter(config);

        assertEquals("custom-model-v3", adapter.getModel());
    }

    @Test
    void testConstructor_WithTimeout_UsesTimeout() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "key");
        config.put("timeout", "45");

        // Should not throw
        assertDoesNotThrow(() -> new QwenASRAdapter(config));
    }

    // --- Helper ---

    private QwenASRAdapter buildAdapter() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-api-key");
        return new QwenASRAdapter(config);
    }
}
