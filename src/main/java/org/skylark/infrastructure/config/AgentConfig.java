package org.skylark.infrastructure.config;

import org.skylark.application.service.AgentMemory;
import org.skylark.application.service.AgentService;
import org.skylark.application.service.ToolRegistry;
import org.skylark.application.service.tool.KnowledgeQueryTool;
import org.skylark.common.util.ConfigReader;
import org.skylark.infrastructure.adapter.LLM;
import org.skylark.infrastructure.adapter.OllamaLLM;
import org.skylark.infrastructure.adapter.OpenAILLM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent Configuration
 * 智能体配置
 *
 * <p>Spring {@code @Configuration} that creates and wires together all
 * Agent-related beans:</p>
 * <ul>
 *   <li>{@link LLM} – loaded from the Skylark YAML config file.</li>
 *   <li>{@link AgentMemory} – per-session conversation memory.</li>
 *   <li>{@link ToolRegistry} – extensible tool registry pre-loaded with
 *       domain-specific tools for the tech-training scenario.</li>
 *   <li>{@link AgentService} – the main agent, combining LLM + memory + tools.</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Configuration
public class AgentConfig {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);

    @Value("${skylark.config.path:config/config.yaml}")
    private String configPath;

    /**
     * Creates the LLM bean from the Skylark YAML configuration.
     * Supports OpenAILLM (default) and OllamaLLM adapter types.
     * If LLM creation fails (e.g., missing or incomplete config), a no-op
     * placeholder LLM is returned so the application can still start.
     *
     * @return Configured LLM instance, or a no-op placeholder on failure
     */
    @Bean
    public LLM llmAdapter() {
        Map<String, Object> config = ConfigReader.readConfig(configPath);

        @SuppressWarnings("unchecked")
        Map<String, Object> llmConfig = (Map<String, Object>) config.getOrDefault("llm", new HashMap<>());

        String className = (String) llmConfig.getOrDefault("class_name", "OpenAILLM");

        // Normalize YAML snake_case keys to camelCase expected by adapters
        Map<String, Object> normalizedConfig = new HashMap<>(llmConfig);
        if (llmConfig.containsKey("api_key")) {
            normalizedConfig.put("apiKey", llmConfig.get("api_key"));
        }
        if (llmConfig.containsKey("model_name")) {
            normalizedConfig.put("modelName", llmConfig.get("model_name"));
        }

        logger.info("Creating LLM adapter: {}", className);
        try {
            if ("OllamaLLM".equals(className)) {
                return new OllamaLLM(normalizedConfig);
            }
            return new OpenAILLM(normalizedConfig);
        } catch (Exception e) {
            logger.warn("Failed to create LLM from config ({}), using no-op placeholder: {}",
                    className, e.getMessage());
            return createNoOpLLM();
        }
    }

    /**
     * Creates the AgentMemory bean, reading the history directory from config.
     *
     * @return Configured AgentMemory instance
     */
    @Bean
    public AgentMemory agentMemory() {
        Map<String, Object> config = ConfigReader.readConfig(configPath);

        @SuppressWarnings("unchecked")
        Map<String, Object> memoryConfig = (Map<String, Object>) config.getOrDefault("memory", new HashMap<>());
        String historyDir = (String) memoryConfig.getOrDefault("path", "tmp/agent");
        logger.info("Creating AgentMemory with history dir: {}", historyDir);
        return new AgentMemory(historyDir);
    }

    /**
     * Creates the ToolRegistry bean and registers domain-specific tools.
     * For the tech-training scenario the {@link KnowledgeQueryTool} is pre-registered.
     *
     * @return Configured ToolRegistry with built-in tools
     */
    @Bean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new KnowledgeQueryTool());
        logger.info("ToolRegistry created with {} tools", registry.getAll().size());
        return registry;
    }

    /**
     * Creates the AgentService bean, wiring LLM + memory + tools and
     * setting the system prompt for the tech-training AI instructor persona.
     *
     * @param llm          LLM adapter bean
     * @param memory       AgentMemory bean
     * @param toolRegistry ToolRegistry bean
     * @return Configured AgentService
     */
    @Bean
    public AgentService agentService(LLM llm, AgentMemory memory, ToolRegistry toolRegistry) {
        Map<String, Object> config = ConfigReader.readConfig(configPath);

        @SuppressWarnings("unchecked")
        Map<String, Object> agentConfig = (Map<String, Object>) config.getOrDefault("agent", new HashMap<>());
        String systemPrompt = (String) agentConfig.get("system_prompt");

        AgentService agentService = new AgentService(llm, memory, toolRegistry);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            agentService.setSystemPrompt(systemPrompt);
            logger.info("AgentService system prompt configured");
        }

        logger.info("AgentService created successfully");
        return agentService;
    }

    /**
     * Creates a no-op LLM that logs a warning and returns an empty response.
     * Used as a safe fallback when LLM configuration is incomplete.
     */
    private LLM createNoOpLLM() {
        return (messages, onChunk, onComplete) -> {
            logger.warn("No-op LLM invoked – please configure a valid LLM in config.yaml");
            onChunk.accept("[LLM未配置，请在config.yaml中设置llm参数]");
            onComplete.run();
        };
    }
}
