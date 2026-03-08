package org.skylark.infrastructure.adapter.webrtc.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.service.OrchestrationService;
import org.skylark.infrastructure.adapter.webrtc.AgoraClientAdapter;

import java.util.Base64;
import java.util.Map;
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
 * <p>Integrates with {@link OrchestrationService} for the
 * VAD → ASR → LLM → TTS audio processing pipeline.
 * When audio frames are received from the remote user, they are forwarded
 * through the orchestration pipeline. TTS output is sent back via
 * {@link AgoraClientAdapter#sendAudioFrame}.</p>
 * 
 * @author Skylark Team
 * @version 1.1.0
 */
public class AgoraChannelStrategy implements WebRTCChannelStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AgoraChannelStrategy.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNELS = 1;
    private static final String TTS_AUDIO_TYPE = "tts_audio";

    private final AgoraClientAdapter agoraClient;
    private final OrchestrationService orchestrationService;
    private final ConcurrentHashMap<String, AgoraSessionInfo> sessions = new ConcurrentHashMap<>();

    public AgoraChannelStrategy(AgoraClientAdapter agoraClient,
                                 OrchestrationService orchestrationService) {
        this.agoraClient = agoraClient;
        this.orchestrationService = orchestrationService;
    }

    @Override
    public String getStrategyName() {
        return "agora";
    }

    @Override
    public String createSession(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }
        try {
            String sessionId = UUID.randomUUID().toString();
            String channelName = "skylark-" + sessionId;
            logger.info("[Agora] Creating session for user: {}, channel: {}", userId, channelName);

            // 1. Server joins the Agora channel
            agoraClient.joinChannel(channelName, "skylark-server-bot");

            // 2. Register audio frame callback: remote PCM → OrchestrationService pipeline
            //    TTS output from pipeline → sendAudioFrame back to the remote user
            OrchestrationService.ResponseCallback responseCallback = (sid, type, data) -> {
                if (TTS_AUDIO_TYPE.equals(type) && data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    String audioBase64 = (String) ((Map<String, Object>) data).get("audio");
                    if (audioBase64 != null) {
                        byte[] ttsAudio = Base64.getDecoder().decode(audioBase64);
                        agoraClient.sendAudioFrame(channelName, ttsAudio,
                            DEFAULT_SAMPLE_RATE, DEFAULT_CHANNELS);
                    }
                }
            };
            agoraClient.registerAudioFrameCallback(channelName,
                (ch, uid, pcmData, sr, ch2) -> {
                    orchestrationService.processAudioStream(sessionId, pcmData, responseCallback);
                });

            // 3. Generate client connection token
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
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("token", session.getClientToken());
            node.put("channelName", session.getChannelName());
            node.put("appId", agoraClient.getAppId());
            node.put("uid", session.getUserId());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("[Agora] Failed to serialize connection info for session: {}", sessionId, e);
            throw new RuntimeException("Failed to serialize Agora connection info", e);
        }
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
                orchestrationService.cleanupSession(sessionId);
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
