package org.skylark.infrastructure.adapter.webrtc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurento Client Adapter Implementation
 * Kurento 客户端适配器实现
 * 
 * <p>Manages connection to Kurento Media Server and provides
 * operations for creating media pipelines and WebRTC endpoints.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Component
public class KurentoClientAdapterImpl implements KurentoClientAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(KurentoClientAdapterImpl.class);
    
    private final WebRTCProperties webRTCProperties;
    
    private KurentoClient kurentoClient;
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    
    @Autowired
    public KurentoClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }
    
    /**
     * Initializes Kurento client connection
     * 初始化 Kurento 客户端连接
     */
    @PostConstruct
    public void init() {
        try {
            String kurentoWsUri = webRTCProperties.getKurento().getWsUri();
            logger.info("Initializing Kurento Client, connecting to: {}", kurentoWsUri);
            kurentoClient = KurentoClient.create(kurentoWsUri);
            logger.info("✅ Kurento Client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Kurento Client. Make sure Kurento Media Server is running at: {}", 
                webRTCProperties.getKurento().getWsUri(), e);
            logger.warn("Kurento WebRTC features will not be available");
        }
    }
    
    /**
     * Destroys Kurento client and releases all resources
     * 销毁 Kurento 客户端并释放所有资源
     */
    @PreDestroy
    public void destroy() {
        if (kurentoClient != null) {
            try {
                // Release all pipelines
                pipelines.values().forEach(pipeline -> {
                    try {
                        pipeline.release();
                    } catch (Exception e) {
                        logger.warn("Error releasing pipeline", e);
                    }
                });
                pipelines.clear();
                
                kurentoClient.destroy();
                logger.info("Kurento Client destroyed");
            } catch (Exception e) {
                logger.error("Error destroying Kurento Client", e);
            }
        }
    }
    
    @Override
    public MediaPipeline createMediaPipeline() {
        if (kurentoClient == null) {
            throw new IllegalStateException("Kurento Client is not initialized. Check if Kurento Media Server is running.");
        }
        
        try {
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            pipelines.put(pipeline.getId(), pipeline);
            logger.debug("Created media pipeline: {}", pipeline.getId());
            return pipeline;
        } catch (Exception e) {
            logger.error("Failed to create media pipeline", e);
            throw new RuntimeException("Failed to create media pipeline", e);
        }
    }
    
    @Override
    public void releaseMediaPipeline(String pipelineId) {
        MediaPipeline pipeline = pipelines.remove(pipelineId);
        if (pipeline != null) {
            try {
                pipeline.release();
                logger.debug("Released media pipeline: {}", pipelineId);
            } catch (Exception e) {
                logger.error("Error releasing media pipeline: {}", pipelineId, e);
            }
        }
    }
    
    @Override
    public WebRtcEndpoint createWebRTCEndpoint(MediaPipeline pipeline) {
        if (pipeline == null) {
            throw new IllegalArgumentException("Media pipeline cannot be null");
        }
        
        try {
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            
            // Configure STUN server
            String stunServer = webRTCProperties.getStun().getServer();
            if (stunServer != null && !stunServer.trim().isEmpty()) {
                webRtcEndpoint.setStunServerAddress(stunServer);
                logger.debug("STUN server configured: {}", stunServer);
            }
            
            // Configure TURN server if enabled
            if (webRTCProperties.getTurn().isEnabled()) {
                String turnUrl = webRTCProperties.getTurn().getTurnUrl();
                String turnUsername = webRTCProperties.getTurn().getUsername();
                String turnPassword = webRTCProperties.getTurn().getPassword();
                
                if (turnUrl != null && !turnUrl.trim().isEmpty()) {
                    webRtcEndpoint.setTurnUrl(turnUrl);
                    logger.debug("TURN server configured: {}", turnUrl);
                    
                    if (turnUsername != null && !turnUsername.trim().isEmpty()) {
                        // Note: Kurento doesn't have a direct API for TURN credentials
                        // They need to be embedded in the TURN URL or configured in Kurento server
                        logger.debug("TURN credentials configured for user: {}", turnUsername);
                    }
                }
            }
            
            logger.debug("Created WebRTC endpoint for pipeline: {}", pipeline.getId());
            return webRtcEndpoint;
        } catch (Exception e) {
            logger.error("Failed to create WebRTC endpoint", e);
            throw new RuntimeException("Failed to create WebRTC endpoint", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return kurentoClient != null;
    }
}
