package org.skylark.langur.domain.service;

import org.skylark.langur.domain.model.plan.Plan;
import org.springframework.stereotype.Service;

/**
 * 规划领域服务 - 负责Plan的创建与管理
 */
@Service
public class PlanningDomainService {

    public Plan createPlan(String agentId) {
        return new Plan(agentId);
    }

    public boolean isPlanComplete(Plan plan) {
        return plan.getSteps().stream()
                .allMatch(step -> step.getStatus() != null &&
                        (step.getStatus().name().equals("COMPLETED") ||
                         step.getStatus().name().equals("FAILED") ||
                         step.getStatus().name().equals("SKIPPED")));
    }
}
