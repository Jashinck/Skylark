package org.skylark.langur.domain.model.plan;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PlanStep {
    private final int index;
    private final String thought;
    private final String action;
    private final Map<String, Object> actionInput;
    private String observation;
    private StepStatus status;

    public PlanStep withObservation(String observation) {
        return PlanStep.builder()
                .index(this.index)
                .thought(this.thought)
                .action(this.action)
                .actionInput(this.actionInput)
                .observation(observation)
                .status(StepStatus.COMPLETED)
                .build();
    }

    public PlanStep withFailure(String errorMessage) {
        return PlanStep.builder()
                .index(this.index)
                .thought(this.thought)
                .action(this.action)
                .actionInput(this.actionInput)
                .observation(errorMessage)
                .status(StepStatus.FAILED)
                .build();
    }
}
