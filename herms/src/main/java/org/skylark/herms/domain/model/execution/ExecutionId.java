package org.skylark.herms.domain.model.execution;

import java.util.Objects;
import java.util.UUID;

/**
 * ExecutionId — value object identifying a single WorkflowExecution.
 */
public final class ExecutionId {

    private final String value;

    private ExecutionId(String value) {
        Objects.requireNonNull(value, "ExecutionId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ExecutionId value must not be blank");
        }
        this.value = value;
    }

    public static ExecutionId generate() {
        return new ExecutionId(UUID.randomUUID().toString());
    }

    public static ExecutionId of(String value) {
        return new ExecutionId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionId that)) return false;
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
