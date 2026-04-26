package org.skylark.langur.domain.model.tool;

import java.util.Map;

/**
 * Tool实体 - 代表Agent可调用的工具
 */
public abstract class Tool {
    private final ToolDefinition definition;

    protected Tool(ToolDefinition definition) {
        this.definition = definition;
    }

    public String getName() {
        return definition.getName();
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public ToolDefinition getDefinition() {
        return definition;
    }

    /**
     * 执行工具
     * @param parameters 工具参数
     * @return 执行结果
     */
    public abstract ToolResult execute(Map<String, Object> parameters);
}
