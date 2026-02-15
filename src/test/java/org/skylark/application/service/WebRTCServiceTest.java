package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.infrastructure.adapter.webrtc.AudioProcessor;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.WebRTCSession;
import org.skylark.infrastructure.adapter.webrtc.strategy.KurentoChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebRTCChannelStrategy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTCService
 * WebRTCService 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WebRTCServiceTest {
    
    @Mock
    private KurentoClientAdapter kurentoClient;
    
    @Mock
    private VADService vadService;
    
    @Mock
    private ASRService asrService;
    
    @Mock
    private TTSService ttsService;
    
    @Mock
    private MediaPipeline mediaPipeline;
    
    @Mock
    private WebRtcEndpoint webRtcEndpoint;
    
    private WebRTCService webRTCService;
    
    @BeforeEach
    void setUp() {
        WebRTCChannelStrategy kurentoStrategy = new KurentoChannelStrategy(kurentoClient);
        webRTCService = new WebRTCService(kurentoClient, vadService, asrService, ttsService, kurentoStrategy);
    }
    
    @Test
    void testCreateSession_Success() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        // Act
        String sessionId = webRTCService.createSession("user-123");
        
        // Assert
        assertNotNull(sessionId);
        assertTrue(webRTCService.sessionExists(sessionId));
        assertEquals(1, webRTCService.getActiveSessionCount());
        verify(kurentoClient, times(1)).createMediaPipeline();
        verify(kurentoClient, times(1)).createWebRTCEndpoint(mediaPipeline);
    }
    
    @Test
    void testProcessOffer_SessionExists_ProcessesSuccessfully() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        String sessionId = webRTCService.createSession("user-123");
        String testOffer = "test-sdp-offer";
        String expectedAnswer = "test-sdp-answer";
        
        when(webRtcEndpoint.processOffer(testOffer)).thenReturn(expectedAnswer);
        
        // Act
        String result = webRTCService.processOffer(sessionId, testOffer);
        
        // Assert
        assertEquals(expectedAnswer, result);
        verify(webRtcEndpoint, times(1)).processOffer(testOffer);
        verify(webRtcEndpoint, times(1)).gatherCandidates();
    }
    
    @Test
    void testProcessOffer_SessionNotExists_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> webRTCService.processOffer("non-existent-session", "test-offer")
        );
        assertTrue(exception.getMessage().contains("Session not found"));
    }
    
    @Test
    void testAddIceCandidate_SessionNotExists_GracefullyHandles() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            webRTCService.addIceCandidate("non-existent-session", "candidate", "audio", 0)
        );
    }
    
    @Test
    void testProcessAudioData_ProcessorNotExists_ReturnsNull() {
        // Act
        String result = webRTCService.processAudioData("non-existent-session", new byte[1024]);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testProcessAudioData_ProcessorExists_ProcessesData() throws Exception {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        String sessionId = webRTCService.createSession("user-123");
        byte[] audioData = new byte[1024];
        
        Map<String, Object> vadResult = new HashMap<>();
        vadResult.put("status", "start");
        when(vadService.detect(anyString(), eq(sessionId))).thenReturn(vadResult);
        
        // Act
        String result = webRTCService.processAudioData(sessionId, audioData);
        
        // Assert
        assertEquals("start", result);
    }
    
    @Test
    void testCloseSession_ExistingSession_PerformsThreeLayerCleanup() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        String sessionId = webRTCService.createSession("user-123");
        assertTrue(webRTCService.sessionExists(sessionId));
        
        // Act
        webRTCService.closeSession(sessionId);
        
        // Assert - verify three-layer cleanup
        assertFalse(webRTCService.sessionExists(sessionId));
        assertEquals(0, webRTCService.getActiveSessionCount());
        
        // 1. Session release
        verify(webRtcEndpoint, times(1)).release();
        verify(mediaPipeline, times(1)).release();
        
        // 2. Processor reset (implicit through session cleanup)
        
        // 3. VAD service reset
        verify(vadService, times(1)).reset(sessionId);
    }
    
    @Test
    void testSessionExists_ExistingSession_ReturnsTrue() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        String sessionId = webRTCService.createSession("user-123");
        
        // Act & Assert
        assertTrue(webRTCService.sessionExists(sessionId));
    }
    
    @Test
    void testSessionExists_NonExistentSession_ReturnsFalse() {
        // Act & Assert
        assertFalse(webRTCService.sessionExists("non-existent-session"));
    }
    
    @Test
    void testGetActiveSessionCount_NoSessions_ReturnsZero() {
        // Act & Assert
        assertEquals(0, webRTCService.getActiveSessionCount());
    }
    
    @Test
    void testGetActiveSessionCount_MultipleSessions_ReturnsCorrectCount() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(kurentoClient.createWebRTCEndpoint(mediaPipeline)).thenReturn(webRtcEndpoint);
        
        webRTCService.createSession("user-1");
        webRTCService.createSession("user-2");
        webRTCService.createSession("user-3");
        
        // Act & Assert
        assertEquals(3, webRTCService.getActiveSessionCount());
    }
    
    @Test
    void testRecognizeSpeech_ProcessorNotExists_ReturnsNull() {
        // Act
        String result = webRTCService.recognizeSpeech("non-existent-session", new byte[1024]);
        
        // Assert
        assertNull(result);
    }
}
