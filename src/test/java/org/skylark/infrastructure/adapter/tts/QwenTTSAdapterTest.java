package org.skylark.infrastructure.adapter.tts;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QwenTTSAdapter
 */
class QwenTTSAdapterTest {

    private static final String DEFAULT_MODEL = "cosyvoice-v1";
    private static final String DEFAULT_VOICE = "longxiaochun";

    // --- Constructor / configuration tests ---

    @Test
    void testConstructor_ValidConfig_CreatesAdapter() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-api-key");

        QwenTTSAdapter adapter = new QwenTTSAdapter(config);

        assertNotNull(adapter);
        assertEquals(DEFAULT_MODEL, adapter.getModel());
        assertEquals(DEFAULT_VOICE, adapter.getVoice());
    }

    @Test
    void testConstructor_WithCustomVoice_UsesCustomVoice() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-key");
        config.put("voice", "longxiaoxia");

        QwenTTSAdapter adapter = new QwenTTSAdapter(config);

        assertEquals("longxiaoxia", adapter.getVoice());
    }

    @Test
    void testConstructor_WithCustomModel_UsesCustomModel() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-key");
        config.put("model", "cosyvoice-v2");

        QwenTTSAdapter adapter = new QwenTTSAdapter(config);

        assertEquals("cosyvoice-v2", adapter.getModel());
    }

    @Test
    void testConstructor_WithCustomServiceUrl_UsesCustomUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-key");
        config.put("serviceUrl", "https://custom.tts.endpoint/v1");

        QwenTTSAdapter adapter = new QwenTTSAdapter(config);

        assertEquals("https://custom.tts.endpoint/v1", adapter.getServiceUrl());
    }

    @Test
    void testConstructor_MissingApiKey_ThrowsIllegalArgumentException() {
        Map<String, Object> config = new HashMap<>();
        config.put("voice", "longxiaochun");

        assertThrows(IllegalArgumentException.class, () -> new QwenTTSAdapter(config));
    }

    @Test
    void testConstructor_NullConfig_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new QwenTTSAdapter(null));
    }

    @Test
    void testConstructor_EmptyConfig_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new QwenTTSAdapter(new HashMap<>()));
    }

    // --- synthesize() input validation (no network calls) ---

    @Test
    void testSynthesize_NullText_ThrowsException() {
        QwenTTSAdapter adapter = buildAdapter();

        Exception ex = assertThrows(Exception.class, () -> adapter.synthesize(null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSynthesize_EmptyText_ThrowsException() {
        QwenTTSAdapter adapter = buildAdapter();

        Exception ex = assertThrows(Exception.class, () -> adapter.synthesize(""));
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSynthesize_BlankText_ThrowsException() {
        QwenTTSAdapter adapter = buildAdapter();

        Exception ex = assertThrows(Exception.class, () -> adapter.synthesize("   "));
        assertNotNull(ex.getMessage());
    }

    // --- Getter tests ---

    @Test
    void testGetModel_DefaultModel() {
        QwenTTSAdapter adapter = buildAdapter();
        assertEquals(DEFAULT_MODEL, adapter.getModel());
    }

    @Test
    void testGetVoice_DefaultVoice() {
        QwenTTSAdapter adapter = buildAdapter();
        assertEquals(DEFAULT_VOICE, adapter.getVoice());
    }

    @Test
    void testGetServiceUrl_DefaultUrl_StartsWithHttps() {
        QwenTTSAdapter adapter = buildAdapter();
        assertNotNull(adapter.getServiceUrl());
        assertTrue(adapter.getServiceUrl().startsWith("https://"));
    }

    @Test
    void testConstructor_WithSampleRate_DoesNotThrow() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "key");
        config.put("sampleRate", "24000");

        assertDoesNotThrow(() -> new QwenTTSAdapter(config));
    }

    @Test
    void testConstructor_WithTimeout_DoesNotThrow() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "key");
        config.put("timeout", "120");

        assertDoesNotThrow(() -> new QwenTTSAdapter(config));
    }

    // --- Helper ---

    private QwenTTSAdapter buildAdapter() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-api-key");
        return new QwenTTSAdapter(config);
    }
}
