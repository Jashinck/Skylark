/**
 * AliRTC WebRTC Client
 * 阿里云 ARTC WebRTC 客户端
 *
 * Implements real-time voice communication using AliRTC (Alibaba Cloud ARTC) Web SDK.
 * 基于阿里云 ARTC Web SDK 实现实时语音通信
 *
 * <p>Connection flow:
 * 1. POST /api/webrtc/alirtc/session  → obtain appId + channelId + userId + authInfo
 * 2. Create AliRtcEngine instance with appId
 * 3. Join channel using channelId + authInfo (nonce / timestamp / token)
 * 4. Publish local microphone audio stream
 * 5. Subscribe to remote audio stream (TTS output from server-side bot)
 * 6. DELETE /api/webrtc/alirtc/session/:id on stop
 * </p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see https://help.aliyun.com/zh/live/artc-use-the-web-sdk
 */
class AliRTCWebRTCClient {
    constructor() {
        this.engine = null;            // AliRtcEngine instance
        this.localStream = null;       // Local microphone MediaStream
        this.sessionId = null;
        this.channelId = null;
        this.apiBaseUrl = this.getApiBaseUrl();
        this.statusCallback = null;
        this.messageCallback = null;
        this.connectionStateCallback = null;

        // Retry configuration (consistent with AgoraWebRTCClient)
        this.maxRetries = 3;
        this.retryDelay = 2000;
        this.retryBackoffMultiplier = 1.5;
        this.retryCount = 0;
        this.isReconnecting = false;
    }

    /**
     * Gets the API base URL dynamically based on current location.
     */
    getApiBaseUrl() {
        const protocol = window.location.protocol;
        const host = window.location.host || 'localhost:8080';
        return `${protocol}//${host}/api/webrtc/alirtc`;
    }

    /** Sets status update callback */
    setStatusCallback(callback) { this.statusCallback = callback; }

    /** Sets message callback for received data */
    setMessageCallback(callback) { this.messageCallback = callback; }

    /** Sets connection state change callback */
    setConnectionStateCallback(callback) { this.connectionStateCallback = callback; }

    /**
     * Updates status and calls the callback if set.
     */
    updateStatus(state, text) {
        console.log(`[AliRTCWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) this.statusCallback(state, text);
    }

    /**
     * Notifies connection state change.
     */
    notifyConnectionStateChange(state) {
        console.log(`[AliRTCWebRTC] Connection state: ${state}`);
        if (this.connectionStateCallback) this.connectionStateCallback(state);
    }

    /**
     * Sends message through callback if set.
     */
    sendMessage(type, data) {
        if (this.messageCallback) this.messageCallback(type, data);
    }

    /**
     * Starts a new AliRTC ARTC session.
     * 启动阿里云 ARTC 会话
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建阿里云 ARTC 会话...');

            // 1. Obtain channelId + authInfo + appId from backend
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            if (!response.ok) throw new Error(`Failed to create session: ${response.status}`);

            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            const { appId, channelId, userId, authInfo } = sessionData;
            this.channelId = channelId;

            console.log('[AliRTCWebRTC] Session created:', this.sessionId);

            if (!appId) {
                throw new Error('AliRTC App ID not provided. Check server configuration (webrtc.strategy=alirtc).');
            }

            this.updateStatus('connecting', '正在创建 ARTC 引擎实例...');

            // 2. Create AliRtcEngine instance
            if (typeof AliRtcEngine === 'undefined') {
                throw new Error('AliRTC Web SDK 未加载，请检查网络连接');
            }
            this.engine = new AliRtcEngine({ appId, logLevel: 'info' });

            // 3. Register event listeners
            this.setupEngineEventListeners();

            // 4. Join channel using authInfo (nonce / timestamp / token)
            let authInfoObj;
            try {
                authInfoObj = JSON.parse(authInfo);
            } catch (_) {
                throw new Error('Invalid authInfo returned from server');
            }
            await this.engine.joinChannel({
                channelId,
                userId,
                token: authInfoObj.token,
                nonce: authInfoObj.nonce,
                timestamp: authInfoObj.timestamp
            });

            // 5. Get microphone and publish local audio stream
            this.localStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    sampleRate: 16000
                },
                video: false
            });
            await this.engine.publishLocalAudioStream(this.localStream);

            this.updateStatus('connected', '阿里云 ARTC 通话已建立');
            this.sendMessage('success', '阿里云 ARTC 连接成功！');
            this.notifyConnectionStateChange('connected');
            this.resetRetryCount();

        } catch (error) {
            console.error('[AliRTCWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }

    /**
     * Sets up AliRtcEngine event listeners.
     * 设置引擎事件监听
     */
    setupEngineEventListeners() {
        if (!this.engine) return;

        // Remote user joined channel
        this.engine.on('onRemoteUserOnline', (channelId, userId) => {
            console.log(`[AliRTCWebRTC] Remote user online: ${userId} in ${channelId}`);
            this.sendMessage('system', `远端用户加入: ${userId}`);
        });

        // Remote user left channel
        this.engine.on('onRemoteUserOffline', (channelId, userId, reason) => {
            console.log(`[AliRTCWebRTC] Remote user offline: ${userId}, reason: ${reason}`);
            this.sendMessage('system', `远端用户离开: ${userId}`);
        });

        // Remote audio track available — subscribe and play
        this.engine.on('onRemoteTrackAvailable', async (channelId, userId, mediaType) => {
            if (mediaType === 'audio') {
                try {
                    const remoteStream = await this.engine.subscribeRemoteAudioStream(channelId, userId);
                    const audioElement = new Audio();
                    audioElement.srcObject = remoteStream;
                    audioElement.id = `alirtc-remote-audio-${userId}`;
                    audioElement.autoplay = true;
                    audioElement.play().catch(e =>
                        console.warn('[AliRTCWebRTC] Auto-play blocked:', e));
                    document.body.appendChild(audioElement);
                    this.sendMessage('system', '收到远端音频流（TTS 输出）');
                } catch (e) {
                    console.error('[AliRTCWebRTC] Failed to subscribe remote audio:', e);
                }
            }
        });

        // Connection state change
        this.engine.on('onConnectionStateChanged', (channelId, state, reason) => {
            console.log(`[AliRTCWebRTC] Connection state: ${state}, reason: ${reason}`);
            this.notifyConnectionStateChange(state.toLowerCase());
            if (state === 'DISCONNECTED') {
                this.handleConnectionFailure();
            }
        });

        // Error event
        this.engine.on('onOccurError', (error, message) => {
            console.error(`[AliRTCWebRTC] Error: ${error} - ${message}`);
            this.sendMessage('error', `ARTC 错误: ${message}`);
        });
    }

    /**
     * Handles connection failure with exponential backoff retry.
     * 断线重连（指数退避）
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

    /** Resets retry counter on successful connection. */
    resetRetryCount() { this.retryCount = 0; }

    /** Sleep helper for async/await. */
    sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

    /**
     * Stops the AliRTC session.
     * 停止会话
     */
    async stop() {
        try {
            this.retryCount = 0;
            this.isReconnecting = false;

            // Stop local audio stream
            if (this.localStream) {
                this.localStream.getTracks().forEach(t => t.stop());
                this.localStream = null;
            }

            // Remove remote audio elements
            document.querySelectorAll('[id^="alirtc-remote-audio-"]').forEach(el => el.remove());

            // Leave channel and destroy engine
            if (this.engine) {
                try {
                    await this.engine.leaveChannel();
                } catch (_) {}
                try {
                    this.engine.destroy();
                } catch (_) {}
                this.engine = null;
            }

            // Notify server to close session
            if (this.sessionId) {
                try {
                    await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, { method: 'DELETE' });
                } catch (_) {}
                this.sessionId = null;
                this.channelId = null;
            }

            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', '阿里云 ARTC 已断开');
        } catch (error) {
            console.error('[AliRTCWebRTC] Failed to stop session:', error);
        }
    }

    /**
     * Returns true if the session is active.
     */
    isActive() {
        return this.sessionId !== null && this.engine !== null;
    }
}

// Export for use in HTML pages
if (typeof window !== 'undefined') {
    window.AliRTCWebRTCClient = AliRTCWebRTCClient;
}
