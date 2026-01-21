package com.bailing.vad;

import com.bailing.service.VADService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;

/**
 * Direct VAD Adapter Implementation
 * 直接调用VAD服务的适配器
 * 
 * <p>Implements the VAD interface by directly calling the VADService
 * instead of making HTTP calls. This provides better performance and
 * eliminates network overhead.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class DirectVADAdapter implements VAD {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectVADAdapter.class);
    
    private VADService vadService;
    
    /**
     * Creates a new Direct VAD adapter with the specified configuration.
     * 
     * @param config Configuration map (used for compatibility with ComponentFactory)
     */
    public DirectVADAdapter(Map<String, Object> config) {
        logger.info("Initialized DirectVADAdapter");
    }
    
    /**
     * Sets the VAD service (called by Spring or ComponentFactory).
     * 
     * @param vadService The VAD service to use
     */
    public void setVadService(VADService vadService) {
        this.vadService = vadService;
    }
    
    /**
     * Detects voice activity by calling VADService directly.
     * 
     * @param audioData Raw audio data as byte array
     * @param sessionId Unique session identifier
     * @return "start", "end", or null based on voice activity
     * @throws Exception if detection fails
     */
    @Override
    public String detect(byte[] audioData, String sessionId) throws Exception {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Received null or empty audio data");
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("Received null or empty session ID");
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        logger.debug("Starting VAD detection for session: {} ({} bytes)", 
            sessionId, audioData.length);
        
        if (vadService == null) {
            throw new IllegalStateException("VADService not initialized. " +
                "Make sure Spring context is properly configured and VADService bean is available. " +
                "Check that ComponentFactoryConfig is loaded and @Service annotation is present on VADService.");
        }
        
        try {
            // Convert audio data to Base64 (as expected by VADService)
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            
            Map<String, Object> result = vadService.detect(base64Audio, sessionId);
            Object statusObj = result.get("status");
            String status = statusObj != null ? statusObj.toString() : null;
            
            if (status != null) {
                logger.info("VAD detected '{}' for session: {}", status, sessionId);
            } else {
                logger.debug("VAD: no state change for session: {}", sessionId);
            }
            
            return status;
            
        } catch (Exception e) {
            logger.error("VAD detection failed for session: {}", sessionId, e);
            throw new Exception("Failed to detect voice activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resets VAD state by calling VADService directly.
     * 
     * @param sessionId Unique session identifier to reset
     * @throws Exception if reset fails
     */
    @Override
    public void reset(String sessionId) throws Exception {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("Received null or empty session ID for reset");
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        logger.debug("Resetting VAD state for session: {}", sessionId);
        
        if (vadService == null) {
            throw new IllegalStateException("VADService not initialized. " +
                "Make sure Spring context is properly configured and VADService bean is available. " +
                "Check that ComponentFactoryConfig is loaded and @Service annotation is present on VADService.");
        }
        
        try {
            vadService.reset(sessionId);
            logger.info("VAD state reset successfully for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("VAD reset failed for session: {}", sessionId, e);
            throw new Exception("Failed to reset VAD state: " + e.getMessage(), e);
        }
    }
}
