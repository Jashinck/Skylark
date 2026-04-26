package org.skylark.bluewhale.domain.model.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MemoryGraph实体 - 以图结构表示记忆间的关联网络
 */
public class MemoryGraph {

    private final String agentId;
    private final Map<String, MemoryNode> nodes;
    private final List<MemoryEdge> edges;

    public MemoryGraph(String agentId) {
        this.agentId = agentId;
        this.nodes = new LinkedHashMap<>();
        this.edges = new ArrayList<>();
    }

    public void addNode(MemoryNode node) {
        nodes.put(node.getMemoryId(), node);
    }

    public void removeNode(String memoryId) {
        nodes.remove(memoryId);
        edges.removeIf(e -> e.getSourceMemoryId().equals(memoryId)
                || e.getTargetMemoryId().equals(memoryId));
    }

    public void addEdge(MemoryEdge edge) {
        edges.add(edge);
    }

    public void strengthenEdge(String sourceId, String targetId, double delta) {
        for (int i = 0; i < edges.size(); i++) {
            MemoryEdge e = edges.get(i);
            if (e.getSourceMemoryId().equals(sourceId) && e.getTargetMemoryId().equals(targetId)) {
                edges.set(i, e.strengthen(delta));
                return;
            }
        }
        // 不存在时新建边
        edges.add(MemoryEdge.builder()
                .sourceMemoryId(sourceId).targetMemoryId(targetId)
                .relation("RELATED_TO").weight(delta).build());
    }

    public List<String> getNeighbors(String memoryId) {
        return edges.stream()
                .filter(e -> e.getSourceMemoryId().equals(memoryId))
                .map(MemoryEdge::getTargetMemoryId)
                .collect(Collectors.toList());
    }

    public Collection<MemoryNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public List<MemoryEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Optional<MemoryNode> getNode(String memoryId) {
        return Optional.ofNullable(nodes.get(memoryId));
    }

    public String getAgentId() {
        return agentId;
    }

    public int nodeCount() {
        return nodes.size();
    }
}
