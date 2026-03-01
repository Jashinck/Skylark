package org.skylark.domain.model;

import java.util.Map;

/**
 * Tool interface for Agent capabilities
 * Agent工具接口
 *
 * <p>Represents a callable tool that can be used by the agent to perform
 * specific tasks such as querying knowledge bases, executing calculations,
 * or interacting with external services.</p>
 *
 * <p>Inspired by AgentScope's tool abstraction, tools are registered in
 * {@code ToolRegistry} and injected into the system prompt so the LLM
 * knows which tools are available.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public interface Tool {

    /**
     * Returns the unique name of this tool.
     *
     * @return Tool name (used to identify and invoke the tool)
     */
    String getName();

    /**
     * Returns a human-readable description of what this tool does.
     * This description is included in the system prompt so the LLM
     * can decide when to use the tool.
     *
     * @return Tool description
     */
    String getDescription();

    /**
     * Executes the tool with the given parameters.
     *
     * @param parameters Map of parameter names to values
     * @return Result string from the tool execution
     */
    String execute(Map<String, Object> parameters);
}
