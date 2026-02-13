package org.skylark.infrastructure.adapter.webrtc;

import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

/**
 * Kurento Client Adapter Interface
 * Kurento 客户端适配器接口
 * 
 * <p>Provides abstraction for Kurento Media Server operations including
 * media pipeline and WebRTC endpoint management.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface KurentoClientAdapter {
    
    /**
     * Creates a new media pipeline for processing
     * 创建新的媒体处理管道
     * 
     * @return MediaPipeline instance
     */
    MediaPipeline createMediaPipeline();
    
    /**
     * Releases a media pipeline and its resources
     * 释放媒体管道及其资源
     * 
     * @param pipelineId Pipeline identifier
     */
    void releaseMediaPipeline(String pipelineId);
    
    /**
     * Creates a WebRTC endpoint for the given pipeline
     * 为指定管道创建 WebRTC 端点
     * 
     * @param pipeline Media pipeline
     * @return WebRtcEndpoint instance
     */
    WebRtcEndpoint createWebRTCEndpoint(MediaPipeline pipeline);
    
    /**
     * Checks if the Kurento client is connected
     * 检查 Kurento 客户端是否已连接
     * 
     * @return true if connected to Kurento Media Server
     */
    boolean isConnected();
}
