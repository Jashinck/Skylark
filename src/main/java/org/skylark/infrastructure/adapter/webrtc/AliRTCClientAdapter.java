package org.skylark.infrastructure.adapter.webrtc;

/**
 * AliRTC Client Adapter Interface
 * 阿里云 ARTC 客户端适配器接口
 * 
 * <p>Provides abstraction for AliRTC Server operations including
 * authentication, channel management, and audio frame processing.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface AliRTCClientAdapter {

    /**
     * Generates authentication info for joining a channel
     * 生成加入频道所需的鉴权信息（Token/AuthInfo）
     *
     * @param channelId  Channel ID
     * @param userId     User ID
     * @return AuthInfo JSON string (contains token, nonce, timestamp, etc.)
     */
    String generateAuthInfo(String channelId, String userId);

    /**
     * Server joins a channel
     * 服务端加入频道
     *
     * @param channelId  Channel ID
     * @param userId     Server-side user ID
     * @param authInfo   Authentication info JSON
     */
    void joinChannel(String channelId, String userId, String authInfo);

    /**
     * Server leaves a channel
     * 服务端离开频道
     *
     * @param channelId Channel ID
     */
    void leaveChannel(String channelId);

    /**
     * Pushes PCM audio frame to channel (TTS output)
     * 向频道推送 PCM 音频帧（TTS 输出）
     *
     * @param channelId  Channel ID
     * @param pcmData    PCM data (16kHz, 16-bit, mono)
     * @param sampleRate Sample rate
     * @param channels   Number of channels
     */
    void pushAudioFrame(String channelId, byte[] pcmData, int sampleRate, int channels);

    /**
     * Registers remote audio data callback (VAD/ASR pipeline entry)
     * 注册远端音频数据回调（VAD/ASR Pipeline 入口）
     *
     * @param channelId Channel ID
     * @param callback  Audio data callback
     */
    void registerAudioDataCallback(String channelId, AudioDataCallback callback);

    /**
     * Checks if SDK is available
     * 检查 SDK 是否可用
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Gets the AliRTC App ID (for frontend use)
     * 获取阿里云 ARTC AppId（供前端使用）
     *
     * @return App ID string
     */
    String getAppId();

    /**
     * Audio data callback functional interface
     */
    @FunctionalInterface
    interface AudioDataCallback {
        /**
         * Called when audio data is received from a remote user
         *
         * @param channelId Source channel ID
         * @param userId    Remote user ID
         * @param pcmData   PCM raw audio data
         * @param sampleRate Sample rate
         * @param channels  Number of channels
         */
        void onAudioData(String channelId, String userId,
                         byte[] pcmData, int sampleRate, int channels);
    }
}
