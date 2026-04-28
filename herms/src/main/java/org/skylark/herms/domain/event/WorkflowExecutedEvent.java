package org.skylark.herms.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a WorkflowExecution completes (successfully or with failure).
 */
@Getter
public class WorkflowExecutedEvent implements DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String executionId;
    private final String workflowId;
    private final int stepsExecuted;
    private final String finalOutput;

    public WorkflowExecutedEvent(String executionId, String workflowId,
                                  int stepsExecuted, String finalOutput) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.stepsExecuted = stepsExecuted;
        this.finalOutput = finalOutput;
    }
}
