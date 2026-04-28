package org.skylark.herms.infrastructure.executor;

import lombok.extern.slf4j.Slf4j;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LlmStepExecutor — executes {@code LLM} workflow steps.
 *
 * <p>Calls an OpenAI-compatible chat completion API (configurable via
 * {@code herms.llm.base-url} and {@code herms.llm.api-key}) with the step's
 * system prompt and user prompt template resolved against the execution context.</p>
 */
@Slf4j
@Component
public class LlmStepExecutor {

    private final WebClient webClient;
    private final String model;

    public LlmStepExecutor(
            WebClient.Builder webClientBuilder,
            @Value("${herms.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${herms.llm.api-key:}") String apiKey,
            @Value("${herms.llm.model:gpt-4o}") String model) {
        this.model = model;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }

    public String execute(WorkflowStep step, Map<String, Object> context) {
        StepConfig config = step.getConfig();
        String systemPrompt = config.getSystemPrompt() != null ? config.getSystemPrompt()
                : "You are a helpful assistant.";
        String userPrompt = CompositeStepExecutor.interpolate(
                config.getUserPromptTemplate(), context);
        String effectiveModel = config.getModel() != null ? config.getModel() : model;

        log.debug("LLM step [{}]: model={}, userPrompt length={}", step.getId(),
                effectiveModel, userPrompt != null ? userPrompt.length() : 0);

        Map<String, Object> requestBody = Map.of(
                "model", effectiveModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt != null ? userPrompt : "")
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .block();

        return extractContent(response);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) return "";
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return "";
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) return "";
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) return "";
        Object content = msg.get("content");
        return content != null ? content.toString() : "";
    }
}
