package org.skylark.herms.domain.model.step;

import lombok.Getter;

import java.util.Objects;

/**
 * WorkflowStep — an immutable node in the workflow definition graph.
 *
 * <p>Each step has a unique {@code id} within the owning Workflow, a {@link StepType},
 * a {@link StepConfig} holding type-specific parameters, and an optional {@code nextStepId}
 * pointing to the default successor. CONDITION steps use {@code config.trueStepId} /
 * {@code config.falseStepId} for branching instead.</p>
 */
@Getter
public class WorkflowStep {

    private final String id;
    private final String name;
    private final StepType type;
    private final StepConfig config;
    /** Id of the next step to execute after this one (null = end of workflow). */
    private final String nextStepId;

    private WorkflowStep(String id, String name, StepType type,
                         StepConfig config, String nextStepId) {
        this.id = Objects.requireNonNull(id, "step id must not be null");
        this.name = Objects.requireNonNull(name, "step name must not be null");
        this.type = Objects.requireNonNull(type, "step type must not be null");
        this.config = Objects.requireNonNull(config, "step config must not be null");
        this.nextStepId = nextStepId;
    }

    public static WorkflowStep of(String id, String name, StepType type,
                                   StepConfig config, String nextStepId) {
        return new WorkflowStep(id, name, type, config, nextStepId);
    }

    /** Returns true when this step ends the workflow (no successor). */
    public boolean isTerminal() {
        return nextStepId == null && type != StepType.CONDITION;
    }
}
