package org.skylark.application.service;

import org.skylark.domain.model.Message;
import org.skylark.infrastructure.adapter.LLM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Service
 * 智能体服务
 *
 * <p>Core agent implementation that augments the base LLM with:
 * <ul>
 *   <li><b>Memory</b> – per-session conversation history via {@link AgentMemory},
 *       so the AI instructor remembers what was discussed earlier.</li>
 *   <li><b>Tools</b> – extensible tool registry via {@link ToolRegistry},
 *       enabling domain-specific capabilities such as knowledge-base queries,
 *       course lookups, and exercise generation for the tech-training domain.</li>
 *   <li><b>System prompt</b> – configurable persona and instructions injected
 *       before every conversation turn.</li>
 * </ul>
 * </p>
 *
 * <p>Inspired by AgentScope's agent abstraction, this service turns Skylark
 * from a simple stateless chat assistant into a stateful, tool-capable agent
 * suited for complex vertical-domain scenarios (e.g., AI技术培训讲解师).</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final LLM llm;
    private final AgentMemory memory;
    private final ToolRegistry toolRegistry;

    private String systemPrompt;

    /**
     * Constructs the AgentService with required dependencies.
     *
     * @param llm          Underlying LLM adapter
     * @param memory       Per-session memory store
     * @param toolRegistry Registry of available tools
     */
    public AgentService(LLM llm, AgentMemory memory, ToolRegistry toolRegistry) {
        this.llm = llm;
        this.memory = memory;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Processes a user message for the given session, incorporating conversation
     * history and tool descriptions, then returns the assistant's response.
     *
     * <p>The method:
     * <ol>
     *   <li>Stores the user message in session memory.</li>
     *   <li>Builds the full message list (system + history).</li>
     *   <li>Calls the LLM and collects the streamed response.</li>
     *   <li>Stores the assistant response in session memory.</li>
     * </ol>
     * </p>
     *
     * @param sessionId  Unique session identifier
     * @param userText   User's input text
     * @return           Assistant's response text
     * @throws Exception if the LLM call fails
     */
    public String chat(String sessionId, String userText) throws Exception {
        logger.info("Agent chat - session: {}, input: {}", sessionId, userText);

        // 1. Add user message to memory
        memory.addMessage(sessionId, Message.user(userText));

        // 2. Build full message list for LLM
        List<Map<String, String>> messages = buildMessages(sessionId);

        // 3. Call LLM, collecting streaming chunks
        StringBuilder response = new StringBuilder();
        llm.chat(messages, response::append, () ->
                logger.debug("LLM stream completed for session {}", sessionId));

        String assistantResponse = response.toString();
        logger.info("Agent response - session: {}, output: {}", sessionId, assistantResponse);

        // 4. Store assistant response in memory
        if (!assistantResponse.isEmpty()) {
            memory.addMessage(sessionId, Message.assistant(assistantResponse));
        }

        return assistantResponse;
    }

    /**
     * Clears the conversation memory for the given session.
     *
     * @param sessionId Unique session identifier
     */
    public void clearSession(String sessionId) {
        memory.clearSession(sessionId);
        logger.info("Cleared agent session: {}", sessionId);
    }

    /**
     * Sets the system prompt used to configure the agent's persona and
     * instructions. Tool descriptions are appended automatically.
     *
     * @param systemPrompt System prompt text
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * Returns the configured system prompt (without tool descriptions).
     *
     * @return System prompt, or null if not set
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Builds the message list to send to the LLM, prepending a system message
     * that includes the configured system prompt and tool descriptions.
     */
    private List<Map<String, String>> buildMessages(String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();

        String effectiveSystemPrompt = buildSystemPrompt();
        if (effectiveSystemPrompt != null && !effectiveSystemPrompt.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", effectiveSystemPrompt);
            messages.add(systemMsg);
        }

        // Append conversation history (already contains user + assistant turns)
        messages.addAll(memory.getMessages(sessionId));

        return messages;
    }

    /**
     * Combines the configured system prompt with tool descriptions.
     */
    private String buildSystemPrompt() {
        String toolDesc = toolRegistry.getToolDescriptions();
        if ((systemPrompt == null || systemPrompt.isEmpty()) && toolDesc.isEmpty()) {
            return null;
        }

        StringBuilder prompt = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            prompt.append(systemPrompt);
        }
        if (!toolDesc.isEmpty()) {
            if (prompt.length() > 0) {
                prompt.append("\n\n");
            }
            prompt.append(toolDesc);
        }
        return prompt.toString();
    }
}
