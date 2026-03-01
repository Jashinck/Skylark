package org.skylark.application.service;

import org.skylark.domain.model.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tool Registry Service
 * 工具注册服务
 *
 * <p>Manages a registry of {@link Tool} instances available to the Agent.
 * Tools can be registered at startup (via {@link AgentConfig}) or
 * dynamically at runtime. The registry also generates tool descriptions
 * for injection into the LLM system prompt.</p>
 *
 * <p>Inspired by AgentScope's tool integration, the registry enables
 * vertical-domain agents (e.g., tech-training AI instructor) to call
 * domain-specific tools such as knowledge-base queries, course lookups,
 * and exercise generators.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /**
     * Registers a tool in the registry.
     * If a tool with the same name already exists it will be replaced.
     *
     * @param tool Tool to register
     */
    public void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        tools.put(tool.getName(), tool);
        logger.info("Registered tool: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * Returns the tool with the given name, if present.
     *
     * @param name Tool name
     * @return Optional containing the tool, or empty if not found
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Returns an unmodifiable view of all registered tools.
     *
     * @return Collection of all registered tools
     */
    public Collection<Tool> getAll() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Returns whether any tools are registered.
     *
     * @return true if at least one tool is registered
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /**
     * Generates a formatted description of all registered tools for
     * inclusion in the LLM system prompt.
     *
     * @return Formatted tool descriptions, or empty string if no tools registered
     */
    public String getToolDescriptions() {
        if (tools.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Available tools:\n");
        for (Tool tool : tools.values()) {
            sb.append("- ").append(tool.getName())
              .append(": ").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
