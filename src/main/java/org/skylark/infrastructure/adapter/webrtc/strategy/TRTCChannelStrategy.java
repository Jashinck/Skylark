package org.skylark.infrastructure.adapter.webrtc.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tencent Cloud TRTC WebRTC Channel Strategy
 * 腾讯云实时音视频（TRTC）通道策略
 *
 * <p>Uses Tencent Cloud TRTC (Tencent Real-Time Communication) for
 * low-latency, high-quality real-time audio communication. TRTC is
 * deeply integrated with Tencent Cloud AI services (ASR/TTS/NLP)
 * and the WeChat/WeCom ecosystem.</p>
 *
 * <p>Key advantages:
 * <ul>
 *   <li>Tencent Cloud AI integration (one-stop ASR/TTS/LLM)</li>
 *   <li>WeChat/WeCom ecosystem compatibility</li>
 *   <li>China's largest C-end user base access</li>
 *   <li>Massive concurrency support (100,000+ rooms)</li>
 * </ul></p>
 *
 * <p>Phase 2 component ([E1] in the full-duplex upgrade roadmap).
 * Follows the same pluggable strategy pattern as AgoraChannelStrategy
 * and LiveKitChannelStrategy.</p>
 *
 * <p>Example config.yaml usage:
 * <pre>
 * webrtc:
 *   strategy: trtc
 *   sdkAppId: ${TRTC_SDK_APP_ID}
 *   secretKey: ${TRTC_SECRET_KEY}
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://cloud.tencent.com/document/product/647">TRTC Documentation</a>
 */
public class TRTCChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TRTCChannelStrategy.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Default token validity period in seconds (24 hours) */
    private static final int DEFAULT_TOKEN_EXPIRE_SECONDS = 86400;

    /** TRTC room prefix */
    private static final String ROOM_PREFIX = "skylark-";

    private final String sdkAppId;
    private final String secretKey;
    private final int tokenExpireSeconds;
    private final ConcurrentHashMap<String, TRTCSessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a TRTCChannelStrategy with the given SDK credentials.
     *
     * @param sdkAppId   TRTC SDK application ID
     * @param secretKey  TRTC secret key for UserSig generation
     */
    public TRTCChannelStrategy(String sdkAppId, String secretKey) {
        this(sdkAppId, secretKey, DEFAULT_TOKEN_EXPIRE_SECONDS);
    }

    /**
     * Creates a TRTCChannelStrategy with custom token expiry.
     *
     * @param sdkAppId           TRTC SDK application ID
     * @param secretKey          TRTC secret key for UserSig generation
     * @param tokenExpireSeconds UserSig validity period
     */
    public TRTCChannelStrategy(String sdkAppId, String secretKey, int tokenExpireSeconds) {
        this.sdkAppId = sdkAppId;
        this.secretKey = secretKey;
        this.tokenExpireSeconds = tokenExpireSeconds;
        logger.info("[TRTC] TRTCChannelStrategy initialized: sdkAppId={}", sdkAppId);
    }

    @Override
    public String getStrategyName() {
        return "trtc";
    }

    /**
     * Creates a new TRTC session: allocates a room and generates a UserSig token.
     *
     * <p>TRTC uses a token-based (UserSig) connection model — clients connect
     * directly to TRTC servers using the returned token, similar to Agora/LiveKit.</p>
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
        String roomId = ROOM_PREFIX + sessionId.substring(0, 8);

        // Generate TRTC UserSig (Phase 2: integrate actual TRTC UserSig SDK)
        String userSig = generateUserSig(userId);

        TRTCSessionInfo sessionInfo = new TRTCSessionInfo(sessionId, userId, roomId, userSig, sdkAppId);
        sessions.put(sessionId, sessionInfo);

        logger.info("[TRTC] Session created: sessionId={}, userId={}, roomId={}", sessionId, userId, roomId);
        return sessionId;
    }

    /**
     * Returns TRTC connection info (sdkAppId + roomId + userId + userSig).
     *
     * <p>TRTC handles SDP and ICE internally. The returned JSON contains all
     * the credentials needed for the client to connect to the TRTC room.</p>
     *
     * @param sessionId Session identifier
     * @param sdpOffer  Ignored — TRTC manages SDP internally
     * @return JSON with TRTC connection parameters
     */
    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        TRTCSessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("sdkAppId", session.getSdkAppId());
            node.put("roomId", session.getRoomId());
            node.put("userId", session.getUserId());
            node.put("userSig", session.getUserSig());
            node.put("strategy", getStrategyName());

            String result = objectMapper.writeValueAsString(node);
            logger.debug("[TRTC] Returning connection info for session: {}", sessionId);
            return result;
        } catch (Exception e) {
            logger.error("[TRTC] Failed to serialize connection info for session: {}", sessionId, e);
            throw new RuntimeException("Failed to serialize TRTC connection info", e);
        }
    }

    /**
     * No-op — TRTC handles ICE negotiation internally.
     */
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        logger.debug("[TRTC] ICE negotiation handled internally by TRTC for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        TRTCSessionInfo session = sessions.remove(sessionId);
        if (session != null) {
            logger.info("[TRTC] Session closed: sessionId={}, roomId={}", sessionId, session.getRoomId());
            // Phase 2: call TRTC REST API to dissolve the room
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
        // Phase 2: Check TRTC service reachability
        return sdkAppId != null && !sdkAppId.isEmpty();
    }

    /**
     * Gets the UserSig token for an active session.
     *
     * @param sessionId Session identifier
     * @return UserSig or null if session not found
     */
    public String getSessionUserSig(String sessionId) {
        TRTCSessionInfo session = sessions.get(sessionId);
        return session != null ? session.getUserSig() : null;
    }

    /**
     * Generates a TRTC UserSig token for the given user.
     *
     * <p>Phase 2: Replace with the official Tencent Cloud TLSSigAPIv2 library
     * (HMAC-SHA256 based). Current placeholder returns a mock token.</p>
     *
     * @param userId User identifier
     * @return TRTC UserSig token
     */
    String generateUserSig(String userId) {
        // Phase 2: Use TLSSigAPIv2.genUserSig(sdkAppId, secretKey, userId, tokenExpireSeconds)
        // Reference: https://cloud.tencent.com/document/product/647/17275
        logger.debug("[TRTC] UserSig generation for userId={} (Phase 2: placeholder)", userId);
        return "trtc-usersig-" + userId + "-" + System.currentTimeMillis();
    }

    /**
     * Internal session info for TRTC strategy.
     */
    static class TRTCSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String roomId;
        private final String userSig;
        private final String sdkAppId;

        TRTCSessionInfo(String sessionId, String userId, String roomId, String userSig, String sdkAppId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.roomId = roomId;
            this.userSig = userSig;
            this.sdkAppId = sdkAppId;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getRoomId() { return roomId; }
        public String getUserSig() { return userSig; }
        public String getSdkAppId() { return sdkAppId; }
    }
}
