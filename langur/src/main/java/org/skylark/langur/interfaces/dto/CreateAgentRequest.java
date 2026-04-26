package org.skylark.langur.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private double temperature;
    private int maxIterations;
    private List<String> toolNames;
}
