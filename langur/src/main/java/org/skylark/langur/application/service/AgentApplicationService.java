package org.skylark.langur.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.langur.application.assembler.AgentAssembler;
import org.skylark.langur.application.command.CreateAgentCommand;
import org.skylark.langur.application.command.RunAgentCommand;
import org.skylark.langur.domain.model.agent.Agent;
import org.skylark.langur.domain.model.agent.AgentConfig;
import org.skylark.langur.domain.model.agent.AgentId;
import org.skylark.langur.domain.model.plan.Plan;
import org.skylark.langur.domain.repository.AgentRepository;
import org.skylark.langur.domain.service.AgentDomainService;
import org.skylark.langur.domain.service.PlanningDomainService;
import org.skylark.langur.interfaces.dto.AgentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent应用服务 - 编排领域对象，处理用例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    private final AgentRepository agentRepository;
    private final AgentDomainService agentDomainService;
    private final PlanningDomainService planningDomainService;
    private final ToolRegistryService toolRegistryService;
    private final AgentAssembler agentAssembler;

    public AgentResponse createAgent(CreateAgentCommand command) {
        AgentConfig config = AgentConfig.builder()
                .name(command.getName())
                .description(command.getDescription())
                .systemPrompt(command.getSystemPrompt() != null
                        ? command.getSystemPrompt()
                        : "You are a helpful assistant.")
                .model(command.getModel() != null ? command.getModel() : "gpt-4o")
                .temperature(command.getTemperature() > 0 ? command.getTemperature() : 0.7)
                .maxIterations(command.getMaxIterations() > 0 ? command.getMaxIterations() : 10)
                .maxTokens(4096)
                .build();

        Agent agent = Agent.create(config);

        List<org.skylark.langur.domain.model.tool.Tool> tools =
                toolRegistryService.getToolsByNames(command.getToolNames());
        tools.forEach(agent::registerTool);

        agentRepository.save(agent);
        log.info("Created agent: {} ({})", agent.getConfig().getName(), agent.getId());
        return agentAssembler.toResponse(agent);
    }

    public AgentResponse runAgent(RunAgentCommand command) {
        Agent agent = agentRepository.findById(AgentId.of(command.getAgentId()))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + command.getAgentId()));

        agent.markRunning();
        agent.addUserMessage(command.getUserMessage());
        Plan plan = planningDomainService.createPlan(agent.getId().getValue());

        try {
            String finalAnswer = null;
            while (finalAnswer == null && !agent.hasExceededMaxIterations()) {
                finalAnswer = agentDomainService.executeReActStep(agent, plan);
            }
            if (finalAnswer == null) {
                agent.markFailed("Max iterations exceeded");
            }
        } catch (Exception e) {
            log.error("Agent execution failed", e);
            agent.markFailed(e.getMessage());
        }

        agentRepository.save(agent);
        return agentAssembler.toResponse(agent);
    }

    public AgentResponse getAgent(String agentId) {
        Agent agent = agentRepository.findById(AgentId.of(agentId))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        return agentAssembler.toResponse(agent);
    }

    public List<AgentResponse> listAgents() {
        return agentRepository.findAll().stream()
                .map(agentAssembler::toResponse)
                .toList();
    }

    public void deleteAgent(String agentId) {
        agentRepository.delete(AgentId.of(agentId));
    }
}
