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
 * Spring MVC integration tests for Agora WebRTC endpoints in RobotController
 * RobotController 声网 WebRTC 端点的 Spring MVC 集成测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@WebMvcTest(RobotController.class)
class RobotControllerAgoraTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebRTCService webRTCService;

    @MockBean
    private OrchestrationService orchestrationService;

    @Test
    void testCreateAgoraSession_Success() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");

        when(webRTCService.createSession("test-user-123"))
            .thenReturn("session-agora-123");
        when(webRTCService.processOffer("session-agora-123", ""))
            .thenReturn("{\"token\":\"agora-test-token\",\"channelName\":\"skylark-123\","
                + "\"appId\":\"test-app-id\",\"uid\":\"test-user-123\"}");

        // Act & Assert
        mockMvc.perform(post("/api/webrtc/agora/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-agora-123"))
            .andExpect(jsonPath("$.token").value("agora-test-token"))
            .andExpect(jsonPath("$.channelName").value("skylark-123"))
            .andExpect(jsonPath("$.appId").value("test-app-id"))
            .andExpect(jsonPath("$.uid").value("test-user-123"))
            .andExpect(jsonPath("$.status").value("created"))
            .andExpect(jsonPath("$.message").value("Agora WebRTC session created successfully"));

        verify(webRTCService, times(1)).createSession("test-user-123");
        verify(webRTCService, times(1)).processOffer("session-agora-123", "");
    }

    @Test
    void testCreateAgoraSession_ServiceError_Returns500() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");

        when(webRTCService.createSession(anyString()))
            .thenThrow(new RuntimeException("Agora service error"));

        // Act & Assert
        mockMvc.perform(post("/api/webrtc/agora/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.sessionId").isEmpty())
            .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testCloseAgoraSession_Success() throws Exception {
        // Arrange
        String sessionId = "session-agora-123";

        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/agora/session/{sessionId}", sessionId))
            .andExpect(status().isOk());

        verify(webRTCService, times(1)).closeSession(sessionId);
    }

    @Test
    void testCloseAgoraSession_ServiceError_Returns500() throws Exception {
        // Arrange
        String sessionId = "session-agora-123";
        doThrow(new RuntimeException("Failed to close"))
            .when(webRTCService).closeSession(anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/agora/session/{sessionId}", sessionId))
            .andExpect(status().isInternalServerError());
    }
}
