package org.skylark.bluewhale.domain.model.evolution;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * EvolutionRecord实体 - 记录每次记忆进化操作的历史
 */
@Getter
@Builder
public class EvolutionRecord {
    private final String id;
    private final String agentId;
    private final EvolutionType type;
    private final String targetMemoryId;
    private final String description;
    private final double scoreBefore;
    private final double scoreAfter;
    private final Instant occurredAt;

    public static EvolutionRecord create(String agentId, EvolutionType type,
                                          String targetMemoryId, String description,
                                          double scoreBefore, double scoreAfter) {
        return EvolutionRecord.builder()
                .id(UUID.randomUUID().toString())
                .agentId(agentId)
                .type(type)
                .targetMemoryId(targetMemoryId)
                .description(description)
                .scoreBefore(scoreBefore)
                .scoreAfter(scoreAfter)
                .occurredAt(Instant.now())
                .build();
    }

    public enum EvolutionType {
        CONSOLIDATION,   // 记忆巩固（多条相似记忆合并）
        FORGETTING,      // 记忆遗忘（低重要性记忆删除）
        STRENGTHENING,   // 记忆强化（频繁访问的记忆重要性提升）
        ABSTRACTION,     // 记忆抽象（从情景记忆提炼语义记忆）
        ASSOCIATION      // 记忆关联（建立新的记忆图边）
    }
}
