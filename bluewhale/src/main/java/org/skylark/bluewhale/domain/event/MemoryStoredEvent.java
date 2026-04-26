package org.skylark.bluewhale.domain.event;

import lombok.Getter;

@Getter
public class MemoryStoredEvent extends DomainEvent {
    private final String memoryId;
    private final String memoryType;
    private final String agentId;

    public MemoryStoredEvent(String memoryId, String memoryType, String agentId) {
        super();
        this.memoryId = memoryId;
        this.memoryType = memoryType;
        this.agentId = agentId;
    }

    @Override
    public String getEventType() { return "MEMORY_STORED"; }
}
