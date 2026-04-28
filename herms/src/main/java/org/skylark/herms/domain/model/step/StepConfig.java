package org.skylark.herms.domain.model.step;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * StepConfig — type-specific configuration carried by a {@link WorkflowStep}.
 *
 * <p>Only the fields relevant to the step's {@link StepType} need to be populated:</p>
 * <ul>
 *   <li>{@code LLM} — {@code systemPrompt}, {@code userPromptTemplate}, {@code model}</li>
 *   <li>{@code HTTP} — {@code url}, {@code method}, {@code headers}, {@code bodyTemplate}</li>
 *   <li>{@code TRANSFORM} — {@code template} (a Mustache-style {@code {{key}}} expression)</li>
 *   <li>{@code CONDITION} — {@code conditionExpression}, {@code trueStepId}, {@code falseStepId}</li>
 *   <li>{@code NOTIFY} — {@code url}, {@code bodyTemplate}</li>
 * </ul>
 */
@Getter
@Builder
public class StepConfig {

    // LLM step fields
    private final String systemPrompt;
    private final String userPromptTemplate;
    private final String model;

    // HTTP / NOTIFY step fields
    private final String url;
    @Builder.Default
    private final String method = "POST";
    @Builder.Default
    private final Map<String, String> headers = Map.of();
    private final String bodyTemplate;

    // TRANSFORM step field
    private final String template;

    // CONDITION step fields
    private final String conditionExpression;
    private final String trueStepId;
    private final String falseStepId;

    // Context key to store this step's output under (used by downstream steps)
    private final String outputKey;
}
