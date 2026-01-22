package com.bailing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ASR (Automatic Speech Recognition) Service Implementation
 * 自动语音识别服务实现
 * 
 * <p>This implementation uses Vosk for offline speech recognition.
 * Supports WAV format audio (16kHz, 16-bit, mono).</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Service
public class ASRService {
    
    private static final Logger logger = LoggerFactory.getLogger(ASRService.class);
    
    private Model model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${asr.model.path:models/vosk-model-small-cn-0.22}")
    private String modelPath;
    
    @Value("${asr.temp.dir:temp/asr}")
    private String tempDir;
    
    /**
     * Initializes the Vosk ASR model.
     * Loads the model from the configured path during service startup.
     * 
     * @throws Exception if model loading fails
     */
    @PostConstruct
    public void init() throws Exception {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            logger.error("❌ Vosk模型文件不存在: {}", modelPath);
            logger.error("请按照以下步骤下载模型:");
            logger.error("1. 创建模型目录: mkdir -p models");
            logger.error("2. 下载模型: cd models && wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip");
            logger.error("3. 解压模型: unzip vosk-model-small-cn-0.22.zip");
            throw new IllegalStateException("Vosk模型文件不存在: " + modelPath);
        }
        
        try {
            // Vosk Model is thread-safe and can be shared across multiple recognizers
            model = new Model(modelPath);
            logger.info("✅ Vosk ASR模型加载成功: {}", modelPath);
        } catch (Exception e) {
            logger.error("❌ 加载Vosk模型失败", e);
            throw new Exception("Failed to load Vosk model from " + modelPath, e);
        }
    }
    
    /**
     * Recognizes speech from audio data using Vosk.
     * 
     * @param audioData Raw audio data (WAV format, 16kHz, 16-bit, mono)
     * @return Map containing "text" and "language" fields
     * @throws Exception if recognition fails
     */
    public Map<String, String> recognize(byte[] audioData) throws Exception {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        logger.info("正在处理音频数据: {} bytes", audioData.length);
        
        // Save audio data temporarily
        File tempFile = saveTempAudioFile(audioData);
        
        try {
            // TODO: Implement actual ASR recognition
            // For now, return a placeholder message
            String recognizedText = performRecognition(tempFile);
            
            Map<String, String> result = new HashMap<>();
            result.put("text", recognizedText);
            result.put("language", "zh");
            
            logger.info("识别完成: {}", recognizedText);
            
            return result;
            
        } finally {
            // Clean up temporary file using Files.deleteIfExists for robustness
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile.toPath());
                    logger.debug("Cleaned up temporary ASR file: {}", tempFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }
    
    /**
     * Performs actual speech recognition using Vosk.
     * 
     * @param audioFile Audio file to recognize (WAV format)
     * @return Recognized text
     * @throws Exception if recognition fails
     */
    private String performRecognition(File audioFile) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Vosk模型未初始化");
        }
        
        logger.debug("使用Vosk识别音频文件: {} ({} bytes)", audioFile.getAbsolutePath(), audioFile.length());
        
        try (Recognizer recognizer = new Recognizer(model, 16000)) {
            // Read audio data
            byte[] audioData = Files.readAllBytes(audioFile.toPath());
            
            // Skip WAV header if present (first 44 bytes)
            int offset = 0;
            int length = audioData.length;
            if (audioData.length > 44 && audioData[0] == 'R' && audioData[1] == 'I' && 
                audioData[2] == 'F' && audioData[3] == 'F') {
                offset = 44;
                length = audioData.length - 44;
            }
            
            // Copy audio data without header
            byte[] pcmData = new byte[length];
            System.arraycopy(audioData, offset, pcmData, 0, length);
            
            // Process audio data
            String result;
            if (recognizer.acceptWaveForm(pcmData, length)) {
                result = recognizer.getResult();
            } else {
                result = recognizer.getPartialResult();
            }
            
            return extractTextFromJson(result);
            
        } catch (Exception e) {
            logger.error("Vosk识别失败", e);
            throw new Exception("Speech recognition failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts text from Vosk JSON result.
     * 
     * @param jsonResult JSON result from Vosk
     * @return Extracted text
     */
    private String extractTextFromJson(String jsonResult) {
        try {
            JsonNode node = objectMapper.readTree(jsonResult);
            if (node.has("text")) {
                return node.get("text").asText();
            } else if (node.has("partial")) {
                return node.get("partial").asText();
            }
            return "";
        } catch (Exception e) {
            logger.warn("解析Vosk结果失败: {}", jsonResult, e);
            return "";
        }
    }
    
    /**
     * Saves audio data to a temporary file.
     * 
     * @param audioData Audio data bytes
     * @return Temporary file
     * @throws Exception if file creation fails
     */
    private File saveTempAudioFile(byte[] audioData) throws Exception {
        Path dirPath = Paths.get(tempDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        String filename = "asr_" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        File tempFile = dirPath.resolve(filename).toFile();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(audioData);
        }
        
        logger.debug("临时音频文件已保存: {}", tempFile.getAbsolutePath());
        
        return tempFile;
    }
}
