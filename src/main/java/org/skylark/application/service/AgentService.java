package org.skylark.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.domain.model.AgentMemory;
import org.skylark.domain.model.Tool;
import org.skylark.domain.model.ToolRegistry;
import org.skylark.infrastructure.adapter.LLM;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent Service - Intelligent Agent with Memory and Tool Capabilities
 * 智能体服务 - 具备记忆和工具能力的智能体
 *
 * <p>Inspired by AgentScope, this service wraps the raw LLM with agent capabilities
 * including conversation memory, tool invocation, and system prompt management.
 * This transforms the simple dialogue assistant into an intelligent agent capable
 * of handling complex vertical business scenarios.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 *   <li><b>Memory</b> - Per-session conversation history for context continuity</li>
 *   <li><b>Tools</b> - Registerable tools for domain-specific actions</li>
 *   <li><b>System Prompt</b> - Configurable agent persona and behavior</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final LLM llmAdapter;
    private final AgentMemory memory;
    private final ToolRegistry toolRegistry;

    /**
     * Constructs an AgentService with the given LLM adapter.
     * Initializes with a default system prompt and empty tool registry.
     *
     * @param llmAdapter LLM adapter for chat interactions
     */
    public AgentService(LLM llmAdapter) {
        this(llmAdapter,
             "You are a helpful AI assistant with expertise in technical training. "
             + "You maintain conversation context and can assist with complex queries.",
             20);
    }

    /**
     * Constructs an AgentService with custom configuration.
     *
     * @param llmAdapter LLM adapter for chat interactions
     * @param systemPrompt System prompt defining agent persona
     * @param maxHistoryTurns Maximum conversation turns to retain per session
     */
    public AgentService(LLM llmAdapter, String systemPrompt, int maxHistoryTurns) {
        this.llmAdapter = llmAdapter;
        this.memory = new AgentMemory(systemPrompt, maxHistoryTurns);
        this.toolRegistry = new ToolRegistry();
        logger.info("AgentService initialized with system prompt and maxHistoryTurns={}",
            maxHistoryTurns);
    }

    /**
     * Processes a user message through the agent pipeline with memory context.
     *
     * <p>Pipeline:</p>
     * <ol>
     *   <li>Add user message to session memory</li>
     *   <li>Build full message list (system prompt + history)</li>
     *   <li>Call LLM with complete context</li>
     *   <li>Store assistant response in memory</li>
     *   <li>Return response</li>
     * </ol>
     *
     * @param sessionId Session identifier for memory management
     * @param userText User input text
     * @return Agent response text
     * @throws Exception if LLM interaction fails
     */
    public String chat(String sessionId, String userText) throws Exception {
        logger.debug("Agent processing message for session {}: {}", sessionId, userText);

        // Step 1: Add user message to memory
        memory.addUserMessage(sessionId, userText);

        // Step 2: Build messages with full context (system prompt + history)
        List<Map<String, String>> messages = memory.buildMessages(sessionId);

        // Step 3: Call LLM with streaming and collect response
        StringBuilder response = new StringBuilder();
        String responseId = UUID.randomUUID().toString();

        try {
            llmAdapter.chat(messages,
                chunk -> response.append(chunk),
                () -> logger.debug("LLM streaming completed for agent response: {}", responseId)
            );
        } catch (Exception e) {
            logger.error("Agent LLM call failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        String responseText = response.toString();

        // Step 4: Store assistant response in memory
        if (!responseText.isEmpty()) {
            memory.addAssistantMessage(sessionId, responseText);
        }

        logger.debug("Agent response for session {}: {}", sessionId,
            responseText.length() > 100 ? responseText.substring(0, 100) + "..." : responseText);

        return responseText;
    }

    /**
     * Registers a tool with the agent.
     *
     * @param tool Tool to register
     */
    public void registerTool(Tool tool) {
        toolRegistry.register(tool);
        logger.info("Tool registered with agent: {}", tool.getName());
    }

    /**
     * Executes a registered tool by name.
     *
     * @param toolName Name of the tool to execute
     * @param parameters Tool parameters
     * @return Tool execution result, or error message if tool not found
     */
    public String executeTool(String toolName, Map<String, Object> parameters) {
        Tool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            logger.warn("Tool not found: {}", toolName);
            return "Tool not found: " + toolName;
        }

        logger.info("Executing tool: {} with parameters: {}", toolName, parameters);
        try {
            return tool.execute(parameters);
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", toolName, e);
            return "Tool execution error: " + e.getMessage();
        }
    }

    /**
     * Cleans up session resources including memory.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        memory.clearSession(sessionId);
        logger.info("Agent session cleared: {}", sessionId);
    }

    /**
     * Gets the conversation history for a session.
     *
     * @param sessionId Session identifier
     * @return List of messages in the session history
     */
    public List<org.skylark.domain.model.Message> getSessionHistory(String sessionId) {
        return memory.getHistory(sessionId);
    }

    /**
     * Gets the agent's memory instance.
     *
     * @return AgentMemory instance
     */
    public AgentMemory getMemory() {
        return memory;
    }

    /**
     * Gets the agent's tool registry.
     *
     * @return ToolRegistry instance
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
}
