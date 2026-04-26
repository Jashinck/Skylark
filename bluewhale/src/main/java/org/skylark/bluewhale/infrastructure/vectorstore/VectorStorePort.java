package org.skylark.bluewhale.infrastructure.vectorstore;

import java.util.List;
import java.util.Map;

/**
 * 向量存储端口接口
 */
public interface VectorStorePort {
    void upsert(String id, float[] vector, Map<String, String> metadata);
    List<SearchResult> search(float[] queryVector, int topK, Map<String, String> filter);
    void delete(String id);
    void clear();

    class SearchResult {
        private final String id;
        private final double score;
        private final Map<String, String> metadata;

        public SearchResult(String id, double score, Map<String, String> metadata) {
            this.id = id;
            this.score = score;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public double getScore() { return score; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
