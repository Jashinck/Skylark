package org.skylark.langur.domain.model.agent;

public enum AgentStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_TOOL,
    COMPLETED,
    FAILED,
    PAUSED
}
