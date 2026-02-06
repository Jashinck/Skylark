package org.skylark.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.dto.SessionStartRequest;
import org.skylark.application.dto.SessionStartResponse;
import org.skylark.application.dto.SessionStatusResponse;
import org.skylark.application.service.OrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTCController
 */
@ExtendWith(MockitoExtension.class)
class WebRTCControllerTest {

    @Mock
    private OrchestrationService orchestrationService;

    private WebRTCController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new WebRTCController(orchestrationService);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testStartSession_Success() {
        // Arrange
        SessionStartRequest request = new SessionStartRequest();
        request.setClientId("test-client");
        
        SessionStartRequest.AudioConfig audioConfig = new SessionStartRequest.AudioConfig();
        audioConfig.setSampleRate(16000);
        audioConfig.setChannels(1);
        audioConfig.setBitDepth(16);
        request.setAudioConfig(audioConfig);

        // Act
        ResponseEntity<SessionStartResponse> response = controller.startSession(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SessionStartResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getSessionId());
        assertEquals("/ws/webrtc", body.getWebsocketUrl());
        assertEquals("started", body.getStatus());
        assertEquals("WebRTC session started successfully", body.getMessage());
    }

    @Test
    void testStartSession_NullClientId() {
        // Arrange
        SessionStartRequest request = new SessionStartRequest();
        request.setClientId(null);

        // Act
        ResponseEntity<SessionStartResponse> response = controller.startSession(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SessionStartResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getSessionId());
    }

    @Test
    void testStopSession_Success() {
        // Arrange
        SessionStartRequest request = new SessionStartRequest();
        request.setClientId("test-client");
        
        // Start a session first
        ResponseEntity<SessionStartResponse> startResponse = controller.startSession(request);
        String sessionId = startResponse.getBody().getSessionId();

        // Act
        ResponseEntity<SessionStatusResponse> response = controller.stopSession(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SessionStatusResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(sessionId, body.getSessionId());
        assertEquals("stopped", body.getStatus());
        assertEquals(false, body.getActive());
        assertEquals("WebRTC session stopped successfully", body.getMessage());
        
        // Verify orchestration service cleanup was called
        verify(orchestrationService, times(1)).cleanupSession(sessionId);
    }

    @Test
    void testStopSession_NotFound() {
        // Arrange
        String nonExistentSessionId = "non-existent-session";

        // Act
        ResponseEntity<SessionStatusResponse> response = controller.stopSession(nonExistentSessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        SessionStatusResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(nonExistentSessionId, body.getSessionId());
        assertEquals("not_found", body.getStatus());
        assertEquals(false, body.getActive());
        assertEquals("Session not found", body.getMessage());
        
        // Verify orchestration service cleanup was not called
        verify(orchestrationService, never()).cleanupSession(anyString());
    }

    @Test
    void testGetSessionStatus_Active() {
        // Arrange
        SessionStartRequest request = new SessionStartRequest();
        request.setClientId("test-client");
        
        // Start a session first
        ResponseEntity<SessionStartResponse> startResponse = controller.startSession(request);
        String sessionId = startResponse.getBody().getSessionId();

        // Act
        ResponseEntity<SessionStatusResponse> response = controller.getSessionStatus(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        SessionStatusResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(sessionId, body.getSessionId());
        assertEquals("active", body.getStatus());
        assertEquals(true, body.getActive());
        assertEquals("Session is active", body.getMessage());
    }

    @Test
    void testGetSessionStatus_NotFound() {
        // Arrange
        String nonExistentSessionId = "non-existent-session";

        // Act
        ResponseEntity<SessionStatusResponse> response = controller.getSessionStatus(nonExistentSessionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        SessionStatusResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(nonExistentSessionId, body.getSessionId());
        assertEquals("not_found", body.getStatus());
        assertEquals(false, body.getActive());
        assertEquals("Session not found", body.getMessage());
    }

    @Test
    void testSessionLifecycle() {
        // Arrange
        SessionStartRequest request = new SessionStartRequest();
        request.setClientId("test-client");

        // Act & Assert
        // 1. Start session
        ResponseEntity<SessionStartResponse> startResponse = controller.startSession(request);
        assertEquals(HttpStatus.OK, startResponse.getStatusCode());
        String sessionId = startResponse.getBody().getSessionId();
        assertNotNull(sessionId);

        // 2. Check status - should be active
        ResponseEntity<SessionStatusResponse> statusResponse = controller.getSessionStatus(sessionId);
        assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
        assertEquals("active", statusResponse.getBody().getStatus());
        assertEquals(true, statusResponse.getBody().getActive());

        // 3. Stop session
        ResponseEntity<SessionStatusResponse> stopResponse = controller.stopSession(sessionId);
        assertEquals(HttpStatus.OK, stopResponse.getStatusCode());
        assertEquals("stopped", stopResponse.getBody().getStatus());

        // 4. Check status again - should not be found
        ResponseEntity<SessionStatusResponse> statusAfterStop = controller.getSessionStatus(sessionId);
        assertEquals(HttpStatus.NOT_FOUND, statusAfterStop.getStatusCode());
        assertEquals("not_found", statusAfterStop.getBody().getStatus());
    }
}
