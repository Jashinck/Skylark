package org.skylark.infrastructure.adapter.webrtc.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.service.OrchestrationService;
import org.skylark.infrastructure.adapter.webrtc.AliRTCClientAdapter;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alibaba Cloud RTC Channel Strategy
 * 阿里云音视频通信（AliRTC）通道策略
 *
 * <p>Uses Alibaba Cloud ARTC for real-time audio communication.
 * AliRTC is deeply integrated with Alibaba's AI ecosystem
 * (Tongyi Qwen ASR/TTS/LLM) and delivers superior network performance
 * via Alibaba CDN edge nodes.</p>
 *
 * <p>Key advantages:
 * <ul>
 *   <li>Seamless Tongyi Qwen AI ecosystem integration</li>
 *   <li>Alibaba Cloud CDN global edge acceleration</li>
 *   <li>Enterprise-grade security and compliance (data residency)</li>
 *   <li>Preferred choice for enterprise customers on Alibaba Cloud</li>
 * </ul></p>
 *
 * <p>Integrates with {@link OrchestrationService} for the
 * VAD → ASR → LLM → TTS audio processing pipeline.
 * When audio frames are received from the remote user, they are forwarded
 * through the orchestration pipeline. TTS output is sent back via
 * {@link AliRTCClientAdapter#pushAudioFrame}.</p>
 *
 * <p>Example application.yaml usage:
 * <pre>
 * webrtc:
 *   strategy: alirtc
 *   alirtc:
 *     app-id: ${ALIRTC_APP_ID}
 *     app-key: ${ALIRTC_APP_KEY}
 *     app-secret: ${ALIRTC_APP_SECRET}
 * </pre></p>
 *
 * @author Skylark Team
 * @version 2.0.0
 * @see <a href="https://help.aliyun.com/zh/live/artc-download-the-sdk">AliRTC Documentation</a>
 */
public class AliRTCChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AliRTCChannelStrategy.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNELS = 1;
    private static final String SERVER_BOT_ID = "skylark-server-bot";
    private static final String TTS_AUDIO_TYPE = "tts_audio";

    private final AliRTCClientAdapter aliRTCClient;
    private final OrchestrationService orchestrationService;
    private final ConcurrentHashMap<String, AliRTCSessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Creates an AliRTCChannelStrategy wired to the given client adapter and
     * orchestration service.
     *
     * @param aliRTCClient         AliRTC client adapter
     * @param orchestrationService VAD/ASR/LLM/TTS orchestration service
     */
    public AliRTCChannelStrategy(AliRTCClientAdapter aliRTCClient,
                                  OrchestrationService orchestrationService) {
        this.aliRTCClient = aliRTCClient;
        this.orchestrationService = orchestrationService;
        logger.info("[AliRTC] AliRTCChannelStrategy initialized");
    }

    @Override
    public String getStrategyName() {
        return "alirtc";
    }

    /**
     * Creates a new AliRTC session: allocates a channel, generates authInfo,
     * has the server join the channel, and wires the audio pipeline.
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

        try {
            String sessionId = UUID.randomUUID().toString();
            String channelId = "skylark-" + sessionId;
            logger.info("[AliRTC] Creating session for user: {}, channelId: {}", userId, channelId);

            // 1. Generate server-side authInfo and join channel
            String serverAuthInfo = aliRTCClient.generateAuthInfo(channelId, SERVER_BOT_ID);
            aliRTCClient.joinChannel(channelId, SERVER_BOT_ID, serverAuthInfo);

            // 2. Register audio data callback: remote PCM → OrchestrationService pipeline
            //    TTS output from pipeline → pushAudioFrame back to the remote user
            OrchestrationService.ResponseCallback responseCallback = (sid, type, data) -> {
                if (TTS_AUDIO_TYPE.equals(type) && data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    String audioBase64 = (String) ((Map<String, Object>) data).get("audio");
                    if (audioBase64 != null) {
                        byte[] ttsAudio = Base64.getDecoder().decode(audioBase64);
                        aliRTCClient.pushAudioFrame(channelId, ttsAudio,
                            DEFAULT_SAMPLE_RATE, DEFAULT_CHANNELS);
                    }
                }
            };
            aliRTCClient.registerAudioDataCallback(channelId,
                (ch, uid, pcmData, sr, ch2) ->
                    orchestrationService.processAudioStream(sessionId, pcmData, responseCallback));

            // 3. Generate client authInfo
            String clientAuthInfo = aliRTCClient.generateAuthInfo(channelId, userId);

            AliRTCSessionInfo sessionInfo = new AliRTCSessionInfo(
                sessionId, userId, channelId, clientAuthInfo,
                aliRTCClient.getAppId());
            sessions.put(sessionId, sessionInfo);

            logger.info("[AliRTC] Session created successfully: {}", sessionId);
            return sessionId;

        } catch (Exception e) {
            logger.error("[AliRTC] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create AliRTC WebRTC session", e);
        }
    }

    /**
     * Returns AliRTC connection info (appId + channelId + userId + authInfo).
     *
     * <p>AliRTC handles SDP and ICE negotiation internally. The returned JSON
     * contains all credentials needed for the Web SDK client to join the channel.</p>
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
            node.put("authInfo", session.getAuthInfo());
            node.put("strategy", getStrategyName());

            logger.debug("[AliRTC] Returning connection info for session: {}", sessionId);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("[AliRTC] Failed to serialize connection info for session: {}", sessionId, e);
            throw new RuntimeException("Failed to serialize AliRTC connection info", e);
        }
    }

    /** No-op — AliRTC handles ICE negotiation internally. */
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        logger.debug("[AliRTC] ICE negotiation handled internally by AliRTC for session: {}", sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        try {
            AliRTCSessionInfo session = sessions.remove(sessionId);
            if (session != null) {
                aliRTCClient.leaveChannel(session.getChannelId());
                orchestrationService.cleanupSession(sessionId);
                logger.info("[AliRTC] Session closed: sessionId={}, channelId={}",
                    sessionId, session.getChannelId());
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
     * Internal session info for AliRTC strategy.
     * 阿里云 ARTC 策略的内部会话信息
     */
    static class AliRTCSessionInfo {
        private final String sessionId;
        private final String userId;
        private final String channelId;
        private final String authInfo;
        private final String appId;

        AliRTCSessionInfo(String sessionId, String userId,
                           String channelId, String authInfo, String appId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.channelId = channelId;
            this.authInfo = authInfo;
            this.appId = appId;
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public String getAuthInfo() { return authInfo; }
        public String getAppId() { return appId; }
    }
}
