package org.skylark.bluewhale.interfaces.dto;

import lombok.Data;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

@Data
public class RecallMemoryRequest {
    private String agentId;
    private String query;
    private int topK;
    private MemoryType filterType;
}
