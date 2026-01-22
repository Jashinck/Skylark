package com.bailing.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VAD (Voice Activity Detection) Service Implementation
 * 语音活动检测服务实现
 * 
 * <p>This implementation uses Silero VAD with ONNX Runtime for accurate
 * voice activity detection. Supports int16 PCM audio at 16kHz.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Service
public class VADService {
    
    private static final Logger logger = LoggerFactory.getLogger(VADService.class);
    
    private OrtEnvironment env;
    private OrtSession session;
    
    @Value("${vad.model.path:models/silero_vad.onnx}")
    private String modelPath;
    
    @Value("${vad.sampling.rate:16000}")
    private int samplingRate;
    
    @Value("${vad.threshold:0.5}")
    private float threshold;
    
    @Value("${vad.min.silence.duration.ms:500}")
    private int minSilenceDurationMs;
    
    private final Map<String, VADState> sessionStates = new ConcurrentHashMap<>();
    
    /**
     * Initializes the Silero VAD model using ONNX Runtime.
     * Loads the ONNX model from the configured path during service startup.
     * 
     * @throws Exception if model loading fails
     */
    @PostConstruct
    public void init() throws Exception {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            logger.error("❌ Silero VAD模型文件不存在: {}", modelPath);
            logger.error("请按照以下步骤下载模型:");
            logger.error("1. 创建模型目录: mkdir -p models");
            logger.error("2. 下载模型: wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx -O {}", modelPath);
            throw new IllegalStateException("Silero VAD模型文件不存在: " + modelPath);
        }
        
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = env.createSession(modelPath, options);
            logger.info("✅ Silero VAD模型加载成功: {}", modelPath);
            logger.info("采样率: {} Hz, 阈值: {}, 最小静音时长: {} ms", 
                samplingRate, threshold, minSilenceDurationMs);
        } catch (Exception e) {
            logger.error("❌ 加载Silero VAD模型失败", e);
            throw new Exception("Failed to load Silero VAD model from " + modelPath, e);
        }
    }
    
    /**
     * Detects voice activity in audio data using Silero VAD.
     * 
     * @param audioDataBase64 Base64-encoded audio data (int16 PCM)
     * @param sessionId Session identifier
     * @return Map containing detection status ("start", "end", or null)
     * @throws Exception if detection fails
     */
    public Map<String, Object> detect(String audioDataBase64, String sessionId) throws Exception {
        if (audioDataBase64 == null || audioDataBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        // Decode audio data
        byte[] audioBytes = Base64.getDecoder().decode(audioDataBase64);
        
        // Get or create session state
        VADState state = sessionStates.computeIfAbsent(sessionId, k -> new VADState());
        
        // Perform VAD detection using ONNX
        String detectionStatus = performDetection(audioBytes, state);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", detectionStatus);
        
        if (detectionStatus != null) {
            result.put("timestamp", System.currentTimeMillis());
            logger.info("VAD检测: session={}, status={}", sessionId, detectionStatus);
        }
        
        return result;
    }
    
    /**
     * Performs actual voice activity detection using Silero VAD ONNX model.
     * 
     * @param audioBytes Audio data bytes (int16 PCM)
     * @param state Session state
     * @return Detection status ("start", "end", or null)
     */
    private String performDetection(byte[] audioBytes, VADState state) {
        if (session == null) {
            throw new IllegalStateException("Silero VAD模型未初始化");
        }
        
        try {
            // Convert bytes to float array
            float[] audioFloat = bytesToFloatArray(audioBytes);
            
            // Prepare ONNX inputs
            long[] shape = {1, audioFloat.length};
            FloatBuffer floatBuffer = FloatBuffer.wrap(audioFloat);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, floatBuffer, shape);
            
            // Create input map
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", inputTensor);
            
            // Run inference
            try (OrtSession.Result result = session.run(inputs)) {
                // Get output tensor and extract probability
                float[][] output = (float[][]) result.get(0).getValue();
                float probability = output[0][0];
                
                logger.debug("VAD probability: {}, threshold: {}", probability, threshold);
                
                // Update state and get status
                return updateStateAndGetStatus(state, probability);
                
            } finally {
                inputTensor.close();
            }
            
        } catch (Exception e) {
            logger.error("Silero VAD检测失败", e);
            // Fallback to energy-based detection
            return performEnergyBasedDetection(audioBytes, state);
        }
    }
    
    /**
     * Converts int16 PCM bytes to float array normalized to [-1, 1].
     * 
     * @param bytes Audio bytes (int16 PCM)
     * @return Float array normalized to [-1, 1]
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[bytes.length / 2];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getShort() / 32768.0f;
        }
        return floats;
    }
    
    /**
     * Updates VAD state and returns detection status.
     * 
     * @param state Session state
     * @param probability Speech probability from model
     * @return Detection status ("start", "end", or null)
     */
    private String updateStateAndGetStatus(VADState state, float probability) {
        boolean isSpeech = probability > threshold;
        
        if (isSpeech && !state.isSpeaking) {
            state.isSpeaking = true;
            state.silenceFrames = 0;
            return "start";
        } else if (!isSpeech && state.isSpeaking) {
            state.silenceFrames++;
            // Simple frame-based silence detection
            // More sophisticated implementations would consider actual time duration
            if (state.silenceFrames >= 5) { // ~5 frames of silence
                state.isSpeaking = false;
                state.silenceFrames = 0;
                return "end";
            }
        } else if (isSpeech) {
            state.silenceFrames = 0;
        }
        
        return null;
    }
    
    /**
     * Fallback energy-based detection when ONNX inference fails.
     * 
     * @param audioBytes Audio data bytes
     * @param state Session state
     * @return Detection status
     */
    private String performEnergyBasedDetection(byte[] audioBytes, VADState state) {
        // Convert bytes to int16 samples
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / 2;
        
        // Calculate simple energy metric
        double energy = 0;
        for (int i = 0; i < numSamples; i++) {
            short sample = buffer.getShort();
            energy += Math.abs(sample);
        }
        energy /= numSamples;
        
        // Normalize to 0-1 range
        double normalizedEnergy = energy / 32768.0;
        
        logger.debug("Fallback energy: {}, threshold: {}", normalizedEnergy, threshold);
        
        // Simple state machine
        boolean isSpeech = normalizedEnergy > threshold;
        
        if (isSpeech && !state.isSpeaking) {
            state.isSpeaking = true;
            state.silenceFrames = 0;
            return "start";
        } else if (!isSpeech && state.isSpeaking) {
            state.silenceFrames++;
            if (state.silenceFrames >= 5) {
                state.isSpeaking = false;
                state.silenceFrames = 0;
                return "end";
            }
        } else if (!isSpeech) {
            state.silenceFrames = 0;
        }
        
        return null;
    }
    
    /**
     * Resets VAD state for a session.
     * 
     * @param sessionId Session identifier
     */
    public void reset(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        VADState state = sessionStates.get(sessionId);
        if (state != null) {
            state.reset();
            logger.info("VAD状态已重置: {}", sessionId);
        }
    }
    
    /**
     * Clears all VAD sessions.
     */
    public void clearAll() {
        sessionStates.clear();
        logger.info("所有VAD会话已清除");
    }
    
    /**
     * VAD state holder for a session.
     */
    private static class VADState {
        boolean isSpeaking = false;
        int silenceFrames = 0;
        
        void reset() {
            isSpeaking = false;
            silenceFrames = 0;
        }
    }
}
