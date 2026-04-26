package org.skylark.langur.domain.repository;

import org.skylark.langur.domain.model.agent.Agent;
import org.skylark.langur.domain.model.agent.AgentId;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    void save(Agent agent);
    Optional<Agent> findById(AgentId id);
    List<Agent> findAll();
    void delete(AgentId id);
}
