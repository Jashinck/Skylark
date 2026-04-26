package org.skylark.bluewhale.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.skylark.bluewhale.application.command.RecallMemoryCommand;
import org.skylark.bluewhale.application.command.StoreMemoryCommand;
import org.skylark.bluewhale.application.service.EvolutionApplicationService;
import org.skylark.bluewhale.application.service.MemoryApplicationService;
import org.skylark.bluewhale.domain.model.evolution.EvolutionRecord;
import org.skylark.bluewhale.interfaces.dto.MemoryResponse;
import org.skylark.bluewhale.interfaces.dto.RecallMemoryRequest;
import org.skylark.bluewhale.interfaces.dto.StoreMemoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryApplicationService memoryApplicationService;
    private final EvolutionApplicationService evolutionApplicationService;

    @PostMapping
    public ResponseEntity<MemoryResponse> storeMemory(@RequestBody StoreMemoryRequest request) {
        StoreMemoryCommand command = StoreMemoryCommand.builder()
                .agentId(request.getAgentId())
                .sessionId(request.getSessionId())
                .type(request.getType())
                .text(request.getText())
                .structuredData(request.getStructuredData())
                .tags(request.getTags())
                .importanceScore(request.getImportanceScore())
                .build();
        return ResponseEntity.ok(memoryApplicationService.storeMemory(command));
    }

    @PostMapping("/recall")
    public ResponseEntity<List<MemoryResponse>> recallMemories(@RequestBody RecallMemoryRequest request) {
        RecallMemoryCommand command = RecallMemoryCommand.builder()
                .agentId(request.getAgentId())
                .query(request.getQuery())
                .topK(request.getTopK())
                .filterType(request.getFilterType())
                .build();
        return ResponseEntity.ok(memoryApplicationService.recallMemories(command));
    }

    @GetMapping
    public ResponseEntity<List<MemoryResponse>> listMemories(@RequestParam String agentId) {
        return ResponseEntity.ok(memoryApplicationService.listMemories(agentId));
    }

    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> deleteMemory(@PathVariable String memoryId) {
        memoryApplicationService.deleteMemory(memoryId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearMemories(@RequestParam String agentId) {
        memoryApplicationService.clearAgentMemories(agentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evolve")
    public ResponseEntity<List<EvolutionRecord>> triggerEvolution(@RequestParam String agentId) {
        return ResponseEntity.ok(evolutionApplicationService.triggerEvolution(agentId));
    }
}
