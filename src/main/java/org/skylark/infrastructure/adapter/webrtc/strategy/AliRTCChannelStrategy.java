package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.AliRTCClientAdapter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AliRTC WebRTC Channel Strategy
 * 阿里云 ARTC WebRTC 通道策略
 * 
 * <p>Uses AliRTC (Alibaba Cloud ARTC) for real-time audio communication.
 * AliRTC handles ICE negotiation internally. Clients connect using
 * AuthInfo containing token, nonce, and timestamp.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class AliRTCChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AliRTCChannelStrategy.class);

    private final AliRTCClientAdapter aliRTCClient;
    private final ConcurrentHashMap<String, AliRTCSessionInfo> sessions = new ConcurrentHashMap<>();

    public AliRTCChannelStrategy(AliRTCClientAdapter aliRTCClient) {
        this.aliRTCClient = aliRTCClient;
    }

    @Override
    public String getStrategyName() {
        return "alirtc";
    }

    @Override
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String channelId = "skylark-" + sessionId;
            logger.info("[AliRTC] Creating session for user: {}, channel: {}", userId, channelId);

            // Generate server-side auth info and join channel
            String serverAuthInfo = aliRTCClient.generateAuthInfo(channelId, "skylark-server-bot");
            aliRTCClient.joinChannel(channelId, "skylark-server-bot", serverAuthInfo);

            // Generate client-side auth info
            String clientAuthInfo = aliRTCClient.generateAuthInfo(channelId, userId);

            AliRTCSessionInfo sessionInfo = new AliRTCSessionInfo(
                sessionId, userId, channelId, clientAuthInfo);
            sessions.put(sessionId, sessionInfo);

            logger.info("[AliRTC] Session created successfully: {}", sessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("[AliRTC] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create AliRTC WebRTC session", e);
        }
    }

    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        AliRTCSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        // AliRTC clients connect via AuthInfo, no SDP negotiation needed
        logger.debug("[AliRTC] Returning connection info for session: {}", sessionId);
        return String.format(
            "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\",\"authInfo\":%s}",
            aliRTCClient.getAppId(),
            session.getChannelId(),
            session.getUserId(),
            session.getClientAuthInfo());
    }

    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        // no-op — AliRTC handles ICE internally
        logger.debug("[AliRTC] ICE handling delegated to AliRTC SDK for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        try {
            AliRTCSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                aliRTCClient.leaveChannel(session.getChannelId());
                logger.info("[AliRTC] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[AliRTC] Error closing session: {}", sessionId, e);
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
        return aliRTCClient.isAvailable();
    }

    /**
     * Internal session info for AliRTC strategy
     * 阿里云 ARTC 策略的内部会话信息
     */
    static class AliRTCSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelId;
        private final String clientAuthInfo;

        AliRTCSessionInfo(String sessionId, String userId,
                           String channelId, String clientAuthInfo) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelId = channelId;
            this.clientAuthInfo = clientAuthInfo;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public String getClientAuthInfo() { return clientAuthInfo; }
    }
}
