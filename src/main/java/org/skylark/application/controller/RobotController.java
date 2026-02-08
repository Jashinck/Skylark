package org.skylark.application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.dto.SessionStartRequest;
import org.skylark.application.dto.SessionStartResponse;
import org.skylark.application.dto.SessionStatusResponse;
import org.skylark.application.service.OrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Robot Controller
 * 机器人控制器
 * 
 * <p>Provides REST API endpoints for managing WebRTC sessions, including starting
 * and stopping real-time call capabilities.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/webrtc")
public class RobotController {

    private static final Logger logger = LoggerFactory.getLogger(RobotController.class);
    
    private final OrchestrationService orchestrationService;
    
    // Track active sessions managed via REST API
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    public RobotController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Start a new WebRTC session
     * 启动新的WebRTC会话
     * 
     * @param request Session start request
     * @return Session start response with session ID and WebSocket URL
     */
    @PostMapping("/session/start")
    public ResponseEntity<SessionStartResponse> startSession(@RequestBody SessionStartRequest request) {
        try {
            logger.info("Starting WebRTC session for client: {}", request.getClientId());
            
            // Generate session ID
            String sessionId = UUID.randomUUID().toString();
            
            // Create session info
            SessionInfo sessionInfo = new SessionInfo(
                sessionId, 
                request.getClientId(),
                System.currentTimeMillis()
            );
            
            // Store session
            activeSessions.put(sessionId, sessionInfo);
            
            // Build WebSocket URL
            String websocketUrl = "/ws/webrtc";
            
            // Create response
            SessionStartResponse response = new SessionStartResponse(
                sessionId,
                websocketUrl,
                "started",
                "WebRTC session started successfully"
            );
            
            logger.info("WebRTC session started: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start WebRTC session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SessionStartResponse(null, null, "error", "Failed to start session"));
        }
    }

    /**
     * Stop an active WebRTC session
     * 停止活动的WebRTC会话
     * 
     * @param sessionId Session ID to stop
     * @return Status response
     */
    @PostMapping("/session/stop/{sessionId}")
    public ResponseEntity<SessionStatusResponse> stopSession(@PathVariable String sessionId) {
        try {
            logger.info("Stopping WebRTC session: {}", sessionId);
            
            SessionInfo sessionInfo = activeSessions.remove(sessionId);
            
            if (sessionInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SessionStatusResponse(sessionId, "not_found", false, "Session not found"));
            }
            
            // Cleanup session resources
            orchestrationService.cleanupSession(sessionId);
            
            SessionStatusResponse response = new SessionStatusResponse(
                sessionId,
                "stopped",
                false,
                "WebRTC session stopped successfully"
            );
            
            logger.info("WebRTC session stopped: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to stop WebRTC session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SessionStatusResponse(sessionId, "error", null, "Failed to stop session"));
        }
    }

    /**
     * Get status of a WebRTC session
     * 获取WebRTC会话状态
     * 
     * @param sessionId Session ID to check
     * @return Session status
     */
    @GetMapping("/session/status/{sessionId}")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        try {
            logger.debug("Checking status for WebRTC session: {}", sessionId);
            
            SessionInfo sessionInfo = activeSessions.get(sessionId);
            
            if (sessionInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SessionStatusResponse(sessionId, "not_found", false, "Session not found"));
            }
            
            SessionStatusResponse response = new SessionStatusResponse(
                sessionId,
                "active",
                true,
                "Session is active"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get session status: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SessionStatusResponse(sessionId, "error", null, "Failed to get session status"));
        }
    }

    /**
     * Internal class to track session information
     */
    private static class SessionInfo {
        private final String sessionId;
        private final String clientId;
        private final long startTime;
        
        public SessionInfo(String sessionId, String clientId, long startTime) {
            this.sessionId = sessionId;
            this.clientId = clientId;
            this.startTime = startTime;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
}
