package org.skylark.infrastructure.adapter.webrtc;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebRTC Session
 * WebRTC 会话管理
 * 
 * <p>Manages a single WebRTC session including media pipeline,
 * WebRTC endpoint, and audio processing components.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class WebRTCSession {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCSession.class);
    
    private final String sessionId;
    private final MediaPipeline pipeline;
    private final WebRtcEndpoint webRtcEndpoint;
    private volatile boolean active;
    
    /**
     * Creates a new WebRTC session
     * 
     * @param sessionId Session identifier
     * @param pipeline Media pipeline
     * @param webRtcEndpoint WebRTC endpoint
     */
    public WebRTCSession(String sessionId, MediaPipeline pipeline, WebRtcEndpoint webRtcEndpoint) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (pipeline == null) {
            throw new IllegalArgumentException("Media pipeline cannot be null");
        }
        if (webRtcEndpoint == null) {
            throw new IllegalArgumentException("WebRTC endpoint cannot be null");
        }
        
        this.sessionId = sessionId;
        this.pipeline = pipeline;
        this.webRtcEndpoint = webRtcEndpoint;
        this.active = true;
        
        setupEventListeners();
    }
    
    /**
     * Sets up event listeners for WebRTC endpoint
     * 设置 WebRTC 端点的事件监听器
     */
    private void setupEventListeners() {
        webRtcEndpoint.addMediaSessionStartedListener(event -> {
            logger.info("Media session started for session: {}", sessionId);
        });
        
        webRtcEndpoint.addMediaSessionTerminatedListener(event -> {
            logger.info("Media session terminated for session: {}", sessionId);
        });
        
        webRtcEndpoint.addIceCandidateFoundListener(event -> {
            IceCandidate candidate = event.getCandidate();
            logger.debug("ICE candidate found for session {}: {}", sessionId, candidate.getCandidate());
        });
        
        webRtcEndpoint.addIceComponentStateChangeListener(event -> {
            logger.debug("ICE component state changed for session {}: {}", 
                sessionId, event.getState());
        });
    }
    
    /**
     * Processes SDP offer and returns SDP answer
     * 处理 SDP offer 并返回 SDP answer
     * 
     * @param sdpOffer SDP offer from client
     * @return SDP answer
     */
    public String processOffer(String sdpOffer) {
        if (!active) {
            throw new IllegalStateException("Session is not active");
        }
        
        try {
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
            logger.debug("Processed SDP offer for session: {}", sessionId);
            return sdpAnswer;
        } catch (Exception e) {
            logger.error("Error processing SDP offer for session: {}", sessionId, e);
            throw new RuntimeException("Failed to process SDP offer", e);
        }
    }
    
    /**
     * Adds ICE candidate to the WebRTC endpoint
     * 向 WebRTC 端点添加 ICE candidate
     * 
     * @param candidate ICE candidate
     */
    public void addIceCandidate(IceCandidate candidate) {
        if (!active) {
            logger.warn("Attempting to add ICE candidate to inactive session: {}", sessionId);
            return;
        }
        
        try {
            webRtcEndpoint.addIceCandidate(candidate);
            logger.debug("Added ICE candidate to session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Error adding ICE candidate to session: {}", sessionId, e);
        }
    }
    
    /**
     * Gathers ICE candidates
     * 收集 ICE candidates
     */
    public void gatherCandidates() {
        if (!active) {
            logger.warn("Attempting to gather candidates for inactive session: {}", sessionId);
            return;
        }
        
        try {
            webRtcEndpoint.gatherCandidates();
            logger.debug("Started gathering ICE candidates for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Error gathering ICE candidates for session: {}", sessionId, e);
        }
    }
    
    /**
     * Releases all resources associated with this session
     * 释放与此会话关联的所有资源
     */
    public void release() {
        if (!active) {
            logger.debug("Session already released: {}", sessionId);
            return;
        }
        
        active = false;
        
        try {
            if (webRtcEndpoint != null) {
                webRtcEndpoint.release();
                logger.debug("Released WebRTC endpoint for session: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("Error releasing WebRTC endpoint for session: {}", sessionId, e);
        }
        
        try {
            if (pipeline != null) {
                pipeline.release();
                logger.debug("Released media pipeline for session: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("Error releasing media pipeline for session: {}", sessionId, e);
        }
        
        logger.info("WebRTC session released: {}", sessionId);
    }
    
    // Getters
    
    public String getSessionId() {
        return sessionId;
    }
    
    public MediaPipeline getPipeline() {
        return pipeline;
    }
    
    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }
    
    public boolean isActive() {
        return active;
    }
}
