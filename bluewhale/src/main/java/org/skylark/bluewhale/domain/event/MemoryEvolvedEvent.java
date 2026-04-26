package org.skylark.bluewhale.domain.event;

import lombok.Getter;

@Getter
public class MemoryEvolvedEvent extends DomainEvent {
    private final String agentId;
    private final String evolutionType;
    private final int affectedCount;

    public MemoryEvolvedEvent(String agentId, String evolutionType, int affectedCount) {
        super();
        this.agentId = agentId;
        this.evolutionType = evolutionType;
        this.affectedCount = affectedCount;
    }

    @Override
    public String getEventType() { return "MEMORY_EVOLVED"; }
}
