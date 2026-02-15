package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket WebRTC Channel Strategy
 * WebSocket WebRTC 通道策略
 * 
 * <p>Uses WebSocket for both signaling and audio transmission.
 * This is a hybrid approach where WebSocket handles SDP signaling
 * and binary audio data transfer.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class WebSocketChannelStrategy implements WebRTCChannelStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketChannelStrategy.class);
    
    private final ConcurrentHashMap<String, WebSocketSessionInfo> sessions = new ConcurrentHashMap<>();
    
    @Override
    public String getStrategyName() {
        return "websocket";
    }
    
    @Override
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        logger.info("[WebSocket] Creating session for user: {}, sessionId: {}", userId, sessionId);
        
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(sessionId, userId);
        sessions.put(sessionId, sessionInfo);
        
        logger.info("[WebSocket] Session created successfully: {}", sessionId);
        return sessionId;
    }
    
    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        WebSocketSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        logger.debug("[WebSocket] Processing SDP offer for session: {}", sessionId);
        
        // WebSocket strategy generates a simplified SDP answer
        // Actual media is transmitted via WebSocket binary messages
        String sdpAnswer = generateSimplifiedSdpAnswer(sdpOffer);
        session.setSdpAnswer(sdpAnswer);
        
        return sdpAnswer;
    }
    
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        WebSocketSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("[WebSocket] Session not found for ICE candidate: {}", sessionId);
            return;
        }
        
        // WebSocket strategy stores ICE candidates for potential P2P fallback
        logger.debug("[WebSocket] ICE candidate received for session: {}", sessionId);
    }
    
    @Override
    public void closeSession(String sessionId) {
        try {
            WebSocketSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                logger.info("[WebSocket] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[WebSocket] Error closing session: {}", sessionId, e);
        }
    }
    
    @Override
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    @Override
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    @Override
    public boolean isAvailable() {
        return true; // WebSocket is always available
    }
    
    /**
     * Generates a simplified SDP answer for WebSocket-based audio streaming
     * 为基于 WebSocket 的音频流生成简化的 SDP 应答
     */
    private String generateSimplifiedSdpAnswer(String sdpOffer) {
        // Simplified for audio streaming via WebSocket
        return "v=0\r\n"
            + "o=- " + System.currentTimeMillis() + " 2 IN IP4 127.0.0.1\r\n"
            + "s=WebSocket Audio Session\r\n"
            + "t=0 0\r\n"
            + "m=audio 0 UDP/TLS/RTP/SAVPF 111\r\n"
            + "a=mid:audio\r\n"
            + "a=rtpmap:111 opus/48000/2\r\n";
    }
    
    /**
     * Internal session info for WebSocket strategy
     * WebSocket 策略的内部会话信息
     */
    static class WebSocketSessionInfo {
        private final String sessionId;
        private final String userId;
        private String sdpAnswer;
        
        WebSocketSessionInfo(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getSdpAnswer() {
            return sdpAnswer;
        }
        
        public void setSdpAnswer(String sdpAnswer) {
            this.sdpAnswer = sdpAnswer;
        }
    }
}
