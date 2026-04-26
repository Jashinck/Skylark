package org.skylark.langur.application.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateAgentCommand {
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final String model;
    private final double temperature;
    private final int maxIterations;
    private final List<String> toolNames;
}
