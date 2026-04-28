package org.skylark.herms.application.assembler;

import org.skylark.herms.domain.model.execution.WorkflowExecution;
import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.interfaces.dto.ExecutionResponse;
import org.skylark.herms.interfaces.dto.WorkflowResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WorkflowAssembler — converts domain aggregates to REST response DTOs.
 */
@Component
public class WorkflowAssembler {

    public WorkflowResponse toResponse(Workflow workflow) {
        List<WorkflowResponse.StepSummary> stepSummaries = workflow.getSteps().stream()
                .map(s -> WorkflowResponse.StepSummary.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .type(s.getType().name())
                        .nextStepId(s.getNextStepId())
                        .terminal(s.isTerminal())
                        .build())
                .toList();

        return WorkflowResponse.builder()
                .id(workflow.getId().getValue())
                .name(workflow.getConfig().getName())
                .description(workflow.getConfig().getDescription())
                .status(workflow.getStatus().name())
                .entryStepId(workflow.getEntryStepId())
                .maxSteps(workflow.getConfig().getMaxSteps())
                .steps(stepSummaries)
                .createdAt(workflow.getCreatedAt().toString())
                .updatedAt(workflow.getUpdatedAt().toString())
                .build();
    }

    public ExecutionResponse toExecutionResponse(WorkflowExecution execution) {
        List<ExecutionResponse.StepResultSummary> resultSummaries =
                execution.getStepResults().stream()
                        .map(r -> ExecutionResponse.StepResultSummary.builder()
                                .stepId(r.getStepId())
                                .stepName(r.getStepName())
                                .success(r.isSuccess())
                                .output(r.getOutput())
                                .error(r.getError())
                                .durationMs(r.getDurationMs())
                                .build())
                        .toList();

        return ExecutionResponse.builder()
                .id(execution.getId().getValue())
                .workflowId(execution.getWorkflowId().getValue())
                .status(execution.getStatus().name())
                .finalOutput(execution.getFinalOutput())
                .errorMessage(execution.getErrorMessage())
                .stepsExecuted(resultSummaries.size())
                .stepResults(resultSummaries)
                .createdAt(execution.getCreatedAt().toString())
                .updatedAt(execution.getUpdatedAt().toString())
                .build();
    }
}
