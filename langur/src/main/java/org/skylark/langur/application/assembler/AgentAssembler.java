package org.skylark.langur.application.assembler;

import org.skylark.langur.domain.model.agent.Agent;
import org.skylark.langur.interfaces.dto.AgentResponse;
import org.springframework.stereotype.Component;

@Component
public class AgentAssembler {

    public AgentResponse toResponse(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId().getValue())
                .name(agent.getConfig().getName())
                .description(agent.getConfig().getDescription())
                .status(agent.getStatus().name())
                .model(agent.getConfig().getModel())
                .iterationCount(agent.getIterationCount())
                .toolCount(agent.getTools().size())
                .lastError(agent.getLastError())
                .createdAt(agent.getCreatedAt().toString())
                .updatedAt(agent.getUpdatedAt().toString())
                .build();
    }
}
