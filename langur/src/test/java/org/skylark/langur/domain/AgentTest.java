package org.skylark.langur.domain;

import org.junit.jupiter.api.Test;
import org.skylark.langur.domain.model.agent.Agent;
import org.skylark.langur.domain.model.agent.AgentConfig;
import org.skylark.langur.domain.model.agent.AgentStatus;
import org.skylark.langur.domain.model.tool.Tool;
import org.skylark.langur.domain.model.tool.ToolDefinition;
import org.skylark.langur.domain.model.tool.ToolResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentTest {

    @Test
    void shouldCreateAgentWithDefaultStatus() {
        AgentConfig config = AgentConfig.defaultConfig("TestAgent");
        Agent agent = Agent.create(config);

        assertNotNull(agent.getId());
        assertEquals("TestAgent", agent.getConfig().getName());
        assertEquals(AgentStatus.IDLE, agent.getStatus());
        assertTrue(agent.getTools().isEmpty());
        assertNotNull(agent.getCreatedAt());
    }

    @Test
    void shouldRegisterTool() {
        Agent agent = Agent.create(AgentConfig.defaultConfig("TestAgent"));
        Tool mockTool = new Tool(ToolDefinition.of("mock_tool", "A mock tool", Map.of())) {
            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                return ToolResult.success("mock result");
            }
        };

        agent.registerTool(mockTool);
        assertEquals(1, agent.getTools().size());
        assertEquals("mock_tool", agent.getTools().get(0).getName());
    }

    @Test
    void shouldNotRegisterDuplicateTool() {
        Agent agent = Agent.create(AgentConfig.defaultConfig("TestAgent"));
        Tool tool = new Tool(ToolDefinition.of("tool", "A tool", Map.of())) {
            @Override
            public ToolResult execute(Map<String, Object> params) {
                return ToolResult.success("ok");
            }
        };
        agent.registerTool(tool);
        assertThrows(IllegalArgumentException.class, () -> agent.registerTool(tool));
    }

    @Test
    void shouldTrackConversationHistory() {
        Agent agent = Agent.create(AgentConfig.defaultConfig("TestAgent"));
        agent.addUserMessage("Hello");
        agent.addAssistantMessage("Hi there!");

        assertEquals(2, agent.getConversationHistory().size());
        assertEquals("user", agent.getConversationHistory().get(0).get("role"));
        assertEquals("assistant", agent.getConversationHistory().get(1).get("role"));
    }

    @Test
    void shouldTransitionStatusCorrectly() {
        Agent agent = Agent.create(AgentConfig.defaultConfig("TestAgent"));
        assertEquals(AgentStatus.IDLE, agent.getStatus());

        agent.markRunning();
        assertEquals(AgentStatus.RUNNING, agent.getStatus());

        agent.markCompleted("Done");
        assertEquals(AgentStatus.COMPLETED, agent.getStatus());
    }

    @Test
    void shouldDetectMaxIterationsExceeded() {
        AgentConfig config = AgentConfig.builder()
                .name("TestAgent").description("").systemPrompt("test")
                .model("gpt-4o").temperature(0.7).maxIterations(3).maxTokens(4096).build();
        Agent agent = Agent.create(config);

        agent.incrementIteration();
        agent.incrementIteration();
        agent.incrementIteration();
        assertTrue(agent.hasExceededMaxIterations());
    }

    @Test
    void shouldEmitDomainEventsOnCreation() {
        Agent agent = Agent.create(AgentConfig.defaultConfig("TestAgent"));
        var events = agent.pullDomainEvents();
        assertEquals(1, events.size());
        assertEquals("AGENT_CREATED", events.get(0).getEventType());
    }
}
