package com.bailing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ai.onnxruntime.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VAD (Voice Activity Detection) Service Implementation
 * 语音活动检测服务实现
 * 
 * <p>This is a Java implementation of the VAD service that was originally
 * written in Python using Silero VAD. This implementation provides a placeholder
 * that can be extended with actual Java-based VAD libraries such as:</p>
 * <ul>
 *   <li>WebRTC VAD (via JNI)</li>
 *   <li>Silero VAD (via ONNX Runtime)</li>
 *   <li>Custom energy-based VAD</li>
 * </ul>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Service
public class VADService {
    
    private static final Logger logger = LoggerFactory.getLogger(VADService.class);
    
    @Value("${vad.sampling.rate:16000}")
    private int samplingRate;
    
    @Value("${vad.threshold:0.5}")
    private double threshold;
    
    @Value("${vad.min.silence.duration.ms:500}")
    private int minSilenceDurationMs;
    
    @Value("${vad.model.path:models/silero_vad.onnx}")
    private String modelPath;
    
    @Value("${vad.frame.duration.ms:50}")
    private int frameDurationMs;
    
    private final Map<String, VADState> sessionStates = new ConcurrentHashMap<>();
    
    // Constants for calculation clarity
    private static final int BYTES_PER_SAMPLE = 2; // int16 = 2 bytes
    private static final int MS_PER_SECOND = 1000;
    
    // ONNX Runtime components
    private OrtEnvironment env;
    private OrtSession session;
    private boolean useOnnx = false;
    
    @PostConstruct
    public void init() {
        logger.info("正在初始化VAD服务...");
        logger.info("采样率: {} Hz, 阈值: {}, 最小静音时长: {} ms", 
            samplingRate, threshold, minSilenceDurationMs);
        
        // Try to initialize Silero VAD with ONNX Runtime
        initSileroVAD();
        
        if (useOnnx) {
            logger.info("✅ VAD服务初始化完成 (使用Silero VAD)");
        } else {
            logger.info("✅ VAD服务初始化完成 (使用简单能量检测)");
        }
    }
    
    /**
     * Initialize Silero VAD with ONNX Runtime.
     */
    private void initSileroVAD() {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                logger.warn("Silero VAD模型文件不存在: {}. 使用简单能量检测。", modelPath);
                logger.warn("请从 https://github.com/snakers4/silero-vad 下载模型文件。");
                return;
            }
            
            logger.info("正在加载Silero VAD模型: {}", modelPath);
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = env.createSession(modelPath, options);
            
            useOnnx = true;
            logger.info("✅ Silero VAD模型加载成功");
            
        } catch (Exception e) {
            logger.error("Silero VAD模型加载失败", e);
            logger.warn("将使用简单能量检测作为后备方案");
            useOnnx = false;
        }
    }
    
    /**
     * Clean up resources on service shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (session != null) {
            try {
                session.close();
                logger.info("ONNX会话已关闭");
            } catch (Exception e) {
                logger.warn("关闭ONNX会话时出错", e);
            }
        }
        if (env != null) {
            try {
                env.close();
                logger.info("ONNX环境已释放");
            } catch (Exception e) {
                logger.warn("释放ONNX环境时出错", e);
            }
        }
    }
    
    /**
     * Detects voice activity in audio data.
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
        
        // Perform VAD detection
        String detectionStatus = useOnnx ? 
            performOnnxDetection(audioBytes, state) : 
            performEnergyDetection(audioBytes, state);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", detectionStatus);
        
        if (detectionStatus != null) {
            result.put("timestamp", System.currentTimeMillis());
            logger.info("VAD检测: session={}, status={}", sessionId, detectionStatus);
        }
        
        return result;
    }
    
    /**
     * Performs VAD detection using Silero VAD (ONNX Runtime).
     * 
     * @param audioBytes Audio data bytes (int16 PCM)
     * @param state Session state
     * @return Detection status ("start", "end", or null)
     */
    private String performOnnxDetection(byte[] audioBytes, VADState state) {
        try {
            // Convert bytes to float array
            float[] audioSamples = convertBytesToFloatArray(audioBytes);
            
            // Prepare input tensor
            long[] shape = {1, audioSamples.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, 
                FloatBuffer.wrap(audioSamples), shape);
            
            // Run inference using singleton map for better performance
            try (OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor))) {
                // Extract speech probability
                float speechProb = extractSpeechProbability(result);
                
                logger.debug("Speech probability: {}, threshold: {}", speechProb, threshold);
                
                // Determine VAD status
                return determineVADStatus(speechProb, state);
                
            } finally {
                inputTensor.close();
            }
            
        } catch (Exception e) {
            logger.error("ONNX VAD检测失败，回退到能量检测", e);
            return performEnergyDetection(audioBytes, state);
        }
    }
    
    /**
     * Converts byte array to float array for ONNX input.
     * 
     * @param audioBytes Audio data bytes (int16 PCM)
     * @return Float array normalized to [-1, 1]
     */
    private float[] convertBytesToFloatArray(byte[] audioBytes) {
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / BYTES_PER_SAMPLE;
        float[] samples = new float[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            short sample = buffer.getShort();
            samples[i] = sample / 32768.0f; // Normalize to [-1, 1]
        }
        
        return samples;
    }
    
    /**
     * Extracts speech probability from ONNX result.
     * 
     * @param result ONNX inference result
     * @return Speech probability [0, 1]
     */
    private float extractSpeechProbability(OrtSession.Result result) throws OrtException {
        // Silero VAD typically outputs speech probability in the first output
        OnnxValue outputValue = result.get(0);
        
        if (outputValue instanceof OnnxTensor) {
            OnnxTensor outputTensor = (OnnxTensor) outputValue;
            float[][] output = (float[][]) outputTensor.getValue();
            
            // Return the speech probability (usually a single value)
            if (output != null && output.length > 0 && output[0].length > 0) {
                return output[0][0];
            }
        }
        
        logger.warn("无法从ONNX结果提取语音概率");
        return 0.0f;
    }
    
    /**
     * Determines VAD status based on speech probability.
     * 
     * @param speechProb Speech probability [0, 1]
     * @param state Session state
     * @return Detection status ("start", "end", or null)
     */
    private String determineVADStatus(float speechProb, VADState state) {
        boolean isSpeech = speechProb > threshold;
        
        if (isSpeech && !state.isSpeaking) {
            state.isSpeaking = true;
            state.silenceFrames = 0;
            return "start";
        } else if (!isSpeech && state.isSpeaking) {
            state.silenceFrames++;
            // Calculate minimum silence frames based on configured frame duration
            int minSilenceFrames = minSilenceDurationMs / frameDurationMs;
            if (state.silenceFrames >= minSilenceFrames) {
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
     * Performs simple energy-based VAD detection (fallback).
     * 
     * @param audioBytes Audio data bytes (int16 PCM)
     * @param state Session state
     * @return Detection status ("start", "end", or null)
     */
    private String performEnergyDetection(byte[] audioBytes, VADState state) {
        // Convert bytes to int16 samples
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / BYTES_PER_SAMPLE;
        
        // Calculate simple energy metric
        double energy = 0;
        for (int i = 0; i < numSamples; i++) {
            short sample = buffer.getShort();
            energy += Math.abs(sample);
        }
        energy /= numSamples;
        
        // Normalize to 0-1 range (int16 max is 32768)
        double normalizedEnergy = energy / 32768.0;
        
        logger.debug("Audio energy: {}, threshold: {}", normalizedEnergy, threshold);
        
        // Simple state machine
        boolean isSpeech = normalizedEnergy > threshold;
        
        if (isSpeech && !state.isSpeaking) {
            state.isSpeaking = true;
            state.silenceFrames = 0;
            return "start";
        } else if (!isSpeech && state.isSpeaking) {
            state.silenceFrames++;
            // Calculate minimum silence frames: (minSilenceDurationMs * samplingRate) / (MS_PER_SECOND * numSamples)
            // This converts: milliseconds → samples → frames
            int minSilenceFrames = calculateMinSilenceFrames(numSamples);
            if (state.silenceFrames >= minSilenceFrames) {
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
     * Calculates minimum silence frames based on configuration.
     * 
     * @param numSamples Number of samples in current audio chunk
     * @return Minimum number of silent frames required to trigger speech end
     */
    private int calculateMinSilenceFrames(int numSamples) {
        // Convert minSilenceDurationMs to number of frames
        // frames = (duration_ms * sampling_rate_hz) / (1000 ms/s * samples_per_frame)
        return (minSilenceDurationMs * samplingRate) / (MS_PER_SECOND * numSamples);
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
