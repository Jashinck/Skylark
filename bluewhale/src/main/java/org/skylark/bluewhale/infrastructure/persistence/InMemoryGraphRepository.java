package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.model.graph.MemoryGraph;
import org.skylark.bluewhale.domain.repository.MemoryGraphRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGraphRepository implements MemoryGraphRepository {

    private final Map<String, MemoryGraph> store = new ConcurrentHashMap<>();

    @Override
    public void save(MemoryGraph graph) {
        store.put(graph.getAgentId(), graph);
    }

    @Override
    public Optional<MemoryGraph> findByAgentId(String agentId) {
        return Optional.ofNullable(store.get(agentId));
    }

    @Override
    public MemoryGraph findOrCreateByAgentId(String agentId) {
        return store.computeIfAbsent(agentId, MemoryGraph::new);
    }
}
