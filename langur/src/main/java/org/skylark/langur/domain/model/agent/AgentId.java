package org.skylark.langur.domain.model.agent;

import java.util.Objects;
import java.util.UUID;

public class AgentId {
    private final String value;

    private AgentId(String value) {
        this.value = Objects.requireNonNull(value, "AgentId value must not be null");
    }

    public static AgentId of(String value) {
        return new AgentId(value);
    }

    public static AgentId generate() {
        return new AgentId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentId)) return false;
        return value.equals(((AgentId) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
