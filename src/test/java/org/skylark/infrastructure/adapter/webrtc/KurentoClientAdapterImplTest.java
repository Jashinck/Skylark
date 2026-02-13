package org.skylark.infrastructure.adapter.webrtc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.lenient;

/**
 * Unit tests for KurentoClientAdapterImpl
 * KurentoClientAdapterImpl 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class KurentoClientAdapterImplTest {
    
    @Mock
    private KurentoClient kurentoClient;
    
    @Mock
    private MediaPipeline mediaPipeline;
    
    @Mock
    private WebRtcEndpoint.Builder endpointBuilder;
    
    @Mock
    private WebRTCProperties webRTCProperties;
    
    @Mock
    private WebRTCProperties.Kurento kurentoConfig;
    
    @Mock
    private WebRTCProperties.Stun stunConfig;
    
    @Mock
    private WebRTCProperties.Turn turnConfig;
    
    private KurentoClientAdapterImpl adapter;
    
    @BeforeEach
    void setUp() {
        // Mock configuration objects with lenient mode
        lenient().when(webRTCProperties.getKurento()).thenReturn(kurentoConfig);
        lenient().when(webRTCProperties.getStun()).thenReturn(stunConfig);
        lenient().when(webRTCProperties.getTurn()).thenReturn(turnConfig);
        lenient().when(kurentoConfig.getWsUri()).thenReturn("ws://localhost:8888/kurento");
        lenient().when(stunConfig.getServer()).thenReturn("stun:stun.l.google.com:19302");
        lenient().when(turnConfig.isEnabled()).thenReturn(false);
        
        adapter = new KurentoClientAdapterImpl(webRTCProperties);
        // Inject mock kurentoClient using reflection
        ReflectionTestUtils.setField(adapter, "kurentoClient", kurentoClient);
    }
    
    @Test
    void testCreateMediaPipeline_KurentoClientNull_ThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(adapter, "kurentoClient", null);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> adapter.createMediaPipeline()
        );
        assertTrue(exception.getMessage().contains("Kurento Client is not initialized"));
    }
    
    @Test
    void testCreateMediaPipeline_Success_ReturnsPipeline() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(mediaPipeline.getId()).thenReturn("pipeline-123");
        
        // Act
        MediaPipeline result = adapter.createMediaPipeline();
        
        // Assert
        assertNotNull(result);
        assertEquals(mediaPipeline, result);
        verify(kurentoClient, times(1)).createMediaPipeline();
    }
    
    @Test
    void testCreateWebRTCEndpoint_NullPipeline_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.createWebRTCEndpoint(null)
        );
        assertEquals("Media pipeline cannot be null", exception.getMessage());
    }
    
    @Test
    void testReleaseMediaPipeline_ExistingPipeline_ReleasesSuccessfully() {
        // Arrange
        when(kurentoClient.createMediaPipeline()).thenReturn(mediaPipeline);
        when(mediaPipeline.getId()).thenReturn("pipeline-123");
        
        MediaPipeline createdPipeline = adapter.createMediaPipeline();
        
        // Act
        adapter.releaseMediaPipeline("pipeline-123");
        
        // Assert
        verify(mediaPipeline, times(1)).release();
    }
    
    @Test
    void testReleaseMediaPipeline_NonExistentPipeline_DoesNotThrowError() {
        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> adapter.releaseMediaPipeline("non-existent-id"));
    }
    
    @Test
    void testDestroy_ReleasesAllPipelines() {
        // Arrange
        MediaPipeline pipeline1 = mock(MediaPipeline.class);
        MediaPipeline pipeline2 = mock(MediaPipeline.class);
        
        when(kurentoClient.createMediaPipeline())
            .thenReturn(pipeline1)
            .thenReturn(pipeline2);
        when(pipeline1.getId()).thenReturn("pipeline-1");
        when(pipeline2.getId()).thenReturn("pipeline-2");
        
        adapter.createMediaPipeline();
        adapter.createMediaPipeline();
        
        // Act
        adapter.destroy();
        
        // Assert
        verify(pipeline1, times(1)).release();
        verify(pipeline2, times(1)).release();
        verify(kurentoClient, times(1)).destroy();
    }
    
    @Test
    void testDestroy_KurentoClientNull_DoesNotThrowError() {
        // Arrange
        ReflectionTestUtils.setField(adapter, "kurentoClient", null);
        
        // Act & Assert
        assertDoesNotThrow(() -> adapter.destroy());
    }
    
    @Test
    void testIsConnected_KurentoClientNotNull_ReturnsTrue() {
        // Act & Assert
        assertTrue(adapter.isConnected());
    }
    
    @Test
    void testIsConnected_KurentoClientNull_ReturnsFalse() {
        // Arrange
        ReflectionTestUtils.setField(adapter, "kurentoClient", null);
        
        // Act & Assert
        assertFalse(adapter.isConnected());
    }
}
