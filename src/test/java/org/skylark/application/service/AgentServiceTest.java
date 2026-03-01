package org.skylark.application.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentService with AgentScope integration
 */
class AgentServiceTest {

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        // Create AgentService with test configuration
        agentService = new AgentService(
            "test-api-key",
            "test-model",
            "https://test.api.com",
            "You are a test assistant.",
            10
        );
    }

    @Test
    void testInitialization() {
        assertNotNull(agentService);
        assertEquals("You are a test assistant.", agentService.getSystemPrompt());
        assertNotNull(agentService.getToolkit());
    }

    @Test
    void testDefaultSystemPrompt() {
        AgentService defaultAgent = new AgentService("default-prompt", 5);
        assertEquals("default-prompt", defaultAgent.getSystemPrompt());
    }

    @Test
    void testNullSystemPromptUsesDefault() {
        AgentService agent = new AgentService(null, 10);
        assertNotNull(agent.getSystemPrompt());
        assertFalse(agent.getSystemPrompt().isEmpty());
    }

    @Test
    void testGetToolkit() {
        Toolkit toolkit = agentService.getToolkit();
        assertNotNull(toolkit);
    }

    @Test
    void testRegisterToolObject() {
        // Register a tool object with @Tool annotated methods
        agentService.registerToolObject(new TestTools());
        // No exception means registration was successful
        assertNotNull(agentService.getToolkit());
    }

    @Test
    void testClearSession() {
        // Should not throw for non-existent session
        assertDoesNotThrow(() -> agentService.clearSession("non-existent"));
    }

    @Test
    void testGetSessionHistory_EmptySession() {
        List<Msg> history = agentService.getSessionHistory("non-existent");
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetActiveSessionCount_Initial() {
        assertEquals(0, agentService.getActiveSessionCount());
    }

    @Test
    void testClearSession_RemovesAgent() {
        // Trigger agent creation by calling getSessionHistory after chat would create it
        // Since we can't actually call chat without a real model, test the clear flow
        agentService.clearSession("session-1");
        assertEquals(0, agentService.getActiveSessionCount());
    }

    @Test
    void testMultipleSessionsTracking() {
        // This tests the session tracking without making actual API calls
        // Real chat() tests would require a running model endpoint
        assertEquals(0, agentService.getActiveSessionCount());
        agentService.clearSession("session-1");
        agentService.clearSession("session-2");
        assertEquals(0, agentService.getActiveSessionCount());
    }

    /**
     * Test tool class using AgentScope's @Tool annotation
     */
    static class TestTools {
        @io.agentscope.core.tool.Tool(name = "get_time", description = "Get current time")
        public String getTime(
                @io.agentscope.core.tool.ToolParam(name = "zone", description = "Time zone") String zone) {
            return java.time.LocalDateTime.now().toString();
        }
    }
}
