package org.skylark.bluewhale.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.bluewhale.domain.model.graph.MemoryGraph;
import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.domain.repository.MemoryGraphRepository;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.skylark.bluewhale.infrastructure.embedding.EmbeddingPort;
import org.skylark.bluewhale.infrastructure.vectorstore.VectorStorePort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆领域服务 - 处理记忆检索、相关性计算和图关联更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryDomainService {

    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStorePort;
    private final MemoryRepository memoryRepository;
    private final MemoryGraphRepository graphRepository;

    /**
     * 存储记忆并生成向量嵌入，同时更新记忆图
     */
    public Memory storeMemory(Memory memory) {
        float[] embedding = embeddingPort.embed(memory.getContent().getText());
        memory.setEmbedding(embedding);
        memoryRepository.save(memory);
        vectorStorePort.upsert(memory.getId().getValue(), embedding,
                Map.of("agentId", memory.getMetadata().getAgentId(),
                       "type", memory.getType().name(),
                       "text", memory.getContent().getText()));

        // 更新记忆图
        updateMemoryGraph(memory);
        log.debug("Stored memory {} for agent {}", memory.getId(), memory.getMetadata().getAgentId());
        return memory;
    }

    /**
     * 语义检索最相关的记忆
     */
    public List<Memory> recallSimilar(String agentId, String query, int topK) {
        float[] queryEmbedding = embeddingPort.embed(query);
        List<VectorStorePort.SearchResult> results =
                vectorStorePort.search(queryEmbedding, topK,
                        Map.of("agentId", agentId));

        List<Memory> recalled = results.stream()
                .map(r -> memoryRepository.findById(
                        org.skylark.bluewhale.domain.model.memory.MemoryId.of(r.getId())))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toList());

        recalled.forEach(m -> {
            m.recordAccess();
            memoryRepository.save(m);
        });

        return recalled;
    }

    /**
     * 强化相关记忆间的图关联
     */
    private void updateMemoryGraph(Memory newMemory) {
        String agentId = newMemory.getMetadata().getAgentId();
        MemoryGraph graph = graphRepository.findOrCreateByAgentId(agentId);

        org.skylark.bluewhale.domain.model.graph.MemoryNode node =
                org.skylark.bluewhale.domain.model.graph.MemoryNode.builder()
                        .memoryId(newMemory.getId().getValue())
                        .label(newMemory.getContent().getText().substring(0,
                                Math.min(50, newMemory.getContent().getText().length())))
                        .weight(newMemory.getMetadata().getImportanceScore())
                        .build();
        graph.addNode(node);

        // 与最近相似记忆建立关联
        if (newMemory.hasEmbedding()) {
            List<VectorStorePort.SearchResult> similar =
                    vectorStorePort.search(newMemory.getEmbedding(), 3,
                            Map.of("agentId", agentId));
            similar.stream()
                    .filter(r -> !r.getId().equals(newMemory.getId().getValue()))
                    .forEach(r -> graph.strengthenEdge(newMemory.getId().getValue(),
                            r.getId(), r.getScore() * 0.5));
        }

        graphRepository.save(graph);
    }
}
