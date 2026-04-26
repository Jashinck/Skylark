package org.skylark.bluewhale.domain.repository;

import org.skylark.bluewhale.domain.model.graph.MemoryGraph;

import java.util.Optional;

public interface MemoryGraphRepository {
    void save(MemoryGraph graph);
    Optional<MemoryGraph> findByAgentId(String agentId);
    MemoryGraph findOrCreateByAgentId(String agentId);
}
