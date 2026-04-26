package org.skylark.langur.infrastructure.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.skylark.langur.domain.model.tool.Tool;
import org.skylark.langur.domain.model.tool.ToolDefinition;
import org.skylark.langur.domain.model.tool.ToolResult;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * HTTP调用工具 - 允许Agent发起HTTP请求
 */
@Slf4j
public class HttpCallTool extends Tool {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "http_call",
            "Make an HTTP GET or POST request to a given URL",
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "url", Map.of("type", "string", "description", "The URL to call"),
                            "method", Map.of("type", "string", "enum", new String[]{"GET", "POST"},
                                    "description", "HTTP method"),
                            "body", Map.of("type", "string", "description", "Request body for POST")
                    ),
                    "required", new String[]{"url", "method"}
            )
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HttpCallTool(WebClient webClient, ObjectMapper objectMapper) {
        super(DEFINITION);
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            String url = (String) parameters.get("url");
            String method = (String) parameters.getOrDefault("method", "GET");
            String body = (String) parameters.getOrDefault("body", null);

            String response;
            if ("POST".equalsIgnoreCase(method)) {
                response = webClient.post()
                        .uri(url)
                        .bodyValue(body != null ? body : "")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } else {
                response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }
            return ToolResult.success(response);
        } catch (Exception e) {
            log.error("HTTP call tool failed", e);
            return ToolResult.failure(e.getMessage());
        }
    }
}
