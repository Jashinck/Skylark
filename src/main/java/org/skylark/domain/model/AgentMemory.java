package org.skylark.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Memory - Per-session Conversation History Manager
 * 智能体记忆 - 会话级对话历史管理
 *
 * <p>Manages conversation history for each session, providing context continuity
 * for multi-turn dialogue. Inspired by AgentScope's memory module, this class
 * enables the agent to maintain context awareness across interactions.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Per-session conversation history with configurable max turns</li>
 *   <li>System prompt management for agent persona definition</li>
 *   <li>Thread-safe concurrent session support</li>
 *   <li>Automatic history trimming to prevent context overflow</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class AgentMemory {

    private static final Logger logger = LoggerFactory.getLogger(AgentMemory.class);

    /**
     * Default maximum number of conversation turns to retain per session.
     */
    private static final int DEFAULT_MAX_HISTORY_TURNS = 20;

    /**
     * Per-session conversation histories.
     * Key: sessionId, Value: list of Messages
     */
    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    /**
     * Maximum number of message pairs (turns) to retain per session.
     */
    private final int maxHistoryTurns;

    /**
     * System prompt that defines the agent's persona and behavior.
     */
    private final String systemPrompt;

    /**
     * Creates an AgentMemory with a system prompt and default max history turns.
     *
     * @param systemPrompt System prompt defining agent persona
     */
    public AgentMemory(String systemPrompt) {
        this(systemPrompt, DEFAULT_MAX_HISTORY_TURNS);
    }

    /**
     * Creates an AgentMemory with a system prompt and specified max history turns.
     *
     * @param systemPrompt System prompt defining agent persona
     * @param maxHistoryTurns Maximum number of conversation turns to retain
     */
    public AgentMemory(String systemPrompt, int maxHistoryTurns) {
        this.systemPrompt = systemPrompt != null ? systemPrompt : "";
        this.maxHistoryTurns = maxHistoryTurns > 0 ? maxHistoryTurns : DEFAULT_MAX_HISTORY_TURNS;
        logger.info("AgentMemory initialized with maxHistoryTurns={}", this.maxHistoryTurns);
    }

    /**
     * Adds a user message to the session's conversation history.
     *
     * @param sessionId Session identifier
     * @param content User message content
     */
    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, Message.user(content));
    }

    /**
     * Adds an assistant message to the session's conversation history.
     *
     * @param sessionId Session identifier
     * @param content Assistant response content
     */
    public void addAssistantMessage(String sessionId, String content) {
        addMessage(sessionId, Message.assistant(content));
    }

    /**
     * Adds a message to the session's conversation history.
     *
     * @param sessionId Session identifier
     * @param message Message to add
     */
    public void addMessage(String sessionId, Message message) {
        List<Message> history = sessionHistories.computeIfAbsent(
            sessionId, k -> Collections.synchronizedList(new ArrayList<>())
        );
        history.add(message);
        trimHistory(sessionId);
        logger.debug("Added {} message to session {}, history size: {}",
            message.getRole(), sessionId, history.size());
    }

    /**
     * Builds the complete message list for LLM interaction, including
     * the system prompt and conversation history.
     *
     * @param sessionId Session identifier
     * @return List of messages suitable for LLM API calls
     */
    public List<Map<String, String>> buildMessages(String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Add system prompt if present
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // Add conversation history
        List<Message> history = sessionHistories.get(sessionId);
        if (history != null) {
            synchronized (history) {
                for (Message msg : history) {
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }
        }

        return messages;
    }

    /**
     * Gets the conversation history for a specific session.
     *
     * @param sessionId Session identifier
     * @return Unmodifiable list of messages, empty if session has no history
     */
    public List<Message> getHistory(String sessionId) {
        List<Message> history = sessionHistories.get(sessionId);
        if (history == null) {
            return Collections.emptyList();
        }
        synchronized (history) {
            return Collections.unmodifiableList(new ArrayList<>(history));
        }
    }

    /**
     * Clears conversation history for a specific session.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
        logger.info("Cleared memory for session: {}", sessionId);
    }

    /**
     * Gets the system prompt.
     *
     * @return System prompt string
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Gets the maximum history turns setting.
     *
     * @return Maximum history turns
     */
    public int getMaxHistoryTurns() {
        return maxHistoryTurns;
    }

    /**
     * Gets the number of active sessions.
     *
     * @return Number of sessions with stored history
     */
    public int getActiveSessionCount() {
        return sessionHistories.size();
    }

    /**
     * Trims conversation history to respect maxHistoryTurns limit.
     * Each turn is counted as 2 messages (user + assistant).
     */
    private void trimHistory(String sessionId) {
        List<Message> history = sessionHistories.get(sessionId);
        if (history == null) {
            return;
        }
        synchronized (history) {
            int maxMessages = maxHistoryTurns * 2;
            while (history.size() > maxMessages) {
                history.remove(0);
            }
        }
    }
}
