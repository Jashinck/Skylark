package org.skylark.langur.interfaces.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentResponse {
    private String id;
    private String name;
    private String description;
    private String status;
    private String model;
    private int iterationCount;
    private int toolCount;
    private String lastError;
    private String createdAt;
    private String updatedAt;
}
