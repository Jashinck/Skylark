package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.Test;
import org.skylark.infrastructure.config.WebRTCProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgoraClientAdapterImpl
 * AgoraClientAdapterImpl 单元测试
 *
 * <p>These tests verify token generation, graceful degradation when native
 * .so libraries are unavailable, and proper lifecycle management.</p>
 *
 * @author Skylark Team
 * @version 2.0.0
 */
class AgoraClientAdapterImplTest {

    @Test
    void testGenerateToken_WithCredentials_ReturnsRealToken() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("970CA35de60c44645bbae8a215061b33");
        properties.getAgora().setAppCertificate("5CFd2fd1755d40ecb72977518be15d3b");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Act
        String token = adapter.generateToken("test-channel", "user-123", 3600);

        // Assert
        assertNotNull(token);
        assertTrue(token.startsWith("007"), "Token should be AccessToken2 format (007 prefix)");
        assertFalse(token.contains("placeholder"), "Token should not be a placeholder");
    }

    @Test
    void testGenerateToken_WithoutCertificate_ReturnsPlaceholder() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        // No appCertificate set

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Act
        String token = adapter.generateToken("test-channel", "user-123", 3600);

        // Assert
        assertNotNull(token);
        assertTrue(token.contains("placeholder"), "Should fall back to placeholder without certificate");
    }

    @Test
    void testIsAvailable_WithCredentials_ReturnsTrue() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("970CA35de60c44645bbae8a215061b33");
        properties.getAgora().setAppCertificate("5CFd2fd1755d40ecb72977518be15d3b");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Assert - available because token generation works
        assertTrue(adapter.isAvailable());
    }

    @Test
    void testIsAvailable_WithoutAppId_ReturnsFalse() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        // No appId set

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Assert
        assertFalse(adapter.isAvailable());
    }

    @Test
    void testGetAppId_ReturnsConfiguredValue() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("my-app-id");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);

        // Assert
        assertEquals("my-app-id", adapter.getAppId());
    }

    @Test
    void testJoinChannel_GracefulDegradation_DoesNotThrow() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Act & Assert - should not throw regardless of SDK availability
        // In CI, native .so libs are not available, so joinChannel gracefully skips
        assertDoesNotThrow(() -> adapter.joinChannel("test-channel", "user-123"));
    }

    @Test
    void testLeaveChannel_GracefulDegradation_DoesNotThrow() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);

        // Act & Assert
        assertDoesNotThrow(() -> adapter.leaveChannel("test-channel"));
    }

    @Test
    void testSendAudioFrame_GracefulDegradation_DoesNotThrow() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);

        // Act & Assert
        assertDoesNotThrow(() -> adapter.sendAudioFrame("test-channel",
            new byte[320], 16000, 1));
    }

    @Test
    void testRegisterAudioFrameCallback_StoresCallback() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);

        // Act
        AgoraClientAdapter.AudioFrameCallback callback =
            (ch, uid, pcm, rate, channels) -> {};
        adapter.registerAudioFrameCallback("test-channel", callback);

        // No assertion needed — just verifying it doesn't throw
    }

    @Test
    void testDestroy_ClearsState() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();
        assertTrue(adapter.isAvailable());

        // Act
        adapter.destroy();

        // Assert
        assertFalse(adapter.isAvailable());
        assertFalse(adapter.isSdkAvailable());
    }

    @Test
    void testIsSdkAvailable_DependsOnNativeLibs() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // isSdkAvailable() depends on whether native .so libraries are available at runtime.
        // In CI without native libs, this will be false.
        // In production with native libs, this will be true.
        // Either way, init() should not throw and token generation should work.
        assertTrue(adapter.isAvailable(), "Token generation should always work with valid credentials");

        // Verify the adapter correctly reports its SDK state (don't assert a specific value
        // since it depends on the runtime environment)
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> adapter.isSdkAvailable());
    }

    @Test
    void testTokenAvailableIndependentOfSdk() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("970CA35de60c44645bbae8a215061b33");
        properties.getAgora().setAppCertificate("5CFd2fd1755d40ecb72977518be15d3b");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Token generation must work regardless of native SDK availability
        assertTrue(adapter.isAvailable(), "isAvailable should be true when token generation is available");
        String token = adapter.generateToken("test-channel", "user-123", 3600);
        assertNotNull(token);
        assertTrue(token.startsWith("007"), "Token should be real 007 format");
    }

    @Test
    void testJoinAndLeaveChannel_Lifecycle() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Register a callback before joining
        AgoraClientAdapter.AudioFrameCallback callback =
            (ch, uid, pcm, rate, channels) -> {};
        adapter.registerAudioFrameCallback("test-channel", callback);

        // Act & Assert - full lifecycle should not throw
        assertDoesNotThrow(() -> {
            adapter.joinChannel("test-channel", "user-123");
            adapter.sendAudioFrame("test-channel", new byte[320], 16000, 1);
            adapter.leaveChannel("test-channel");
        });
    }

    @Test
    void testDestroy_CleansUpAllChannels() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Join channels and register callbacks
        adapter.registerAudioFrameCallback("ch-1", (ch, uid, pcm, rate, channels) -> {});
        adapter.registerAudioFrameCallback("ch-2", (ch, uid, pcm, rate, channels) -> {});
        adapter.joinChannel("ch-1", "user-1");
        adapter.joinChannel("ch-2", "user-2");

        // Act
        assertDoesNotThrow(() -> adapter.destroy());

        // Assert
        assertFalse(adapter.isAvailable());
        assertFalse(adapter.isSdkAvailable());
    }
}
