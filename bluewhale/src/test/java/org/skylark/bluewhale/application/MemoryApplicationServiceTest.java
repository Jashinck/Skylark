package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.assembler.MemoryAssembler;
import org.skylark.bluewhale.application.command.StoreMemoryCommand;
import org.skylark.bluewhale.application.service.MemoryApplicationService;
import org.skylark.bluewhale.domain.model.memory.MemoryType;
import org.skylark.bluewhale.domain.repository.MemoryGraphRepository;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.skylark.bluewhale.domain.service.MemoryDomainService;
import org.skylark.bluewhale.infrastructure.embedding.EmbeddingPort;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryGraphRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryMemoryRepository;
import org.skylark.bluewhale.infrastructure.vectorstore.InMemoryVectorStore;
import org.skylark.bluewhale.infrastructure.vectorstore.VectorStorePort;
import org.skylark.bluewhale.interfaces.dto.MemoryResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryApplicationServiceTest {

    private MemoryApplicationService memoryApplicationService;

    @BeforeEach
    void setUp() {
        MemoryRepository memoryRepository = new InMemoryMemoryRepository();
        MemoryGraphRepository graphRepository = new InMemoryGraphRepository();
        VectorStorePort vectorStore = new InMemoryVectorStore();
        EmbeddingPort embeddingPort = new EmbeddingPort() {
            @Override
            public float[] embed(String text) {
                float[] v = new float[4];
                v[0] = text.length() * 0.1f;
                v[1] = text.hashCode() * 0.00001f;
                v[2] = 0.5f;
                v[3] = 0.3f;
                return v;
            }
            @Override
            public List<float[]> embedBatch(List<String> texts) {
                return texts.stream().map(this::embed).toList();
            }
            @Override
            public int getDimension() { return 4; }
        };
        MemoryDomainService domainService = new MemoryDomainService(
                embeddingPort, vectorStore, memoryRepository, graphRepository);
        MemoryAssembler assembler = new MemoryAssembler();
        memoryApplicationService = new MemoryApplicationService(domainService, memoryRepository, assembler);
    }

    @Test
    void shouldStoreMemory() {
        StoreMemoryCommand cmd = StoreMemoryCommand.builder()
                .agentId("agent1").sessionId("s1")
                .type(MemoryType.EPISODIC).text("Today I learned about DDD")
                .importanceScore(0.8).build();

        MemoryResponse response = memoryApplicationService.storeMemory(cmd);

        assertNotNull(response.getId());
        assertEquals("EPISODIC", response.getType());
        assertEquals("Today I learned about DDD", response.getText());
        assertEquals("agent1", response.getAgentId());
    }

    @Test
    void shouldListMemoriesForAgent() {
        for (int i = 0; i < 3; i++) {
            memoryApplicationService.storeMemory(StoreMemoryCommand.builder()
                    .agentId("agent1").type(MemoryType.SEMANTIC)
                    .text("Memory " + i).importanceScore(0.5).build());
        }
        List<MemoryResponse> memories = memoryApplicationService.listMemories("agent1");
        assertEquals(3, memories.size());
    }

    @Test
    void shouldDeleteMemory() {
        MemoryResponse stored = memoryApplicationService.storeMemory(
                StoreMemoryCommand.builder()
                        .agentId("agent1").type(MemoryType.WORKING)
                        .text("Temp memory").importanceScore(0.3).build());

        memoryApplicationService.deleteMemory(stored.getId());
        List<MemoryResponse> memories = memoryApplicationService.listMemories("agent1");
        assertTrue(memories.isEmpty());
    }

    @Test
    void shouldClearAgentMemories() {
        for (int i = 0; i < 5; i++) {
            memoryApplicationService.storeMemory(StoreMemoryCommand.builder()
                    .agentId("agent2").type(MemoryType.EPISODIC)
                    .text("Memory " + i).importanceScore(0.5).build());
        }
        memoryApplicationService.clearAgentMemories("agent2");
        assertTrue(memoryApplicationService.listMemories("agent2").isEmpty());
    }
}
