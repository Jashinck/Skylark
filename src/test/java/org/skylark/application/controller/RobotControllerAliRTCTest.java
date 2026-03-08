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
 * Spring MVC integration tests for AliRTC WebRTC endpoints in RobotController
 * RobotController 阿里云 ARTC 端点的 Spring MVC 集成测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@WebMvcTest(RobotController.class)
class RobotControllerAliRTCTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebRTCService webRTCService;

    @MockBean
    private OrchestrationService orchestrationService;

    @Test
    void testCreateAliRTCSession_Success() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");

        when(webRTCService.createSession("test-user-123"))
            .thenReturn("session-alirtc-123");
        when(webRTCService.processOffer("session-alirtc-123", ""))
            .thenReturn("{\"appId\":\"test-app-id\",\"channelId\":\"skylark-123\","
                + "\"userId\":\"test-user-123\",\"authInfo\":{\"token\":\"test-token\","
                + "\"nonce\":\"abc\",\"timestamp\":1234}}");

        // Act & Assert
        mockMvc.perform(post("/api/webrtc/alirtc/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session-alirtc-123"))
            .andExpect(jsonPath("$.appId").value("test-app-id"))
            .andExpect(jsonPath("$.channelId").value("skylark-123"))
            .andExpect(jsonPath("$.userId").value("test-user-123"))
            .andExpect(jsonPath("$.status").value("created"))
            .andExpect(jsonPath("$.message").value("AliRTC session created successfully"));

        verify(webRTCService, times(1)).createSession("test-user-123");
        verify(webRTCService, times(1)).processOffer("session-alirtc-123", "");
    }

    @Test
    void testCreateAliRTCSession_ServiceError_Returns500() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId("test-user-123");

        when(webRTCService.createSession(anyString()))
            .thenThrow(new RuntimeException("AliRTC service error"));

        // Act & Assert
        mockMvc.perform(post("/api/webrtc/alirtc/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.sessionId").isEmpty())
            .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testCloseAliRTCSession_Success() throws Exception {
        // Arrange
        String sessionId = "session-alirtc-123";

        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/alirtc/session/{sessionId}", sessionId))
            .andExpect(status().isOk());

        verify(webRTCService, times(1)).closeSession(sessionId);
    }

    @Test
    void testCloseAliRTCSession_ServiceError_Returns500() throws Exception {
        // Arrange
        String sessionId = "session-alirtc-123";
        doThrow(new RuntimeException("Failed to close"))
            .when(webRTCService).closeSession(anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/webrtc/alirtc/session/{sessionId}", sessionId))
            .andExpect(status().isInternalServerError());
    }
}
