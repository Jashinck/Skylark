package org.skylark.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI-compatible LLM implementation supporting streaming chat completions.
 * Compatible with OpenAI API and other providers following the same API format.
 */
public class OpenAILLM implements LLM {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAILLM.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String url;
    private final String apiKey;
    private final String modelName;
    private final WebClient webClient;
    
    /**
     * Constructs an OpenAI LLM client.
     * 
     * @param config Configuration map containing:
     *               - url: API endpoint URL (required)
     *               - apiKey: API authentication key (required)
     *               - modelName: Model identifier (required)
     */
    public OpenAILLM(Map<String, Object> config) {
        this.url = (String) config.get("url");
        this.apiKey = (String) config.get("apiKey");
        this.modelName = (String) config.get("modelName");
        
        if (url == null || apiKey == null || modelName == null) {
            throw new IllegalArgumentException("OpenAI LLM requires url, apiKey, and modelName in config");
        }
        
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        logger.info("OpenAI LLM initialized with URL: {} and model: {}", url, modelName);
    }
    
    @Override
    public void chat(List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onComplete) throws Exception {
        logger.debug("Starting chat with {} messages", messages.size());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        
        try {
            Flux<String> responseFlux = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class);
            
            responseFlux
                    .doOnNext(line -> {
                        try {
                            processSSELine(line, onChunk);
                        } catch (Exception e) {
                            logger.error("Error processing SSE line: {}", line, e);
                            throw new RuntimeException("Failed to process SSE response", e);
                        }
                    })
                    .doOnComplete(() -> {
                        logger.debug("Stream completed successfully");
                        onComplete.run();
                    })
                    .doOnError(error -> {
                        logger.error("Error during streaming", error);
                    })
                    .blockLast();
                    
        } catch (Exception e) {
            logger.error("Failed to execute streaming chat", e);
            throw new Exception("OpenAI chat request failed", e);
        }
    }
    
    /**
     * Processes a single SSE line from the stream.
     * 
     * @param line SSE formatted line (e.g., "data: {...}")
     * @param onChunk Callback to receive extracted content
     */
    private void processSSELine(String line, Consumer<String> onChunk) throws Exception {
        line = line.trim();
        
        if (line.isEmpty()) {
            return;
        }
        
        if (!line.startsWith("data: ")) {
            logger.trace("Skipping non-data SSE line: {}", line);
            return;
        }
        
        String data = line.substring(6).trim();
        
        if ("[DONE]".equals(data)) {
            logger.debug("Received [DONE] signal");
            return;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            JsonNode choices = jsonNode.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                
                if (delta != null && delta.has("content")) {
                    String content = delta.get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        logger.trace("Extracted content chunk: {}", content);
                        onChunk.accept(content);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse JSON from SSE data: {}", data, e);
            throw e;
        }
    }
}
