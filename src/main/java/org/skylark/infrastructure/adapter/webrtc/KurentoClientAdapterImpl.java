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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private static final int MAX_RECONNECT_DELAY_MS = 60000; // 60 seconds
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000; // 1 second
    
    private final WebRTCProperties webRTCProperties;
    
    private KurentoClient kurentoClient;
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private int reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
    private long lastReconnectAttempt = 0;
    
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
        connectToKurento();
    }
    
    /**
     * Connects to Kurento Media Server
     * 连接到 Kurento 媒体服务器
     */
    private void connectToKurento() {
        try {
            String kurentoWsUri = webRTCProperties.getKurento().getWsUri();
            logger.info("Connecting to Kurento Media Server: {}", kurentoWsUri);
            kurentoClient = KurentoClient.create(kurentoWsUri);
            connected = true;
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS; // Reset reconnect delay on success
            logger.info("✅ Kurento Client connected successfully");
        } catch (Exception e) {
            logger.error("Failed to connect to Kurento Media Server at: {}", 
                webRTCProperties.getKurento().getWsUri(), e);
            logger.warn("Kurento WebRTC features will not be available. Will retry automatically.");
            connected = false;
            kurentoClient = null;
        }
    }
    
    /**
     * Health check scheduled task - runs every 30 seconds
     * 健康检查定时任务 - 每 30 秒执行一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void healthCheck() {
        if (kurentoClient != null) {
            try {
                // Try to get server info to check if connection is alive
                kurentoClient.getServerManager().getInfo();
                
                if (!connected) {
                    logger.info("✅ Kurento connection restored");
                    connected = true;
                    reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
                }
                
            } catch (Exception e) {
                logger.warn("Kurento health check failed: {}", e.getMessage());
                connected = false;
                attemptReconnect();
            }
        } else {
            // Client is null, attempt to reconnect
            if (!connected) {
                attemptReconnect();
            }
        }
    }
    
    /**
     * Attempts to reconnect to Kurento with exponential backoff
     * 尝试使用指数退避重连到 Kurento
     */
    private void attemptReconnect() {
        long now = System.currentTimeMillis();
        
        // Check if enough time has passed since last reconnect attempt
        if (now - lastReconnectAttempt < reconnectDelayMs) {
            return;
        }
        
        lastReconnectAttempt = now;
        logger.info("Attempting to reconnect to Kurento (delay: {}ms)...", reconnectDelayMs);
        
        try {
            // Clean up old client if it exists
            if (kurentoClient != null) {
                try {
                    kurentoClient.destroy();
                } catch (Exception e) {
                    logger.debug("Error destroying old Kurento client", e);
                }
            }
            
            // Attempt new connection
            connectToKurento();
            
            if (connected) {
                logger.info("✅ Reconnected to Kurento successfully");
            } else {
                // Increase delay for next attempt (exponential backoff)
                reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
            }
            
        } catch (Exception e) {
            logger.error("Reconnect attempt failed: {}", e.getMessage());
            // Increase delay for next attempt
            reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
        }
    }
    
    /**
     * Destroys Kurento client and releases all resources
     * 销毁 Kurento 客户端并释放所有资源
     */
    @PreDestroy
    public void destroy() {
        connected = false;
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
        return connected && kurentoClient != null;
    }
}
