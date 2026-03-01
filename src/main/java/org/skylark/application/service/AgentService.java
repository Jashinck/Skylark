package org.skylark.application.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent Service - Deep Integration with AgentScope Framework
 * 智能体服务 - 深度集成 AgentScope 框架
 *
 * <p>Integrates the AgentScope framework to provide production-grade AI agent
 * capabilities including ReAct reasoning, per-session memory management,
 * tool invocation via {@code @Tool} annotations, and OpenAI-compatible model support.</p>
 *
 * <p>This replaces the previous custom agent implementation with AgentScope's
 * battle-tested components:</p>
 * <ul>
 *   <li><b>ReActAgent</b> - ReAct (Reasoning + Acting) loop for autonomous task execution</li>
 *   <li><b>OpenAIChatModel</b> - OpenAI-compatible model (supports DeepSeek, vLLM, etc.)</li>
 *   <li><b>InMemoryMemory</b> - Per-session conversation history management</li>
 *   <li><b>Toolkit</b> - Annotation-based tool registration and invocation</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 2.0.0
 * @see <a href="https://github.com/agentscope-ai/agentscope-java">AgentScope Java</a>
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private static final String DEFAULT_SYSTEM_PROMPT =
        "You are a professional AI training instructor with expertise in technical education. "
        + "You maintain conversation context across interactions and provide detailed, "
        + "accurate explanations. You can assist with complex queries in vertical business domains.";

    private static final int DEFAULT_MAX_ITERS = 10;

    private final OpenAIChatModel chatModel;
    private final String systemPrompt;
    private final Toolkit sharedToolkit;
    private final int maxIters;

    /**
     * Per-session ReActAgent instances. Each agent maintains its own memory (conversation history)
     * and is stateful, as required by AgentScope's design.
     */
    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    /**
     * Constructs an AgentService with the given LLM adapter.
     * Creates an AgentScope OpenAIChatModel using environment configuration.
     *
     * @param llmAdapter LLM adapter for backward compatibility
     */
    public AgentService(org.skylark.infrastructure.adapter.LLM llmAdapter) {
        this(DEFAULT_SYSTEM_PROMPT, DEFAULT_MAX_ITERS);
    }

    /**
     * Constructs an AgentService with custom system prompt and default model configuration.
     *
     * @param systemPrompt System prompt defining agent persona
     * @param maxIters Maximum ReAct iterations per request
     */
    public AgentService(String systemPrompt, int maxIters) {
        this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
        this.maxIters = maxIters > 0 ? maxIters : DEFAULT_MAX_ITERS;
        this.sharedToolkit = new Toolkit();

        // Create OpenAIChatModel for DeepSeek (OpenAI-compatible API)
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "sk-placeholder";
            logger.warn("DEEPSEEK_API_KEY environment variable not set, using placeholder. "
                + "Set DEEPSEEK_API_KEY before starting the application for production use.");
        }

        this.chatModel = OpenAIChatModel.builder()
            .apiKey(apiKey)
            .modelName("deepseek-chat")
            .baseUrl("https://api.deepseek.com")
            .build();

        logger.info("AgentService initialized with AgentScope ReActAgent, model=deepseek-chat, maxIters={}",
            this.maxIters);
    }

    /**
     * Constructs an AgentService with explicit model configuration.
     *
     * @param apiKey API key for the model provider
     * @param modelName Model name (e.g., "deepseek-chat", "gpt-4o")
     * @param baseUrl API base URL (e.g., "https://api.deepseek.com")
     * @param systemPrompt System prompt defining agent persona
     * @param maxIters Maximum ReAct iterations per request
     */
    public AgentService(String apiKey, String modelName, String baseUrl,
                        String systemPrompt, int maxIters) {
        this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
        this.maxIters = maxIters > 0 ? maxIters : DEFAULT_MAX_ITERS;
        this.sharedToolkit = new Toolkit();

        this.chatModel = OpenAIChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .baseUrl(baseUrl)
            .build();

        logger.info("AgentService initialized with AgentScope ReActAgent, model={}, baseUrl={}, maxIters={}",
            modelName, baseUrl, this.maxIters);
    }

    /**
     * Processes a user message through the AgentScope ReAct pipeline with session memory.
     *
     * <p>AgentScope ReAct pipeline:</p>
     * <ol>
     *   <li>Get or create per-session ReActAgent (with InMemoryMemory)</li>
     *   <li>Build user message as AgentScope Msg</li>
     *   <li>Agent executes ReAct loop (Reasoning → Acting → Reasoning...)</li>
     *   <li>Memory automatically maintained by AgentScope</li>
     *   <li>Return agent response text</li>
     * </ol>
     *
     * @param sessionId Session identifier for per-session agent management
     * @param userText User input text
     * @return Agent response text
     * @throws Exception if agent interaction fails
     */
    public String chat(String sessionId, String userText) throws Exception {
        logger.debug("AgentScope processing message for session {}: {}", sessionId, userText);

        // Get or create per-session ReActAgent
        ReActAgent agent = sessionAgents.computeIfAbsent(sessionId, this::createAgent);

        // Build AgentScope message
        Msg userMsg = Msg.builder()
            .textContent(userText)
            .build();

        // Execute AgentScope ReAct loop (blocking for synchronous orchestration)
        Msg response = agent.call(userMsg).block();

        String responseText = response != null ? response.getTextContent() : "";

        logger.debug("AgentScope response for session {}: {}", sessionId,
            responseText != null && responseText.length() > 100
                ? responseText.substring(0, 100) + "..." : responseText);

        return responseText != null ? responseText : "";
    }

    /**
     * Registers a tool object with the shared toolkit.
     * Tool methods should be annotated with {@code @Tool} and {@code @ToolParam}
     * following AgentScope's annotation-based tool registration.
     *
     * <p>Example:</p>
     * <pre>
     * public class MyTools {
     *     &#64;Tool(name = "search", description = "Search knowledge base")
     *     public String search(&#64;ToolParam(name = "query", description = "Search query") String query) {
     *         return "Search results for: " + query;
     *     }
     * }
     * agentService.registerToolObject(new MyTools());
     * </pre>
     *
     * @param toolObject Object containing @Tool annotated methods
     */
    public void registerToolObject(Object toolObject) {
        sharedToolkit.registerTool(toolObject);
        logger.info("Tool object registered with AgentScope Toolkit: {}", toolObject.getClass().getSimpleName());
    }

    /**
     * Cleans up session resources, removing the per-session ReActAgent and its memory.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        sessionAgents.remove(sessionId);
        logger.info("AgentScope session cleared: {}", sessionId);
    }

    /**
     * Gets the conversation history for a session from AgentScope's Memory.
     *
     * @param sessionId Session identifier
     * @return List of AgentScope Msg objects in the session history
     */
    public List<Msg> getSessionHistory(String sessionId) {
        ReActAgent agent = sessionAgents.get(sessionId);
        if (agent != null) {
            Memory memory = agent.getMemory();
            if (memory != null) {
                return memory.getMessages();
            }
        }
        return List.of();
    }

    /**
     * Gets the AgentScope Toolkit for direct tool management.
     *
     * @return AgentScope Toolkit instance
     */
    public Toolkit getToolkit() {
        return sharedToolkit;
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
     * Gets the number of active sessions.
     *
     * @return Number of active session agents
     */
    public int getActiveSessionCount() {
        return sessionAgents.size();
    }

    /**
     * Creates a new per-session ReActAgent with AgentScope components.
     *
     * @param sessionId Session identifier (used for agent naming)
     * @return New ReActAgent instance with InMemoryMemory
     */
    private ReActAgent createAgent(String sessionId) {
        logger.info("Creating AgentScope ReActAgent for session: {}", sessionId);

        return ReActAgent.builder()
            .name("Skylark-" + sessionId)
            .sysPrompt(systemPrompt)
            .model(chatModel)
            .toolkit(sharedToolkit)
            .memory(new InMemoryMemory())
            .maxIters(maxIters)
            .build();
    }
}
