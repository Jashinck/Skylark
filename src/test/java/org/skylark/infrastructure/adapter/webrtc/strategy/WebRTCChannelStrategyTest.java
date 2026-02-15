package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.LiveKitClientAdapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTC Channel Strategy implementations
 * WebRTC 通道策略实现的单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WebRTCChannelStrategyTest {
    
    @Mock
    private KurentoClientAdapter kurentoClient;
    
    @Mock
    private LiveKitClientAdapter liveKitClient;
    
    @Mock
    private MediaPipeline mediaPipeline;
    
    @Mock
    private WebRtcEndpoint webRtcEndpoint;
    
    // ========== WebSocket Strategy Tests ==========
    
    @Test
    void testWebSocketStrategy_GetStrategyName() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        assertEquals("websocket", strategy.getStrategyName());
    }
    
    @Test
    void testWebSocketStrategy_IsAlwaysAvailable() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        assertTrue(strategy.isAvailable());
    }
    
    @Test
    void testWebSocketStrategy_CreateSession() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        
        String sessionId = strategy.createSession("user-123");
        
        assertNotNull(sessionId);
        assertTrue(strategy.sessionExists(sessionId));
        assertEquals(1, strategy.getActiveSessionCount());
    }
    
    @Test
    void testWebSocketStrategy_ProcessOffer() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        String sessionId = strategy.createSession("user-123");
        
        String answer = strategy.processOffer(sessionId, "test-offer");
        
        assertNotNull(answer);
        assertTrue(answer.contains("v=0"));
    }
    
    @Test
    void testWebSocketStrategy_ProcessOffer_SessionNotFound() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        
        assertThrows(IllegalArgumentException.class,
            () -> strategy.processOffer("non-existent", "test-offer"));
    }
    
    @Test
    void testWebSocketStrategy_CloseSession() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        String sessionId = strategy.createSession("user-123");
        assertTrue(strategy.sessionExists(sessionId));
        
        strategy.closeSession(sessionId);
        
        assertFalse(strategy.sessionExists(sessionId));
        assertEquals(0, strategy.getActiveSessionCount());
    }
    
    @Test
    void testWebSocketStrategy_AddIceCandidate_NoOp() {
        WebSocketChannelStrategy strategy = new WebSocketChannelStrategy();
        String sessionId = strategy.createSession("user-123");
        
        // Should not throw
        assertDoesNotThrow(() ->
            strategy.addIceCandidate(sessionId, "candidate", "audio", 0));
    }
    
    // ========== Kurento Strategy Tests ==========
    
    @Test
    void testKurentoStrategy_GetStrategyName() {
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        assertEquals("kurento", strategy.getStrategyName());
    }
    
    @Test
    void testKurentoStrategy_IsAvailable_Connected() {
        when(kurentoClient.isConnected()).thenReturn(true);
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        
        assertTrue(strategy.isAvailable());
    }
    
    @Test
    void testKurentoStrategy_IsAvailable_Disconnected() {
        when(kurentoClient.isConnected()).thenReturn(false);
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        
        assertFalse(strategy.isAvailable());
    }
    
    @Test
    void testKurentoStrategy_CreateSession() {
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        String sessionId = strategy.createSession("user-123");
        
        assertNotNull(sessionId);
        assertTrue(strategy.sessionExists(sessionId));
        assertEquals(1, strategy.getActiveSessionCount());
        verify(kurentoClient).createMediaPipeline();
        verify(kurentoClient).createWebRTCEndpoint(mediaPipeline);
    }
    
    @Test
    void testKurentoStrategy_ProcessOffer() {
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        when(webRtcEndpoint.processOffer("test-offer")).thenReturn("test-answer");
        
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        String sessionId = strategy.createSession("user-123");
        
        String answer = strategy.processOffer(sessionId, "test-offer");
        
        assertEquals("test-answer", answer);
        verify(webRtcEndpoint).processOffer("test-offer");
        verify(webRtcEndpoint).gatherCandidates();
    }
    
    @Test
    void testKurentoStrategy_ProcessOffer_SessionNotFound() {
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        
        assertThrows(IllegalArgumentException.class,
            () -> strategy.processOffer("non-existent", "test-offer"));
    }
    
    @Test
    void testKurentoStrategy_CloseSession() {
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        KurentoChannelStrategy strategy = new KurentoChannelStrategy(kurentoClient);
        String sessionId = strategy.createSession("user-123");
        
        strategy.closeSession(sessionId);
        
        assertFalse(strategy.sessionExists(sessionId));
        verify(webRtcEndpoint).release();
        verify(mediaPipeline).release();
    }
    
    // ========== LiveKit Strategy Tests ==========
    
    @Test
    void testLiveKitStrategy_GetStrategyName() {
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        assertEquals("livekit", strategy.getStrategyName());
    }
    
    @Test
    void testLiveKitStrategy_IsAvailable_Connected() {
        when(liveKitClient.isConnected()).thenReturn(true);
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        
        assertTrue(strategy.isAvailable());
    }
    
    @Test
    void testLiveKitStrategy_IsAvailable_Disconnected() {
        when(liveKitClient.isConnected()).thenReturn(false);
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        
        assertFalse(strategy.isAvailable());
    }
    
    @Test
    void testLiveKitStrategy_CreateSession() {
        when(liveKitClient.createRoom(anyString())).thenReturn("skylark-room");
        when(liveKitClient.generateToken(anyString(), eq("user-123"))).thenReturn("test-token");
        when(liveKitClient.getServerUrl()).thenReturn("wss://livekit.example.com");
        
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        String sessionId = strategy.createSession("user-123");
        
        assertNotNull(sessionId);
        assertTrue(strategy.sessionExists(sessionId));
        assertEquals(1, strategy.getActiveSessionCount());
        verify(liveKitClient).createRoom(anyString());
        verify(liveKitClient).generateToken(anyString(), eq("user-123"));
    }
    
    @Test
    void testLiveKitStrategy_ProcessOffer_ReturnsConnectionInfo() {
        when(liveKitClient.createRoom(anyString())).thenReturn("skylark-room");
        when(liveKitClient.generateToken(anyString(), eq("user-123"))).thenReturn("test-jwt-token");
        when(liveKitClient.getServerUrl()).thenReturn("wss://livekit.example.com");
        
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        String sessionId = strategy.createSession("user-123");
        
        String result = strategy.processOffer(sessionId, "ignored-sdp-offer");
        
        assertNotNull(result);
        assertTrue(result.contains("test-jwt-token"));
        assertTrue(result.contains("wss://livekit.example.com"));
    }
    
    @Test
    void testLiveKitStrategy_ProcessOffer_SessionNotFound() {
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        
        assertThrows(IllegalArgumentException.class,
            () -> strategy.processOffer("non-existent", "test-offer"));
    }
    
    @Test
    void testLiveKitStrategy_AddIceCandidate_NoOp() {
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        
        // Should not throw - LiveKit handles ICE internally
        assertDoesNotThrow(() ->
            strategy.addIceCandidate("any-session", "candidate", "audio", 0));
    }
    
    @Test
    void testLiveKitStrategy_CloseSession() {
        when(liveKitClient.createRoom(anyString())).thenReturn("skylark-room");
        when(liveKitClient.generateToken(anyString(), eq("user-123"))).thenReturn("test-token");
        when(liveKitClient.getServerUrl()).thenReturn("wss://livekit.example.com");
        
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        String sessionId = strategy.createSession("user-123");
        
        strategy.closeSession(sessionId);
        
        assertFalse(strategy.sessionExists(sessionId));
        verify(liveKitClient).deleteRoom(anyString());
    }
    
    @Test
    void testLiveKitStrategy_GetSessionToken() {
        when(liveKitClient.createRoom(anyString())).thenReturn("skylark-room");
        when(liveKitClient.generateToken(anyString(), eq("user-123"))).thenReturn("test-jwt-token");
        when(liveKitClient.getServerUrl()).thenReturn("wss://livekit.example.com");
        
        LiveKitChannelStrategy strategy = new LiveKitChannelStrategy(liveKitClient);
        String sessionId = strategy.createSession("user-123");
        
        assertEquals("test-jwt-token", strategy.getSessionToken(sessionId));
        assertNull(strategy.getSessionToken("non-existent"));
    }
    
    // ========== Strategy Interface Contract Tests ==========
    
    @Test
    void testAllStrategies_SessionNotExists_ReturnsFalse() {
        WebSocketChannelStrategy wsStrategy = new WebSocketChannelStrategy();
        KurentoChannelStrategy kurentoStrategy = new KurentoChannelStrategy(kurentoClient);
        LiveKitChannelStrategy liveKitStrategy = new LiveKitChannelStrategy(liveKitClient);
        
        assertFalse(wsStrategy.sessionExists("non-existent"));
        assertFalse(kurentoStrategy.sessionExists("non-existent"));
        assertFalse(liveKitStrategy.sessionExists("non-existent"));
    }
    
    @Test
    void testAllStrategies_InitialSessionCount_Zero() {
        WebSocketChannelStrategy wsStrategy = new WebSocketChannelStrategy();
        KurentoChannelStrategy kurentoStrategy = new KurentoChannelStrategy(kurentoClient);
        LiveKitChannelStrategy liveKitStrategy = new LiveKitChannelStrategy(liveKitClient);
        
        assertEquals(0, wsStrategy.getActiveSessionCount());
        assertEquals(0, kurentoStrategy.getActiveSessionCount());
        assertEquals(0, liveKitStrategy.getActiveSessionCount());
    }
}
