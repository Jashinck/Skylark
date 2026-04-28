package org.skylark.herms.domain.service;

import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.domain.model.workflow.WorkflowConfig;
import org.skylark.herms.domain.model.workflow.WorkflowStatus;
import org.springframework.stereotype.Service;

/**
 * WorkflowDomainService — domain logic for Workflow lifecycle management.
 */
@Service
public class WorkflowDomainService {

    /**
     * Creates a new DRAFT workflow with the given config.
     */
    public Workflow createWorkflow(WorkflowConfig config) {
        return Workflow.create(config);
    }

    /**
     * Validates and activates a workflow, making it triggerable.
     *
     * @throws IllegalStateException if the workflow is not ready for activation
     */
    public void activateWorkflow(Workflow workflow) {
        workflow.activate();
    }

    /**
     * Returns true if the workflow can currently be triggered.
     */
    public boolean canTrigger(Workflow workflow) {
        return workflow.getStatus() == WorkflowStatus.ACTIVE
                && !workflow.getSteps().isEmpty()
                && workflow.getEntryStepId() != null;
    }
}
