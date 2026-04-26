package org.skylark.langur.domain.model.agent;

import lombok.Getter;
import org.skylark.langur.domain.event.AgentCreatedEvent;
import org.skylark.langur.domain.event.AgentExecutedEvent;
import org.skylark.langur.domain.event.DomainEvent;
import org.skylark.langur.domain.model.tool.Tool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Agent聚合根 - 代表一个具备自主决策和工具调用能力的智能体
 */
@Getter
public class Agent {

    private final AgentId id;
    private AgentConfig config;
    private AgentStatus status;
    private final List<Tool> tools;
    private final List<Map<String, String>> conversationHistory;
    private final List<DomainEvent> domainEvents;
    private int iterationCount;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private Agent(AgentId id, AgentConfig config) {
        this.id = id;
        this.config = config;
        this.status = AgentStatus.IDLE;
        this.tools = new ArrayList<>();
        this.conversationHistory = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.iterationCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        recordEvent(new AgentCreatedEvent(id.getValue(), config.getName()));
    }

    public static Agent create(AgentConfig config) {
        return new Agent(AgentId.generate(), config);
    }

    public static Agent restore(AgentId id, AgentConfig config, AgentStatus status,
                                 List<Tool> tools, List<Map<String, String>> history,
                                 int iterationCount, Instant createdAt) {
        Agent agent = new Agent(id, config);
        agent.status = status;
        agent.tools.addAll(tools);
        agent.conversationHistory.addAll(history);
        agent.iterationCount = iterationCount;
        agent.domainEvents.clear(); // clear creation event on restore
        return agent;
    }

    public void registerTool(Tool tool) {
        if (tools.stream().anyMatch(t -> t.getName().equals(tool.getName()))) {
            throw new IllegalArgumentException("Tool already registered: " + tool.getName());
        }
        tools.add(tool);
        updatedAt = Instant.now();
    }

    public void addUserMessage(String content) {
        conversationHistory.add(Map.of("role", "user", "content", content));
        updatedAt = Instant.now();
    }

    public void addAssistantMessage(String content) {
        conversationHistory.add(Map.of("role", "assistant", "content", content));
        updatedAt = Instant.now();
    }

    public void addToolResultMessage(String toolName, String result) {
        conversationHistory.add(Map.of("role", "tool", "name", toolName, "content", result));
        updatedAt = Instant.now();
    }

    public void markRunning() {
        validateTransition(AgentStatus.RUNNING);
        this.status = AgentStatus.RUNNING;
        this.lastError = null;
        updatedAt = Instant.now();
    }

    public void markCompleted(String finalAnswer) {
        this.status = AgentStatus.COMPLETED;
        addAssistantMessage(finalAnswer);
        recordEvent(new AgentExecutedEvent(id.getValue(), iterationCount, finalAnswer));
        updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = AgentStatus.FAILED;
        this.lastError = error;
        updatedAt = Instant.now();
    }

    public void incrementIteration() {
        this.iterationCount++;
    }

    public boolean hasExceededMaxIterations() {
        return iterationCount >= config.getMaxIterations();
    }

    public List<Tool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public List<Map<String, String>> getConversationHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private void validateTransition(AgentStatus target) {
        if (status == AgentStatus.RUNNING && target == AgentStatus.RUNNING) {
            throw new IllegalStateException("Agent is already running");
        }
    }
}
