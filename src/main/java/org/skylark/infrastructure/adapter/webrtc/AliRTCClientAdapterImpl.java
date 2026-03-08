package org.skylark.infrastructure.adapter.webrtc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AliRTC Client Adapter Implementation
 * 阿里云 ARTC 客户端适配器实现
 * 
 * <p>Manages AliRTC Engine lifecycle and provides operations for
 * authentication, channel management, and audio frame processing.</p>
 * 
 * <p>Requires AliRTC Linux SDK (com.aliyun.artc:alirtc-linux-sdk) to be installed.
 * When the SDK is not available, the adapter gracefully degrades and
 * {@link #isAvailable()} returns false.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Component
public class AliRTCClientAdapterImpl implements AliRTCClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AliRTCClientAdapterImpl.class);

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioDataCallback> callbacks = new ConcurrentHashMap<>();

    private volatile boolean available = false;

    @Autowired
    public AliRTCClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    /**
     * Initializes the AliRTC Engine
     * 初始化阿里云 ARTC 引擎
     */
    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.AliRTC config = webRTCProperties.getAlirtc();
            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[AliRTC] appId not configured. AliRTC features will not be available.");
                return;
            }

            // AliRTC Linux SDK (com.aliyun.artc:alirtc-linux-sdk) is required for full functionality.
            // When the SDK is installed, uncomment and implement the SDK initialization:
            //
            // System.loadLibrary("AliRtcEngine");
            // engine = AliRtcEngine.create("{}", eventListener);
            // engine.registerAudioObserver(audioObserver);
            // engine.setExternalAudioSource(true, config.getSampleRate(), config.getChannels());

            logger.warn("[AliRTC] AliRTC Linux SDK not installed. "
                + "Install com.aliyun.artc:alirtc-linux-sdk to enable full AliRTC functionality. "
                + "AppId configured: {}", config.getAppId().substring(0, Math.min(4, config.getAppId().length())) + "***");
            available = false;
        } catch (Exception e) {
            logger.error("[AliRTC] Failed to initialize AliRTC Engine", e);
            available = false;
        }
    }

    @Override
    public String generateAuthInfo(String channelId, String userId) {
        try {
            WebRTCProperties.AliRTC config = webRTCProperties.getAlirtc();
            String nonce = UUID.randomUUID().toString().replace("-", "");
            long timestamp = System.currentTimeMillis() / 1000;

            String appKey = config.getAppKey();
            String appSecret = config.getAppSecret();

            if (appKey != null && !appKey.isEmpty() && appSecret != null && !appSecret.isEmpty()) {
                // Generate HMAC-SHA256 signature
                String rawText = appKey + channelId + userId + nonce + timestamp;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(rawText.getBytes(StandardCharsets.UTF_8)));

                return String.format(
                    "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\","
                        + "\"nonce\":\"%s\",\"timestamp\":%d,\"token\":\"%s\"}",
                    config.getAppId(), channelId, userId, nonce, timestamp, signature);
            } else {
                // Return placeholder auth info when credentials not configured
                logger.warn("[AliRTC] AppKey/AppSecret not configured, returning placeholder authInfo");
                return String.format(
                    "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\","
                        + "\"nonce\":\"%s\",\"timestamp\":%d,\"token\":\"placeholder-token\"}",
                    config.getAppId(), channelId, userId, nonce, timestamp);
            }
        } catch (Exception e) {
            throw new RuntimeException("[AliRTC] Failed to generate authInfo", e);
        }
    }

    @Override
    public void joinChannel(String channelId, String userId, String authInfo) {
        if (!available) {
            logger.warn("[AliRTC] SDK not available, cannot join channel: {}", channelId);
            return;
        }
        throw new IllegalStateException("AliRTC Engine not initialized");
    }

    @Override
    public void leaveChannel(String channelId) {
        callbacks.remove(channelId);
        if (!available) {
            logger.debug("[AliRTC] SDK not available, channel cleanup for: {}", channelId);
            return;
        }
    }

    @Override
    public void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels) {
        if (!available) {
            return;
        }
    }

    @Override
    public void registerAudioDataCallback(String channelId, AudioDataCallback callback) {
        callbacks.put(channelId, callback);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAlirtc().getAppId();
    }

    /**
     * Cleans up AliRTC Engine resources
     * 清理阿里云 ARTC 引擎资源
     */
    @PreDestroy
    public void destroy() {
        callbacks.clear();
        available = false;
        logger.info("[AliRTC] AliRTC Client Adapter destroyed");
    }
}
