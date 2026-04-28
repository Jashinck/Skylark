package org.skylark.herms.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * REST response DTO for a WorkflowExecution run.
 */
@Getter
@Builder
public class ExecutionResponse {

    private final String id;
    private final String workflowId;
    private final String status;
    private final String finalOutput;
    private final String errorMessage;
    private final int stepsExecuted;
    private final List<StepResultSummary> stepResults;
    private final String createdAt;
    private final String updatedAt;

    @Getter
    @Builder
    public static class StepResultSummary {
        private final String stepId;
        private final String stepName;
        private final boolean success;
        private final String output;
        private final String error;
        private final long durationMs;
    }
}
