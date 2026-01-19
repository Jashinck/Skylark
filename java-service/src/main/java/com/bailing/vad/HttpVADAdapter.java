package com.bailing.vad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * HTTP-based VAD Adapter Implementation
 * 基于HTTP的语音活动检测适配器
 * 
 * <p>Implements the VAD interface using HTTP JSON POST requests
 * to communicate with remote VAD services.</p>
 * 
 * <p>Configuration parameters:</p>
 * <ul>
 *   <li><b>serviceUrl</b> (required): Base HTTP endpoint for VAD service</li>
 *   <li><b>timeout</b> (optional): Request timeout in seconds (default: 10)</li>
 * </ul>
 * 
 * <p>The adapter uses two endpoints:</p>
 * <ul>
 *   <li><b>POST {serviceUrl}</b>: Detect voice activity</li>
 *   <li><b>POST {serviceUrl}/reset</b>: Reset session state</li>
 * </ul>
 * 
 * <p>Detection request format:</p>
 * <pre>
 * {
 *   "audio": "base64_encoded_audio_data",
 *   "session_id": "unique_session_identifier"
 * }
 * </pre>
 * 
 * <p>Detection response format:</p>
 * <pre>
 * {
 *   "status": "start" | "end" | null
 * }
 * </pre>
 * 
 * <p>Reset request format:</p>
 * <pre>
 * {
 *   "session_id": "unique_session_identifier"
 * }
 * </pre>
 * 
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, Object&gt; config = new HashMap&lt;&gt;();
 * config.put("serviceUrl", "http://localhost:8080/vad/detect");
 * 
 * VAD vad = new HttpVADAdapter(config);
 * String status = vad.detect(audioData, "session123");
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class HttpVADAdapter implements VAD {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpVADAdapter.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final String RESET_ENDPOINT = "/reset";
    
    private final String serviceUrl;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final int timeout;
    
    /**
     * Creates a new HTTP VAD adapter with the specified configuration.
     * 
     * @param config Configuration map containing:
     *               <ul>
     *                 <li>serviceUrl (String, required): VAD service endpoint URL</li>
     *                 <li>timeout (Integer, optional): Timeout in seconds</li>
     *               </ul>
     * @throws IllegalArgumentException if serviceUrl is not provided
     */
    public HttpVADAdapter(Map<String, Object> config) {
        if (config == null || !config.containsKey("serviceUrl")) {
            throw new IllegalArgumentException("serviceUrl is required in configuration");
        }
        
        this.serviceUrl = config.get("serviceUrl").toString();
        this.timeout = config.containsKey("timeout") 
            ? Integer.parseInt(config.get("timeout").toString()) 
            : DEFAULT_TIMEOUT_SECONDS;
        
        this.webClient = WebClient.builder()
            .baseUrl(this.serviceUrl)
            .build();
        
        this.objectMapper = new ObjectMapper();
        
        logger.info("Initialized HttpVADAdapter: serviceUrl={}, timeout={}s", 
            serviceUrl, timeout);
    }
    
    /**
     * Detects voice activity by sending audio data to HTTP VAD service.
     * 
     * <p>Process flow:</p>
     * <ol>
     *   <li>Validate audio data and session ID</li>
     *   <li>Encode audio data as Base64</li>
     *   <li>Create JSON request with audio and session_id</li>
     *   <li>POST to VAD service endpoint</li>
     *   <li>Parse JSON response for "status" field</li>
     * </ol>
     * 
     * @param audioData Raw audio data as byte array
     * @param sessionId Unique session identifier
     * @return "start", "end", or null based on voice activity
     * @throws Exception if detection fails
     */
    @Override
    public String detect(byte[] audioData, String sessionId) throws Exception {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Received null or empty audio data");
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("Received null or empty session ID");
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        logger.debug("Starting VAD detection for session: {} ({} bytes)", 
            sessionId, audioData.length);
        
        try {
            String requestJson = createDetectionRequest(audioData, sessionId);
            logger.trace("VAD request JSON length: {} characters", requestJson.length());
            
            String responseJson = sendDetectionRequest(requestJson);
            logger.debug("Received VAD response for session {}: {}", sessionId, responseJson);
            
            String status = parseDetectionResponse(responseJson);
            
            if (status != null) {
                logger.info("VAD detected '{}' for session: {}", status, sessionId);
            } else {
                logger.debug("VAD: no state change for session: {}", sessionId);
            }
            
            return status;
            
        } catch (Exception e) {
            logger.error("VAD detection failed for session: {}", sessionId, e);
            throw new Exception("Failed to detect voice activity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resets VAD state by calling the reset endpoint.
     * 
     * <p>Process flow:</p>
     * <ol>
     *   <li>Validate session ID</li>
     *   <li>Create JSON request with session_id</li>
     *   <li>POST to /reset endpoint</li>
     * </ol>
     * 
     * @param sessionId Unique session identifier to reset
     * @throws Exception if reset fails
     */
    @Override
    public void reset(String sessionId) throws Exception {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("Received null or empty session ID for reset");
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        logger.debug("Resetting VAD state for session: {}", sessionId);
        
        try {
            String requestJson = createResetRequest(sessionId);
            sendResetRequest(requestJson);
            
            logger.info("VAD state reset successfully for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("VAD reset failed for session: {}", sessionId, e);
            throw new Exception("Failed to reset VAD state: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates JSON request for voice activity detection.
     * 
     * @param audioData Raw audio data
     * @param sessionId Session identifier
     * @return JSON string for request body
     */
    private String createDetectionRequest(byte[] audioData, String sessionId) throws Exception {
        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("audio", base64Audio);
            requestNode.put("session_id", sessionId);
            
            return objectMapper.writeValueAsString(requestNode);
            
        } catch (Exception e) {
            logger.error("Failed to create VAD detection request JSON", e);
            throw new Exception("Failed to create request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates JSON request for VAD state reset.
     * 
     * @param sessionId Session identifier
     * @return JSON string for request body
     */
    private String createResetRequest(String sessionId) throws Exception {
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("session_id", sessionId);
            
            return objectMapper.writeValueAsString(requestNode);
            
        } catch (Exception e) {
            logger.error("Failed to create VAD reset request JSON", e);
            throw new Exception("Failed to create reset request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sends detection request to VAD service.
     * 
     * @param requestJson JSON request body
     * @return JSON response from service
     * @throws Exception if request fails
     */
    private String sendDetectionRequest(String requestJson) throws Exception {
        try {
            Mono<String> responseMono = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout));
            
            String response = responseMono.block();
            
            if (response == null || response.trim().isEmpty()) {
                throw new Exception("Received empty response from VAD service");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to send detection request to VAD service: {}", serviceUrl, e);
            throw new Exception("VAD service communication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sends reset request to VAD service.
     * 
     * @param requestJson JSON request body
     * @throws Exception if request fails
     */
    private void sendResetRequest(String requestJson) throws Exception {
        try {
            Mono<String> responseMono = webClient.post()
                .uri(RESET_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout));
            
            responseMono.block();
            
            logger.debug("Reset request sent successfully to: {}{}", serviceUrl, RESET_ENDPOINT);
            
        } catch (Exception e) {
            logger.error("Failed to send reset request to VAD service: {}{}", 
                serviceUrl, RESET_ENDPOINT, e);
            throw new Exception("VAD reset communication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses VAD service JSON response to extract status.
     * 
     * <p>Expected JSON format:</p>
     * <pre>
     * {
     *   "status": "start" | "end" | null
     * }
     * </pre>
     * 
     * @param jsonResponse JSON response string
     * @return Status value ("start", "end", or null)
     * @throws Exception if JSON parsing fails
     */
    private String parseDetectionResponse(String jsonResponse) throws Exception {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            if (rootNode.has("status")) {
                JsonNode statusNode = rootNode.get("status");
                
                if (statusNode.isNull()) {
                    return null;
                }
                
                String status = statusNode.asText();
                
                if ("start".equals(status) || "end".equals(status)) {
                    return status;
                } else if (status.isEmpty()) {
                    return null;
                } else {
                    logger.warn("Unexpected VAD status value: {}", status);
                    return null;
                }
            } else {
                logger.warn("Response JSON does not contain 'status' field: {}", jsonResponse);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse VAD response JSON", e);
            throw new Exception("Invalid VAD response format: " + e.getMessage(), e);
        }
    }
}
