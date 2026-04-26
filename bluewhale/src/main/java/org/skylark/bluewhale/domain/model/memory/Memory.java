package org.skylark.bluewhale.domain.model.memory;

import lombok.Getter;
import org.skylark.bluewhale.domain.event.DomainEvent;
import org.skylark.bluewhale.domain.event.MemoryStoredEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Memory聚合根 - 代表一条记忆单元
 * 支持向量化嵌入、重要性评分、时间衰减与自进化
 */
@Getter
public class Memory {

    private final MemoryId id;
    private final MemoryType type;
    private MemoryContent content;
    private MemoryMetadata metadata;
    private float[] embedding;
    private final List<DomainEvent> domainEvents;

    private Memory(MemoryId id, MemoryType type, MemoryContent content, MemoryMetadata metadata) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.metadata = metadata;
        this.domainEvents = new ArrayList<>();
        recordEvent(new MemoryStoredEvent(id.getValue(), type.name(), metadata.getAgentId()));
    }

    public static Memory create(MemoryType type, MemoryContent content, MemoryMetadata metadata) {
        MemoryId id = MemoryId.generate();
        return new Memory(id, type, content, metadata);
    }

    public static Memory restore(MemoryId id, MemoryType type, MemoryContent content,
                                  MemoryMetadata metadata, float[] embedding) {
        Memory memory = new Memory(id, type, content, metadata);
        memory.embedding = embedding;
        memory.domainEvents.clear();
        return memory;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void updateContent(MemoryContent newContent) {
        this.content = newContent;
    }

    public void recordAccess() {
        this.metadata = metadata.withAccess();
    }

    public void strengthen(double importanceDelta) {
        double newImportance = metadata.getImportanceScore() + importanceDelta;
        this.metadata = metadata.withImportance(newImportance);
    }

    public void weaken(double importanceDelta) {
        double newImportance = metadata.getImportanceScore() - importanceDelta;
        this.metadata = metadata.withImportance(newImportance);
    }

    public boolean shouldBeForgotten(double threshold) {
        return metadata.computeEffectiveImportance() < threshold;
    }

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}
