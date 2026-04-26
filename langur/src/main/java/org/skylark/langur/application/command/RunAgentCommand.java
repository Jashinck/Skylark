package org.skylark.langur.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunAgentCommand {
    private final String agentId;
    private final String userMessage;
}
