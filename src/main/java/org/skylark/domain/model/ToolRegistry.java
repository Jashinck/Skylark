package org.skylark.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool Registry for Agent Tool Management
 * 智能体工具注册中心
 *
 * <p>Manages the registration and lookup of tools available to the agent.
 * Inspired by AgentScope's service module, this registry provides a
 * centralized place to register and discover tools.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

    /**
     * Registered tools, keyed by tool name.
     */
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * Registers a tool in the registry.
     *
     * @param tool Tool to register
     * @throws IllegalArgumentException if tool or its name is null
     */
    public void register(Tool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("Tool and tool name must not be null");
        }
        tools.put(tool.getName(), tool);
        logger.info("Registered tool: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * Unregisters a tool from the registry.
     *
     * @param toolName Name of the tool to unregister
     * @return The removed tool, or null if not found
     */
    public Tool unregister(String toolName) {
        Tool removed = tools.remove(toolName);
        if (removed != null) {
            logger.info("Unregistered tool: {}", toolName);
        }
        return removed;
    }

    /**
     * Gets a tool by name.
     *
     * @param toolName Name of the tool
     * @return The tool, or null if not found
     */
    public Tool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * Checks if a tool is registered.
     *
     * @param toolName Name of the tool
     * @return true if the tool is registered
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Gets all registered tools.
     *
     * @return Unmodifiable collection of all registered tools
     */
    public Collection<Tool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Gets the number of registered tools.
     *
     * @return Number of registered tools
     */
    public int size() {
        return tools.size();
    }

    /**
     * Builds a tool description string for inclusion in LLM system prompts.
     * This helps the LLM understand what tools are available.
     *
     * @return Formatted string describing all available tools
     */
    public String buildToolDescriptions() {
        if (tools.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\nAvailable tools:\n");
        for (Tool tool : tools.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
