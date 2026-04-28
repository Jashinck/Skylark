package org.skylark.herms.application.command;

import lombok.Builder;
import lombok.Getter;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.StepType;

import java.util.List;

/**
 * Command to create a new Workflow definition.
 */
@Getter
@Builder
public class CreateWorkflowCommand {

    private final String name;
    private final String description;
    @Builder.Default
    private final int maxSteps = 50;
    private final List<StepDefinition> steps;
    private final String entryStepId;

    /**
     * Inline step definition used within the create command.
     */
    @Getter
    @Builder
    public static class StepDefinition {
        private final String id;
        private final String name;
        private final StepType type;
        private final StepConfig config;
        private final String nextStepId;
    }
}
