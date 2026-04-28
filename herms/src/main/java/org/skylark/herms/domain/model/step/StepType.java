package org.skylark.herms.domain.model.step;

/**
 * StepType — the kind of processing a WorkflowStep performs.
 */
public enum StepType {
    /** Send a prompt to a large language model and capture the reply. */
    LLM,
    /** Issue an outbound HTTP request and capture the response body. */
    HTTP,
    /** Apply a string template to the current context to produce a new value. */
    TRANSFORM,
    /** Evaluate a condition and route to one of two subsequent steps. */
    CONDITION,
    /** Emit a notification (webhook, log, etc.) without blocking the pipeline. */
    NOTIFY
}
