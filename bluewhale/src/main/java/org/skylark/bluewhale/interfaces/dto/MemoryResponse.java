package org.skylark.bluewhale.interfaces.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemoryResponse {
    private String id;
    private String type;
    private String text;
    private String agentId;
    private String sessionId;
    private List<String> tags;
    private double importanceScore;
    private double effectiveImportance;
    private int accessCount;
    private String createdAt;
    private String lastAccessedAt;
}
