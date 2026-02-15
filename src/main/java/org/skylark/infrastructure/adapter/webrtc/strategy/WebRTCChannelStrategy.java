package org.skylark.infrastructure.adapter.webrtc.strategy;

/**
 * WebRTC Channel Strategy Interface
 * WebRTC 通道策略接口
 * 
 * <p>Defines a pluggable strategy for WebRTC communication channels.
 * Supports WebSocket, Kurento, and LiveKit implementations.</p>
 * 
 * <p>定义可插拔的 WebRTC 通信通道策略，支持 WebSocket、Kurento 和 LiveKit 实现。</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface WebRTCChannelStrategy {
    
    /**
     * Gets the strategy name identifier
     * 获取策略名称标识
     * 
     * @return Strategy name (e.g., "websocket", "kurento", "livekit")
     */
    String getStrategyName();
    
    /**
     * Creates a new WebRTC session
     * 创建新的 WebRTC 会话
     * 
     * @param userId User identifier
     * @return Session ID
     */
    String createSession(String userId);
    
    /**
     * Processes SDP offer from client
     * 处理来自客户端的 SDP offer
     * 
     * <p>For strategies that do not use SDP (e.g., LiveKit), this may return
     * connection information such as a token or URL instead.</p>
     * 
     * @param sessionId Session identifier
     * @param sdpOffer SDP offer from client
     * @return SDP answer or connection info
     */
    String processOffer(String sessionId, String sdpOffer);
    
    /**
     * Adds ICE candidate to session
     * 向会话添加 ICE candidate
     * 
     * <p>For strategies that handle ICE internally (e.g., LiveKit), this is a no-op.</p>
     * 
     * @param sessionId Session identifier
     * @param candidate ICE candidate string
     * @param sdpMid SDP media ID
     * @param sdpMLineIndex SDP media line index
     */
    void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex);
    
    /**
     * Closes a WebRTC session and releases resources
     * 关闭 WebRTC 会话并释放资源
     * 
     * @param sessionId Session identifier
     */
    void closeSession(String sessionId);
    
    /**
     * Checks if a session exists
     * 检查会话是否存在
     * 
     * @param sessionId Session identifier
     * @return true if session exists
     */
    boolean sessionExists(String sessionId);
    
    /**
     * Gets the number of active sessions
     * 获取活动会话数
     * 
     * @return Number of active sessions
     */
    int getActiveSessionCount();
    
    /**
     * Checks if this strategy is available and ready to use
     * 检查此策略是否可用且准备就绪
     * 
     * @return true if the strategy is available
     */
    boolean isAvailable();
}
