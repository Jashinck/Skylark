package org.skylark.herms.domain.model.execution;

import lombok.Builder;
import lombok.Getter;

/**
 * StepResult — the outcome of executing a single {@link org.skylark.herms.domain.model.step.WorkflowStep}.
 */
@Getter
@Builder
public class StepResult {

    private final String stepId;
    private final String stepName;
    private final boolean success;
    private final String output;
    private final String error;
    private final long durationMs;

    public static StepResult success(String stepId, String stepName, String output, long durationMs) {
        return StepResult.builder()
                .stepId(stepId)
                .stepName(stepName)
                .success(true)
                .output(output)
                .durationMs(durationMs)
                .build();
    }

    public static StepResult failure(String stepId, String stepName, String error, long durationMs) {
        return StepResult.builder()
                .stepId(stepId)
                .stepName(stepName)
                .success(false)
                .error(error)
                .durationMs(durationMs)
                .build();
    }

    /** Returns the effective content: output on success, error description on failure. */
    public String getEffectiveContent() {
        return success ? output : ("ERROR: " + error);
    }
}
