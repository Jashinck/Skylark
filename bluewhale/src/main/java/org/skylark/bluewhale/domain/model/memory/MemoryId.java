package org.skylark.bluewhale.domain.model.memory;

import java.util.Objects;
import java.util.UUID;

public class MemoryId {
    private final String value;

    private MemoryId(String value) {
        this.value = Objects.requireNonNull(value, "MemoryId must not be null");
    }

    public static MemoryId of(String value) {
        return new MemoryId(value);
    }

    public static MemoryId generate() {
        return new MemoryId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryId)) return false;
        return value.equals(((MemoryId) o).value);
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
