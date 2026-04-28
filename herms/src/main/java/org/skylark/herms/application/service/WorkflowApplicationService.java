package org.skylark.herms.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.herms.application.assembler.WorkflowAssembler;
import org.skylark.herms.application.command.CreateWorkflowCommand;
import org.skylark.herms.application.command.TriggerWorkflowCommand;
import org.skylark.herms.domain.model.execution.WorkflowExecution;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.domain.model.workflow.WorkflowConfig;
import org.skylark.herms.domain.model.workflow.WorkflowId;
import org.skylark.herms.domain.repository.ExecutionRepository;
import org.skylark.herms.domain.repository.WorkflowRepository;
import org.skylark.herms.domain.service.ExecutionDomainService;
import org.skylark.herms.domain.service.WorkflowDomainService;
import org.skylark.herms.interfaces.dto.ExecutionResponse;
import org.skylark.herms.interfaces.dto.WorkflowResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WorkflowApplicationService — application-layer use cases for the Herms engine.
 *
 * <p>Orchestrates domain objects and repositories to implement the following use cases:</p>
 * <ul>
 *   <li>Create a workflow definition</li>
 *   <li>Activate / archive a workflow</li>
 *   <li>Trigger a workflow execution</li>
 *   <li>Query workflows and executions</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApplicationService {

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final WorkflowDomainService workflowDomainService;
    private final ExecutionDomainService executionDomainService;
    private final WorkflowAssembler assembler;

    // ── Workflow CRUD ─────────────────────────────────────────────────────────

    public WorkflowResponse createWorkflow(CreateWorkflowCommand command) {
        WorkflowConfig config = WorkflowConfig.builder()
                .name(command.getName())
                .description(command.getDescription() != null ? command.getDescription() : "")
                .maxSteps(command.getMaxSteps() > 0 ? command.getMaxSteps() : 50)
                .build();

        Workflow workflow = workflowDomainService.createWorkflow(config);

        // Add steps
        if (command.getSteps() != null) {
            for (CreateWorkflowCommand.StepDefinition sd : command.getSteps()) {
                WorkflowStep step = WorkflowStep.of(
                        sd.getId(), sd.getName(), sd.getType(),
                        sd.getConfig(), sd.getNextStepId());
                workflow.addStep(step);
            }
        }

        // Override entry step if specified
        if (command.getEntryStepId() != null) {
            workflow.setEntryStep(command.getEntryStepId());
        }

        workflowRepository.save(workflow);
        log.info("Created workflow: {} ({})", config.getName(), workflow.getId());
        return assembler.toResponse(workflow);
    }

    public WorkflowResponse activateWorkflow(String workflowId) {
        Workflow workflow = requireWorkflow(workflowId);
        workflowDomainService.activateWorkflow(workflow);
        workflowRepository.save(workflow);
        log.info("Activated workflow: {}", workflowId);
        return assembler.toResponse(workflow);
    }

    public WorkflowResponse archiveWorkflow(String workflowId) {
        Workflow workflow = requireWorkflow(workflowId);
        workflow.archive();
        workflowRepository.save(workflow);
        log.info("Archived workflow: {}", workflowId);
        return assembler.toResponse(workflow);
    }

    public WorkflowResponse getWorkflow(String workflowId) {
        return assembler.toResponse(requireWorkflow(workflowId));
    }

    public List<WorkflowResponse> listWorkflows() {
        return workflowRepository.findAll().stream()
                .map(assembler::toResponse)
                .toList();
    }

    public void deleteWorkflow(String workflowId) {
        workflowRepository.delete(WorkflowId.of(workflowId));
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    public ExecutionResponse triggerWorkflow(TriggerWorkflowCommand command) {
        Workflow workflow = requireWorkflow(command.getWorkflowId());
        if (!workflowDomainService.canTrigger(workflow)) {
            throw new IllegalStateException(
                    "Workflow is not ACTIVE or has no steps: " + command.getWorkflowId());
        }

        WorkflowExecution execution = WorkflowExecution.create(
                workflow.getId(), command.getInitialContext());
        executionRepository.save(execution);

        log.info("Triggering workflow {} → execution {}", workflow.getId(), execution.getId());
        executionDomainService.run(workflow, execution);
        executionRepository.save(execution);

        return assembler.toExecutionResponse(execution);
    }

    public ExecutionResponse getExecution(String executionId) {
        WorkflowExecution execution = executionRepository
                .findById(org.skylark.herms.domain.model.execution.ExecutionId.of(executionId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Execution not found: " + executionId));
        return assembler.toExecutionResponse(execution);
    }

    public List<ExecutionResponse> listExecutionsByWorkflow(String workflowId) {
        return executionRepository
                .findByWorkflowId(WorkflowId.of(workflowId))
                .stream()
                .map(assembler::toExecutionResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Workflow requireWorkflow(String workflowId) {
        return workflowRepository.findById(WorkflowId.of(workflowId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + workflowId));
    }
}
