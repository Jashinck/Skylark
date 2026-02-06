package org.skylark.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.service.OrchestrationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC Signaling Handler
 * WebRTC信令处理器
 * 
 * <p>Handles WebRTC signaling messages (SDP offer/answer, ICE candidates) and
 * audio data streams for real-time voice communication.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Component
public class WebRTCSignalingHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebRTCSignalingHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrchestrationService orchestrationService;
    
    // Store active sessions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public WebRTCSignalingHandler(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebRTC connection established: {}", sessionId);
        
        // Send connection confirmation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "connected");
        response.put("sessionId", sessionId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("Received text message: {}", payload);
        
        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";
            
            switch (type) {
                case "offer":
                    handleOffer(session, json);
                    break;
                case "answer":
                    handleAnswer(session, json);
                    break;
                case "ice-candidate":
                    handleIceCandidate(session, json);
                    break;
                case "text":
                    handleTextInput(session, json);
                    break;
                default:
                    logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling text message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer buffer = message.getPayload();
        byte[] audioData = new byte[buffer.remaining()];
        buffer.get(audioData);
        
        logger.debug("Received audio data: {} bytes from session {}", audioData.length, session.getId());
        
        // Process audio through VAD->ASR->LLM->TTS pipeline
        try {
            orchestrationService.processAudioStream(session.getId(), audioData, 
                (sessionId, type, data) -> sendResponse(sessionId, type, data));
        } catch (Exception e) {
            logger.error("Error processing audio stream", e);
            sendError(session, "Error processing audio: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        orchestrationService.cleanupSession(sessionId);
        logger.info("WebRTC connection closed: {} with status: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session: {}", session.getId(), exception);
        sessions.remove(session.getId());
        orchestrationService.cleanupSession(session.getId());
    }

    /**
     * Handle WebRTC offer from client
     */
    private void handleOffer(WebSocketSession session, JsonNode json) throws Exception {
        logger.info("Received WebRTC offer from session: {}", session.getId());
        
        // For browser-based WebRTC, we send back an "answer" that accepts the connection
        // In a full WebRTC implementation, this would involve creating a peer connection
        ObjectNode answer = objectMapper.createObjectNode();
        answer.put("type", "answer");
        answer.put("sdp", "accepted"); // Simplified for audio streaming via WebSocket
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(answer)));
    }

    /**
     * Handle WebRTC answer from client
     */
    private void handleAnswer(WebSocketSession session, JsonNode json) throws Exception {
        logger.info("Received WebRTC answer from session: {}", session.getId());
        // Process answer if needed
    }

    /**
     * Handle ICE candidate from client
     */
    private void handleIceCandidate(WebSocketSession session, JsonNode json) throws Exception {
        logger.debug("Received ICE candidate from session: {}", session.getId());
        // Process ICE candidate if needed
    }

    /**
     * Handle text input from client
     */
    private void handleTextInput(WebSocketSession session, JsonNode json) throws Exception {
        if (json.has("content")) {
            String text = json.get("content").asText();
            logger.info("Received text input from session {}: {}", session.getId(), text);
            
            // Process text through LLM->TTS pipeline (skip VAD/ASR)
            orchestrationService.processTextInput(session.getId(), text, 
                (sessionId, type, data) -> sendResponse(sessionId, type, data));
        }
    }

    /**
     * Send response back to client
     */
    private void sendResponse(String sessionId, String type, Object data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("type", type);
                response.set("data", objectMapper.valueToTree(data));
                
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            } catch (Exception e) {
                logger.error("Error sending response to session: {}", sessionId, e);
            }
        }
    }

    /**
     * Send error message to client
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", "error");
            error.put("message", errorMessage);
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            logger.error("Error sending error message", e);
        }
    }
}
