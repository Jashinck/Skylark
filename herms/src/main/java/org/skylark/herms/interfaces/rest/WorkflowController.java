package org.skylark.herms.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.skylark.herms.application.command.CreateWorkflowCommand;
import org.skylark.herms.application.command.TriggerWorkflowCommand;
import org.skylark.herms.application.service.WorkflowApplicationService;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.skylark.herms.interfaces.dto.CreateWorkflowRequest;
import org.skylark.herms.interfaces.dto.ExecutionResponse;
import org.skylark.herms.interfaces.dto.TriggerWorkflowRequest;
import org.skylark.herms.interfaces.dto.WorkflowResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @RequestBody CreateWorkflowRequest request) {
        List<CreateWorkflowCommand.StepDefinition> stepDefs = null;
        if (request.getSteps() != null) {
            stepDefs = request.getSteps().stream()
                    .map(s -> CreateWorkflowCommand.StepDefinition.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .type(s.getType())
                            .config(s.getConfig())
                            .nextStepId(s.getNextStepId())
                            .build())
                    .collect(Collectors.toList());
        }
        CreateWorkflowCommand command = CreateWorkflowCommand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxSteps(request.getMaxSteps())
                .steps(stepDefs)
                .entryStepId(request.getEntryStepId())
                .build();
        return ResponseEntity.ok(workflowApplicationService.createWorkflow(command));
    }

    @PostMapping("/{workflowId}/activate")
    public ResponseEntity<WorkflowResponse> activateWorkflow(
            @PathVariable String workflowId) {
        return ResponseEntity.ok(workflowApplicationService.activateWorkflow(workflowId));
    }

    @PostMapping("/{workflowId}/archive")
    public ResponseEntity<WorkflowResponse> archiveWorkflow(
            @PathVariable String workflowId) {
        return ResponseEntity.ok(workflowApplicationService.archiveWorkflow(workflowId));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowApplicationService.getWorkflow(workflowId));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> listWorkflows() {
        return ResponseEntity.ok(workflowApplicationService.listWorkflows());
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String workflowId) {
        workflowApplicationService.deleteWorkflow(workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/trigger")
    public ResponseEntity<ExecutionResponse> triggerWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) TriggerWorkflowRequest request) {
        Map<String, Object> ctx = (request != null && request.getInitialContext() != null)
                ? request.getInitialContext() : Map.of();
        TriggerWorkflowCommand command = TriggerWorkflowCommand.builder()
                .workflowId(workflowId)
                .initialContext(ctx)
                .build();
        return ResponseEntity.ok(workflowApplicationService.triggerWorkflow(command));
    }

    @GetMapping("/{workflowId}/executions")
    public ResponseEntity<List<ExecutionResponse>> listExecutions(
            @PathVariable String workflowId) {
        return ResponseEntity.ok(
                workflowApplicationService.listExecutionsByWorkflow(workflowId));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable String executionId) {
        return ResponseEntity.ok(workflowApplicationService.getExecution(executionId));
    }
}
