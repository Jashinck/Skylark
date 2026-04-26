package org.skylark.bluewhale.application.command;

import lombok.Builder;
import lombok.Getter;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class StoreMemoryCommand {
    private final String agentId;
    private final String sessionId;
    private final MemoryType type;
    private final String text;
    private final Map<String, Object> structuredData;
    private final List<String> tags;
    private final double importanceScore;
}
