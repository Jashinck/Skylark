package org.skylark.bluewhale.domain.model.memory;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MemoryMetadata {
    private final String agentId;
    private final String sessionId;
    private final String source;
    private final List<String> tags;
    private final Instant createdAt;
    private final Instant lastAccessedAt;
    private int accessCount;
    /** 重要性分数 [0.0, 1.0] */
    private double importanceScore;
    /** 衰减因子：记忆随时间遗忘的速率 */
    private double decayFactor;

    public MemoryMetadata withAccess() {
        return MemoryMetadata.builder()
                .agentId(agentId).sessionId(sessionId).source(source).tags(tags)
                .createdAt(createdAt).lastAccessedAt(Instant.now())
                .accessCount(accessCount + 1)
                .importanceScore(importanceScore)
                .decayFactor(decayFactor)
                .build();
    }

    public MemoryMetadata withImportance(double newImportance) {
        return MemoryMetadata.builder()
                .agentId(agentId).sessionId(sessionId).source(source).tags(tags)
                .createdAt(createdAt).lastAccessedAt(lastAccessedAt)
                .accessCount(accessCount)
                .importanceScore(Math.max(0.0, Math.min(1.0, newImportance)))
                .decayFactor(decayFactor)
                .build();
    }

    /**
     * 计算当前有效重要性（考虑时间衰减）
     */
    public double computeEffectiveImportance() {
        long secondsSinceAccess = Instant.now().getEpochSecond() -
                (lastAccessedAt != null ? lastAccessedAt.getEpochSecond() : createdAt.getEpochSecond());
        double decayedScore = importanceScore * Math.exp(-decayFactor * secondsSinceAccess / 86400.0);
        return Math.max(0.0, decayedScore);
    }
}
