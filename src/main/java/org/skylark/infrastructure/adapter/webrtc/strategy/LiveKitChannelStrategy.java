package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.LiveKitClientAdapter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LiveKit WebRTC Channel Strategy
 * LiveKit WebRTC 通道策略
 * 
 * <p>Uses LiveKit Server for scalable, production-grade WebRTC communication.
 * LiveKit handles ICE negotiation, media routing, and room management internally.
 * Clients connect directly to the LiveKit server using an access token.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class LiveKitChannelStrategy implements WebRTCChannelStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveKitChannelStrategy.class);
    
    private final LiveKitClientAdapter liveKitClient;
    private final ConcurrentHashMap<String, LiveKitSessionInfo> sessions = new ConcurrentHashMap<>();
    
    public LiveKitChannelStrategy(LiveKitClientAdapter liveKitClient) {
        this.liveKitClient = liveKitClient;
    }
    
    @Override
    public String getStrategyName() {
        return "livekit";
    }
    
    @Override
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String roomName = "skylark-" + sessionId;
            logger.info("[LiveKit] Creating session for user: {}, room: {}", userId, roomName);
            
            // Create a LiveKit room
            liveKitClient.createRoom(roomName);
            
            // Generate access token for the participant
            String token = liveKitClient.generateToken(roomName, userId);
            String serverUrl = liveKitClient.getServerUrl();
            
            LiveKitSessionInfo sessionInfo = new LiveKitSessionInfo(sessionId, userId, roomName, token, serverUrl);
            sessions.put(sessionId, sessionInfo);
            
            logger.info("[LiveKit] Session created successfully: {}", sessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("[LiveKit] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create LiveKit WebRTC session", e);
        }
    }
    
    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        LiveKitSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        // LiveKit handles SDP internally; return connection info (token + URL)
        // The client uses this token to connect directly to the LiveKit server
        logger.debug("[LiveKit] Returning connection info for session: {}", sessionId);
        return "{\"token\":\"" + session.getToken() + "\",\"url\":\"" + session.getServerUrl() + "\"}";
    }
    
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        // LiveKit handles ICE negotiation internally - no-op
        logger.debug("[LiveKit] ICE candidate handling delegated to LiveKit server for session: {}", sessionId);
    }
    
    @Override
    public void closeSession(String sessionId) {
        try {
            LiveKitSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                liveKitClient.deleteRoom(session.getRoomName());
                logger.info("[LiveKit] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[LiveKit] Error closing session: {}", sessionId, e);
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
        return liveKitClient.isConnected();
    }
    
    /**
     * Gets the connection token for a session
     * 获取会话的连接令牌
     * 
     * @param sessionId Session identifier
     * @return Access token or null if session not found
     */
    public String getSessionToken(String sessionId) {
        LiveKitSessionInfo session = sessions.get(sessionId);
        return session != null ? session.getToken() : null;
    }
    
    /**
     * Internal session info for LiveKit strategy
     * LiveKit 策略的内部会话信息
     */
    static class LiveKitSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String roomName;
        private final String token;
        private final String serverUrl;
        
        LiveKitSessionInfo(String sessionId, String userId, String roomName, String token, String serverUrl) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.roomName = roomName;
            this.token = token;
            this.serverUrl = serverUrl;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getRoomName() {
            return roomName;
        }
        
        public String getToken() {
            return token;
        }
        
        public String getServerUrl() {
            return serverUrl;
        }
    }
}
