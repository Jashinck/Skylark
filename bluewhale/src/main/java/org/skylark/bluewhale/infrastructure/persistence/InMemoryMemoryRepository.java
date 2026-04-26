package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.model.memory.MemoryId;
import org.skylark.bluewhale.domain.model.memory.MemoryType;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryMemoryRepository implements MemoryRepository {

    private final Map<String, Memory> store = new ConcurrentHashMap<>();

    @Override
    public void save(Memory memory) {
        store.put(memory.getId().getValue(), memory);
    }

    @Override
    public Optional<Memory> findById(MemoryId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<Memory> findByAgentId(String agentId) {
        return store.values().stream()
                .filter(m -> agentId.equals(m.getMetadata().getAgentId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Memory> findByAgentIdAndType(String agentId, MemoryType type) {
        return store.values().stream()
                .filter(m -> agentId.equals(m.getMetadata().getAgentId()) && type == m.getType())
                .collect(Collectors.toList());
    }

    @Override
    public void delete(MemoryId id) {
        store.remove(id.getValue());
    }

    @Override
    public void deleteByAgentId(String agentId) {
        store.values().removeIf(m -> agentId.equals(m.getMetadata().getAgentId()));
    }
}
