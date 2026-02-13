package org.skylark.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebRTCProperties
 * WebRTCProperties 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
class WebRTCPropertiesTest {
    
    @Test
    void testDefaultValues() {
        // Arrange & Act
        WebRTCProperties properties = new WebRTCProperties();
        
        // Assert - Kurento defaults
        assertNotNull(properties.getKurento());
        assertEquals("ws://localhost:8888/kurento", properties.getKurento().getWsUri());
        
        // Assert - STUN defaults
        assertNotNull(properties.getStun());
        assertEquals("stun:stun.l.google.com:19302", properties.getStun().getServer());
        
        // Assert - TURN defaults
        assertNotNull(properties.getTurn());
        assertFalse(properties.getTurn().isEnabled());
        assertEquals("", properties.getTurn().getServer());
        assertEquals("", properties.getTurn().getUsername());
        assertEquals("", properties.getTurn().getPassword());
        assertEquals("udp", properties.getTurn().getTransport());
    }
    
    @Test
    void testKurentoConfiguration() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        
        // Act
        properties.getKurento().setWsUri("ws://custom-server:8888/kurento");
        
        // Assert
        assertEquals("ws://custom-server:8888/kurento", properties.getKurento().getWsUri());
    }
    
    @Test
    void testStunConfiguration() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        
        // Act
        properties.getStun().setServer("stun:custom-stun.example.com:3478");
        
        // Assert
        assertEquals("stun:custom-stun.example.com:3478", properties.getStun().getServer());
    }
    
    @Test
    void testTurnEnabled() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        
        // Act
        turn.setEnabled(true);
        turn.setServer("turn.example.com:3478");
        turn.setUsername("testuser");
        turn.setPassword("testpass");
        turn.setTransport("tcp");
        
        // Assert
        assertTrue(turn.isEnabled());
        assertEquals("turn.example.com:3478", turn.getServer());
        assertEquals("testuser", turn.getUsername());
        assertEquals("testpass", turn.getPassword());
        assertEquals("tcp", turn.getTransport());
    }
    
    @Test
    void testTurnDisabled() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        
        // Act
        properties.getTurn().setEnabled(false);
        
        // Assert
        assertFalse(properties.getTurn().isEnabled());
    }
    
    @Test
    void testGetTurnUrl_WithoutPrefix() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer("turn.example.com:3478");
        turn.setTransport("tcp");
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertEquals("turn:turn.example.com:3478?transport=tcp", turnUrl);
    }
    
    @Test
    void testGetTurnUrl_WithPrefix() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer("turn:turn.example.com:3478");
        turn.setTransport("udp");
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertEquals("turn:turn.example.com:3478?transport=udp", turnUrl);
    }
    
    @Test
    void testGetTurnUrl_EmptyServer_ReturnsNull() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer("");
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertNull(turnUrl);
    }
    
    @Test
    void testGetTurnUrl_NullServer_ReturnsNull() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer(null);
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertNull(turnUrl);
    }
    
    @Test
    void testGetTurnUrl_WhitespaceServer_ReturnsNull() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer("   ");
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertNull(turnUrl);
    }
    
    @Test
    void testGetTurnUrl_NoTransport() {
        // Arrange
        WebRTCProperties properties = new WebRTCProperties();
        WebRTCProperties.Turn turn = properties.getTurn();
        turn.setServer("turn.example.com:3478");
        turn.setTransport("");
        
        // Act
        String turnUrl = turn.getTurnUrl();
        
        // Assert
        assertEquals("turn:turn.example.com:3478", turnUrl);
    }
}
