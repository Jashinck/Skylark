package org.skylark.bluewhale.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.bluewhale.domain.model.evolution.EvolutionRecord;
import org.skylark.bluewhale.domain.service.MemoryEvolutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 进化应用服务 - 触发记忆自进化周期（定时任务 + 手动触发）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvolutionApplicationService {

    private final MemoryEvolutionService memoryEvolutionService;

    /**
     * 手动触发指定Agent的完整进化周期
     */
    public List<EvolutionRecord> triggerEvolution(String agentId) {
        log.info("Starting evolution cycle for agent {}", agentId);
        List<EvolutionRecord> all = new ArrayList<>();
        all.addAll(memoryEvolutionService.runStrengtheningCycle(agentId));
        all.addAll(memoryEvolutionService.runForgettingCycle(agentId));
        all.addAll(memoryEvolutionService.runAbstractionCycle(agentId));
        log.info("Evolution cycle complete for agent {}: {} records", agentId, all.size());
        return all;
    }
}
