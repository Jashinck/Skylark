package org.skylark.langur.domain.event;

import lombok.Getter;

@Getter
public class AgentCreatedEvent extends DomainEvent {
    private final String agentId;
    private final String agentName;

    public AgentCreatedEvent(String agentId, String agentName) {
        super();
        this.agentId = agentId;
        this.agentName = agentName;
    }

    @Override
    public String getEventType() {
        return "AGENT_CREATED";
    }
}
