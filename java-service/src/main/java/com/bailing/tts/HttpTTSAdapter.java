package com.bailing.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP-based TTS Adapter Implementation
 * 基于HTTP的文本转语音适配器
 * 
 * <p>Implements the TTS interface using HTTP JSON POST requests
 * to communicate with remote TTS services.</p>
 * 
 * <p>Configuration parameters:</p>
 * <ul>
 *   <li><b>serviceUrl</b> (required): HTTP endpoint for TTS service</li>
 *   <li><b>outputFile</b> (optional): Base directory for audio files</li>
 *   <li><b>voice</b> (optional): Voice identifier for synthesis (default: "default")</li>
 *   <li><b>timeout</b> (optional): Request timeout in seconds (default: 60)</li>
 * </ul>
 * 
 * <p>The adapter performs the following steps:</p>
 * <ol>
 *   <li>Creates JSON request with "text" and "voice" fields</li>
 *   <li>Sends HTTP POST request to configured serviceUrl</li>
 *   <li>Receives audio data in response body</li>
 *   <li>Generates unique filename using UUID</li>
 *   <li>Saves audio data to file</li>
 *   <li>Returns absolute path to saved file</li>
 * </ol>
 * 
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, Object&gt; config = new HashMap&lt;&gt;();
 * config.put("serviceUrl", "http://localhost:8080/tts/synthesize");
 * config.put("outputFile", "/tmp/tts");
 * config.put("voice", "en-US-Neural");
 * 
 * TTS tts = new HttpTTSAdapter(config);
 * String audioPath = tts.synthesize("Hello, world!");
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class HttpTTSAdapter implements TTS {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpTTSAdapter.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final String DEFAULT_OUTPUT_BASE = "temp/tts";
    private static final String DEFAULT_VOICE = "default";
    private static final String DEFAULT_FILE_EXTENSION = ".wav";
    
    private final String serviceUrl;
    private final String outputFile;
    private final String voice;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final int timeout;
    
    /**
     * Creates a new HTTP TTS adapter with the specified configuration.
     * 
     * @param config Configuration map containing:
     *               <ul>
     *                 <li>serviceUrl (String, required): TTS service endpoint URL</li>
     *                 <li>outputFile (String, optional): Base directory for audio files</li>
     *                 <li>voice (String, optional): Voice identifier</li>
     *                 <li>timeout (Integer, optional): Timeout in seconds</li>
     *               </ul>
     * @throws IllegalArgumentException if serviceUrl is not provided
     */
    public HttpTTSAdapter(Map<String, Object> config) {
        if (config == null || !config.containsKey("serviceUrl")) {
            throw new IllegalArgumentException("serviceUrl is required in configuration");
        }
        
        this.serviceUrl = config.get("serviceUrl").toString();
        this.outputFile = config.getOrDefault("outputFile", DEFAULT_OUTPUT_BASE).toString();
        this.voice = config.getOrDefault("voice", DEFAULT_VOICE).toString();
        this.timeout = config.containsKey("timeout") 
            ? Integer.parseInt(config.get("timeout").toString()) 
            : DEFAULT_TIMEOUT_SECONDS;
        
        this.webClient = WebClient.builder()
            .baseUrl(this.serviceUrl)
            .build();
        
        this.objectMapper = new ObjectMapper();
        
        logger.info("Initialized HttpTTSAdapter: serviceUrl={}, outputFile={}, voice={}, timeout={}s", 
            serviceUrl, outputFile, voice, timeout);
    }
    
    /**
     * Synthesizes speech from text by sending request to HTTP TTS service.
     * 
     * <p>Process flow:</p>
     * <ol>
     *   <li>Validate text is not null/empty</li>
     *   <li>Create JSON request payload with text and voice</li>
     *   <li>Send HTTP POST request to TTS service</li>
     *   <li>Generate unique output filename using UUID</li>
     *   <li>Download and save audio response to file</li>
     *   <li>Return absolute path to saved audio file</li>
     * </ol>
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
        
        try {
            String outputPath = generateOutputPath();
            
            logger.debug("Sending synthesis request to TTS service");
            downloadAudioData(text, outputPath);
            
            logger.info("TTS synthesis completed successfully: {}", outputPath);
            return outputPath;
            
        } catch (Exception e) {
            logger.error("TTS synthesis failed", e);
            throw new Exception("Failed to synthesize speech: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a unique output file path for the synthesized audio.
     * 
     * @return Absolute path with unique filename
     * @throws IOException if directory creation fails
     */
    private String generateOutputPath() throws IOException {
        String uniqueId = UUID.randomUUID().toString();
        String filename = uniqueId + DEFAULT_FILE_EXTENSION;
        Path dirPath = Paths.get(outputFile);
        
        if (!Files.exists(dirPath)) {
            logger.debug("Creating output directory: {}", dirPath);
            Files.createDirectories(dirPath);
        }
        
        Path filePath = dirPath.resolve(filename);
        String absolutePath = filePath.toAbsolutePath().toString();
        
        logger.debug("Generated output path: {}", absolutePath);
        return absolutePath;
    }
    
    /**
     * Sends TTS request and downloads audio data to file.
     * 
     * <p>Creates JSON request body:</p>
     * <pre>
     * {
     *   "text": "text to synthesize",
     *   "voice": "voice identifier"
     * }
     * </pre>
     * 
     * @param text Text to synthesize
     * @param outputPath Path to save audio file
     * @throws Exception if request fails or file cannot be written
     */
    private void downloadAudioData(String text, String outputPath) throws Exception {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", text);
            requestBody.put("voice", voice);
            
            logger.debug("TTS request: voice={}, textLength={}", voice, text.length());
            
            Flux<DataBuffer> audioDataFlux = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofSeconds(timeout));
            
            Path filePath = Paths.get(outputPath);
            Mono<Void> writeMono = DataBufferUtils.write(
                audioDataFlux, 
                filePath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE
            );
            
            writeMono.block();
            
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                throw new IOException("Failed to save audio data or received empty response");
            }
            
            logger.debug("Audio data saved: {} ({} bytes)", outputPath, Files.size(filePath));
            
        } catch (Exception e) {
            logger.error("Failed to download audio from TTS service: {}", serviceUrl, e);
            
            try {
                Path filePath = Paths.get(outputPath);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.debug("Cleaned up incomplete audio file: {}", outputPath);
                }
            } catch (IOException cleanupEx) {
                logger.warn("Failed to cleanup incomplete file: {}", outputPath, cleanupEx);
            }
            
            throw new Exception("TTS service communication failed: " + e.getMessage(), e);
        }
    }
}
