package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTCSession
 * WebRTCSession 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WebRTCSessionTest {
    
    @Mock
    private MediaPipeline mediaPipeline;
    
    @Mock
    private WebRtcEndpoint webRtcEndpoint;
    
    private static final String TEST_SESSION_ID = "test-session-123";
    private WebRTCSession session;
    
    @BeforeEach
    void setUp() {
        session = new WebRTCSession(TEST_SESSION_ID, mediaPipeline, webRtcEndpoint);
    }
    
    @Test
    void testConstructor_NullSessionId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new WebRTCSession(null, mediaPipeline, webRtcEndpoint)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testConstructor_EmptySessionId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new WebRTCSession("   ", mediaPipeline, webRtcEndpoint)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testConstructor_NullPipeline_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new WebRTCSession(TEST_SESSION_ID, null, webRtcEndpoint)
        );
        assertEquals("Media pipeline cannot be null", exception.getMessage());
    }
    
    @Test
    void testConstructor_NullEndpoint_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new WebRTCSession(TEST_SESSION_ID, mediaPipeline, null)
        );
        assertEquals("WebRTC endpoint cannot be null", exception.getMessage());
    }
    
    @Test
    void testProcessOffer_ActiveSession_ProcessesSuccessfully() {
        // Arrange
        String testOffer = "test-sdp-offer";
        String expectedAnswer = "test-sdp-answer";
        when(webRtcEndpoint.processOffer(testOffer)).thenReturn(expectedAnswer);
        
        // Act
        String result = session.processOffer(testOffer);
        
        // Assert
        assertEquals(expectedAnswer, result);
        assertTrue(session.isActive());
        verify(webRtcEndpoint, times(1)).processOffer(testOffer);
    }
    
    @Test
    void testProcessOffer_InactiveSession_ThrowsException() {
        // Arrange
        session.release(); // Make session inactive
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> session.processOffer("test-sdp-offer")
        );
        assertEquals("Session is not active", exception.getMessage());
        verify(webRtcEndpoint, never()).processOffer(anyString());
    }
    
    @Test
    void testAddIceCandidate_ActiveSession_AddsSuccessfully() {
        // Arrange
        IceCandidate candidate = new IceCandidate("candidate:test", "audio", 0);
        
        // Act
        session.addIceCandidate(candidate);
        
        // Assert
        assertTrue(session.isActive());
        verify(webRtcEndpoint, times(1)).addIceCandidate(candidate);
    }
    
    @Test
    void testAddIceCandidate_InactiveSession_DoesNotExecute() {
        // Arrange
        session.release(); // Make session inactive
        IceCandidate candidate = new IceCandidate("candidate:test", "audio", 0);
        
        // Act
        session.addIceCandidate(candidate);
        
        // Assert
        assertFalse(session.isActive());
        verify(webRtcEndpoint, never()).addIceCandidate(any(IceCandidate.class));
    }
    
    @Test
    void testRelease_ReleasesResourcesAndSetsInactive() {
        // Arrange
        assertTrue(session.isActive());
        
        // Act
        session.release();
        
        // Assert
        assertFalse(session.isActive());
        verify(webRtcEndpoint, times(1)).release();
        verify(mediaPipeline, times(1)).release();
    }
    
    @Test
    void testRelease_CalledMultipleTimes_IsIdempotent() {
        // Act
        session.release();
        session.release();
        session.release();
        
        // Assert
        assertFalse(session.isActive());
        // Should only release once
        verify(webRtcEndpoint, times(1)).release();
        verify(mediaPipeline, times(1)).release();
    }
    
    @Test
    void testGetters_ReturnCorrectValues() {
        // Act & Assert
        assertEquals(TEST_SESSION_ID, session.getSessionId());
        assertEquals(mediaPipeline, session.getPipeline());
        assertEquals(webRtcEndpoint, session.getWebRtcEndpoint());
        assertTrue(session.isActive());
    }
    
    @Test
    void testGatherCandidates_ActiveSession_GathersSuccessfully() {
        // Act
        session.gatherCandidates();
        
        // Assert
        verify(webRtcEndpoint, times(1)).gatherCandidates();
    }
    
    @Test
    void testGatherCandidates_InactiveSession_DoesNotExecute() {
        // Arrange
        session.release();
        
        // Act
        session.gatherCandidates();
        
        // Assert
        verify(webRtcEndpoint, never()).gatherCandidates();
    }
}
