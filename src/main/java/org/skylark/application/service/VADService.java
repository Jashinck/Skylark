package org.skylark.application.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Base64;
import java.util.Collections;
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
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class VADService {
    
    private static final Logger logger = LoggerFactory.getLogger(VADService.class);
    
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int MS_PER_SECOND = 1000;
    
    private OrtEnvironment env;
    private OrtSession session;
    private boolean useOnnx = false;
    
    @Value("${vad.model.path:models/silero_vad.onnx}")
    private String modelPath;
    
    @Value("${vad.sampling.rate:16000}")
    private int samplingRate;
    
    @Value("${vad.threshold:0.5}")
    private float threshold;
    
    @Value("${vad.min.silence.duration.ms:500}")
    private int minSilenceDurationMs;
    
    @Value("${vad.frame.duration.ms:50}")
    private int frameDurationMs;
    
    private final Map<String, VADState> sessionStates = new ConcurrentHashMap<>();
    
    /**
     * Initializes the Silero VAD model using ONNX Runtime.
     */
    @PostConstruct
    public void init() {
        logger.info("正在初始化VAD服务...");
        logger.info("采样率: {} Hz, 阈值: {}, 最小静音时长: {} ms", 
            samplingRate, threshold, minSilenceDurationMs);
        
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
     * Performs ONNX-based VAD detection using Silero VAD.
     * 
     * @param audioBytes Audio data bytes (int16 PCM)
     * @param state Session state
     * @return Detection status ("start", "end", or null)
     */
    private String performOnnxDetection(byte[] audioBytes, VADState state) {
        try {
            float[] audioSamples = convertBytesToFloatArray(audioBytes);
            
            long[] shape = {1, audioSamples.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, 
                FloatBuffer.wrap(audioSamples), shape);
            
            try (OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor))) {
                float speechProb = extractSpeechProbability(result);
                logger.debug("Speech probability: {}, threshold: {}", speechProb, threshold);
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
            samples[i] = sample / 32768.0f;
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
        OnnxValue outputValue = result.get(0);
        
        if (outputValue instanceof OnnxTensor) {
            OnnxTensor outputTensor = (OnnxTensor) outputValue;
            float[][] output = (float[][]) outputTensor.getValue();
            
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
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / 2;
        
        double energy = 0;
        for (int i = 0; i < numSamples; i++) {
            short sample = buffer.getShort();
            energy += Math.abs(sample);
        }
        energy /= numSamples;
        
        double normalizedEnergy = energy / 32768.0;
        logger.debug("Energy: {}, threshold: {}", normalizedEnergy, threshold);
        
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
