package org.skylark.langur.domain.model.agent;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentConfig {
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final String model;
    private final double temperature;
    private final int maxIterations;
    private final int maxTokens;

    public static AgentConfig defaultConfig(String name) {
        return AgentConfig.builder()
                .name(name)
                .description("")
                .systemPrompt("You are a helpful assistant. Use the available tools to answer questions step by step.")
                .model("gpt-4o")
                .temperature(0.7)
                .maxIterations(10)
                .maxTokens(4096)
                .build();
    }
}
