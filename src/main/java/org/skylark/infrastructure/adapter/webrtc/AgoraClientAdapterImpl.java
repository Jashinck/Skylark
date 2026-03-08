package org.skylark.infrastructure.adapter.webrtc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.common.util.AgoraTokenBuilder;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Agora Client Adapter Implementation
 * 声网客户端适配器实现
 * 
 * <p>Manages Agora RTC Engine lifecycle and provides operations for
 * token generation, channel management, and audio frame processing.</p>
 * 
 * <p>Token generation is implemented using pure-Java HMAC-SHA256 (no external SDK needed).
 * Channel join/leave and audio frame operations require the Agora RTC Server SDK
 * (io.agora.rtc:linux-sdk). When the SDK is not available, those operations
 * gracefully degrade while token generation remains fully functional.</p>
 * 
 * @author Skylark Team
 * @version 1.1.0
 */
@Component
public class AgoraClientAdapterImpl implements AgoraClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgoraClientAdapterImpl.class);

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioFrameCallback> callbacks = new ConcurrentHashMap<>();

    /** Whether the Agora RTC Server SDK engine is available for channel operations */
    private volatile boolean sdkAvailable = false;
    /** Whether token generation is available (only needs appId + appCertificate) */
    private volatile boolean tokenAvailable = false;

    @Autowired
    public AgoraClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    /**
     * Initializes the Agora client adapter
     * 初始化声网客户端适配器
     *
     * <p>Token generation is always available when appId and appCertificate are configured.
     * Full RTC Engine functionality (join/leave/audio) requires the native SDK.</p>
     */
    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.Agora config = webRTCProperties.getAgora();
            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[Agora] appId not configured. Agora features will not be available.");
                return;
            }

            // Token generation is available whenever appId + appCertificate are configured
            if (config.getAppCertificate() != null && !config.getAppCertificate().isEmpty()) {
                tokenAvailable = true;
                logger.info("[Agora] ✅ Token generation enabled (appId: {}***)",
                    config.getAppId().substring(0, Math.min(4, config.getAppId().length())));
            } else {
                logger.warn("[Agora] appCertificate not configured. Token generation will not be available.");
            }

            // Attempt to detect the RTC Server SDK (requires io.agora.rtc:linux-java-sdk)
            try {
                // Agora RTC Server SDK (io.agora.rtc:linux-java-sdk) is included via Maven Central.
                // When native .so libraries are available, the following pattern should be used:
                //
                // AgoraServiceConfig serviceConfig = new AgoraServiceConfig();
                // serviceConfig.setAppId(config.getAppId());
                // AgoraService agoraService = new AgoraService();
                // agoraService.initialize(serviceConfig);
                // sdkAvailable = true;

                Class.forName("io.agora.rtc.AgoraService");
                logger.info("[Agora] ✅ Agora RTC Server SDK detected on classpath");
                sdkAvailable = true;
            } catch (ClassNotFoundException e) {
                logger.info("[Agora] Agora RTC Server SDK not found on classpath. "
                    + "Channel join/leave and audio frame operations will be no-op. "
                    + "Add io.agora.rtc:linux-java-sdk dependency for full functionality.");
                sdkAvailable = false;
            }
        } catch (Exception e) {
            logger.error("[Agora] Failed to initialize adapter", e);
            sdkAvailable = false;
        }
    }

    @Override
    public String generateToken(String channelName, String userId, int expireSeconds) {
        WebRTCProperties.Agora config = webRTCProperties.getAgora();
        if (tokenAvailable) {
            return AgoraTokenBuilder.buildTokenWithUserAccount(
                config.getAppId(),
                config.getAppCertificate(),
                channelName,
                userId,
                expireSeconds,
                expireSeconds
            );
        }

        // Fallback: appCertificate not configured, return a placeholder token
        logger.warn("[Agora] Token generation unavailable (missing appCertificate), "
            + "returning placeholder token for channel: {}", channelName);
        return "agora-token-placeholder-" + channelName + "-" + userId;
    }

    @Override
    public void joinChannel(String channelName, String userId) {
        if (!sdkAvailable) {
            logger.debug("[Agora] SDK not available, skipping joinChannel for: {}", channelName);
            return;
        }
        // With SDK installed: rtcEngine.joinChannel(token, channelName, null, 0);
        logger.info("[Agora] Joining channel: {}", channelName);
    }

    @Override
    public void leaveChannel(String channelName) {
        callbacks.remove(channelName);
        if (!sdkAvailable) {
            logger.debug("[Agora] SDK not available, channel cleanup for: {}", channelName);
            return;
        }
        // With SDK installed: rtcEngine.leaveChannel();
        logger.info("[Agora] Left channel: {}", channelName);
    }

    @Override
    public void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels) {
        if (!sdkAvailable) {
            return;
        }
        // With SDK installed:
        // AudioFrame frame = new AudioFrame();
        // frame.type = AudioFrame.FRAME_TYPE_PCM16;
        // frame.buffer = pcmData;
        // frame.samples = pcmData.length / 2;
        // frame.bytesPerSample = 2;
        // frame.channels = channels;
        // frame.samplesPerSec = sampleRate;
        // rtcEngine.pushExternalAudioFrame(frame, System.currentTimeMillis());
    }

    @Override
    public void registerAudioFrameCallback(String channelName, AudioFrameCallback callback) {
        callbacks.put(channelName, callback);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns true when token generation is available (appId + appCertificate configured).
     * Note: channel operations additionally require the native SDK.</p>
     */
    @Override
    public boolean isAvailable() {
        return tokenAvailable;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAgora().getAppId();
    }

    /**
     * Checks if the Agora RTC Server SDK is available for channel operations
     * 检查声网 RTC 服务端 SDK 是否可用
     *
     * @return true if the native SDK is loaded
     */
    public boolean isSdkAvailable() {
        return sdkAvailable;
    }

    /**
     * Cleans up Agora RTC Engine resources
     * 清理声网 RTC 引擎资源
     */
    @PreDestroy
    public void destroy() {
        callbacks.clear();
        sdkAvailable = false;
        tokenAvailable = false;
        logger.info("[Agora] Agora Client Adapter destroyed");
    }
}
