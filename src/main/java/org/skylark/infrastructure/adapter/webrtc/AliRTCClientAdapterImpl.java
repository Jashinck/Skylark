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
 * <p>Manages the AliRTC (Alibaba Cloud ARTC) Linux SDK lifecycle and provides operations for
 * authInfo generation, channel management, and audio frame processing.</p>
 *
 * <p><b>AuthInfo generation</b> is implemented in pure Java (HMAC-SHA256, no external SDK required)
 * and is available whenever {@code webrtc.alirtc.appId}, {@code appKey} and {@code appSecret}
 * are configured.</p>
 *
 * <p><b>Full channel operations</b> (join/leave, audio push/receive) require:
 * <ol>
 *   <li>The AliRTC Linux SDK JAR placed in {@code libs/alirtc-linux-sdk-2.0.0.jar}</li>
 *   <li>The native {@code libAliRtcEngine.so} library in the system library path</li>
 * </ol>
 * When these are not available the adapter starts in <em>degraded mode</em> — authInfo generation
 * still works; channel join/leave and audio push/receive are silent no-ops.</p>
 *
 * <p><b>Audio pipeline integration:</b></p>
 * <ul>
 *   <li><b>Receiving audio:</b> The AliRTC SDK fires the {@code AliRtcAudioObserver.onRemoteAudioData}
 *       callback which is forwarded to the registered {@link AliRTCClientAdapter.AudioDataCallback}
 *       (typically wired to {@code OrchestrationService.processAudioStream}).</li>
 *   <li><b>Sending audio:</b> TTS output PCM data is pushed to the channel via
 *       {@link #pushAudioFrame}, which calls {@code AliRtcEngine.pushExternalAudioFrame}.</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see AliRTCClientAdapter
 * @see <a href="https://help.aliyun.com/zh/live/artc-download-the-sdk">AliRTC SDK Download</a>
 */
@Component
public class AliRTCClientAdapterImpl implements AliRTCClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AliRTCClientAdapterImpl.class);

    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNELS = 1;

    /**
     * Attempt to load the AliRTC native library at class initialization time.
     * Failure is not fatal — the adapter runs in degraded mode.
     */
    static {
        try {
            System.loadLibrary("AliRtcEngine");
            logger.info("[AliRTC] Native library AliRtcEngine loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            // Not fatal — degraded mode (channel ops become no-op)
            System.err.println("[AliRTC] Warning: Native library AliRtcEngine not found: " + e.getMessage()
                + ". Channel operations will be no-op.");
        }
    }

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioDataCallback> callbacks = new ConcurrentHashMap<>();

    /**
     * The engine handle — kept as Object so the class compiles even when the
     * AliRTC SDK JAR is not on the classpath (system-scope, optional).
     */
    private volatile Object engine;
    private volatile boolean sdkAvailable = false;
    private volatile boolean authAvailable = false;

    @Autowired
    public AliRTCClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    /**
     * Initializes the adapter.
     * AuthInfo generation is enabled when appId + appKey + appSecret are configured.
     * Full SDK operations require the native library.
     */
    @PostConstruct
    public void init() {
        try {
            WebRTCProperties.AliRtc config = webRTCProperties.getAlirtc();

            if (config.getAppId() == null || config.getAppId().isEmpty()) {
                logger.warn("[AliRTC] appId not configured. AliRTC features will not be available.");
                return;
            }

            // Auth generation needs appId + appKey + appSecret
            if (config.getAppKey() != null && !config.getAppKey().isEmpty()
                    && config.getAppSecret() != null && !config.getAppSecret().isEmpty()) {
                authAvailable = true;
                logger.info("[AliRTC] ✅ AuthInfo generation enabled (appId: {}***)",
                    config.getAppId().substring(0, Math.min(4, config.getAppId().length())));
            } else {
                logger.warn("[AliRTC] appKey or appSecret not configured. "
                    + "AuthInfo generation will use placeholder tokens.");
            }

            // Attempt to initialize the native engine
            initEngine(config);

        } catch (Exception e) {
            logger.error("[AliRTC] Failed to initialize adapter", e);
            sdkAvailable = false;
        }
    }

    /**
     * Attempts to create the AliRTC engine using reflection so the class compiles
     * without the SDK JAR on the classpath.
     */
    private void initEngine(WebRTCProperties.AliRtc config) {
        try {
            Class<?> engineClass = Class.forName("com.aliyun.artc.AliRtcEngine");
            // AliRtcEngine.create(extras, eventListener) — Linux SDK signature
            Class<?> listenerClass = Class.forName("com.aliyun.artc.AliRtcEngine$AliRtcEventListener");
            java.lang.reflect.Method createMethod = engineClass.getMethod("create", String.class, listenerClass);

            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("onJoinChannelResult".equals(name)) {
                        logger.info("[AliRTC] Joined channel: {}, userId: {}, result: {}",
                            args[1], args[2], args[0]);
                    } else if ("onLeaveChannelResult".equals(name)) {
                        logger.info("[AliRTC] Left channel, result: {}", args[0]);
                    } else if ("onRemoteUserOnLine".equals(name)) {
                        logger.info("[AliRTC] Remote user online: {} in {}", args[1], args[0]);
                    } else if ("onRemoteUserOffLine".equals(name)) {
                        logger.info("[AliRTC] Remote user offline: {} in {}, reason: {}",
                            args[1], args[0], args[2]);
                    } else if ("onOccurError".equals(name)) {
                        logger.error("[AliRTC] Error: {} - {}", args[0], args[1]);
                    }
                    return null;
                });

            this.engine = createMethod.invoke(null, "{}", listener);

            // Register audio observer for receiving remote PCM frames
            Class<?> observerClass = Class.forName("com.aliyun.artc.AliRtcAudioObserver");
            Class<?> audioDataClass = Class.forName("com.aliyun.artc.AliRtcAudioData");
            java.lang.reflect.Method registerObserver = engineClass.getMethod(
                "registerAudioObserver", observerClass);

            Object observer = java.lang.reflect.Proxy.newProxyInstance(
                observerClass.getClassLoader(),
                new Class<?>[]{observerClass},
                (proxy, method, args) -> {
                    if ("onRemoteAudioData".equals(method.getName())) {
                        String channelId = (String) args[0];
                        String userId = (String) args[1];
                        Object audioData = args[2];
                        byte[] data = (byte[]) audioDataClass.getField("data").get(audioData);
                        int sr = (int) audioDataClass.getField("sampleRate").get(audioData);
                        int ch = (int) audioDataClass.getField("channels").get(audioData);
                        AudioDataCallback cb = callbacks.get(channelId);
                        if (cb != null) {
                            cb.onAudioData(channelId, userId, data, sr, ch);
                        }
                        return true;
                    }
                    return null;
                });
            registerObserver.invoke(this.engine, observer);

            // Enable external audio source for TTS push
            java.lang.reflect.Method setExternal = engineClass.getMethod(
                "setExternalAudioSource", boolean.class, int.class, int.class);
            setExternal.invoke(this.engine, true,
                config.getSampleRate(), config.getChannels());

            sdkAvailable = true;
            logger.info("[AliRTC] ✅ Engine initialized. Channel join/leave and audio frame operations are available.");

        } catch (ClassNotFoundException e) {
            logger.info("[AliRTC] AliRTC SDK JAR not on classpath ({}). "
                + "Place libs/alirtc-linux-sdk-2.0.0.jar to enable full channel operations. "
                + "AuthInfo generation remains functional.", e.getMessage());
        } catch (UnsatisfiedLinkError e) {
            logger.info("[AliRTC] Native .so libraries not available ({}). "
                + "Channel operations will be no-op. AuthInfo generation remains functional.",
                e.getMessage());
        } catch (Exception e) {
            logger.warn("[AliRTC] Engine initialization failed ({}). "
                + "Channel operations will be no-op.", e.getMessage());
        }
    }

    @Override
    public String generateAuthInfo(String channelId, String userId) {
        WebRTCProperties.AliRtc config = webRTCProperties.getAlirtc();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis() / 1000;

        if (authAvailable) {
            try {
                String rawText = config.getAppKey() + channelId + userId + nonce + timestamp;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(
                    config.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(rawText.getBytes(StandardCharsets.UTF_8)));

                return String.format(
                    "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\","
                        + "\"nonce\":\"%s\",\"timestamp\":%d,\"token\":\"%s\"}",
                    config.getAppId(), channelId, userId, nonce, timestamp, signature);
            } catch (Exception e) {
                throw new RuntimeException("[AliRTC] Failed to generate authInfo", e);
            }
        }

        // Placeholder (appKey/appSecret not configured)
        logger.debug("[AliRTC] authKey not configured — returning placeholder authInfo for channel: {}",
            channelId);
        return String.format(
            "{\"appId\":\"%s\",\"channelId\":\"%s\",\"userId\":\"%s\","
                + "\"nonce\":\"%s\",\"timestamp\":%d,\"token\":\"placeholder-%s\"}",
            config.getAppId(), channelId, userId, nonce, timestamp, channelId);
    }

    @Override
    public void joinChannel(String channelId, String userId, String authInfo) {
        if (!sdkAvailable || engine == null) {
            logger.debug("[AliRTC] joinChannel no-op (SDK not available): {}", channelId);
            return;
        }
        try {
            Class<?> engineClass = engine.getClass();
            java.lang.reflect.Method joinMethod = engineClass.getMethod(
                "joinChannel", String.class, String.class, String.class, Object.class);
            int ret = (int) joinMethod.invoke(engine, authInfo, channelId, userId, null);
            if (ret != 0) {
                logger.warn("[AliRTC] joinChannel returned error code: {} for channel: {}", ret, channelId);
            } else {
                logger.info("[AliRTC] Joining channel: {}", channelId);
            }
        } catch (Exception e) {
            logger.error("[AliRTC] joinChannel failed for channel: {}", channelId, e);
        }
    }

    @Override
    public void leaveChannel(String channelId) {
        callbacks.remove(channelId);
        if (!sdkAvailable || engine == null) {
            logger.debug("[AliRTC] leaveChannel no-op (SDK not available): {}", channelId);
            return;
        }
        try {
            engine.getClass().getMethod("leaveChannel").invoke(engine);
            logger.info("[AliRTC] Left channel: {}", channelId);
        } catch (Exception e) {
            logger.error("[AliRTC] leaveChannel failed for channel: {}", channelId, e);
        }
    }

    @Override
    public void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels) {
        if (!sdkAvailable || engine == null || pcmData == null || pcmData.length == 0) {
            return;
        }
        try {
            Class<?> engineClass = engine.getClass();
            Class<?> audioDataClass = Class.forName("com.aliyun.artc.AliRtcAudioData");
            Object frame = audioDataClass.getConstructor().newInstance();
            audioDataClass.getField("data").set(frame, pcmData);
            audioDataClass.getField("sampleRate").setInt(frame, sampleRate);
            audioDataClass.getField("channels").setInt(frame, channels);
            audioDataClass.getField("samples").setInt(frame, pcmData.length / (channels * 2));

            java.lang.reflect.Method pushMethod = engineClass.getMethod(
                "pushExternalAudioFrame", audioDataClass, long.class);
            pushMethod.invoke(engine, frame, System.currentTimeMillis());
        } catch (ClassNotFoundException e) {
            // SDK not available — silent no-op
        } catch (Exception e) {
            logger.debug("[AliRTC] pushAudioFrame failed: {}", e.getMessage());
        }
    }

    @Override
    public void registerAudioDataCallback(String channelId, AudioDataCallback callback) {
        if (callback != null) {
            callbacks.put(channelId, callback);
        } else {
            callbacks.remove(channelId);
        }
    }

    @Override
    public boolean isAvailable() {
        return sdkAvailable && engine != null;
    }

    @Override
    public String getAppId() {
        return webRTCProperties.getAlirtc().getAppId();
    }

    @PreDestroy
    public void destroy() {
        if (engine != null) {
            try {
                engine.getClass().getMethod("leaveChannel").invoke(engine);
                Class.forName("com.aliyun.artc.AliRtcEngine")
                    .getMethod("destroy").invoke(null);
            } catch (Exception e) {
                logger.debug("[AliRTC] Error during destroy: {}", e.getMessage());
            }
            engine = null;
            sdkAvailable = false;
            logger.info("[AliRTC] Engine destroyed");
        }
    }
}
