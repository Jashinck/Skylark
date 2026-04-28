package org.skylark.herms.domain.model.workflow;

import java.util.Objects;
import java.util.UUID;

/**
 * WorkflowId — value object identifying a Workflow aggregate.
 */
public final class WorkflowId {

    private final String value;

    private WorkflowId(String value) {
        Objects.requireNonNull(value, "WorkflowId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId value must not be blank");
        }
        this.value = value;
    }

    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID().toString());
    }

    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
