package org.skylark.langur.domain.repository;

import org.skylark.langur.domain.model.plan.Plan;

import java.util.Optional;

public interface PlanRepository {
    void save(Plan plan);
    Optional<Plan> findByAgentId(String agentId);
    void delete(String agentId);
}
