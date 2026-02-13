/**
 * Kurento WebRTC Client
 * Kurento WebRTC 客户端
 * 
 * Implements WebRTC 1v1 real-time voice communication using Kurento Media Server
 * with automatic reconnection support
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
class KurentoWebRTCClient {
    constructor() {
        this.webRtcPeer = null;
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
        return `${protocol}//${host}/api/webrtc/kurento`;
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
        console.log(`[KurentoWebRTC] Status: ${state} - ${text}`);
        if (this.statusCallback) {
            this.statusCallback(state, text);
        }
    }
    
    /**
     * Notifies connection state change
     */
    notifyConnectionStateChange(state) {
        console.log(`[KurentoWebRTC] Connection state: ${state}`);
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
     * Starts a new WebRTC session with Kurento
     */
    async start() {
        try {
            this.updateStatus('connecting', '正在创建 Kurento 会话...');
            
            // 1. Create session
            const response = await fetch(`${this.apiBaseUrl}/session`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: 'user-' + Date.now() })
            });
            
            if (!response.ok) {
                throw new Error(`Failed to create session: ${response.status}`);
            }
            
            const session = await response.json();
            this.sessionId = session.sessionId;
            
            console.log('[KurentoWebRTC] Session created:', this.sessionId);
            this.updateStatus('connecting', 'Kurento 会话已创建，正在建立 WebRTC 连接...');
            
            // 2. Create WebRTC Peer with Kurento Utils
            await this.createWebRtcPeer();
            
        } catch (error) {
            console.error('[KurentoWebRTC] Failed to start session:', error);
            this.updateStatus('error', '启动失败: ' + error.message);
            throw error;
        }
    }
    
    /**
     * Creates WebRTC peer connection using Kurento Utils
     */
    async createWebRtcPeer() {
        return new Promise((resolve, reject) => {
            console.log('[KurentoWebRTC] Creating WebRTC peer...');
            
            const options = {
                localVideo: null,  // No video, audio only
                remoteVideo: null, // No video, audio only
                onicecandidate: this.onIceCandidate.bind(this),
                mediaConstraints: {
                    audio: {
                        echoCancellation: true,
                        noiseSuppression: true,
                        autoGainControl: true
                    },
                    video: false
                }
            };
            
            // Use WebRtcPeerSendrecv for bidirectional audio
            this.webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, 
                (error) => {
                    if (error) {
                        console.error('[KurentoWebRTC] Error creating WebRTC peer:', error);
                        reject(error);
                        return;
                    }
                    
                    console.log('[KurentoWebRTC] WebRTC peer created, generating offer...');
                    
                    // Set up ICE connection state monitoring
                    this.setupIceConnectionMonitoring();
                    
                    // Generate SDP offer
                    this.webRtcPeer.generateOffer((error, offerSdp) => {
                        if (error) {
                            console.error('[KurentoWebRTC] Error generating offer:', error);
                            reject(error);
                            return;
                        }
                        
                        console.log('[KurentoWebRTC] SDP offer generated');
                        
                        // Send offer to server
                        this.sendOffer(offerSdp)
                            .then(() => resolve())
                            .catch(reject);
                    });
                });
        });
    }
    
    /**
     * Sets up ICE connection state monitoring for reconnection
     */
    setupIceConnectionMonitoring() {
        if (!this.webRtcPeer || !this.webRtcPeer.peerConnection) {
            return;
        }
        
        const pc = this.webRtcPeer.peerConnection;
        
        pc.oniceconnectionstatechange = () => {
            const state = pc.iceConnectionState;
            console.log('[KurentoWebRTC] ICE connection state changed:', state);
            this.notifyConnectionStateChange(state);
            
            if (state === 'connected' || state === 'completed') {
                // Connection successful, reset retry count
                this.resetRetryCount();
                this.updateStatus('connected', 'Kurento WebRTC 通话已建立');
                
            } else if (state === 'disconnected') {
                console.warn('[KurentoWebRTC] Connection disconnected');
                this.updateStatus('disconnected', '连接断开');
                
            } else if (state === 'failed') {
                console.error('[KurentoWebRTC] Connection failed');
                this.updateStatus('error', '连接失败');
                this.handleConnectionFailure();
            }
        };
    }
    
    /**
     * Handles connection failure and triggers reconnection
     */
    async handleConnectionFailure() {
        if (this.isReconnecting) {
            console.log('[KurentoWebRTC] Already reconnecting, skipping...');
            return;
        }
        
        if (this.retryCount >= this.maxRetries) {
            console.error('[KurentoWebRTC] Max retries reached, giving up');
            this.updateStatus('error', `连接失败，已重试 ${this.maxRetries} 次`);
            this.sendMessage('system', 'Kurento WebRTC 连接失败，请刷新页面重试');
            return;
        }
        
        this.retryCount++;
        this.isReconnecting = true;
        
        const delay = this.retryDelay * Math.pow(this.retryBackoffMultiplier, this.retryCount - 1);
        console.log(`[KurentoWebRTC] Attempting reconnection ${this.retryCount}/${this.maxRetries} in ${delay}ms...`);
        this.updateStatus('reconnecting', `正在重连... (${this.retryCount}/${this.maxRetries})`);
        
        try {
            // Dispose current peer
            if (this.webRtcPeer) {
                this.webRtcPeer.dispose();
                this.webRtcPeer = null;
            }
            
            // Wait for retry delay
            await this.sleep(delay);
            
            // Close old session and create new one
            if (this.sessionId) {
                try {
                    await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, {
                        method: 'DELETE'
                    });
                } catch (e) {
                    console.warn('[KurentoWebRTC] Failed to close old session:', e);
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
            
            const session = await response.json();
            this.sessionId = session.sessionId;
            
            console.log('[KurentoWebRTC] New session created:', this.sessionId);
            
            // Create new WebRTC peer
            await this.createWebRtcPeer();
            
            this.isReconnecting = false;
            console.log('[KurentoWebRTC] Reconnection successful');
            this.updateStatus('connected', '重连成功');
            this.sendMessage('system', 'Kurento WebRTC 已重新连接');
            
        } catch (error) {
            console.error('[KurentoWebRTC] Reconnection attempt failed:', error);
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
            console.log('[KurentoWebRTC] Connection established, resetting retry count');
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
     * Sends SDP offer to Kurento server
     */
    async sendOffer(sdpOffer) {
        try {
            console.log('[KurentoWebRTC] Sending SDP offer to server...');
            
            const response = await fetch(
                `${this.apiBaseUrl}/session/${this.sessionId}/offer`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sdpOffer })
                }
            );
            
            if (!response.ok) {
                throw new Error(`Failed to process offer: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('[KurentoWebRTC] Received SDP answer from server');
            
            // Process answer
            this.webRtcPeer.processAnswer(data.sdpAnswer, (error) => {
                if (error) {
                    console.error('[KurentoWebRTC] Error processing answer:', error);
                    this.updateStatus('error', '处理 SDP answer 失败');
                    return;
                }
                
                console.log('[KurentoWebRTC] SDP answer processed successfully');
                this.updateStatus('connected', 'Kurento WebRTC 通话已建立');
                this.sendMessage('system', 'Kurento WebRTC 连接成功！');
                this.resetRetryCount();
            });
            
        } catch (error) {
            console.error('[KurentoWebRTC] Failed to send offer:', error);
            throw error;
        }
    }
    
    /**
     * Handles ICE candidates
     */
    async onIceCandidate(candidate) {
        try {
            console.log('[KurentoWebRTC] Sending ICE candidate:', candidate.candidate);
            
            await fetch(
                `${this.apiBaseUrl}/session/${this.sessionId}/ice-candidate`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        candidate: candidate.candidate,
                        sdpMid: candidate.sdpMid,
                        sdpMLineIndex: candidate.sdpMLineIndex
                    })
                }
            );
            
            console.log('[KurentoWebRTC] ICE candidate sent successfully');
            
        } catch (error) {
            console.error('[KurentoWebRTC] Failed to send ICE candidate:', error);
        }
    }
    
    /**
     * Stops the WebRTC session
     */
    async stop() {
        try {
            console.log('[KurentoWebRTC] Stopping session...');
            
            // Reset retry state
            this.retryCount = 0;
            this.isReconnecting = false;
            
            if (this.webRtcPeer) {
                this.webRtcPeer.dispose();
                this.webRtcPeer = null;
                console.log('[KurentoWebRTC] WebRTC peer disposed');
            }
            
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/session/${this.sessionId}`, {
                    method: 'DELETE'
                });
                console.log('[KurentoWebRTC] Session closed on server');
                this.sessionId = null;
            }
            
            this.updateStatus('disconnected', '未连接');
            this.sendMessage('system', 'Kurento WebRTC 已断开');
            
        } catch (error) {
            console.error('[KurentoWebRTC] Failed to stop session:', error);
        }
    }
    
    /**
     * Checks if session is active
     */
    isActive() {
        return this.sessionId !== null && this.webRtcPeer !== null;
    }
}

// Export for use in webrtc.html
if (typeof window !== 'undefined') {
    window.KurentoWebRTCClient = KurentoWebRTCClient;
}
