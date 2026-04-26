package org.skylark.bluewhale.domain.model.graph;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemoryEdge {
    private final String sourceMemoryId;
    private final String targetMemoryId;
    /** 关系类型，例如：RELATED_TO, CAUSED_BY, PART_OF, CONTRADICTS */
    private final String relation;
    /** 关系强度 [0.0, 1.0] */
    private double weight;

    public MemoryEdge strengthen(double delta) {
        return MemoryEdge.builder()
                .sourceMemoryId(sourceMemoryId).targetMemoryId(targetMemoryId)
                .relation(relation).weight(Math.min(1.0, weight + delta)).build();
    }
}
