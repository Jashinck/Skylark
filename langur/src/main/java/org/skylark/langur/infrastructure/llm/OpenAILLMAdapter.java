package org.skylark.langur.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.skylark.langur.domain.model.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI兼容LLM适配器 - 支持OpenAI/DeepSeek/Qwen等OpenAI格式接口
 */
@Slf4j
@Component
public class OpenAILLMAdapter implements LLMPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${langur.llm.api-key:}")
    private String apiKey;

    @Value("${langur.llm.model:gpt-4o}")
    private String defaultModel;

    public OpenAILLMAdapter(
            @Value("${langur.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public LLMDecision decide(String systemPrompt,
                               List<Map<String, String>> conversationHistory,
                               List<Tool> availableTools) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", defaultModel);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);

            for (Map<String, String> msg : conversationHistory) {
                ObjectNode msgNode = messages.addObject();
                msg.forEach(msgNode::put);
            }

            if (!availableTools.isEmpty()) {
                ArrayNode tools = requestBody.putArray("tools");
                for (Tool tool : availableTools) {
                    ObjectNode toolNode = tools.addObject();
                    toolNode.put("type", "function");
                    ObjectNode fn = toolNode.putObject("function");
                    fn.put("name", tool.getName());
                    fn.put("description", tool.getDescription());
                    fn.set("parameters", objectMapper.valueToTree(tool.getDefinition().getParametersSchema()));
                }
            }

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(response);
        } catch (Exception e) {
            log.error("LLM call failed", e);
            return LLMDecision.finalAnswer("Error communicating with LLM: " + e.getMessage());
        }
    }

    @Override
    public String complete(String systemPrompt, String userMessage) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", defaultModel);
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userMessage);

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            log.error("LLM completion failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private LLMDecision parseResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode message = root.at("/choices/0/message");

        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty()) {
            JsonNode call = toolCalls.get(0);
            String toolName = call.at("/function/name").asText();
            String argsJson = call.at("/function/arguments").asText();
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            String thought = message.has("content") && !message.get("content").isNull()
                    ? message.get("content").asText() : "Calling tool: " + toolName;
            return LLMDecision.toolCall(thought, toolName, args);
        }

        String content = message.at("/content").asText();
        return LLMDecision.finalAnswer(content);
    }
}
