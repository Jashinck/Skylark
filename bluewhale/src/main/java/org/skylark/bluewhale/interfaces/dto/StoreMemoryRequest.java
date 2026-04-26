package org.skylark.bluewhale.interfaces.dto;

import lombok.Data;
import org.skylark.bluewhale.domain.model.memory.MemoryType;

import java.util.List;
import java.util.Map;

@Data
public class StoreMemoryRequest {
    private String agentId;
    private String sessionId;
    private MemoryType type;
    private String text;
    private Map<String, Object> structuredData;
    private List<String> tags;
    private double importanceScore;
}
