package org.skylark.herms.domain.repository;

import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.domain.model.workflow.WorkflowId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Workflow} aggregates.
 */
public interface WorkflowRepository {
    void save(Workflow workflow);
    Optional<Workflow> findById(WorkflowId id);
    List<Workflow> findAll();
    void delete(WorkflowId id);
}
