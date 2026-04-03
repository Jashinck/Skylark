package org.skylark.infrastructure.adapter.webrtc;

/**
 * AliRTC Client Adapter Interface
 * 阿里云 ARTC 客户端适配器接口
 *
 * <p>Provides abstraction for AliRTC (Alibaba Cloud ARTC) Server SDK operations including
 * authInfo generation, channel management, and audio frame processing.</p>
 *
 * <p>The AliRTC Linux SDK is not available on Maven Central and must be manually
 * downloaded and placed in {@code libs/alirtc-linux-sdk-2.0.0.jar}.
 * When the SDK is not present, implementations degrade gracefully
 * (token generation still works; channel operations become no-op).</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://help.aliyun.com/zh/live/artc-download-the-sdk">AliRTC SDK Download</a>
 */
public interface AliRTCClientAdapter {

    /**
     * Generates authentication info (authInfo JSON) for joining a channel.
     * 生成加入频道所需的鉴权信息（authInfo JSON）
     *
     * <p>AliRTC auth is HMAC-SHA256 based:
     * {@code signature = HMAC-SHA256(appKey + channelId + userId + nonce + timestamp, appSecret)}
     * </p>
     *
     * @param channelId Channel identifier
     * @param userId    User identifier
     * @return AuthInfo JSON string containing appId, channelId, userId, nonce, timestamp, token
     */
    String generateAuthInfo(String channelId, String userId);

    /**
     * Server-side joins an AliRTC channel.
     * 服务端加入频道
     *
     * @param channelId Channel identifier
     * @param userId    Server-side user identifier (e.g. "skylark-server-bot")
     * @param authInfo  AuthInfo JSON from {@link #generateAuthInfo}
     */
    void joinChannel(String channelId, String userId, String authInfo);

    /**
     * Server-side leaves an AliRTC channel.
     * 服务端离开频道
     *
     * @param channelId Channel identifier
     */
    void leaveChannel(String channelId);

    /**
     * Pushes PCM audio frame to channel (TTS output).
     * 向频道推送 PCM 音频帧（TTS 输出）
     *
     * @param channelId  Channel identifier
     * @param pcmData    PCM audio data (16kHz, 16-bit, mono)
     * @param sampleRate Sample rate (default 16000)
     * @param channels   Number of channels (default 1)
     */
    void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels);

    /**
     * Registers a callback for remote audio data received from the channel (VAD/ASR pipeline entry).
     * 注册远端音频数据回调（VAD/ASR Pipeline 入口）
     *
     * @param channelId Channel identifier
     * @param callback  Audio data callback
     */
    void registerAudioDataCallback(String channelId, AudioDataCallback callback);

    /**
     * Checks if the adapter is initialized and available for channel operations.
     * 检查适配器是否已初始化并可用于频道操作
     *
     * @return true if SDK is available
     */
    boolean isAvailable();

    /**
     * Gets the AliRTC App ID (for frontend SDK use).
     * 获取阿里云 ARTC App ID（供前端 SDK 使用）
     *
     * @return App ID string
     */
    String getAppId();

    /**
     * Functional interface for receiving audio data from a remote user.
     * 接收远端用户音频数据的函数式接口
     */
    @FunctionalInterface
    interface AudioDataCallback {
        /**
         * Called when audio data is received from a remote user.
         *
         * @param channelId  Source channel identifier
         * @param userId     Remote user identifier
         * @param pcmData    Raw PCM audio data
         * @param sampleRate Sample rate of the audio
         * @param channels   Number of audio channels
         */
        void onAudioData(String channelId, String userId,
                         byte[] pcmData, int sampleRate, int channels);
    }
}
