package org.skylark.bluewhale.domain.event;

import lombok.Getter;

@Getter
public class MemoryRetrievedEvent extends DomainEvent {
    private final String memoryId;
    private final String agentId;
    private final double similarityScore;

    public MemoryRetrievedEvent(String memoryId, String agentId, double similarityScore) {
        super();
        this.memoryId = memoryId;
        this.agentId = agentId;
        this.similarityScore = similarityScore;
    }

    @Override
    public String getEventType() { return "MEMORY_RETRIEVED"; }
}
