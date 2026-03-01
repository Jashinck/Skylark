package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.domain.model.Message;
import org.skylark.domain.model.Tool;
import org.skylark.infrastructure.adapter.LLM;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentService
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private LLM llmAdapter;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(llmAdapter, "You are a test assistant.", 10);
    }

    @Test
    void testChat_BasicResponse() throws Exception {
        // Mock LLM streaming response
        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Hello, how can I help?");
            onComplete.run();
            return null;
        }).when(llmAdapter).chat(anyList(), any(), any());

        String response = agentService.chat("session-1", "Hi");

        assertEquals("Hello, how can I help?", response);
        verify(llmAdapter, times(1)).chat(anyList(), any(), any());
    }

    @Test
    void testChat_MaintainsConversationHistory() throws Exception {
        // First call
        doAnswer(invocation -> {
            List<Map<String, String>> messages = invocation.getArgument(0);
            // First call: system prompt + user message
            assertEquals(2, messages.size());
            assertEquals("system", messages.get(0).get("role"));
            assertEquals("user", messages.get(1).get("role"));
            assertEquals("Hello", messages.get(1).get("content"));

            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Hi there!");
            onComplete.run();
            return null;
        }).when(llmAdapter).chat(anyList(), any(), any());

        agentService.chat("session-1", "Hello");

        // Second call - should include previous conversation
        doAnswer(invocation -> {
            List<Map<String, String>> messages = invocation.getArgument(0);
            // Second call: system + user1 + assistant1 + user2
            assertEquals(4, messages.size());
            assertEquals("system", messages.get(0).get("role"));
            assertEquals("user", messages.get(1).get("role"));
            assertEquals("Hello", messages.get(1).get("content"));
            assertEquals("assistant", messages.get(2).get("role"));
            assertEquals("Hi there!", messages.get(2).get("content"));
            assertEquals("user", messages.get(3).get("role"));
            assertEquals("What is Java?", messages.get(3).get("content"));

            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Java is a programming language.");
            onComplete.run();
            return null;
        }).when(llmAdapter).chat(anyList(), any(), any());

        String response = agentService.chat("session-1", "What is Java?");

        assertEquals("Java is a programming language.", response);
    }

    @Test
    void testChat_SeparateSessionMemories() throws Exception {
        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Response");
            onComplete.run();
            return null;
        }).when(llmAdapter).chat(anyList(), any(), any());

        agentService.chat("session-1", "Hello from session 1");
        agentService.chat("session-2", "Hello from session 2");

        List<Message> history1 = agentService.getSessionHistory("session-1");
        List<Message> history2 = agentService.getSessionHistory("session-2");

        assertEquals(2, history1.size()); // user + assistant
        assertEquals(2, history2.size()); // user + assistant
        assertEquals("Hello from session 1", history1.get(0).getContent());
        assertEquals("Hello from session 2", history2.get(0).getContent());
    }

    @Test
    void testChat_LLMError() throws Exception {
        doThrow(new RuntimeException("LLM error")).when(llmAdapter).chat(anyList(), any(), any());

        assertThrows(RuntimeException.class, () -> agentService.chat("session-1", "Hello"));
    }

    @Test
    void testClearSession() throws Exception {
        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Response");
            onComplete.run();
            return null;
        }).when(llmAdapter).chat(anyList(), any(), any());

        agentService.chat("session-1", "Hello");
        assertEquals(2, agentService.getSessionHistory("session-1").size());

        agentService.clearSession("session-1");
        assertTrue(agentService.getSessionHistory("session-1").isEmpty());
    }

    @Test
    void testRegisterTool() {
        Tool tool = createTestTool("search", "Search knowledge base");
        agentService.registerTool(tool);

        assertTrue(agentService.getToolRegistry().hasTool("search"));
    }

    @Test
    void testExecuteTool_Success() {
        Tool tool = new Tool() {
            @Override public String getName() { return "greet"; }
            @Override public String getDescription() { return "Greet"; }
            @Override public String execute(Map<String, Object> parameters) {
                return "Hello, " + parameters.get("name") + "!";
            }
        };
        agentService.registerTool(tool);

        String result = agentService.executeTool("greet", Map.of("name", "World"));
        assertEquals("Hello, World!", result);
    }

    @Test
    void testExecuteTool_NotFound() {
        String result = agentService.executeTool("non-existent", Map.of());
        assertTrue(result.contains("Tool not found"));
    }

    @Test
    void testExecuteTool_ExecutionError() {
        Tool errorTool = new Tool() {
            @Override public String getName() { return "error-tool"; }
            @Override public String getDescription() { return "Error tool"; }
            @Override public String execute(Map<String, Object> parameters) {
                throw new RuntimeException("Tool failed");
            }
        };
        agentService.registerTool(errorTool);

        String result = agentService.executeTool("error-tool", Map.of());
        assertTrue(result.contains("Tool execution error"));
    }

    @Test
    void testGetMemory() {
        assertNotNull(agentService.getMemory());
        assertEquals("You are a test assistant.", agentService.getMemory().getSystemPrompt());
    }

    @Test
    void testDefaultConstructor() {
        AgentService defaultAgent = new AgentService(llmAdapter);
        assertNotNull(defaultAgent.getMemory());
        assertNotNull(defaultAgent.getToolRegistry());
    }

    private Tool createTestTool(String name, String description) {
        return new Tool() {
            @Override public String getName() { return name; }
            @Override public String getDescription() { return description; }
            @Override public String execute(Map<String, Object> parameters) { return "result"; }
        };
    }
}
