package com.bailing.tts;

import com.bailing.service.TTSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Direct TTS Adapter Implementation
 * 直接调用TTS服务的适配器
 * 
 * <p>Implements the TTS interface by directly calling the TTSService
 * instead of making HTTP calls. This provides better performance and
 * eliminates network overhead.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class DirectTTSAdapter implements TTS {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectTTSAdapter.class);
    
    private TTSService ttsService;
    private String voice;
    
    /**
     * Creates a new Direct TTS adapter with the specified configuration.
     * 
     * @param config Configuration map (used for compatibility with ComponentFactory)
     */
    public DirectTTSAdapter(Map<String, Object> config) {
        // Extract voice configuration if provided
        if (config != null && config.containsKey("voice")) {
            this.voice = config.get("voice").toString();
        }
        logger.info("Initialized DirectTTSAdapter with voice: {}", voice);
    }
    
    /**
     * Sets the TTS service (called by Spring or ComponentFactory).
     * 
     * @param ttsService The TTS service to use
     */
    public void setTtsService(TTSService ttsService) {
        this.ttsService = ttsService;
    }
    
    /**
     * Synthesizes speech from text by calling TTSService directly.
     * 
     * @param text Text to synthesize into speech
     * @return Absolute path to the generated audio file
     * @throws Exception if any error occurs during synthesis
     */
    @Override
    public String synthesize(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Received null or empty text");
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        logger.debug("Starting TTS synthesis for text: {} characters", text.length());
        
        if (ttsService == null) {
            throw new IllegalStateException("TTSService not initialized. " +
                "Make sure Spring context is properly configured and TTSService bean is available. " +
                "Check that ComponentFactoryConfig is loaded and @Service annotation is present on TTSService.");
        }
        
        try {
            File audioFile = ttsService.synthesize(text, voice);
            String audioPath = audioFile.getAbsolutePath();
            
            logger.info("TTS synthesis completed successfully: {}", audioPath);
            return audioPath;
            
        } catch (Exception e) {
            logger.error("TTS synthesis failed", e);
            throw new Exception("Failed to synthesize speech: " + e.getMessage(), e);
        }
    }
}
