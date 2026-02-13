package org.skylark.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skylark.application.dto.webrtc.CreateSessionRequest;
import org.skylark.application.dto.webrtc.IceCandidateRequest;
import org.skylark.application.dto.webrtc.SdpOfferRequest;
import org.skylark.application.service.OrchestrationService;
import org.skylark.application.service.WebRTCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC integration tests for Kurento WebRTC endpoints in RobotController
 * RobotController Kurento WebRTC 端点的 Spring MVC 集成测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@WebMvcTest(RobotController.class)
class RobotControllerKurentoTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private WebRTCService webRTCService;
    
    @MockBean
    private OrchestrationService orchestrationService;
    
    @Test
    void testCreateKurentoSession_Success() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");
        
        when(webRTCService.createSession("test-user-123"))
            .thenReturn("session-123");
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-123"))
            .andExpect(jsonPath("$.status").value("created"))
            .andExpect(jsonPath("$.message").value("Kurento WebRTC session created successfully"));
        
        verify(webRTCService, times(1)).createSession("test-user-123");
    }
    
    @Test
    void testCreateKurentoSession_ServiceError_Returns500() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");
        
        when(webRTCService.createSession(anyString()))
            .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.sessionId").isEmpty())
            .andExpect(jsonPath("$.status").value("error"));
    }
    
    @Test
    void testProcessOffer_Success() throws Exception {
        // Arrange
        String sessionId = "session-123";
        SdpOfferRequest request = new SdpOfferRequest();
        request.setSdpOffer("test-sdp-offer");
        
        when(webRTCService.processOffer(sessionId, "test-sdp-offer"))
            .thenReturn("test-sdp-answer");
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session/{sessionId}/offer", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sdpAnswer").value("test-sdp-answer"));
        
        verify(webRTCService, times(1)).processOffer(sessionId, "test-sdp-offer");
    }
    
    @Test
    void testProcessOffer_SessionNotFound_Returns404() throws Exception {
        // Arrange
        String sessionId = "non-existent-session";
        SdpOfferRequest request = new SdpOfferRequest();
        request.setSdpOffer("test-sdp-offer");
        
        when(webRTCService.processOffer(eq(sessionId), anyString()))
            .thenThrow(new IllegalArgumentException("Session not found"));
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session/{sessionId}/offer", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
        
        verify(webRTCService, times(1)).processOffer(eq(sessionId), anyString());
    }
    
    @Test
    void testAddIceCandidate_Success() throws Exception {
        // Arrange
        String sessionId = "session-123";
        IceCandidateRequest request = new IceCandidateRequest();
        request.setCandidate("candidate:test");
        request.setSdpMid("audio");
        request.setSdpMLineIndex(0);
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session/{sessionId}/ice-candidate", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        verify(webRTCService, times(1)).addIceCandidate(
            eq(sessionId),
            eq("candidate:test"),
            eq("audio"),
            eq(0)
        );
    }
    
    @Test
    void testAddIceCandidate_ServiceError_Returns500() throws Exception {
        // Arrange
        String sessionId = "session-123";
        IceCandidateRequest request = new IceCandidateRequest();
        request.setCandidate("candidate:test");
        request.setSdpMid("audio");
        request.setSdpMLineIndex(0);
        
        doThrow(new RuntimeException("Service error"))
            .when(webRTCService).addIceCandidate(anyString(), anyString(), anyString(), anyInt());
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/kurento/session/{sessionId}/ice-candidate", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
    }
    
    @Test
    void testCloseKurentoSession_Success() throws Exception {
        // Arrange
        String sessionId = "session-123";
        when(webRTCService.sessionExists(sessionId)).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/kurento/session/{sessionId}", sessionId))
            .andExpect(status().isOk());
        
        verify(webRTCService, times(1)).closeSession(sessionId);
    }
    
    @Test
    void testCloseKurentoSession_SessionNotFound_Returns200() throws Exception {
        // Arrange
        String sessionId = "non-existent-session";
        when(webRTCService.sessionExists(sessionId)).thenReturn(false);
        
        // Act & Assert - Controller doesn't check existence, just calls closeSession
        mockMvc.perform(delete("/api/webrtc/kurento/session/{sessionId}", sessionId))
            .andExpect(status().isOk());
        
        verify(webRTCService, times(1)).closeSession(sessionId);
    }
}
