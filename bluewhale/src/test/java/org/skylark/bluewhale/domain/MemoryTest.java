package org.skylark.bluewhale.domain;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.model.memory.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTest {

    private MemoryMetadata defaultMetadata(String agentId) {
        return MemoryMetadata.builder()
                .agentId(agentId).sessionId("s1").source("test")
                .tags(List.of("tag1")).createdAt(Instant.now()).lastAccessedAt(Instant.now())
                .accessCount(0).importanceScore(0.5).decayFactor(0.1).build();
    }

    @Test
    void shouldCreateMemoryWithGeneratedId() {
        Memory m = Memory.create(MemoryType.EPISODIC,
                MemoryContent.ofText("Hello world"), defaultMetadata("agent1"));
        assertNotNull(m.getId());
        assertEquals(MemoryType.EPISODIC, m.getType());
        assertEquals("Hello world", m.getContent().getText());
    }

    @Test
    void shouldEmitMemoryStoredEvent() {
        Memory m = Memory.create(MemoryType.SEMANTIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        var events = m.pullDomainEvents();
        assertEquals(1, events.size());
        assertEquals("MEMORY_STORED", events.get(0).getEventType());
    }

    @Test
    void shouldRecordAccessAndIncrementCount() {
        Memory m = Memory.create(MemoryType.EPISODIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        m.recordAccess();
        m.recordAccess();
        assertEquals(2, m.getMetadata().getAccessCount());
    }

    @Test
    void shouldStrengthenImportance() {
        Memory m = Memory.create(MemoryType.EPISODIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        double before = m.getMetadata().getImportanceScore();
        m.strengthen(0.2);
        assertTrue(m.getMetadata().getImportanceScore() > before);
    }

    @Test
    void shouldWeakenImportance() {
        Memory m = Memory.create(MemoryType.EPISODIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        m.weaken(0.3);
        assertTrue(m.getMetadata().getImportanceScore() < 0.5);
    }

    @Test
    void shouldNotExceedImportanceBounds() {
        Memory m = Memory.create(MemoryType.EPISODIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        m.strengthen(10.0);
        assertTrue(m.getMetadata().getImportanceScore() <= 1.0);
        m.weaken(20.0);
        assertTrue(m.getMetadata().getImportanceScore() >= 0.0);
    }

    @Test
    void shouldSetAndDetectEmbedding() {
        Memory m = Memory.create(MemoryType.SEMANTIC,
                MemoryContent.ofText("Test"), defaultMetadata("agent1"));
        assertFalse(m.hasEmbedding());
        m.setEmbedding(new float[]{0.1f, 0.2f, 0.3f});
        assertTrue(m.hasEmbedding());
    }

    @Test
    void shouldDetectForgottenMemory() {
        MemoryMetadata lowMetadata = MemoryMetadata.builder()
                .agentId("agent1").sessionId("s1").source("test")
                .tags(List.of()).createdAt(Instant.now().minusSeconds(86400 * 10))
                .lastAccessedAt(Instant.now().minusSeconds(86400 * 10))
                .accessCount(0).importanceScore(0.01).decayFactor(1.0).build();
        Memory m = Memory.create(MemoryType.EPISODIC, MemoryContent.ofText("Old"), lowMetadata);
        assertTrue(m.shouldBeForgotten(0.05));
    }
}
