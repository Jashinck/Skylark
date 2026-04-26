package org.skylark.bluewhale.application.assembler;

import org.skylark.bluewhale.domain.model.memory.Memory;
import org.skylark.bluewhale.interfaces.dto.MemoryResponse;
import org.springframework.stereotype.Component;

@Component
public class MemoryAssembler {

    public MemoryResponse toResponse(Memory memory) {
        return MemoryResponse.builder()
                .id(memory.getId().getValue())
                .type(memory.getType().name())
                .text(memory.getContent().getText())
                .agentId(memory.getMetadata().getAgentId())
                .sessionId(memory.getMetadata().getSessionId())
                .tags(memory.getMetadata().getTags())
                .importanceScore(memory.getMetadata().getImportanceScore())
                .effectiveImportance(memory.getMetadata().computeEffectiveImportance())
                .accessCount(memory.getMetadata().getAccessCount())
                .createdAt(memory.getMetadata().getCreatedAt() != null
                        ? memory.getMetadata().getCreatedAt().toString() : null)
                .lastAccessedAt(memory.getMetadata().getLastAccessedAt() != null
                        ? memory.getMetadata().getLastAccessedAt().toString() : null)
                .build();
    }
}
