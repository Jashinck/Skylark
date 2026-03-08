package org.skylark.infrastructure.adapter.webrtc;

/**
 * Agora Client Adapter Interface
 * 声网客户端适配器接口
 * 
 * <p>Provides abstraction for Agora RTC Server operations including
 * token generation, channel management, and audio frame processing.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface AgoraClientAdapter {

    /**
     * Generates an RTC Token for Web SDK client
     * 生成 RTC Token（供客户端 Web SDK 使用）
     *
     * @param channelName Channel name
     * @param userId      User ID
     * @param expireSeconds Token expiration in seconds
     * @return RTC Token string
     */
    String generateToken(String channelName, String userId, int expireSeconds);

    /**
     * Server joins a channel (Server SDK join channel)
     * 服务端加入频道
     *
     * @param channelName Channel name
     * @param userId      Server-side UID
     */
    void joinChannel(String channelName, String userId);

    /**
     * Server leaves a channel
     * 服务端离开频道
     *
     * @param channelName Channel name
     */
    void leaveChannel(String channelName);

    /**
     * Sends PCM audio frame to channel (TTS output)
     * 向频道发送 PCM 音频帧（TTS 输出）
     *
     * @param channelName Channel name
     * @param pcmData     PCM audio data (16kHz, 16-bit, mono)
     * @param sampleRate  Sample rate (default 16000)
     * @param channels    Number of channels (default 1)
     */
    void sendAudioFrame(String channelName, byte[] pcmData, int sampleRate, int channels);

    /**
     * Registers audio frame callback for received remote audio (VAD/ASR pipeline entry)
     * 注册音频帧接收回调（VAD/ASR Pipeline 入口）
     *
     * @param channelName Channel name
     * @param callback    Audio frame callback
     */
    void registerAudioFrameCallback(String channelName, AudioFrameCallback callback);

    /**
     * Checks if SDK is initialized and available
     * 检查 SDK 是否已初始化并可用
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Gets the Agora App ID (for frontend use)
     * 获取 Agora App ID（供前端使用）
     *
     * @return App ID string
     */
    String getAppId();

    /**
     * Functional interface for audio frame callback
     */
    @FunctionalInterface
    interface AudioFrameCallback {
        /**
         * Called when an audio frame is received from a remote user
         *
         * @param channelName Source channel name
         * @param userId      Remote user UID
         * @param pcmData     PCM raw audio data
         * @param sampleRate  Sample rate
         * @param channels    Number of channels
         */
        void onAudioFrame(String channelName, String userId,
                          byte[] pcmData, int sampleRate, int channels);
    }
}
