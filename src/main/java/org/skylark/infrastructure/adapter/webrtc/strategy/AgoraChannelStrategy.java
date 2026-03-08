package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.AgoraClientAdapter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agora WebRTC Channel Strategy
 * 声网 WebRTC 通道策略
 * 
 * <p>Uses Agora RTC Server SDK for real-time audio communication.
 * Agora handles ICE negotiation internally. Clients connect using
 * an RTC Token and Channel Name.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class AgoraChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AgoraChannelStrategy.class);

    private final AgoraClientAdapter agoraClient;
    private final ConcurrentHashMap<String, AgoraSessionInfo> sessions = new ConcurrentHashMap<>();

    public AgoraChannelStrategy(AgoraClientAdapter agoraClient) {
        this.agoraClient = agoraClient;
    }

    @Override
    public String getStrategyName() {
        return "agora";
    }

    @Override
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String channelName = "skylark-" + sessionId;
            logger.info("[Agora] Creating session for user: {}, channel: {}", userId, channelName);

            // Server joins the Agora channel
            agoraClient.joinChannel(channelName, "skylark-server-bot");

            // Generate client connection token
            String clientToken = agoraClient.generateToken(channelName, userId, 3600);

            AgoraSessionInfo sessionInfo = new AgoraSessionInfo(
                sessionId, userId, channelName, clientToken);
            sessions.put(sessionId, sessionInfo);

            logger.info("[Agora] Session created successfully: {}", sessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("[Agora] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create Agora WebRTC session", e);
        }
    }

    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        AgoraSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        // Agora clients connect via Token + ChannelName, no SDP negotiation needed
        logger.debug("[Agora] Returning connection info for session: {}", sessionId);
        return String.format(
            "{\"token\":\"%s\",\"channelName\":\"%s\",\"appId\":\"%s\",\"uid\":\"%s\"}",
            session.getClientToken(),
            session.getChannelName(),
            agoraClient.getAppId(),
            session.getUserId());
    }

    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        // no-op — Agora handles ICE internally
        logger.debug("[Agora] ICE handling delegated to Agora SDK for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        try {
            AgoraSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                agoraClient.leaveChannel(session.getChannelName());
                logger.info("[Agora] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[Agora] Error closing session: {}", sessionId, e);
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
        return agoraClient.isAvailable();
    }

    /**
     * Internal session info for Agora strategy
     * 声网策略的内部会话信息
     */
    static class AgoraSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelName;
        private final String clientToken;

        AgoraSessionInfo(String sessionId, String userId,
                          String channelName, String clientToken) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelName = channelName;
            this.clientToken = clientToken;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelName() { return channelName; }
        public String getClientToken() { return clientToken; }
    }
}
