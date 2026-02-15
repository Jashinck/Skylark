package org.skylark.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skylark.application.dto.webrtc.CreateSessionRequest;
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
 * Spring MVC integration tests for LiveKit WebRTC endpoints in RobotController
 * RobotController LiveKit WebRTC 端点的 Spring MVC 集成测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@WebMvcTest(RobotController.class)
class RobotControllerLiveKitTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private WebRTCService webRTCService;
    
    @MockBean
    private OrchestrationService orchestrationService;
    
    @Test
    void testCreateLiveKitSession_Success() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");
        
        when(webRTCService.createSession("test-user-123"))
            .thenReturn("session-lk-123");
        when(webRTCService.processOffer("session-lk-123", ""))
            .thenReturn("{\"token\":\"test-jwt-token\",\"url\":\"wss://livekit.example.com\"}");
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/livekit/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-lk-123"))
            .andExpect(jsonPath("$.token").value("test-jwt-token"))
            .andExpect(jsonPath("$.url").value("wss://livekit.example.com"))
            .andExpect(jsonPath("$.status").value("created"))
            .andExpect(jsonPath("$.message").value("LiveKit WebRTC session created successfully"));
        
        verify(webRTCService, times(1)).createSession("test-user-123");
        verify(webRTCService, times(1)).processOffer("session-lk-123", "");
    }
    
    @Test
    void testCreateLiveKitSession_ServiceError_Returns500() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");
        
        when(webRTCService.createSession(anyString()))
            .thenThrow(new RuntimeException("LiveKit service error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/webrtc/livekit/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.sessionId").isEmpty())
            .andExpect(jsonPath("$.status").value("error"));
    }
    
    @Test
    void testCreateLiveKitSession_NonJsonResponse_HandlesGracefully() throws Exception {
        // Arrange - strategy returns non-JSON string (e.g., websocket strategy)
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");
        
        when(webRTCService.createSession("test-user-123"))
            .thenReturn("session-ws-123");
        when(webRTCService.processOffer("session-ws-123", ""))
            .thenReturn("non-json-response");
        
        // Act & Assert - should still succeed with null token/url
        mockMvc.perform(post("/api/webrtc/livekit/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-ws-123"))
            .andExpect(jsonPath("$.status").value("created"));
    }
    
    @Test
    void testCloseLiveKitSession_Success() throws Exception {
        // Arrange
        String sessionId = "session-lk-123";
        
        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/livekit/session/{sessionId}", sessionId))
            .andExpect(status().isOk());
        
        verify(webRTCService, times(1)).closeSession(sessionId);
    }
    
    @Test
    void testCloseLiveKitSession_ServiceError_Returns500() throws Exception {
        // Arrange
        String sessionId = "session-lk-123";
        doThrow(new RuntimeException("Failed to close"))
            .when(webRTCService).closeSession(anyString());
        
        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/livekit/session/{sessionId}", sessionId))
            .andExpect(status().isInternalServerError());
    }
}
