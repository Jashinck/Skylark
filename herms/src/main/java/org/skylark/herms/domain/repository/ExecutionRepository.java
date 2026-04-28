package org.skylark.herms.domain.repository;

import org.skylark.herms.domain.model.execution.ExecutionId;
import org.skylark.herms.domain.model.execution.WorkflowExecution;
import org.skylark.herms.domain.model.workflow.WorkflowId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link WorkflowExecution} aggregates.
 */
public interface ExecutionRepository {
    void save(WorkflowExecution execution);
    Optional<WorkflowExecution> findById(ExecutionId id);
    List<WorkflowExecution> findByWorkflowId(WorkflowId workflowId);
    List<WorkflowExecution> findAll();
    void delete(ExecutionId id);
}
