package org.skylark.infrastructure.adapter.webrtc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>Requires Agora RTC Server SDK (io.agora.rtc:linux-sdk) to be installed.
 * When the SDK is not available, the adapter gracefully degrades and
 * {@link #isAvailable()} returns false.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Component
public class AgoraClientAdapterImpl implements AgoraClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgoraClientAdapterImpl.class);

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioFrameCallback> callbacks = new ConcurrentHashMap<>();

    private volatile boolean available = false;

    @Autowired
    public AgoraClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    /**
     * Initializes the Agora RTC Engine
     * 初始化声网 RTC 引擎
     */
    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.Agora config = webRTCProperties.getAgora();
            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[Agora] appId not configured. Agora features will not be available.");
                return;
            }

            // Agora RTC Server SDK (io.agora.rtc:linux-sdk) is required for full functionality.
            // When the SDK JAR is installed, uncomment and implement the SDK initialization:
            //
            // RtcEngineConfig engineConfig = new RtcEngineConfig();
            // engineConfig.mAppId = config.getAppId();
            // engineConfig.mEventHandler = new RtcEngine.RtcEventHandler() { ... };
            // rtcEngine = RtcEngine.create(engineConfig);
            // rtcEngine.enableExternalAudioSource(true, config.getSampleRate(), config.getChannels());
            // rtcEngine.registerAudioFrameObserver(buildAudioFrameObserver());

            logger.warn("[Agora] Agora RTC Server SDK not installed. "
                + "Install io.agora.rtc:linux-sdk to enable full Agora RTC functionality. "
                + "AppId configured: {}", config.getAppId().substring(0, Math.min(4, config.getAppId().length())) + "***");
            available = false;
        } catch (Exception e) {
            logger.error("[Agora] Failed to initialize RTC Engine", e);
            available = false;
        }
    }

    @Override
    public String generateToken(String channelName, String userId, int expireSeconds) {
        WebRTCProperties.Agora config = webRTCProperties.getAgora();
        if (!available) {
            // Return a placeholder token when SDK is not available
            // In production with SDK installed, use RtcTokenBuilder2:
            // return RtcTokenBuilder2.buildTokenWithUserAccount(
            //     config.getAppId(), config.getAppCertificate(),
            //     channelName, userId, Role.ROLE_PUBLISHER,
            //     expireSeconds, expireSeconds);
            logger.warn("[Agora] SDK not available, returning placeholder token for channel: {}", channelName);
            return "agora-token-placeholder-" + channelName + "-" + userId;
        }
        throw new IllegalStateException("Agora Engine not initialized");
    }

    @Override
    public void joinChannel(String channelName, String userId) {
        if (!available) {
            logger.warn("[Agora] SDK not available, cannot join channel: {}", channelName);
            return;
        }
        throw new IllegalStateException("Agora Engine not initialized");
    }

    @Override
    public void leaveChannel(String channelName) {
        callbacks.remove(channelName);
        if (!available) {
            logger.debug("[Agora] SDK not available, channel cleanup for: {}", channelName);
            return;
        }
    }

    @Override
    public void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels) {
        if (!available) {
            return;
        }
    }

    @Override
    public void registerAudioFrameCallback(String channelName, AudioFrameCallback callback) {
        callbacks.put(channelName, callback);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAgora().getAppId();
    }

    /**
     * Cleans up Agora RTC Engine resources
     * 清理声网 RTC 引擎资源
     */
    @PreDestroy
    public void destroy() {
        callbacks.clear();
        available = false;
        logger.info("[Agora] Agora Client Adapter destroyed");
    }
}
