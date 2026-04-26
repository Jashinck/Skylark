package org.skylark.bluewhale.domain.model.graph;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemoryNode {
    private final String memoryId;
    private final String label;
    /** 节点权重，反映该记忆在图中的中心性 */
    private final double weight;

    public MemoryNode withWeight(double newWeight) {
        return MemoryNode.builder()
                .memoryId(memoryId).label(label).weight(newWeight).build();
    }
}
