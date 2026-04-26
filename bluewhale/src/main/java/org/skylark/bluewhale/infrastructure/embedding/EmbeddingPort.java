package org.skylark.bluewhale.infrastructure.embedding;

import java.util.List;

/**
 * 向量嵌入端口接口 - 依赖倒置，领域层不依赖具体嵌入实现
 */
public interface EmbeddingPort {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    int getDimension();
}
