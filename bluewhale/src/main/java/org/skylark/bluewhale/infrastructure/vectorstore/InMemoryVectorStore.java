package org.skylark.bluewhale.infrastructure.vectorstore;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储 - 基于余弦相似度的轻量级向量检索
 */
@Component
public class InMemoryVectorStore implements VectorStorePort {

    private record VectorEntry(String id, float[] vector, Map<String, String> metadata) {}

    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(String id, float[] vector, Map<String, String> metadata) {
        store.put(id, new VectorEntry(id, vector, new HashMap<>(metadata)));
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK, Map<String, String> filter) {
        return store.values().stream()
                .filter(e -> matchesFilter(e.metadata(), filter))
                .map(e -> new SearchResult(e.id(), cosineSimilarity(queryVector, e.vector()), e.metadata()))
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public void clear() {
        store.clear();
    }

    private boolean matchesFilter(Map<String, String> metadata, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) return true;
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String val = metadata.get(entry.getKey());
            if (!entry.getValue().equals(val)) return false;
        }
        return true;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
