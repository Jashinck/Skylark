package org.skylark.herms.domain.model.workflow;

/**
 * Lifecycle status of a Workflow definition.
 */
public enum WorkflowStatus {
    /** Workflow is being defined and is not yet executable. */
    DRAFT,
    /** Workflow is published and may be triggered. */
    ACTIVE,
    /** Workflow has been retired and can no longer be triggered. */
    ARCHIVED
}
