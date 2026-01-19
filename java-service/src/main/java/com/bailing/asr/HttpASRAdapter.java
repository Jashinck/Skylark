package com.bailing.asr;

import com.bailing.utils.AudioUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP-based ASR Adapter Implementation
 * 基于HTTP的语音识别适配器
 * 
 * <p>Implements the ASR interface using HTTP multipart file upload
 * to communicate with remote ASR services.</p>
 * 
 * <p>Configuration parameters:</p>
 * <ul>
 *   <li><b>serviceUrl</b> (required): HTTP endpoint for ASR service</li>
 *   <li><b>outputFile</b> (optional): Base path for temporary audio files</li>
 *   <li><b>timeout</b> (optional): Request timeout in seconds (default: 30)</li>
 * </ul>
 * 
 * <p>The adapter performs the following steps:</p>
 * <ol>
 *   <li>Saves audio data as temporary WAV file using AudioUtils</li>
 *   <li>Creates multipart HTTP POST request with file parameter</li>
 *   <li>Sends request to configured serviceUrl</li>
 *   <li>Parses JSON response to extract "text" field</li>
 *   <li>Cleans up temporary file</li>
 * </ol>
 * 
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, Object&gt; config = new HashMap&lt;&gt;();
 * config.put("serviceUrl", "http://localhost:8080/asr/recognize");
 * config.put("outputFile", "/tmp/audio");
 * 
 * ASR asr = new HttpASRAdapter(config);
 * String text = asr.recognize(audioData);
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class HttpASRAdapter implements ASR {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpASRAdapter.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final String DEFAULT_OUTPUT_BASE = "temp/asr";
    
    private final String serviceUrl;
    private final String outputFile;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AudioFormat audioFormat;
    private final int timeout;
    
    /**
     * Creates a new HTTP ASR adapter with the specified configuration.
     * 
     * @param config Configuration map containing:
     *               <ul>
     *                 <li>serviceUrl (String, required): ASR service endpoint URL</li>
     *                 <li>outputFile (String, optional): Base path for temp files</li>
     *                 <li>timeout (Integer, optional): Timeout in seconds</li>
     *               </ul>
     * @throws IllegalArgumentException if serviceUrl is not provided
     */
    public HttpASRAdapter(Map<String, Object> config) {
        if (config == null || !config.containsKey("serviceUrl")) {
            throw new IllegalArgumentException("serviceUrl is required in configuration");
        }
        
        this.serviceUrl = config.get("serviceUrl").toString();
        this.outputFile = config.getOrDefault("outputFile", DEFAULT_OUTPUT_BASE).toString();
        this.timeout = config.containsKey("timeout") 
            ? Integer.parseInt(config.get("timeout").toString()) 
            : DEFAULT_TIMEOUT_SECONDS;
        
        this.webClient = WebClient.builder()
            .baseUrl(this.serviceUrl)
            .build();
        
        this.objectMapper = new ObjectMapper();
        this.audioFormat = AudioUtils.create16kHz16BitMono();
        
        logger.info("Initialized HttpASRAdapter: serviceUrl={}, outputFile={}, timeout={}s", 
            serviceUrl, outputFile, timeout);
    }
    
    /**
     * Recognizes speech from audio data by uploading to HTTP ASR service.
     * 
     * <p>Process flow:</p>
     * <ol>
     *   <li>Validate audio data is not null/empty</li>
     *   <li>Generate unique temporary WAV file path</li>
     *   <li>Save audio data as WAV file</li>
     *   <li>Upload file via HTTP multipart POST</li>
     *   <li>Parse JSON response for "text" field</li>
     *   <li>Clean up temporary file</li>
     * </ol>
     * 
     * @param audioData Raw audio data as byte array
     * @return Recognized text from ASR service, or empty string if no text
     * @throws Exception if any error occurs during recognition
     */
    @Override
    public String recognize(byte[] audioData) throws Exception {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Received null or empty audio data");
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        logger.debug("Starting ASR recognition for {} bytes of audio data", audioData.length);
        
        File tempFile = null;
        try {
            tempFile = createTemporaryAudioFile(audioData);
            
            logger.debug("Uploading audio file to ASR service: {}", tempFile.getAbsolutePath());
            String responseJson = uploadAudioFile(tempFile);
            
            logger.debug("Received ASR response: {}", responseJson);
            String recognizedText = parseRecognitionResponse(responseJson);
            
            logger.info("ASR recognition completed successfully: {} characters recognized", 
                recognizedText != null ? recognizedText.length() : 0);
            
            return recognizedText != null ? recognizedText : "";
            
        } catch (Exception e) {
            logger.error("ASR recognition failed", e);
            throw new Exception("Failed to recognize speech: " + e.getMessage(), e);
        } finally {
            cleanupTemporaryFile(tempFile);
        }
    }
    
    /**
     * Creates a temporary WAV file from audio data.
     * 
     * @param audioData Raw audio data
     * @return Temporary file containing WAV audio
     * @throws IOException if file creation fails
     */
    private File createTemporaryAudioFile(byte[] audioData) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = outputFile + "_" + timestamp + ".wav";
        Path filePath = Paths.get(filename);
        
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            logger.debug("Creating directory for temporary files: {}", parentDir);
            Files.createDirectories(parentDir);
        }
        
        logger.debug("Creating temporary WAV file: {}", filename);
        AudioUtils.saveAsWav(audioData, filename, audioFormat);
        
        File file = filePath.toFile();
        if (!file.exists() || file.length() == 0) {
            throw new IOException("Failed to create valid temporary WAV file");
        }
        
        logger.debug("Temporary WAV file created: {} ({} bytes)", filename, file.length());
        return file;
    }
    
    /**
     * Uploads audio file to ASR service via HTTP multipart POST.
     * 
     * @param audioFile File to upload
     * @return JSON response from ASR service
     * @throws Exception if upload fails
     */
    private String uploadAudioFile(File audioFile) throws Exception {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(audioFile));
            
            Mono<String> responseMono = webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout));
            
            String response = responseMono.block();
            
            if (response == null || response.trim().isEmpty()) {
                throw new Exception("Received empty response from ASR service");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to upload audio file to ASR service: {}", serviceUrl, e);
            throw new Exception("ASR service communication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses ASR service JSON response to extract recognized text.
     * 
     * <p>Expected JSON format:</p>
     * <pre>
     * {
     *   "text": "recognized text here",
     *   "status": "success"
     * }
     * </pre>
     * 
     * @param jsonResponse JSON response string
     * @return Recognized text, or empty string if not found
     * @throws Exception if JSON parsing fails
     */
    private String parseRecognitionResponse(String jsonResponse) throws Exception {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            if (rootNode.has("text")) {
                String text = rootNode.get("text").asText();
                logger.debug("Parsed recognition text: {}", text);
                return text;
            } else {
                logger.warn("Response JSON does not contain 'text' field: {}", jsonResponse);
                return "";
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse ASR response JSON", e);
            throw new Exception("Invalid ASR response format: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cleans up temporary audio file.
     * 
     * @param file File to delete, or null
     */
    private void cleanupTemporaryFile(File file) {
        if (file != null && file.exists()) {
            try {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.debug("Temporary file deleted: {}", file.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.error("Error deleting temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}
