/**
 * Agora WebRTC Client
 * 声网 WebRTC 客户端
 *
 * Implements real-time voice communication using Agora Web SDK
 * 基于声网 Web SDK 实现实时语音通信
 *
 * @author Skylark Team
 * @version 1.0.0
 */
class AgoraWebRTCClient {
    constructor() {
        this.client = null;           // AgoraRTC.createClient instance
        this.localAudioTrack = null;  // Local microphone audio track
        this.sessionId = null;
        this.channelName = null;
        this.apiBaseUrl = this.getApiBaseUrl();
        this.statusCallback = null;
        this.messageCallback = null;
        this.connectionStateCallback = null;

        // Retry configuration (consistent with LiveKitWebRTCClient)
        this.maxRetries = 3;
        this.retryDelay = 2000;
        this.retryBackoffMultiplier = 1.5;
        this.retryCount = 0;
        this.isReconnecting = false;
    }

    /**
     * Gets the API base URL dynamically based on current location
     */
    getApiBaseUrl() {
        const protocol = window.location.protocol;
        const host = window.location.host || 'localhost:8080';
        return `${protocol}//${host}/api/webrtc/agora`;
    }

    /**
     * Sets status update callback
     */
    setStatusCallback(callback) { this.statusCallback = callback; }

    /**
     * Sets message callback for received data
     */
    setMessageCallback(callback) { this.messageCallback = callback; }

    /**
     * Sets connection state change callback
     */
    setConnectionStateCallback(callback) { this.connectionStateCallback = callback; }

    /**
     * Updates status and calls callback if set
     */
    updateStatus(state, text) {
        console.log(`[AgoraWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) this.statusCallback(state, text);
    }

    /**
     * Notifies connection state change
     */
    notifyConnectionStateChange(state) {
        console.log(`[AgoraWebRTC] Connection state: ${state}`);
        if (this.connectionStateCallback) this.connectionStateCallback(state);
    }

    /**
     * Sends message through callback if set
     */
    sendMessage(type, data) {
        if (this.messageCallback) this.messageCallback(type, data);
    }

    /**
     * Starts a new Agora WebRTC session
     * 启动声网 WebRTC 会话
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建声网会话...');

            // 1. Get Token + ChannelName + AppId from backend
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            if (!response.ok) throw new Error(`Failed to create session: ${response.status}`);

            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            const { token, channelName, appId, uid } = sessionData;
            this.channelName = channelName;

            console.log('[AgoraWebRTC] Session created:', this.sessionId);

            if (!appId) {
                throw new Error('Agora App ID not provided. Check server configuration (webrtc.strategy=agora).');
            }

            this.updateStatus('connecting', '正在加入声网频道...');

            // 2. Create AgoraRTC client
            this.client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
            this.setupClientEventListeners();

            // 3. Join channel
            await this.client.join(appId, channelName, token, uid || null);

            // 4. Create and publish local microphone audio track
            this.localAudioTrack = await AgoraRTC.createMicrophoneAudioTrack({
                encoderConfig: {
                    sampleRate: 16000,
                    stereo: false,
                    bitrate: 48
                },
                AEC: true,
                ANS: true,
                AGC: true
            });
            await this.client.publish([this.localAudioTrack]);

            this.updateStatus('connected', '声网 WebRTC 通话已建立');
            this.sendMessage('success', '声网 WebRTC 连接成功！');
            this.notifyConnectionStateChange('connected');
            this.resetRetryCount();

        } catch (error) {
            console.error('[AgoraWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }

    /**
     * Sets up client event listeners
     * 设置客户端事件监听
     */
    setupClientEventListeners() {
        if (!this.client) return;

        // Remote user published audio stream
        this.client.on('user-published', async (user, mediaType) => {
            if (mediaType === 'audio') {
                await this.client.subscribe(user, mediaType);
                const remoteAudioTrack = user.audioTrack;
                remoteAudioTrack.play();
                this.sendMessage('system', '收到远端音频流');
            }
        });

        // Remote user unpublished
        this.client.on('user-unpublished', (user, mediaType) => {
            if (mediaType === 'audio') {
                this.sendMessage('system', '远端音频流已停止');
            }
        });

        // Connection state change
        this.client.on('connection-state-change', (curState, revState, reason) => {
            console.log(`[AgoraWebRTC] Connection: ${revState} -> ${curState}`, reason);
            this.notifyConnectionStateChange(curState.toLowerCase());

            if (curState === 'DISCONNECTED' && reason === 'NETWORK_ERROR') {
                this.handleConnectionFailure();
            }
        });

        // User left
        this.client.on('user-left', (user) => {
            console.log('[AgoraWebRTC] User left:', user.uid);
        });
    }

    /**
     * Handles connection failure with retry logic
     * 断线重连（与 LiveKitWebRTCClient 保持相同的重连策略）
     */
    async handleConnectionFailure() {
        if (this.isReconnecting || this.retryCount >= this.maxRetries) {
            if (this.retryCount >= this.maxRetries) {
                this.updateStatus('error', `连接失败，已重试 ${this.maxRetries} 次`);
            }
            return;
        }
        this.retryCount++;
        this.isReconnecting = true;
        const delay = this.retryDelay * Math.pow(this.retryBackoffMultiplier, this.retryCount - 1);
        this.updateStatus('reconnecting', `正在重连... (${this.retryCount}/${this.maxRetries})`);
        await this.sleep(delay);
        try {
            await this.stop();
            await this.start();
            this.isReconnecting = false;
        } catch (e) {
            this.isReconnecting = false;
            if (this.retryCount < this.maxRetries) await this.handleConnectionFailure();
        }
    }

    /**
     * Resets retry counter on successful connection
     */
    resetRetryCount() { this.retryCount = 0; }

    /**
     * Sleep helper for async/await
     */
    sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    /**
     * Stops the Agora WebRTC session
     * 停止会话
     */
    async stop() {
        try {
            this.retryCount = 0;
            this.isReconnecting = false;

            // Unpublish and close local audio track
            if (this.localAudioTrack) {
                if (this.client) {
                    await this.client.unpublish([this.localAudioTrack]);
                }
                this.localAudioTrack.close();
                this.localAudioTrack = null;
            }

            // Leave channel
            if (this.client) {
                await this.client.leave();
                this.client = null;
            }

            // Notify server to close session
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, { method: 'DELETE' });
                this.sessionId = null;
                this.channelName = null;
            }

            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', '声网 WebRTC 已断开');
        } catch (error) {
            console.error('[AgoraWebRTC] Failed to stop session:', error);
        }
    }

    /**
     * Checks if session is active
     */
    isActive() {
        return this.sessionId !== null && this.client !== null;
    }
}

// Export for use in HTML pages
if (typeof window !== 'undefined') {
    window.AgoraWebRTCClient = AgoraWebRTCClient;
}
