/**
 * LiveKit WebRTC Client
 * LiveKit WebRTC 客户端
 * 
 * Implements WebRTC real-time voice communication using LiveKit Server
 * with automatic reconnection support
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
class LiveKitWebRTCClient {
    constructor() {
        this.room = null;
        this.sessionId = null;
        this.apiBaseUrl = this.getApiBaseUrl();
        this.statusCallback = null;
        this.messageCallback = null;
        
        // Retry configuration
        this.maxRetries = 3;
        this.retryDelay = 2000; // 2 seconds initial delay
        this.retryBackoffMultiplier = 1.5;
        this.retryCount = 0;
        this.isReconnecting = false;
        this.connectionStateCallback = null;
    }
    
    /**
     * Gets the API base URL dynamically based on current location
     */
    getApiBaseUrl() {
        const protocol = window.location.protocol;
        const host = window.location.host || 'localhost:8080';
        return `${protocol}//${host}/api/webrtc/livekit`;
    }
    
    /**
     * Sets status update callback
     */
    setStatusCallback(callback) {
        this.statusCallback = callback;
    }
    
    /**
     * Sets message callback for received data
     */
    setMessageCallback(callback) {
        this.messageCallback = callback;
    }
    
    /**
     * Sets connection state change callback
     */
    setConnectionStateCallback(callback) {
        this.connectionStateCallback = callback;
    }
    
    /**
     * Updates status and calls callback if set
     */
    updateStatus(state, text) {
        console.log(`[LiveKitWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) {
            this.statusCallback(state, text);
        }
    }
    
    /**
     * Notifies connection state change
     */
    notifyConnectionStateChange(state) {
        console.log(`[LiveKitWebRTC] Connection state: ${state}`);
        if (this.connectionStateCallback) {
            this.connectionStateCallback(state);
        }
    }
    
    /**
     * Sends message through callback if set
     */
    sendMessage(type, data) {
        if (this.messageCallback) {
            this.messageCallback(type, data);
        }
    }
    
    /**
     * Starts a new WebRTC session with LiveKit
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建 LiveKit 会话...');
            
            // 1. Create session and get connection info from server
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            
            if (!response.ok) {
                throw new Error(`Failed to create session: ${response.status}`);
            }
            
            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            const token = sessionData.token;
            const url = sessionData.url;
            
            console.log('[LiveKitWebRTC] Session created:', this.sessionId);
            
            if (!token || !url) {
                throw new Error('LiveKit token or URL not provided. Check server configuration (webrtc.strategy=livekit).');
            }
            
            this.updateStatus('connecting', 'LiveKit 会话已创建，正在连接房间...');
            
            // 2. Connect to LiveKit room
            await this.connectToRoom(url, token);
            
        } catch (error) {
            console.error('[LiveKitWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }
    
    /**
     * Connects to a LiveKit room using the provided token
     */
    async connectToRoom(url, token) {
        console.log('[LiveKitWebRTC] Connecting to LiveKit room...');
        
        // Create a new Room instance
        this.room = new LivekitClient.Room({
            adaptiveStream: true,
            dynacast: true,
            audioCaptureDefaults: {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
            }
        });
        
        // Set up room event listeners
        this.setupRoomEventListeners();
        
        // Connect to LiveKit server
        await this.room.connect(url, token, {
            autoSubscribe: true
        });
        
        console.log('[LiveKitWebRTC] Connected to LiveKit room');
        
        // Enable microphone (publish local audio)
        await this.room.localParticipant.setMicrophoneEnabled(true);
        console.log('[LiveKitWebRTC] Microphone enabled');
    }
    
    /**
     * Sets up LiveKit room event listeners
     */
    setupRoomEventListeners() {
        if (!this.room) return;
        
        const RoomEvent = LivekitClient.RoomEvent;
        
        // Connected
        this.room.on(RoomEvent.Connected, () => {
            console.log('[LiveKitWebRTC] Room connected');
            this.resetRetryCount();
            this.updateStatus('connected', 'LiveKit WebRTC 通话已建立');
            this.sendMessage('success', 'LiveKit WebRTC 连接成功！');
            this.notifyConnectionStateChange('connected');
        });
        
        // Disconnected
        this.room.on(RoomEvent.Disconnected, (reason) => {
            console.log('[LiveKitWebRTC] Room disconnected, reason:', reason);
            this.updateStatus('disconnected', '连接断开');
            this.notifyConnectionStateChange('disconnected');
        });
        
        // Reconnecting
        this.room.on(RoomEvent.Reconnecting, () => {
            console.log('[LiveKitWebRTC] Reconnecting...');
            this.updateStatus('reconnecting', '正在重连...');
            this.notifyConnectionStateChange('reconnecting');
        });
        
        // Reconnected
        this.room.on(RoomEvent.Reconnected, () => {
            console.log('[LiveKitWebRTC] Reconnected successfully');
            this.updateStatus('connected', '重连成功');
            this.sendMessage('system', 'LiveKit WebRTC 已重新连接');
            this.notifyConnectionStateChange('connected');
        });
        
        // Track subscribed (remote audio/video track received)
        this.room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
            console.log('[LiveKitWebRTC] Track subscribed:', track.kind, 'from', participant.identity);
            
            if (track.kind === LivekitClient.Track.Kind.Audio) {
                // Attach remote audio track to audio element
                const audioElement = track.attach();
                audioElement.id = 'livekit-remote-audio-' + participant.sid;
                document.body.appendChild(audioElement);
                this.sendMessage('system', '收到远端音频流');
            }
        });
        
        // Track unsubscribed
        this.room.on(RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
            console.log('[LiveKitWebRTC] Track unsubscribed:', track.kind);
            
            // Detach track elements
            const elements = track.detach();
            elements.forEach(el => el.remove());
        });
        
        // Connection quality changed
        this.room.on(RoomEvent.ConnectionQualityChanged, (quality, participant) => {
            console.log('[LiveKitWebRTC] Connection quality:', quality, 'for', participant.identity);
        });
        
        // Media device failure
        this.room.on(RoomEvent.MediaDevicesError, (error) => {
            console.error('[LiveKitWebRTC] Media device error:', error);
            this.sendMessage('error', '媒体设备错误: ' + error.message);
        });
    }
    
    /**
     * Handles connection failure and triggers reconnection
     */
    async handleConnectionFailure() {
        if (this.isReconnecting) {
            console.log('[LiveKitWebRTC] Already reconnecting, skipping...');
            return;
        }
        
        if (this.retryCount >= this.maxRetries) {
            console.error('[LiveKitWebRTC] Max retries reached, giving up');
            this.updateStatus('error', `连接失败，已重试 ${this.maxRetries} 次`);
            this.sendMessage('system', 'LiveKit WebRTC 连接失败，请刷新页面重试');
            return;
        }
        
        this.retryCount++;
        this.isReconnecting = true;
        
        const delay = this.retryDelay * Math.pow(this.retryBackoffMultiplier, this.retryCount - 1);
        console.log(`[LiveKitWebRTC] Attempting reconnection ${this.retryCount}/${this.maxRetries} in ${delay}ms...`);
        this.updateStatus('reconnecting', `正在重连... (${this.retryCount}/${this.maxRetries})`);
        
        try {
            // Wait for retry delay
            await this.sleep(delay);
            
            // Disconnect and cleanup
            if (this.room) {
                await this.room.disconnect();
                this.room = null;
            }
            
            // Close old session on server
            if (this.sessionId) {
                try {
                    await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, {
                        method: 'DELETE'
                    });
                } catch (e) {
                    console.warn('[LiveKitWebRTC] Failed to close old session:', e);
                }
            }
            
            // Create new session
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            
            if (!response.ok) {
                throw new Error(`Failed to create session: ${response.status}`);
            }
            
            const sessionData = await response.json();
            this.sessionId = sessionData.sessionId;
            
            console.log('[LiveKitWebRTC] New session created:', this.sessionId);
            
            // Connect to new room
            await this.connectToRoom(sessionData.url, sessionData.token);
            
            this.isReconnecting = false;
            console.log('[LiveKitWebRTC] Reconnection successful');
            this.updateStatus('connected', '重连成功');
            this.sendMessage('system', 'LiveKit WebRTC 已重新连接');
            
        } catch (error) {
            console.error('[LiveKitWebRTC] Reconnection attempt failed:', error);
            this.isReconnecting = false;
            
            // Try again if we haven't hit max retries
            if (this.retryCount < this.maxRetries) {
                await this.handleConnectionFailure();
            }
        }
    }
    
    /**
     * Resets retry counter on successful connection
     */
    resetRetryCount() {
        if (this.retryCount > 0) {
            console.log('[LiveKitWebRTC] Connection established, resetting retry count');
        }
        this.retryCount = 0;
    }
    
    /**
     * Sleep helper for async/await
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    /**
     * Stops the LiveKit WebRTC session
     */
    async stop() {
        try {
            console.log('[LiveKitWebRTC] Stopping session...');
            
            // Reset retry state
            this.retryCount = 0;
            this.isReconnecting = false;
            
            // Disconnect from LiveKit room
            if (this.room) {
                await this.room.disconnect();
                this.room = null;
                console.log('[LiveKitWebRTC] Disconnected from LiveKit room');
            }
            
            // Remove any attached audio elements
            document.querySelectorAll('[id^="livekit-remote-audio-"]').forEach(el => el.remove());
            
            // Close session on server
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, {
                    method: 'DELETE'
                });
                console.log('[LiveKitWebRTC] Session closed on server');
                this.sessionId = null;
            }
            
            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', 'LiveKit WebRTC 已断开');
            
        } catch (error) {
            console.error('[LiveKitWebRTC] Failed to stop session:', error);
        }
    }
    
    /**
     * Checks if session is active
     */
    isActive() {
        return this.sessionId !== null && this.room !== null;
    }
}

// Export for use in HTML pages
if (typeof window !== 'undefined') {
    window.LiveKitWebRTCClient = LiveKitWebRTCClient;
}
