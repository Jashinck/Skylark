package org.skylark.herms.domain.model.workflow;

import lombok.Builder;
import lombok.Getter;

/**
 * WorkflowConfig — immutable configuration for a Workflow definition.
 */
@Getter
@Builder
public class WorkflowConfig {

    private final String name;
    private final String description;
    /** Maximum number of steps that may execute in a single run (guards against loops). */
    @Builder.Default
    private final int maxSteps = 50;

    public static WorkflowConfig defaultConfig(String name) {
        return WorkflowConfig.builder()
                .name(name)
                .description("")
                .maxSteps(50)
                .build();
    }
}
