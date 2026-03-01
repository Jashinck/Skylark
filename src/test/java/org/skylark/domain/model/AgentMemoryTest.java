package org.skylark.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentMemory
 */
class AgentMemoryTest {

    private AgentMemory memory;

    @BeforeEach
    void setUp() {
        memory = new AgentMemory("You are a helpful assistant.", 5);
    }

    @Test
    void testAddUserMessage() {
        memory.addUserMessage("session-1", "Hello");
        List<Message> history = memory.getHistory("session-1");

        assertEquals(1, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("Hello", history.get(0).getContent());
    }

    @Test
    void testAddAssistantMessage() {
        memory.addAssistantMessage("session-1", "Hi there!");
        List<Message> history = memory.getHistory("session-1");

        assertEquals(1, history.size());
        assertEquals("assistant", history.get(0).getRole());
        assertEquals("Hi there!", history.get(0).getContent());
    }

    @Test
    void testBuildMessages_IncludesSystemPromptAndHistory() {
        memory.addUserMessage("session-1", "What is Java?");
        memory.addAssistantMessage("session-1", "Java is a programming language.");
        memory.addUserMessage("session-1", "Tell me more.");

        List<Map<String, String>> messages = memory.buildMessages("session-1");

        // System prompt + 3 history messages
        assertEquals(4, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("You are a helpful assistant.", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("assistant", messages.get(2).get("role"));
        assertEquals("user", messages.get(3).get("role"));
    }

    @Test
    void testBuildMessages_EmptySession() {
        List<Map<String, String>> messages = memory.buildMessages("non-existent");

        // Only system prompt
        assertEquals(1, messages.size());
        assertEquals("system", messages.get(0).get("role"));
    }

    @Test
    void testBuildMessages_NoSystemPrompt() {
        AgentMemory noPromptMemory = new AgentMemory("", 5);
        noPromptMemory.addUserMessage("session-1", "Hello");

        List<Map<String, String>> messages = noPromptMemory.buildMessages("session-1");

        // No system prompt, just the user message
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
    }

    @Test
    void testHistoryTrimming() {
        // Max 5 turns = 10 messages
        for (int i = 0; i < 8; i++) {
            memory.addUserMessage("session-1", "User message " + i);
            memory.addAssistantMessage("session-1", "Assistant message " + i);
        }

        List<Message> history = memory.getHistory("session-1");

        // Should be trimmed to 10 messages (5 turns x 2)
        assertEquals(10, history.size());
        // Oldest messages should have been removed
        assertEquals("User message 3", history.get(0).getContent());
    }

    @Test
    void testClearSession() {
        memory.addUserMessage("session-1", "Hello");
        memory.addAssistantMessage("session-1", "Hi!");

        memory.clearSession("session-1");
        List<Message> history = memory.getHistory("session-1");

        assertTrue(history.isEmpty());
    }

    @Test
    void testMultipleSessions() {
        memory.addUserMessage("session-1", "Hello from session 1");
        memory.addUserMessage("session-2", "Hello from session 2");

        assertEquals(1, memory.getHistory("session-1").size());
        assertEquals(1, memory.getHistory("session-2").size());
        assertEquals(2, memory.getActiveSessionCount());

        // Clear one session doesn't affect the other
        memory.clearSession("session-1");
        assertTrue(memory.getHistory("session-1").isEmpty());
        assertEquals(1, memory.getHistory("session-2").size());
    }

    @Test
    void testGetSystemPrompt() {
        assertEquals("You are a helpful assistant.", memory.getSystemPrompt());
    }

    @Test
    void testGetMaxHistoryTurns() {
        assertEquals(5, memory.getMaxHistoryTurns());
    }

    @Test
    void testDefaultMaxHistoryTurns() {
        AgentMemory defaultMemory = new AgentMemory("prompt");
        assertEquals(20, defaultMemory.getMaxHistoryTurns());
    }

    @Test
    void testNullSystemPrompt() {
        AgentMemory nullPromptMemory = new AgentMemory(null);
        assertEquals("", nullPromptMemory.getSystemPrompt());
    }

    @Test
    void testInvalidMaxHistoryTurns() {
        AgentMemory invalidMemory = new AgentMemory("prompt", -1);
        assertEquals(20, invalidMemory.getMaxHistoryTurns());
    }

    @Test
    void testGetHistory_ReturnsUnmodifiableList() {
        memory.addUserMessage("session-1", "Hello");
        List<Message> history = memory.getHistory("session-1");

        assertThrows(UnsupportedOperationException.class, () -> history.add(new Message("user", "test")));
    }
}
