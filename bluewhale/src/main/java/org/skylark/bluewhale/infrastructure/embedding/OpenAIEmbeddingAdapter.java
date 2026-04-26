package org.skylark.bluewhale.infrastructure.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI嵌入适配器 - 使用OpenAI text-embedding-3-small生成向量
 */
@Slf4j
@Component
public class OpenAIEmbeddingAdapter implements EmbeddingPort {

    private static final int DIMENSION = 1536;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAIEmbeddingAdapter(
            @Value("${bluewhale.embedding.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${bluewhale.embedding.api-key:}") String apiKey,
            @Value("${bluewhale.embedding.model:text-embedding-3-small}") String model,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return generateFallbackEmbedding(text);
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("input", text);

            String response = webClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingArray = root.at("/data/0/embedding");
            float[] result = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                result[i] = (float) embeddingArray.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.warn("Embedding API call failed, using fallback: {}", e.getMessage());
            return generateFallbackEmbedding(text);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    /**
     * 无API key时的降级方案：基于文本哈希生成伪嵌入向量（仅用于开发测试）
     */
    private float[] generateFallbackEmbedding(String text) {
        float[] embedding = new float[DIMENSION];
        int hash = text.hashCode();
        for (int i = 0; i < DIMENSION; i++) {
            embedding[i] = (float) Math.sin(hash * (i + 1) * 0.001);
        }
        return embedding;
    }
}
