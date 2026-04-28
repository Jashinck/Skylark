package org.skylark.herms.infrastructure.executor;

import org.skylark.herms.domain.model.step.WorkflowStep;

import java.util.Map;

/**
 * StepExecutorPort — hexagonal port for executing a single workflow step.
 *
 * <p>Implementations dispatch to the appropriate backend (LLM API, HTTP endpoint,
 * template engine, etc.) based on the step's {@link org.skylark.herms.domain.model.step.StepType}.</p>
 */
public interface StepExecutorPort {

    /**
     * Executes the given step using the provided execution context.
     *
     * @param step    the step to execute
     * @param context mutable key-value context produced by preceding steps
     * @return the string output of this step (stored back into context)
     * @throws Exception if execution fails (caller records a StepResult failure)
     */
    String execute(WorkflowStep step, Map<String, Object> context) throws Exception;
}
