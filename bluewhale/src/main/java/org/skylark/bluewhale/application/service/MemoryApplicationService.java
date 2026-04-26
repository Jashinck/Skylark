package org.skylark.bluewhale.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.bluewhale.application.assembler.MemoryAssembler;
import org.skylark.bluewhale.application.command.RecallMemoryCommand;
import org.skylark.bluewhale.application.command.StoreMemoryCommand;
import org.skylark.bluewhale.domain.model.memory.*;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.skylark.bluewhale.domain.service.MemoryDomainService;
import org.skylark.bluewhale.interfaces.dto.MemoryResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 记忆应用服务 - 编排存取用例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryApplicationService {

    private final MemoryDomainService memoryDomainService;
    private final MemoryRepository memoryRepository;
    private final MemoryAssembler memoryAssembler;

    public MemoryResponse storeMemory(StoreMemoryCommand command) {
        MemoryContent content = command.getStructuredData() != null
                ? MemoryContent.ofStructured(command.getText(), command.getStructuredData())
                : MemoryContent.ofText(command.getText());

        MemoryMetadata metadata = MemoryMetadata.builder()
                .agentId(command.getAgentId())
                .sessionId(command.getSessionId())
                .source("application")
                .tags(command.getTags())
                .createdAt(Instant.now())
                .lastAccessedAt(Instant.now())
                .accessCount(0)
                .importanceScore(command.getImportanceScore() > 0 ? command.getImportanceScore() : 0.5)
                .decayFactor(0.1)
                .build();

        MemoryType type = command.getType() != null ? command.getType() : MemoryType.EPISODIC;
        Memory memory = Memory.create(type, content, metadata);
        Memory stored = memoryDomainService.storeMemory(memory);

        log.info("Stored {} memory {} for agent {}", type, stored.getId(), command.getAgentId());
        return memoryAssembler.toResponse(stored);
    }

    public List<MemoryResponse> recallMemories(RecallMemoryCommand command) {
        int topK = command.getTopK() > 0 ? command.getTopK() : 5;
        List<Memory> memories = memoryDomainService.recallSimilar(
                command.getAgentId(), command.getQuery(), topK);
        return memories.stream().map(memoryAssembler::toResponse).toList();
    }

    public List<MemoryResponse> listMemories(String agentId) {
        return memoryRepository.findByAgentId(agentId)
                .stream().map(memoryAssembler::toResponse).toList();
    }

    public void deleteMemory(String memoryId) {
        memoryRepository.delete(MemoryId.of(memoryId));
    }

    public void clearAgentMemories(String agentId) {
        memoryRepository.deleteByAgentId(agentId);
    }
}
