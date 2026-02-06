package org.skylark.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.ASR;
import org.skylark.infrastructure.adapter.LLM;
import org.skylark.infrastructure.adapter.TTS;
import org.skylark.infrastructure.adapter.VAD;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestration Service
 * 编排服务
 * 
 * <p>Orchestrates the VAD->ASR->LLM->TTS pipeline for real-time voice interaction.
 * Manages session state and coordinates between different AI services.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class OrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);
    
    private final VADService vadService;
    private final ASRService asrService;
    private final TTSService ttsService;
    private final LLM llmAdapter;
    
    // For synchronous LLM responses
    private final Map<String, StringBuilder> llmResponses = new ConcurrentHashMap<>();
    
    // Session audio buffers for VAD processing
    private final Map<String, ByteArrayOutputStream> sessionBuffers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionSpeaking = new ConcurrentHashMap<>();
    
    // Temp directory for audio processing
    private final String tempDir = "temp/orchestration";
    
    public OrchestrationService(VADService vadService, ASRService asrService, 
                               TTSService ttsService, LLM llmAdapter) {
        this.vadService = vadService;
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.llmAdapter = llmAdapter;
        
        // Create temp directory
        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (Exception e) {
            logger.error("Failed to create temp directory", e);
        }
    }

    /**
     * Callback interface for sending responses
     */
    @FunctionalInterface
    public interface ResponseCallback {
        void send(String sessionId, String type, Object data);
    }

    /**
     * Process audio stream through VAD->ASR->LLM->TTS pipeline
     * 
     * @param sessionId Session identifier
     * @param audioData Raw PCM audio data (16kHz, 16-bit, mono)
     * @param callback Callback for sending responses
     */
    public void processAudioStream(String sessionId, byte[] audioData, ResponseCallback callback) {
        try {
            // Get or create buffer for this session
            ByteArrayOutputStream buffer = sessionBuffers.computeIfAbsent(
                sessionId, k -> new ByteArrayOutputStream()
            );
            
            // Append audio data to buffer
            buffer.write(audioData);
            
            // Check for voice activity using VAD
            boolean isSpeaking = detectVoiceActivity(sessionId, audioData);
            Boolean wasSpeaking = sessionSpeaking.getOrDefault(sessionId, false);
            
            // If speech just ended, process the buffered audio
            if (wasSpeaking && !isSpeaking && buffer.size() > 0) {
                logger.info("Speech ended for session {}, processing buffer with {} bytes", 
                    sessionId, buffer.size());
                
                // Process the complete speech segment
                processCompleteSpeech(sessionId, buffer.toByteArray(), callback);
                
                // Reset buffer
                buffer.reset();
            }
            
            // Update speaking state
            sessionSpeaking.put(sessionId, isSpeaking);
            
        } catch (Exception e) {
            logger.error("Error processing audio stream for session: {}", sessionId, e);
            callback.send(sessionId, "error", Map.of("message", "Error processing audio: " + e.getMessage()));
        }
    }

    /**
     * Process text input through LLM->TTS pipeline
     * 
     * @param sessionId Session identifier
     * @param text User text input
     * @param callback Callback for sending responses
     */
    public void processTextInput(String sessionId, String text, ResponseCallback callback) {
        try {
            logger.info("Processing text input for session {}: {}", sessionId, text);
            
            // Send ASR result notification
            callback.send(sessionId, "asr_result", Map.of("text", text));
            
            // Get LLM response
            String llmResponse = getLLMResponse(text);
            logger.info("LLM response for session {}: {}", sessionId, llmResponse);
            
            // Send LLM response
            callback.send(sessionId, "llm_response", Map.of("text", llmResponse));
            
            // Generate TTS audio
            byte[] ttsAudio = generateTTS(llmResponse);
            if (ttsAudio != null && ttsAudio.length > 0) {
                String audioBase64 = Base64.getEncoder().encodeToString(ttsAudio);
                callback.send(sessionId, "tts_audio", Map.of("audio", audioBase64));
            }
            
        } catch (Exception e) {
            logger.error("Error processing text input for session: {}", sessionId, e);
            callback.send(sessionId, "error", Map.of("message", "Error processing text: " + e.getMessage()));
        }
    }

    /**
     * Cleanup session resources
     * 
     * @param sessionId Session identifier
     */
    public void cleanupSession(String sessionId) {
        sessionBuffers.remove(sessionId);
        sessionSpeaking.remove(sessionId);
        logger.info("Cleaned up session: {}", sessionId);
    }

    /**
     * Detect voice activity in audio data
     */
    private boolean detectVoiceActivity(String sessionId, byte[] audioData) {
        try {
            // Convert byte array to base64 for VAD service
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            
            // Run VAD detection
            Map<String, Object> result = vadService.detect(audioBase64, sessionId);
            return (Boolean) result.getOrDefault("isSpeaking", false);
        } catch (Exception e) {
            logger.error("Error detecting voice activity", e);
            return false;
        }
    }

    /**
     * Process complete speech segment through ASR->LLM->TTS
     */
    private void processCompleteSpeech(String sessionId, byte[] audioData, ResponseCallback callback) {
        try {
            // Step 1: ASR - Convert speech to text
            String transcription = performASR(audioData);
            if (transcription == null || transcription.trim().isEmpty()) {
                logger.warn("No transcription result for session: {}", sessionId);
                return;
            }
            
            logger.info("ASR result for session {}: {}", sessionId, transcription);
            callback.send(sessionId, "asr_result", Map.of("text", transcription));
            
            // Step 2: LLM - Get intelligent response
            String llmResponse = getLLMResponse(transcription);
            logger.info("LLM response for session {}: {}", sessionId, llmResponse);
            callback.send(sessionId, "llm_response", Map.of("text", llmResponse));
            
            // Step 3: TTS - Convert response to speech
            byte[] ttsAudio = generateTTS(llmResponse);
            if (ttsAudio != null && ttsAudio.length > 0) {
                String audioBase64 = Base64.getEncoder().encodeToString(ttsAudio);
                callback.send(sessionId, "tts_audio", Map.of("audio", audioBase64));
            }
            
        } catch (Exception e) {
            logger.error("Error processing complete speech for session: {}", sessionId, e);
            callback.send(sessionId, "error", Map.of("message", "Error processing speech: " + e.getMessage()));
        }
    }

    /**
     * Get LLM response synchronously
     */
    private String getLLMResponse(String text) throws Exception {
        String responseId = UUID.randomUUID().toString();
        StringBuilder response = new StringBuilder();
        llmResponses.put(responseId, response);
        
        try {
            // Create message list for LLM
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            messages.add(userMessage);
            
            // Call LLM with streaming and collect response
            llmAdapter.chat(messages, 
                chunk -> response.append(chunk),
                () -> logger.debug("LLM streaming completed for response: {}", responseId)
            );
            
            return response.toString();
        } finally {
            llmResponses.remove(responseId);
        }
    }

    /**
     * Perform ASR on audio data
     */
    private String performASR(byte[] audioData) {
        try {
            // Perform recognition
            Map<String, String> result = asrService.recognize(audioData);
            return result.get("text");
        } catch (Exception e) {
            logger.error("Error performing ASR", e);
            return null;
        }
    }

    /**
     * Generate TTS audio from text
     */
    private byte[] generateTTS(String text) {
        try {
            File audioFile = ttsService.synthesize(text, null);
            
            // Read the generated audio file
            if (audioFile != null && audioFile.exists()) {
                return Files.readAllBytes(audioFile.toPath());
            }
            return null;
        } catch (Exception e) {
            logger.error("Error generating TTS", e);
            return null;
        }
    }
}
