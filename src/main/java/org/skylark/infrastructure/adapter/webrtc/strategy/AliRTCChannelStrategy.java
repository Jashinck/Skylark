package org.skylark.infrastructure.adapter.webrtc.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alibaba Cloud RTC Channel Strategy
 * 阿里云音视频通信（AliRTC）通道策略
 *
 * <p>Uses Alibaba Cloud RTC for real-time audio communication.
 * AliRTC is deeply integrated with Alibaba's AI ecosystem
 * (Tongyi Qwen ASR/TTS/LLM) and delivers superior network
 * performance via Alibaba CDN edge nodes.</p>
 *
 * <p>Key advantages:
 * <ul>
 *   <li>Seamless Tongyi Qwen AI ecosystem integration</li>
 *   <li>Alibaba Cloud CDN global edge acceleration</li>
 *   <li>Enterprise-grade security and compliance (data residency)</li>
 *   <li>Preferred choice for enterprise customers on Alibaba Cloud</li>
 * </ul></p>
 *
 * <p>Phase 2/3 component ([E2] in the full-duplex upgrade roadmap).
 * Recommended to deploy alongside Tongyi Qwen ASR/TTS adapters for
 * a unified Alibaba Cloud AI pipeline with minimal latency.</p>
 *
 * <p>Example config.yaml usage:
 * <pre>
 * webrtc:
 *   strategy: alirtc
 *   appId: ${ALIRTC_APP_ID}
 *   appKey: ${ALIRTC_APP_KEY}
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://help.aliyun.com/zh/live-and-rtc/">AliRTC Documentation</a>
 */
public class AliRTCChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AliRTCChannelStrategy.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Default token TTL in seconds (1 hour) */
    private static final int DEFAULT_TOKEN_TTL_SECONDS = 3600;

    /** AliRTC channel name prefix */
    private static final String CHANNEL_PREFIX = "skylark-";

    private final String appId;
    private final String appKey;
    private final int tokenTtlSeconds;
    private final ConcurrentHashMap<String, AliRTCSessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Creates an AliRTCChannelStrategy with the given app credentials.
     *
     * @param appId  AliRTC application ID
     * @param appKey AliRTC application key for token generation
     */
    public AliRTCChannelStrategy(String appId, String appKey) {
        this(appId, appKey, DEFAULT_TOKEN_TTL_SECONDS);
    }

    /**
     * Creates an AliRTCChannelStrategy with custom token TTL.
     *
     * @param appId           AliRTC application ID
     * @param appKey          AliRTC application key
     * @param tokenTtlSeconds Token validity period in seconds
     */
    public AliRTCChannelStrategy(String appId, String appKey, int tokenTtlSeconds) {
        this.appId = appId;
        this.appKey = appKey;
        this.tokenTtlSeconds = tokenTtlSeconds;
        logger.info("[AliRTC] AliRTCChannelStrategy initialized: appId={}", appId);
    }

    @Override
    public String getStrategyName() {
        return "alirtc";
    }

    /**
     * Creates a new AliRTC session: allocates a channel and generates an auth token.
     *
     * <p>AliRTC uses a token-based connection model — clients connect directly
     * to AliRTC servers using the channel name and auth token.</p>
     *
     * @param userId User identifier
     * @return Session ID
     * @throws IllegalArgumentException if userId is null or empty
     */
    @Override
    public String createSession(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }

        String sessionId = UUID.randomUUID().toString();
        String channelId = CHANNEL_PREFIX + sessionId.substring(0, 8);

        // Generate AliRTC auth token (Phase 2: use official AliRTC SDK token generation)
        String token = generateToken(channelId, userId);

        AliRTCSessionInfo sessionInfo = new AliRTCSessionInfo(sessionId, userId, channelId, token, appId);
        sessions.put(sessionId, sessionInfo);

        logger.info("[AliRTC] Session created: sessionId={}, userId={}, channelId={}",
                sessionId, userId, channelId);
        return sessionId;
    }

    /**
     * Returns AliRTC connection info (appId + channelId + userId + token).
     *
     * <p>AliRTC handles SDP and ICE negotiation internally. The returned JSON
     * contains all credentials needed for the client to join the RTC channel.</p>
     *
     * @param sessionId Session identifier
     * @param sdpOffer  Ignored — AliRTC manages SDP internally
     * @return JSON with AliRTC connection parameters
     */
    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        AliRTCSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("appId", session.getAppId());
            node.put("channelId", session.getChannelId());
            node.put("userId", session.getUserId());
            node.put("token", session.getToken());
            node.put("strategy", getStrategyName());

            String result = objectMapper.writeValueAsString(node);
            logger.debug("[AliRTC] Returning connection info for session: {}", sessionId);
            return result;
        } catch (Exception e) {
            logger.error("[AliRTC] Failed to serialize connection info for session: {}", sessionId, e);
            throw new RuntimeException("Failed to serialize AliRTC connection info", e);
        }
    }

    /**
     * No-op — AliRTC handles ICE negotiation internally.
     */
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        logger.debug("[AliRTC] ICE negotiation handled internally by AliRTC for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        AliRTCSessionInfo session = sessions.remove(sessionId);
        if (session != null) {
            logger.info("[AliRTC] Session closed: sessionId={}, channelId={}",
                    sessionId, session.getChannelId());
            // Phase 2: call AliRTC REST API to remove the channel
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
        // Phase 2: probe AliRTC service endpoint
        return appId != null && !appId.isEmpty();
    }

    /**
     * Gets the auth token for an active session.
     *
     * @param sessionId Session identifier
     * @return Auth token or null if session not found
     */
    public String getSessionToken(String sessionId) {
        AliRTCSessionInfo session = sessions.get(sessionId);
        return session != null ? session.getToken() : null;
    }

    /**
     * Generates an AliRTC auth token for the given channel and user.
     *
     * <p>Phase 2: Replace with the official Alibaba Cloud RTC SDK token generation
     * (HMAC-SHA256 based). Current placeholder returns a mock token.</p>
     *
     * @param channelId Channel identifier
     * @param userId    User identifier
     * @return AliRTC auth token
     */
    String generateToken(String channelId, String userId) {
        // Phase 2: Use AliRTC SDK: RtcTokenBuilder.buildToken(appId, appKey, channelId, userId, ttl)
        // Reference: https://help.aliyun.com/zh/live-and-rtc/rtc/developer-reference/token-based-authentication
        logger.debug("[AliRTC] Token generation for channelId={}, userId={} (Phase 2: placeholder)",
                channelId, userId);
        return "alirtc-token-" + channelId + "-" + userId + "-" + System.currentTimeMillis();
    }

    /**
     * Internal session info for AliRTC strategy.
     */
    static class AliRTCSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelId;
        private final String token;
        private final String appId;

        AliRTCSessionInfo(String sessionId, String userId, String channelId, String token, String appId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelId = channelId;
            this.token = token;
            this.appId = appId;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public String getToken() { return token; }
        public String getAppId() { return appId; }
    }
}
