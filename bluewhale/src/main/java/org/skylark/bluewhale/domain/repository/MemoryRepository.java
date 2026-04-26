package org.skylark.bluewhale.domain.repository;

import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.model.memory.MemoryId;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

import java.util.List;
import java.util.Optional;

public interface MemoryRepository {
    void save(Memory memory);
    Optional<Memory> findById(MemoryId id);
    List<Memory> findByAgentId(String agentId);
    List<Memory> findByAgentIdAndType(String agentId, MemoryType type);
    void delete(MemoryId id);
    void deleteByAgentId(String agentId);
}
