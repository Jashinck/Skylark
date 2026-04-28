package org.skylark.herms.interfaces.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.StepType;

import java.util.List;

/**
 * REST request body for POST /api/workflows.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateWorkflowRequest {

    private String name;
    private String description;
    private int maxSteps;
    private List<StepRequest> steps;
    private String entryStepId;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StepRequest {
        private String id;
        private String name;
        private StepType type;
        private StepConfig config;
        private String nextStepId;
    }
}
