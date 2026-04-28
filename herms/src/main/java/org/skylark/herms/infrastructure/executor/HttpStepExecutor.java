package org.skylark.herms.infrastructure.executor;

import lombok.extern.slf4j.Slf4j;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * HttpStepExecutor — executes {@code HTTP} and {@code NOTIFY} workflow steps.
 *
 * <p>Resolves the target URL and request body from the step's {@link StepConfig}
 * using {@code {{key}}} template interpolation, then issues a blocking HTTP request
 * via Spring WebFlux {@link WebClient}.</p>
 */
@Slf4j
@Component
public class HttpStepExecutor {

    private final WebClient webClient;
    private final long readTimeoutMs;

    public HttpStepExecutor(
            WebClient.Builder webClientBuilder,
            @Value("${herms.http.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${herms.http.read-timeout-ms:30000}") long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
        this.webClient = webClientBuilder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }

    public String execute(WorkflowStep step, Map<String, Object> context) {
        StepConfig config = step.getConfig();
        String url = CompositeStepExecutor.interpolate(config.getUrl(), context);
        String body = CompositeStepExecutor.interpolate(config.getBodyTemplate(), context);
        String method = config.getMethod() != null ? config.getMethod().toUpperCase() : "POST";

        log.debug("HTTP step [{}]: {} {}", step.getId(), method, url);

        WebClient.RequestBodySpec request = webClient
                .method(HttpMethod.valueOf(method))
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);

        // Add custom headers
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(request::header);
        }

        return request
                .bodyValue(body != null ? body : "")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(readTimeoutMs))
                .block();
    }
}
