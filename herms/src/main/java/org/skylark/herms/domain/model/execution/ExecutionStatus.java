package org.skylark.herms.domain.model.execution;

/**
 * Lifecycle status of a single WorkflowExecution run.
 */
public enum ExecutionStatus {
    /** Execution has been created but not yet started. */
    PENDING,
    /** Execution is actively processing steps. */
    RUNNING,
    /** All steps completed successfully. */
    COMPLETED,
    /** Execution stopped due to a step failure. */
    FAILED,
    /** Execution was cancelled by the caller. */
    CANCELLED
}
