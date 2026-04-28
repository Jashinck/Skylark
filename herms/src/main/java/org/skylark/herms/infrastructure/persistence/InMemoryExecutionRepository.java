package org.skylark.herms.infrastructure.persistence;

import org.skylark.herms.domain.model.execution.ExecutionId;
import org.skylark.herms.domain.model.execution.WorkflowExecution;
import org.skylark.herms.domain.model.workflow.WorkflowId;
import org.skylark.herms.domain.repository.ExecutionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<String, WorkflowExecution> store = new ConcurrentHashMap<>();

    @Override
    public void save(WorkflowExecution execution) {
        store.put(execution.getId().getValue(), execution);
    }

    @Override
    public Optional<WorkflowExecution> findById(ExecutionId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<WorkflowExecution> findByWorkflowId(WorkflowId workflowId) {
        return store.values().stream()
                .filter(e -> e.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowExecution> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(ExecutionId id) {
        store.remove(id.getValue());
    }
}
