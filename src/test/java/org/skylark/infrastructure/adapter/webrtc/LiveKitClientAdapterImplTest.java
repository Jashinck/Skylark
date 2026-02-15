package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.infrastructure.config.WebRTCProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LiveKitClientAdapterImpl
 * LiveKitClientAdapterImpl 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
class LiveKitClientAdapterImplTest {
    
    private WebRTCProperties webRTCProperties;
    
    @BeforeEach
    void setUp() {
        webRTCProperties = new WebRTCProperties();
    }
    
    @Test
    void testInit_EmptyUrl_NotConnected() {
        // Arrange - default empty URL
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        
        // Act
        adapter.init();
        
        // Assert
        assertFalse(adapter.isConnected());
    }
    
    @Test
    void testInit_EmptyCredentials_NotConnected() {
        // Arrange
        webRTCProperties.getLivekit().setUrl("wss://livekit.example.com");
        // API key and secret are empty by default
        
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        
        // Act
        adapter.init();
        
        // Assert
        assertFalse(adapter.isConnected());
    }
    
    @Test
    void testGetServerUrl() {
        // Arrange
        webRTCProperties.getLivekit().setUrl("wss://livekit.example.com");
        
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        
        // Act & Assert
        assertEquals("wss://livekit.example.com", adapter.getServerUrl());
    }
    
    @Test
    void testCreateRoom_NotInitialized_ThrowsException() {
        // Arrange
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        adapter.init(); // will not connect (empty config)
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> adapter.createRoom("test-room"));
    }
    
    @Test
    void testDeleteRoom_NotInitialized_GracefullyHandles() {
        // Arrange
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        adapter.init();
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> adapter.deleteRoom("test-room"));
    }
    
    @Test
    void testGenerateToken_NullCredentials_ThrowsException() {
        // Arrange
        webRTCProperties.getLivekit().setApiKey(null);
        webRTCProperties.getLivekit().setApiSecret(null);
        
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        
        // Act & Assert
        assertThrows(IllegalStateException.class,
            () -> adapter.generateToken("room", "participant"));
    }
    
    @Test
    void testGenerateToken_ValidCredentials_ReturnsJwt() {
        // Arrange
        webRTCProperties.getLivekit().setApiKey("test-api-key");
        webRTCProperties.getLivekit().setApiSecret("test-api-secret-that-is-long-enough-for-jwt");
        
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        
        // Act
        String token = adapter.generateToken("test-room", "test-participant");
        
        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT tokens have three parts separated by dots
        assertEquals(3, token.split("\\.").length);
    }
    
    @Test
    void testDestroy() {
        // Arrange
        LiveKitClientAdapterImpl adapter = new LiveKitClientAdapterImpl(webRTCProperties);
        adapter.init();
        
        // Act
        adapter.destroy();
        
        // Assert
        assertFalse(adapter.isConnected());
    }
}
