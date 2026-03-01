package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private LLM llm;

    private AgentMemory agentMemory;
    private ToolRegistry toolRegistry;
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentMemory = new AgentMemory(null);
        toolRegistry = new ToolRegistry();
        agentService = new AgentService(llm, agentMemory, toolRegistry);
    }

    @Test
    void testChat_returnsLLMResponse() throws Exception {
        String sessionId = "s1";
        String userText = "What is Java?";
        String expectedResponse = "Java is a programming language.";

        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept(expectedResponse);
            onComplete.run();
            return null;
        }).when(llm).chat(anyList(), any(), any());

        String result = agentService.chat(sessionId, userText);

        assertEquals(expectedResponse, result);
    }

    @Test
    void testChat_storesMessagesInMemory() throws Exception {
        String sessionId = "s2";

        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onChunk.accept("Response");
            onComplete.run();
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "Hello");

        List<Map<String, String>> messages = agentMemory.getMessages(sessionId);
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("Hello", messages.get(0).get("content"));
        assertEquals("assistant", messages.get(1).get("role"));
        assertEquals("Response", messages.get(1).get("content"));
    }

    @Test
    void testChat_buildsConversationHistory() throws Exception {
        String sessionId = "s3";

        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            onChunk.accept("Answer 1");
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "First question");

        // On second call, LLM should receive history
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = invocation.getArgument(0);
            // history includes: user(Q1) + assistant(A1) + user(Q2)
            assertTrue(messages.size() >= 3);
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            onChunk.accept("Answer 2");
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "Second question");
    }

    @Test
    void testChat_includesSystemPrompt() throws Exception {
        String sessionId = "s4";
        agentService.setSystemPrompt("You are a helpful assistant.");

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = invocation.getArgument(0);
            // First message should be system
            assertEquals("system", messages.get(0).get("role"));
            assertTrue(messages.get(0).get("content").contains("You are a helpful assistant."));
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            onChunk.accept("OK");
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "Hi");
    }

    @Test
    void testClearSession() throws Exception {
        String sessionId = "s5";

        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            onChunk.accept("Reply");
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "Hello");
        assertEquals(2, agentMemory.getMessages(sessionId).size());

        agentService.clearSession(sessionId);

        assertEquals(0, agentMemory.getMessages(sessionId).size());
    }

    @Test
    void testGetSetSystemPrompt() {
        assertNull(agentService.getSystemPrompt());
        agentService.setSystemPrompt("Test prompt");
        assertEquals("Test prompt", agentService.getSystemPrompt());
    }

    @Test
    void testToolDescriptionsAppendedToSystemPrompt() throws Exception {
        String sessionId = "s6";
        agentService.setSystemPrompt("Base prompt.");
        toolRegistry.register(new org.skylark.domain.model.Tool() {
            @Override public String getName() { return "my_tool"; }
            @Override public String getDescription() { return "Does things"; }
            @Override public String execute(Map<String, Object> p) { return "result"; }
        });

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = invocation.getArgument(0);
            String systemContent = messages.get(0).get("content");
            assertTrue(systemContent.contains("Base prompt."));
            assertTrue(systemContent.contains("my_tool"));
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            onChunk.accept("OK");
            return null;
        }).when(llm).chat(anyList(), any(), any());

        agentService.chat(sessionId, "Test");
    }
}
