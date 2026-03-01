package org.skylark.domain.model;

import java.util.Map;

/**
 * Tool Interface for Agent Capabilities
 * 智能体工具接口
 *
 * <p>Defines a callable tool that can be invoked by the agent during
 * conversation processing. Inspired by AgentScope's service module,
 * tools enable the agent to perform specific actions beyond simple
 * text generation.</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Knowledge base retrieval for technical training</li>
 *   <li>Course progress tracking</li>
 *   <li>External API calls for real-time data</li>
 * </ul>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public interface Tool {

    /**
     * Gets the unique name of this tool.
     *
     * @return Tool name used for identification and invocation
     */
    String getName();

    /**
     * Gets a human-readable description of what this tool does.
     *
     * @return Tool description for LLM context
     */
    String getDescription();

    /**
     * Executes the tool with the given parameters.
     *
     * @param parameters Map of parameter names to values
     * @return Result of the tool execution as a string
     */
    String execute(Map<String, Object> parameters);
}
