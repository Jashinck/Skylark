package org.skylark.herms.application.command;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Command to trigger execution of an existing ACTIVE Workflow.
 */
@Getter
@Builder
public class TriggerWorkflowCommand {

    private final String workflowId;
    /** Initial context values injected into the execution. */
    @Builder.Default
    private final Map<String, Object> initialContext = Map.of();
}
