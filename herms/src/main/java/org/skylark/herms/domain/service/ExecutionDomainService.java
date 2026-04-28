package org.skylark.herms.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.herms.domain.model.execution.ExecutionStatus;
import org.skylark.herms.domain.model.execution.StepResult;
import org.skylark.herms.domain.model.execution.WorkflowExecution;
import org.skylark.herms.domain.model.step.StepType;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.infrastructure.executor.StepExecutorPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ExecutionDomainService — drives the step-by-step execution of a workflow run.
 *
 * <p>Implements the core dispatch loop: resolve the current step → delegate to the
 * appropriate {@link StepExecutorPort} → record the result → advance to the next step.
 * Execution terminates when no successor step is found or a step fails.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionDomainService {

    private final StepExecutorPort stepExecutor;

    /**
     * Runs the workflow to completion from its entry step.
     *
     * @param workflow  the Workflow definition
     * @param execution the mutable execution aggregate
     */
    public void run(Workflow workflow, WorkflowExecution execution) {
        execution.start(workflow.getEntryStepId());
        int stepCount = 0;

        while (execution.getStatus() == ExecutionStatus.RUNNING
                && execution.getCurrentStepId() != null) {

            if (stepCount >= workflow.getConfig().getMaxSteps()) {
                execution.fail("Max step limit reached: " + workflow.getConfig().getMaxSteps());
                return;
            }

            String currentId = execution.getCurrentStepId();
            Optional<WorkflowStep> stepOpt = workflow.findStep(currentId);
            if (stepOpt.isEmpty()) {
                execution.fail("Step not found in workflow definition: " + currentId);
                return;
            }

            WorkflowStep step = stepOpt.get();
            log.debug("Executing step [{}] type={} for execution {}",
                    step.getId(), step.getType(), execution.getId());

            StepResult result = executeStep(step, execution);
            execution.recordStepResult(result);
            stepCount++;

            if (!result.isSuccess()) {
                execution.fail("Step '" + step.getName() + "' failed: " + result.getError());
                return;
            }

            // Determine next step
            String nextId = resolveNextStep(step, result, execution);
            if (nextId == null) {
                // Pipeline complete
                String finalOutput = result.getOutput() != null ? result.getOutput() : "";
                execution.complete(finalOutput);
            } else {
                execution.advanceTo(nextId);
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private StepResult executeStep(WorkflowStep step, WorkflowExecution execution) {
        long start = System.currentTimeMillis();
        try {
            String output = stepExecutor.execute(step, execution.getContext());
            return StepResult.success(step.getId(), step.getName(),
                    output, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Step '{}' threw exception: {}", step.getId(), e.getMessage());
            return StepResult.failure(step.getId(), step.getName(),
                    e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private String resolveNextStep(WorkflowStep step, StepResult result,
                                    WorkflowExecution execution) {
        if (step.getType() == StepType.CONDITION) {
            // Condition step: output is "true" or "false"
            boolean condResult = "true".equalsIgnoreCase(
                    result.getOutput() != null ? result.getOutput().trim() : "false");
            return condResult
                    ? step.getConfig().getTrueStepId()
                    : step.getConfig().getFalseStepId();
        }
        return step.getNextStepId();
    }
}
