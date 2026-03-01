package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.domain.model.Message;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentMemory
 */
class AgentMemoryTest {

    private AgentMemory agentMemory;

    @BeforeEach
    void setUp() {
        // Use null historyDir so no files are written during tests
        agentMemory = new AgentMemory(null);
    }

    @Test
    void testAddAndRetrieveMessages() {
        String sessionId = "session-1";

        agentMemory.addMessage(sessionId, Message.user("Hello"));
        agentMemory.addMessage(sessionId, Message.assistant("Hi there!"));

        List<Map<String, String>> messages = agentMemory.getMessages(sessionId);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("Hello", messages.get(0).get("content"));
        assertEquals("assistant", messages.get(1).get("role"));
        assertEquals("Hi there!", messages.get(1).get("content"));
    }

    @Test
    void testSessionIsolation() {
        agentMemory.addMessage("session-A", Message.user("Message A"));
        agentMemory.addMessage("session-B", Message.user("Message B"));

        assertEquals(1, agentMemory.getMessages("session-A").size());
        assertEquals(1, agentMemory.getMessages("session-B").size());
        assertEquals("Message A", agentMemory.getMessages("session-A").get(0).get("content"));
        assertEquals("Message B", agentMemory.getMessages("session-B").get(0).get("content"));
    }

    @Test
    void testClearSession() {
        String sessionId = "session-clear";
        agentMemory.addMessage(sessionId, Message.user("Hello"));
        assertEquals(1, agentMemory.getMessages(sessionId).size());

        agentMemory.clearSession(sessionId);

        // After clearing, a new empty dialogue is created on next access
        assertEquals(0, agentMemory.getMessages(sessionId).size());
    }

    @Test
    void testGetSessionCount() {
        assertEquals(0, agentMemory.getSessionCount());

        agentMemory.addMessage("s1", Message.user("Hello"));
        assertEquals(1, agentMemory.getSessionCount());

        agentMemory.addMessage("s2", Message.user("World"));
        assertEquals(2, agentMemory.getSessionCount());
    }

    @Test
    void testEmptySessionReturnsEmptyList() {
        List<Map<String, String>> messages = agentMemory.getMessages("nonexistent-session");
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void testDefaultConstructor() {
        AgentMemory defaultMemory = new AgentMemory();
        assertNotNull(defaultMemory);
        assertEquals(0, defaultMemory.getSessionCount());
    }
}
