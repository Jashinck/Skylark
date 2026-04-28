package org.skylark.herms.infrastructure.executor;

import lombok.extern.slf4j.Slf4j;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.StepType;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CompositeStepExecutor — default {@link StepExecutorPort} implementation.
 *
 * <p>Dispatches each step to the appropriate execution strategy based on
 * {@link StepType}. Uses simple {@code {{key}}} template interpolation to
 * substitute context values into prompts, URLs, and body templates.</p>
 */
@Slf4j
@Component
public class CompositeStepExecutor implements StepExecutorPort {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final HttpStepExecutor httpStepExecutor;
    private final LlmStepExecutor llmStepExecutor;

    public CompositeStepExecutor(HttpStepExecutor httpStepExecutor,
                                  LlmStepExecutor llmStepExecutor) {
        this.httpStepExecutor = httpStepExecutor;
        this.llmStepExecutor = llmStepExecutor;
    }

    @Override
    public String execute(WorkflowStep step, Map<String, Object> context) throws Exception {
        log.debug("Dispatching step id={} type={}", step.getId(), step.getType());
        return switch (step.getType()) {
            case LLM       -> llmStepExecutor.execute(step, context);
            case HTTP      -> httpStepExecutor.execute(step, context);
            case NOTIFY    -> httpStepExecutor.execute(step, context);
            case TRANSFORM -> executeTransform(step.getConfig(), context);
            case CONDITION -> evaluateCondition(step.getConfig(), context);
        };
    }

    // ── TRANSFORM ────────────────────────────────────────────────────────────

    private String executeTransform(StepConfig config, Map<String, Object> context) {
        String template = config.getTemplate();
        if (template == null || template.isBlank()) {
            return "";
        }
        return interpolate(template, context);
    }

    // ── CONDITION ────────────────────────────────────────────────────────────

    private String evaluateCondition(StepConfig config, Map<String, Object> context) {
        String expression = config.getConditionExpression();
        if (expression == null || expression.isBlank()) {
            return "false";
        }
        // Simple equality check: "{{key}} == value"
        String interpolated = interpolate(expression, context);
        // Treat non-empty, non-"false", non-"0" as truthy
        return isTruthy(interpolated) ? "true" : "false";
    }

    private boolean isTruthy(String value) {
        if (value == null || value.isBlank()) return false;
        return !value.equalsIgnoreCase("false") && !value.equals("0");
    }

    // ── Template interpolation ────────────────────────────────────────────────

    /**
     * Replaces {@code {{key}}} placeholders with corresponding values from {@code context}.
     */
    static String interpolate(String template, Map<String, Object> context) {
        if (template == null) return "";
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.get(key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(
                    value != null ? value.toString() : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
