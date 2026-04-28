package org.skylark.herms.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a new Workflow definition is created.
 */
@Getter
public class WorkflowCreatedEvent implements DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String workflowId;
    private final String workflowName;

    public WorkflowCreatedEvent(String workflowId, String workflowName) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.workflowId = workflowId;
        this.workflowName = workflowName;
    }
}
