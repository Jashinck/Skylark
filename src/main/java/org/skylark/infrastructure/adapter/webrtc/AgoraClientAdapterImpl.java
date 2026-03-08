package org.skylark.infrastructure.adapter.webrtc;

import io.agora.rtc.AgoraLocalAudioTrack;
import io.agora.rtc.AgoraLocalUser;
import io.agora.rtc.AgoraAudioPcmDataSender;
import io.agora.rtc.AgoraMediaNodeFactory;
import io.agora.rtc.AgoraRtcConn;
import io.agora.rtc.AgoraService;
import io.agora.rtc.AgoraServiceConfig;
import io.agora.rtc.AudioFrame;
import io.agora.rtc.DefaultAudioFrameObserver;
import io.agora.rtc.DefaultLocalUserObserver;
import io.agora.rtc.DefaultRtcConnObserver;
import io.agora.rtc.RtcConnConfig;
import io.agora.rtc.RtcConnInfo;
import io.agora.rtc.VadProcessResult;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.common.util.AgoraTokenBuilder;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agora Client Adapter Implementation
 * 声网客户端适配器实现
 *
 * <p>Manages Agora RTC Server SDK lifecycle and provides operations for
 * token generation, channel management, and audio frame processing.</p>
 *
 * <p>Token generation is implemented using pure-Java HMAC-SHA256 (no external SDK needed).
 * Full channel operations (join/leave, audio send/receive) require the Agora RTC Server SDK
 * (io.agora.rtc:linux-java-sdk, Maven Central) and its native .so libraries.</p>
 *
 * <p><b>Audio pipeline integration:</b></p>
 * <ul>
 *   <li><b>Receiving audio:</b> When a remote user sends audio, the SDK fires
 *       {@code IAudioFrameObserver.onPlaybackAudioFrameBeforeMixing()}, which extracts
 *       the PCM data and forwards it to the registered {@link AudioFrameCallback}
 *       (typically wired to {@code OrchestrationService.processAudioStream}).</li>
 *   <li><b>Sending audio:</b> TTS output PCM data is pushed to the channel via
 *       {@link #sendAudioFrame}, which uses {@code AgoraAudioPcmDataSender.send()}.</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 2.0.0
 * @see AgoraClientAdapter
 * @see io.agora.rtc.AgoraService
 */
@Component
public class AgoraClientAdapterImpl implements AgoraClientAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgoraClientAdapterImpl.class);

    /** Default sample rate for audio frames (16 kHz) */
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    /** Default number of audio channels (mono) */
    private static final int DEFAULT_CHANNELS = 1;
    /** Bytes per PCM16 sample */
    private static final int BYTES_PER_SAMPLE = 2;

    private final WebRTCProperties webRTCProperties;
    private final ConcurrentHashMap<String, AudioFrameCallback> callbacks = new ConcurrentHashMap<>();
    /** Per-channel SDK resources for active connections */
    private final ConcurrentHashMap<String, ChannelContext> channelContexts = new ConcurrentHashMap<>();

    /** Singleton AgoraService instance (null if native SDK not initialized) */
    private volatile AgoraService agoraService;
    /** Whether the Agora RTC Server SDK engine is available for channel operations */
    private volatile boolean sdkAvailable = false;
    /** Whether token generation is available (only needs appId + appCertificate) */
    private volatile boolean tokenAvailable = false;

    @Autowired
    public AgoraClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }

    /**
     * Initializes the Agora client adapter.
     * 初始化声网客户端适配器
     *
     * <p>Token generation is always available when appId and appCertificate are configured.
     * Full RTC Engine functionality (join/leave/audio) requires the native .so libraries.</p>
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

            // Attempt to initialize the Agora RTC Server SDK
            initAgoraService(config);

        } catch (Exception e) {
            logger.error("[Agora] Failed to initialize adapter", e);
            sdkAvailable = false;
        }
    }

    /**
     * Initializes the AgoraService singleton with the given configuration.
     * The SDK JAR (io.agora.rtc:linux-java-sdk) is on the classpath via Maven Central.
     * Native .so libraries must be in LD_LIBRARY_PATH for full functionality.
     */
    private void initAgoraService(WebRTCProperties.Agora config) {
        try {
            AgoraServiceConfig serviceConfig = new AgoraServiceConfig();
            serviceConfig.setAppId(config.getAppId());
            serviceConfig.setEnableAudioDevice(0);       // no physical audio device on server
            serviceConfig.setEnableAudioProcessor(1);     // enable audio processing
            serviceConfig.setEnableVideo(0);              // audio only

            AgoraService service = new AgoraService();
            int ret = service.initialize(serviceConfig);
            if (ret != 0) {
                logger.warn("[Agora] AgoraService.initialize() returned error code: {}. "
                    + "Native .so libraries may not be available. "
                    + "Channel operations will be no-op.", ret);
                return;
            }

            this.agoraService = service;
            sdkAvailable = true;
            logger.info("[Agora] ✅ AgoraService initialized successfully. "
                + "Channel join/leave and audio frame operations are available.");
        } catch (UnsatisfiedLinkError e) {
            logger.info("[Agora] Native .so libraries not available ({}). "
                + "Channel join/leave and audio frame operations will be no-op. "
                + "Token generation remains functional.", e.getMessage());
            sdkAvailable = false;
        } catch (Exception e) {
            logger.info("[Agora] AgoraService initialization failed ({}). "
                + "Channel operations will be no-op. Token generation remains functional.",
                e.getMessage());
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
        if (!sdkAvailable || agoraService == null) {
            logger.debug("[Agora] SDK not available, skipping joinChannel for: {}", channelName);
            return;
        }

        try {
            // 1. Create RTC connection config (audio only, broadcaster role)
            RtcConnConfig connConfig = new RtcConnConfig();
            connConfig.setAutoSubscribeAudio(1);
            connConfig.setAutoSubscribeVideo(0);
            connConfig.setClientRoleType(1);  // BROADCASTER
            connConfig.setChannelProfile(1);  // LIVE_BROADCASTING

            // 2. Create connection from AgoraService
            AgoraRtcConn conn = agoraService.agoraRtcConnCreate(connConfig);
            if (conn == null) {
                logger.error("[Agora] Failed to create AgoraRtcConn for channel: {}", channelName);
                return;
            }

            // 3. Register connection observer for lifecycle events
            conn.registerObserver(new DefaultRtcConnObserver() {
                @Override
                public void onConnected(AgoraRtcConn agConn, RtcConnInfo connInfo, int reason) {
                    logger.info("[Agora] Connected to channel: {}, reason: {}", channelName, reason);
                }

                @Override
                public void onDisconnected(AgoraRtcConn agConn, RtcConnInfo connInfo, int reason) {
                    logger.info("[Agora] Disconnected from channel: {}, reason: {}", channelName, reason);
                }

                @Override
                public void onUserJoined(AgoraRtcConn agConn, String remoteUserId) {
                    logger.info("[Agora] Remote user joined channel {}: {}", channelName, remoteUserId);
                }

                @Override
                public void onUserLeft(AgoraRtcConn agConn, String remoteUserId, int reason) {
                    logger.info("[Agora] Remote user left channel {}: {}, reason: {}",
                        channelName, remoteUserId, reason);
                }

                @Override
                public void onError(AgoraRtcConn agConn, int error, String msg) {
                    logger.error("[Agora] Error on channel {}: code={}, msg={}", channelName, error, msg);
                }
            });

            // 4. Get local user and set up audio receiving
            AgoraLocalUser localUser = conn.getLocalUser();
            localUser.setUserRole(1);  // BROADCASTER
            localUser.subscribeAllAudio();

            // 5. Register local user observer for remote audio track events
            localUser.registerObserver(new DefaultLocalUserObserver() {
                @Override
                public void onUserAudioTrackSubscribed(AgoraLocalUser user, String remoteUserId,
                        io.agora.rtc.AgoraRemoteAudioTrack remoteAudioTrack) {
                    logger.info("[Agora] Subscribed to audio track from user {} in channel {}",
                        remoteUserId, channelName);
                }
            });

            // 6. Register audio frame observer — this is the core audio listener
            //    Receives remote PCM audio data and forwards to registered callback
            localUser.setPlaybackAudioFrameBeforeMixingParameters(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNELS);
            localUser.registerAudioFrameObserver(new DefaultAudioFrameObserver() {
                @Override
                public int onPlaybackAudioFrameBeforeMixing(AgoraLocalUser user, String channelId,
                        String remoteUserId, AudioFrame frame, VadProcessResult vadResult) {
                    AudioFrameCallback cb = callbacks.get(channelName);
                    if (cb != null) {
                        ByteBuffer buffer = frame.getBuffer();
                        if (buffer != null) {
                            byte[] pcmData = new byte[buffer.remaining()];
                            buffer.get(pcmData);
                            cb.onAudioFrame(channelName, remoteUserId, pcmData,
                                frame.getSamplesPerSec(), frame.getChannels());
                        }
                    }
                    return 1; // return 1 to indicate the frame is consumed
                }

                @Override
                public int getObservedAudioFramePosition() {
                    return 0x0020; // AUDIO_FRAME_POSITION_BEFORE_MIXING
                }
            });

            // 7. Set up audio sending (for TTS output)
            AgoraMediaNodeFactory factory = agoraService.createMediaNodeFactory();
            AgoraAudioPcmDataSender pcmSender = factory.createAudioPcmDataSender();
            AgoraLocalAudioTrack audioTrack = agoraService.createCustomAudioTrackPcm(pcmSender);
            audioTrack.setEnabled(1);
            localUser.publishAudio(audioTrack);

            // 8. Generate server token and connect to channel
            String serverToken = generateToken(channelName, userId, 86400);
            int connectResult = conn.connect(serverToken, channelName, userId);
            if (connectResult != 0) {
                logger.error("[Agora] Failed to connect to channel: {}, error: {}", channelName, connectResult);
                // Cleanup on failure
                audioTrack.destroy();
                pcmSender.destroy();
                factory.destroy();
                conn.destroy();
                return;
            }

            // 9. Store per-channel context for later use
            ChannelContext ctx = new ChannelContext(conn, localUser, pcmSender, audioTrack, factory);
            channelContexts.put(channelName, ctx);

            logger.info("[Agora] ✅ Joined channel: {} as user: {} with audio listener active",
                channelName, userId);

        } catch (Exception e) {
            logger.error("[Agora] Failed to join channel: {}", channelName, e);
        }
    }

    @Override
    public void leaveChannel(String channelName) {
        callbacks.remove(channelName);

        // Clean up SDK resources for this channel
        ChannelContext ctx = channelContexts.remove(channelName);
        if (ctx != null) {
            try {
                // Unpublish and disable audio track
                ctx.localUser.unpublishAudio(ctx.audioTrack);
                ctx.audioTrack.setEnabled(0);

                // Unregister observers
                ctx.localUser.unregisterAudioFrameObserver();
                ctx.localUser.unregisterObserver();

                // Disconnect from channel
                ctx.conn.disconnect();

                // Destroy resources in order
                ctx.audioTrack.destroy();
                ctx.pcmSender.destroy();
                ctx.factory.destroy();
                ctx.conn.unregisterObserver();
                ctx.conn.destroy();

                logger.info("[Agora] Left channel and cleaned up resources: {}", channelName);
            } catch (Exception e) {
                logger.error("[Agora] Error cleaning up channel resources: {}", channelName, e);
            }
        } else if (!sdkAvailable) {
            logger.debug("[Agora] SDK not available, channel cleanup for: {}", channelName);
        } else {
            logger.debug("[Agora] No active connection for channel: {}", channelName);
        }
    }

    @Override
    public void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels) {
        if (!sdkAvailable) {
            return;
        }

        ChannelContext ctx = channelContexts.get(channelName);
        if (ctx == null) {
            logger.debug("[Agora] No active channel context for sendAudioFrame: {}", channelName);
            return;
        }

        try {
            int samplesPerChannel = pcmData.length / (channels * BYTES_PER_SAMPLE);
            long timestamp = System.currentTimeMillis();
            ctx.pcmSender.send(pcmData, (int) timestamp, samplesPerChannel,
                BYTES_PER_SAMPLE, channels, sampleRate);
        } catch (Exception e) {
            logger.error("[Agora] Failed to send audio frame on channel: {}", channelName, e);
        }
    }

    @Override
    public void registerAudioFrameCallback(String channelName, AudioFrameCallback callback) {
        callbacks.put(channelName, callback);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns true when token generation is available (appId + appCertificate configured).
     * Note: channel operations additionally require the native SDK ({@link #isSdkAvailable()}).</p>
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
     * Checks if the Agora RTC Server SDK is available for channel operations.
     * 检查声网 RTC 服务端 SDK 是否可用于频道操作
     *
     * <p>Returns true only when AgoraService was successfully initialized,
     * meaning both the SDK JAR and native .so libraries are available.</p>
     *
     * @return true if the SDK is fully initialized
     */
    public boolean isSdkAvailable() {
        return sdkAvailable;
    }

    /**
     * Cleans up Agora RTC Engine resources.
     * 清理声网 RTC 引擎资源
     */
    @PreDestroy
    public void destroy() {
        // Clean up all active channel connections
        for (String channelName : channelContexts.keySet()) {
            leaveChannel(channelName);
        }
        callbacks.clear();

        // Destroy the AgoraService singleton
        if (agoraService != null) {
            try {
                agoraService.destroy();
            } catch (Exception e) {
                logger.warn("[Agora] Error destroying AgoraService", e);
            }
            agoraService = null;
        }

        sdkAvailable = false;
        tokenAvailable = false;
        logger.info("[Agora] Agora Client Adapter destroyed");
    }

    /**
     * Per-channel SDK resource context.
     * Holds all Agora SDK objects associated with a single channel connection.
     */
    static class ChannelContext {
        final AgoraRtcConn conn;
        final AgoraLocalUser localUser;
        final AgoraAudioPcmDataSender pcmSender;
        final AgoraLocalAudioTrack audioTrack;
        final AgoraMediaNodeFactory factory;

        ChannelContext(AgoraRtcConn conn, AgoraLocalUser localUser,
                       AgoraAudioPcmDataSender pcmSender, AgoraLocalAudioTrack audioTrack,
                       AgoraMediaNodeFactory factory) {
            this.conn = conn;
            this.localUser = localUser;
            this.pcmSender = pcmSender;
            this.audioTrack = audioTrack;
            this.factory = factory;
        }
    }
}
