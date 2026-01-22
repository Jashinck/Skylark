package com.bailing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
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
 * <p>This is a Java implementation of the ASR service that was originally
 * written in Python using FunASR. This implementation provides a placeholder
 * that can be extended with actual Java-based ASR libraries such as:</p>
 * <ul>
 *   <li>Vosk (offline ASR)</li>
 *   <li>Google Cloud Speech-to-Text</li>
 *   <li>Azure Speech Services</li>
 *   <li>Kaldi (via JNI)</li>
 *   <li>Whisper (via ONNX Runtime)</li>
 * </ul>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Service
public class ASRService {
    
    private static final Logger logger = LoggerFactory.getLogger(ASRService.class);
    
    @Value("${asr.model.dir:models/vosk-model-small-cn-0.22}")
    private String modelDir;
    
    @Value("${asr.device:cpu}")
    private String device;
    
    @Value("${asr.temp.dir:temp/asr}")
    private String tempDir;
    
    @Value("${asr.sample.rate:16000}")
    private int sampleRate;
    
    private Model voskModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Initialize Vosk model on service startup.
     */
    @PostConstruct
    public void initVosk() {
        try {
            logger.info("正在初始化Vosk ASR模型...");
            logger.info("模型路径: {}", modelDir);
            
            File modelPath = new File(modelDir);
            if (!modelPath.exists()) {
                logger.warn("Vosk模型目录不存在: {}. ASR服务将使用占位符模式。", modelDir);
                logger.warn("请从 https://alphacephei.com/vosk/models 下载模型并解压到指定目录。");
                voskModel = null;
                return;
            }
            
            voskModel = new Model(modelDir);
            logger.info("✅ Vosk ASR模型初始化成功");
            logger.info("采样率: {} Hz", sampleRate);
            
        } catch (Exception e) {
            logger.error("Vosk模型初始化失败", e);
            logger.warn("ASR服务将使用占位符模式");
            voskModel = null;
        }
    }
    
    /**
     * Clean up resources on service shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (voskModel != null) {
            try {
                voskModel.close();
                logger.info("Vosk模型资源已释放");
            } catch (Exception e) {
                logger.warn("释放Vosk模型资源时出错", e);
            }
        }
    }
    
    /**
     * Recognizes speech from audio data.
     * 
     * <p>TODO: Implement actual ASR using Java libraries like:</p>
     * <ul>
     *   <li>Vosk for offline recognition</li>
     *   <li>Cloud-based APIs (Google, Azure, AWS)</li>
     *   <li>ONNX Runtime for running ML models</li>
     * </ul>
     * 
     * @param audioData Raw audio data (WAV format)
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
     * @param audioFile Audio file to recognize
     * @return Recognized text
     */
    private String performRecognition(File audioFile) {
        // If Vosk model is not initialized, use placeholder
        if (voskModel == null) {
            logger.warn("ASR服务正在使用占位符实现。请配置Vosk模型。");
            logger.info("音频文件: {} ({} bytes)", audioFile.getAbsolutePath(), audioFile.length());
            return "[ASR占位符: 请配置实际的语音识别服务]";
        }
        
        try (FileInputStream fis = new FileInputStream(audioFile);
             Recognizer recognizer = new Recognizer(voskModel, sampleRate)) {
            
            logger.debug("开始Vosk识别: {}", audioFile.getName());
            
            // Skip WAV header (assumes standard 44-byte header)
            // Note: Standard WAV files have a 44-byte header. If using non-standard WAV formats,
            // this may need adjustment or proper header parsing.
            fis.skip(44);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    // Partial result available
                    String partialResult = recognizer.getResult();
                    logger.debug("部分识别结果: {}", partialResult);
                }
            }
            
            // Get final result
            String finalResult = recognizer.getFinalResult();
            logger.debug("最终识别结果: {}", finalResult);
            
            // Extract text from JSON result
            String recognizedText = extractTextFromVoskResult(finalResult);
            
            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                logger.warn("Vosk识别结果为空");
                return "[无法识别语音内容]";
            }
            
            return recognizedText;
            
        } catch (Exception e) {
            logger.error("Vosk识别过程出错", e);
            return "[识别失败: " + e.getMessage() + "]";
        }
    }
    
    /**
     * Extracts recognized text from Vosk JSON result.
     * 
     * @param voskResult JSON result from Vosk
     * @return Extracted text
     */
    private String extractTextFromVoskResult(String voskResult) {
        try {
            JsonNode rootNode = objectMapper.readTree(voskResult);
            
            // Vosk returns result in "text" field
            if (rootNode.has("text")) {
                return rootNode.get("text").asText();
            }
            
            logger.warn("Vosk结果中没有找到'text'字段: {}", voskResult);
            return "";
            
        } catch (Exception e) {
            logger.error("解析Vosk JSON结果失败", e);
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
