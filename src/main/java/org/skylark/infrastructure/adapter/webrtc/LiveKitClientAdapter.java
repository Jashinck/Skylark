package org.skylark.infrastructure.adapter.webrtc;

/**
 * LiveKit Client Adapter Interface
 * LiveKit 客户端适配器接口
 * 
 * <p>Provides abstraction for LiveKit Server operations including
 * room management and access token generation.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface LiveKitClientAdapter {
    
    /**
     * Creates a new LiveKit room
     * 创建新的 LiveKit 房间
     * 
     * @param roomName Room name
     * @return Room name (confirmed)
     */
    String createRoom(String roomName);
    
    /**
     * Deletes a LiveKit room
     * 删除 LiveKit 房间
     * 
     * @param roomName Room name to delete
     */
    void deleteRoom(String roomName);
    
    /**
     * Generates an access token for a participant to join a room
     * 为参与者生成加入房间的访问令牌
     * 
     * @param roomName Room name
     * @param participantIdentity Participant identity
     * @return Access token string
     */
    String generateToken(String roomName, String participantIdentity);
    
    /**
     * Checks if the LiveKit server is connected and available
     * 检查 LiveKit 服务器是否已连接并可用
     * 
     * @return true if connected to LiveKit server
     */
    boolean isConnected();
    
    /**
     * Gets the LiveKit server URL for client connection
     * 获取客户端连接用的 LiveKit 服务器 URL
     * 
     * @return LiveKit server URL
     */
    String getServerUrl();
}
