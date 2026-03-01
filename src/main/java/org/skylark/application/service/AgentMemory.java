package org.skylark.application.service;

import org.skylark.domain.model.Dialogue;
import org.skylark.domain.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Memory Service
 * Agent记忆服务
 *
 * <p>Manages per-session conversation history for the Agent. Each session
 * maintains its own {@link Dialogue} instance, providing context memory
 * across multiple turns of conversation.</p>
 *
 * <p>Inspired by AgentScope's memory module, this service enables the AI
 * instructor to remember what was discussed earlier in the session and
 * provide contextually relevant responses.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class AgentMemory {

    private static final Logger logger = LoggerFactory.getLogger(AgentMemory.class);

    private final Map<String, Dialogue> sessionDialogues = new ConcurrentHashMap<>();
    private final String historyDir;

    /**
     * Creates an AgentMemory with the default history directory.
     */
    public AgentMemory() {
        this.historyDir = "tmp/agent";
    }

    /**
     * Creates an AgentMemory with a custom history directory.
     * Pass {@code null} to disable file persistence (in-memory only).
     *
     * @param historyDir Directory to persist per-session dialogue files, or null for in-memory only
     */
    public AgentMemory(String historyDir) {
        this.historyDir = (historyDir != null && !historyDir.trim().isEmpty()) ? historyDir : null;
    }

    /**
     * Adds a message to the specified session's dialogue history.
     *
     * @param sessionId Unique session identifier
     * @param message   Message to add
     */
    public void addMessage(String sessionId, Message message) {
        getOrCreateDialogue(sessionId).addMessage(message);
        logger.debug("Added {} message to session {}", message.getRole(), sessionId);
    }

    /**
     * Returns all messages for the specified session in LLM-compatible format.
     *
     * @param sessionId Unique session identifier
     * @return List of maps containing "role" and "content" keys
     */
    public List<Map<String, String>> getMessages(String sessionId) {
        return getOrCreateDialogue(sessionId).getLLMMessages();
    }

    /**
     * Clears the conversation history for the specified session.
     *
     * @param sessionId Unique session identifier
     */
    public void clearSession(String sessionId) {
        Dialogue dialogue = sessionDialogues.remove(sessionId);
        if (dialogue != null) {
            dialogue.clear();
            logger.info("Cleared agent memory for session {}", sessionId);
        }
    }

    /**
     * Returns the number of active sessions in memory.
     *
     * @return Number of active sessions
     */
    public int getSessionCount() {
        return sessionDialogues.size();
    }

    /**
     * Gets or creates a Dialogue instance for the given session.
     * Uses file persistence if historyDir is configured, otherwise in-memory only.
     */
    private Dialogue getOrCreateDialogue(String sessionId) {
        return sessionDialogues.computeIfAbsent(sessionId,
                id -> (historyDir != null)
                        ? new Dialogue(historyDir + "/" + id + ".json")
                        : new Dialogue());
    }
}
