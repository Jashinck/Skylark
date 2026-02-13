package org.skylark.infrastructure.adapter.webrtc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.application.service.ASRService;
import org.skylark.application.service.VADService;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

/**
 * Audio Processor
 * 音频流处理器
 * 
 * <p>Processes audio streams from WebRTC endpoint and integrates
 * with VAD and ASR services for speech detection and recognition.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class AudioProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioProcessor.class);
    
    private final VADService vadService;
    private final ASRService asrService;
    private final String sessionId;
    
    private final ByteArrayOutputStream audioBuffer;
    private volatile boolean isSpeaking;
    
    /**
     * Creates a new audio processor
     * 
     * @param vadService VAD service for voice activity detection
     * @param asrService ASR service for speech recognition
     * @param sessionId Session identifier
     */
    public AudioProcessor(VADService vadService, ASRService asrService, String sessionId) {
        if (vadService == null) {
            throw new IllegalArgumentException("VAD service cannot be null");
        }
        if (asrService == null) {
            throw new IllegalArgumentException("ASR service cannot be null");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        this.vadService = vadService;
        this.asrService = asrService;
        this.sessionId = sessionId;
        this.audioBuffer = new ByteArrayOutputStream();
        this.isSpeaking = false;
    }
    
    /**
     * Processes an audio chunk through VAD pipeline
     * 通过 VAD 管道处理音频块
     * 
     * @param audioData Raw PCM audio data (16kHz, 16-bit, mono)
     * @return VAD detection status ("start", "end", or null)
     */
    public String processAudioChunk(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return null;
        }
        
        try {
            // Convert audio data to base64 for VAD service
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            
            // Perform VAD detection
            Map<String, Object> vadResult = vadService.detect(audioBase64, sessionId);
            String detectionStatus = (String) vadResult.get("status");
            
            if (detectionStatus != null) {
                handleVADStatus(detectionStatus, audioData);
            } else {
                // No status change, continue accumulating if speaking
                if (isSpeaking) {
                    audioBuffer.write(audioData);
                }
            }
            
            return detectionStatus;
            
        } catch (Exception e) {
            logger.error("Error processing audio chunk for session: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Handles VAD status changes
     * 处理 VAD 状态变化
     * 
     * @param status VAD status ("start" or "end")
     * @param audioData Current audio data
     */
    private void handleVADStatus(String status, byte[] audioData) {
        try {
            if ("start".equals(status)) {
                logger.info("Speech started for session: {}", sessionId);
                isSpeaking = true;
                audioBuffer.reset();
                audioBuffer.write(audioData);
                
            } else if ("end".equals(status)) {
                logger.info("Speech ended for session: {}", sessionId);
                isSpeaking = false;
                
                // Add the last chunk
                audioBuffer.write(audioData);
                
                // Trigger ASR recognition on accumulated audio
                byte[] completeAudio = audioBuffer.toByteArray();
                if (completeAudio.length > 0) {
                    recognizeSpeech(completeAudio);
                }
                
                // Reset buffer
                audioBuffer.reset();
            }
        } catch (Exception e) {
            logger.error("Error handling VAD status for session: {}", sessionId, e);
        }
    }
    
    /**
     * Performs speech recognition on accumulated audio
     * 对累积的音频执行语音识别
     * 
     * @param audioData Complete audio data to recognize
     * @return Recognized text or null if recognition fails
     */
    public String recognizeSpeech(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("No audio data to recognize for session: {}", sessionId);
            return null;
        }
        
        try {
            logger.debug("Starting ASR recognition for session: {} with {} bytes", 
                sessionId, audioData.length);
            
            Map<String, String> asrResult = asrService.recognize(audioData);
            String recognizedText = asrResult.get("text");
            
            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                logger.info("ASR result for session {}: {}", sessionId, recognizedText);
                return recognizedText;
            } else {
                logger.debug("No text recognized for session: {}", sessionId);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error performing ASR for session: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Resets the audio processor state
     * 重置音频处理器状态
     */
    public void reset() {
        isSpeaking = false;
        audioBuffer.reset();
        logger.debug("Audio processor reset for session: {}", sessionId);
    }
    
    /**
     * Gets the current speaking state
     * 获取当前说话状态
     * 
     * @return true if speech is being detected
     */
    public boolean isSpeaking() {
        return isSpeaking;
    }
    
    /**
     * Gets the session ID
     * 获取会话 ID
     * 
     * @return Session identifier
     */
    public String getSessionId() {
        return sessionId;
    }
}
