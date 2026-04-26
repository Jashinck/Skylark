package org.skylark.bluewhale.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.bluewhale.domain.model.evolution.EvolutionRecord;
import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.model.memory.MemoryContent;
import org.skylark.bluewhale.domain.model.memory.MemoryType;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆进化领域服务 - 实现自进化：遗忘、巩固、强化、抽象
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryEvolutionService {

    private static final double FORGETTING_THRESHOLD = 0.05;
    private static final double STRENGTHENING_DELTA = 0.1;
    private static final double CONSOLIDATION_SIMILARITY = 0.9;

    private final MemoryRepository memoryRepository;

    /**
     * 遗忘进化：删除重要性低于阈值的记忆（艾宾浩斯遗忘曲线模拟）
     */
    public List<EvolutionRecord> runForgettingCycle(String agentId) {
        List<Memory> memories = memoryRepository.findByAgentId(agentId);
        List<EvolutionRecord> records = new ArrayList<>();

        memories.stream()
                .filter(m -> m.getType() != MemoryType.PROCEDURAL) // 程序记忆不遗忘
                .filter(m -> m.shouldBeForgotten(FORGETTING_THRESHOLD))
                .forEach(m -> {
                    double scoreBefore = m.getMetadata().getImportanceScore();
                    memoryRepository.delete(m.getId());
                    records.add(EvolutionRecord.create(agentId,
                            EvolutionRecord.EvolutionType.FORGETTING,
                            m.getId().getValue(),
                            "Forgotten due to low importance: " + scoreBefore,
                            scoreBefore, 0.0));
                    log.debug("Forgot memory {} (score={})", m.getId(), scoreBefore);
                });

        return records;
    }

    /**
     * 强化进化：对频繁访问的记忆提升重要性
     */
    public List<EvolutionRecord> runStrengtheningCycle(String agentId) {
        List<Memory> memories = memoryRepository.findByAgentId(agentId);
        List<EvolutionRecord> records = new ArrayList<>();

        memories.stream()
                .filter(m -> m.getMetadata().getAccessCount() > 3)
                .forEach(m -> {
                    double before = m.getMetadata().getImportanceScore();
                    m.strengthen(STRENGTHENING_DELTA);
                    memoryRepository.save(m);
                    records.add(EvolutionRecord.create(agentId,
                            EvolutionRecord.EvolutionType.STRENGTHENING,
                            m.getId().getValue(),
                            "Strengthened due to frequent access (" +
                                    m.getMetadata().getAccessCount() + " times)",
                            before, m.getMetadata().getImportanceScore()));
                });

        return records;
    }

    /**
     * 抽象进化：从情景记忆中提炼出语义记忆摘要
     */
    public List<EvolutionRecord> runAbstractionCycle(String agentId) {
        List<Memory> episodics = memoryRepository.findByAgentIdAndType(agentId, MemoryType.EPISODIC);
        List<EvolutionRecord> records = new ArrayList<>();

        // 收集高重要性情景记忆，尝试抽象出语义知识
        List<Memory> highImportance = episodics.stream()
                .filter(m -> m.getMetadata().getImportanceScore() > 0.7)
                .collect(Collectors.toList());

        if (highImportance.size() >= 3) {
            String abstractText = "Abstracted knowledge from " + highImportance.size() +
                    " episodic memories: " +
                    highImportance.stream()
                            .map(m -> m.getContent().getText().substring(0,
                                    Math.min(30, m.getContent().getText().length())))
                            .collect(Collectors.joining("; "));

            Memory semantic = Memory.create(
                    MemoryType.SEMANTIC,
                    MemoryContent.ofText(abstractText),
                    highImportance.get(0).getMetadata()
                            .withImportance(0.8)
            );
            memoryRepository.save(semantic);

            records.add(EvolutionRecord.create(agentId,
                    EvolutionRecord.EvolutionType.ABSTRACTION,
                    semantic.getId().getValue(),
                    "Abstracted from " + highImportance.size() + " episodic memories",
                    0.0, 0.8));
        }

        return records;
    }
}
