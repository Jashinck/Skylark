package org.skylark.herms.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * REST response DTO for a Workflow definition.
 */
@Getter
@Builder
public class WorkflowResponse {

    private final String id;
    private final String name;
    private final String description;
    private final String status;
    private final String entryStepId;
    private final int maxSteps;
    private final List<StepSummary> steps;
    private final String createdAt;
    private final String updatedAt;

    @Getter
    @Builder
    public static class StepSummary {
        private final String id;
        private final String name;
        private final String type;
        private final String nextStepId;
        private final boolean terminal;
    }
}
