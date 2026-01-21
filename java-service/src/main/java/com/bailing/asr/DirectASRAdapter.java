package com.bailing.asr;

import com.bailing.service.ASRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Direct ASR Adapter Implementation
 * 直接调用ASR服务的适配器
 * 
 * <p>Implements the ASR interface by directly calling the ASRService
 * instead of making HTTP calls. This provides better performance and
 * eliminates network overhead.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class DirectASRAdapter implements ASR {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectASRAdapter.class);
    
    private ASRService asrService;
    
    /**
     * Creates a new Direct ASR adapter with the specified configuration.
     * 
     * @param config Configuration map (used for compatibility with ComponentFactory)
     */
    public DirectASRAdapter(Map<String, Object> config) {
        logger.info("Initialized DirectASRAdapter");
    }
    
    /**
     * Sets the ASR service (called by Spring or ComponentFactory).
     * 
     * @param asrService The ASR service to use
     */
    public void setAsrService(ASRService asrService) {
        this.asrService = asrService;
    }
    
    /**
     * Recognizes speech from audio data by calling ASRService directly.
     * 
     * @param audioData Raw audio data as byte array
     * @return Recognized text from ASR service
     * @throws Exception if any error occurs during recognition
     */
    @Override
    public String recognize(byte[] audioData) throws Exception {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Received null or empty audio data");
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        logger.debug("Starting ASR recognition for {} bytes of audio data", audioData.length);
        
        if (asrService == null) {
            throw new IllegalStateException("ASRService not initialized. " +
                "Make sure Spring context is properly configured and ASRService bean is available. " +
                "Check that ComponentFactoryConfig is loaded and @Service annotation is present on ASRService.");
        }
        
        try {
            Map<String, String> result = asrService.recognize(audioData);
            String recognizedText = result.get("text");
            
            logger.info("ASR recognition completed successfully: {} characters recognized", 
                recognizedText != null ? recognizedText.length() : 0);
            
            return recognizedText != null ? recognizedText : "";
            
        } catch (Exception e) {
            logger.error("ASR recognition failed", e);
            throw new Exception("Failed to recognize speech: " + e.getMessage(), e);
        }
    }
}
