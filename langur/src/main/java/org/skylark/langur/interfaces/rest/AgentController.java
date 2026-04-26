package org.skylark.langur.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.skylark.langur.application.command.CreateAgentCommand;
import org.skylark.langur.application.command.RunAgentCommand;
import org.skylark.langur.application.service.AgentApplicationService;
import org.skylark.langur.interfaces.dto.AgentResponse;
import org.skylark.langur.interfaces.dto.CreateAgentRequest;
import org.skylark.langur.interfaces.dto.RunAgentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentApplicationService agentApplicationService;

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        CreateAgentCommand command = CreateAgentCommand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .systemPrompt(request.getSystemPrompt())
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxIterations(request.getMaxIterations())
                .toolNames(request.getToolNames())
                .build();
        return ResponseEntity.ok(agentApplicationService.createAgent(command));
    }

    @PostMapping("/{agentId}/run")
    public ResponseEntity<AgentResponse> runAgent(
            @PathVariable String agentId,
            @RequestBody RunAgentRequest request) {
        RunAgentCommand command = RunAgentCommand.builder()
                .agentId(agentId)
                .userMessage(request.getUserMessage())
                .build();
        return ResponseEntity.ok(agentApplicationService.runAgent(command));
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentResponse> getAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(agentApplicationService.getAgent(agentId));
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> listAgents() {
        return ResponseEntity.ok(agentApplicationService.listAgents());
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String agentId) {
        agentApplicationService.deleteAgent(agentId);
        return ResponseEntity.noContent().build();
    }
}
