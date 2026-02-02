package com.bailing.infrastructure.adapter;

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
 * Ollama LLM implementation supporting streaming chat completions.
 * Compatible with Ollama's local LLM API.
 */
public class OllamaLLM implements LLM {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaLLM.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_URL = "http://localhost:11434";
    
    private final String url;
    private final String modelName;
    private final WebClient webClient;
    
    /**
     * Constructs an Ollama LLM client.
     * 
     * @param config Configuration map containing:
     *               - url: API endpoint URL (optional, defaults to http://localhost:11434)
     *               - modelName: Model identifier (required)
     */
    public OllamaLLM(Map<String, Object> config) {
        this.url = config.containsKey("url") ? (String) config.get("url") : DEFAULT_URL;
        this.modelName = (String) config.get("modelName");
        
        if (modelName == null) {
            throw new IllegalArgumentException("Ollama LLM requires modelName in config");
        }
        
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        logger.info("Ollama LLM initialized with URL: {} and model: {}", url, modelName);
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
                    .uri("/api/chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class);
            
            responseFlux
                    .doOnNext(line -> {
                        try {
                            processNDJSONLine(line, onChunk);
                        } catch (Exception e) {
                            logger.error("Error processing NDJSON line: {}", line, e);
                            throw new RuntimeException("Failed to process NDJSON response", e);
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
            throw new Exception("Ollama chat request failed", e);
        }
    }
    
    /**
     * Processes a single NDJSON line from the stream.
     * 
     * @param line NDJSON formatted line (one complete JSON object)
     * @param onChunk Callback to receive extracted content
     */
    private void processNDJSONLine(String line, Consumer<String> onChunk) throws Exception {
        line = line.trim();
        
        if (line.isEmpty()) {
            return;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            
            JsonNode doneNode = jsonNode.get("done");
            if (doneNode != null && doneNode.asBoolean()) {
                logger.debug("Received done signal");
                return;
            }
            
            JsonNode message = jsonNode.get("message");
            if (message != null && message.has("content")) {
                String content = message.get("content").asText();
                if (content != null && !content.isEmpty()) {
                    logger.trace("Extracted content chunk: {}", content);
                    onChunk.accept(content);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse JSON from NDJSON line: {}", line, e);
            throw e;
        }
    }
}
