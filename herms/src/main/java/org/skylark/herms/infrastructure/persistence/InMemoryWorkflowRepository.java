package org.skylark.herms.infrastructure.persistence;

import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.domain.model.workflow.WorkflowId;
import org.skylark.herms.domain.repository.WorkflowRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public void save(Workflow workflow) {
        store.put(workflow.getId().getValue(), workflow);
    }

    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<Workflow> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(WorkflowId id) {
        store.remove(id.getValue());
    }
}
