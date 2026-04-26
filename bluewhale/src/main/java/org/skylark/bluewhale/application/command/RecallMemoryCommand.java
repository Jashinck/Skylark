package org.skylark.bluewhale.application.command;

import lombok.Builder;
import lombok.Getter;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

@Getter
@Builder
public class RecallMemoryCommand {
    private final String agentId;
    private final String query;
    private final int topK;
    private final MemoryType filterType;
}
