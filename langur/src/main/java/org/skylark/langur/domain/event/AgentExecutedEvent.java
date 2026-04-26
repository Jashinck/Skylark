package org.skylark.langur.domain.event;

import lombok.Getter;

@Getter
public class AgentExecutedEvent extends DomainEvent {
    private final String agentId;
    private final int iterations;
    private final String finalAnswer;

    public AgentExecutedEvent(String agentId, int iterations, String finalAnswer) {
        super();
        this.agentId = agentId;
        this.iterations = iterations;
        this.finalAnswer = finalAnswer;
    }

    @Override
    public String getEventType() {
        return "AGENT_EXECUTED";
    }
}
