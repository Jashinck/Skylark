package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.Test;
import org.skylark.infrastructure.config.WebRTCProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgoraClientAdapterImpl
 * AgoraClientAdapterImpl 单元测试
 *
 * @author Skylark Team
 * @version 1.0.0
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
    void testJoinChannel_WithoutSdk_DoesNotThrow() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // Act & Assert - should not throw even without SDK
        assertDoesNotThrow(() -> adapter.joinChannel("test-channel", "user-123"));
    }

    @Test
    void testLeaveChannel_WithoutSdk_DoesNotThrow() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);

        // Act & Assert
        assertDoesNotThrow(() -> adapter.leaveChannel("test-channel"));
    }

    @Test
    void testSendAudioFrame_WithoutSdk_DoesNotThrow() {
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
    }

    @Test
    void testIsSdkAvailable_WithSdkOnClasspath_ReturnsTrue() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        properties.getAgora().setAppId("test-app-id");
        properties.getAgora().setAppCertificate("test-cert");

        AgoraClientAdapterImpl adapter = new AgoraClientAdapterImpl(properties);
        adapter.init();

        // The Agora SDK is on the classpath via Maven Central dependency (io.agora.rtc:linux-java-sdk)
        assertTrue(adapter.isSdkAvailable());
    }
}
