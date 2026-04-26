package org.skylark.langur.domain.model.tool;

import lombok.Getter;

@Getter
public class ToolResult {
    private final boolean success;
    private final String content;
    private final String error;

    private ToolResult(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }

    public static ToolResult success(String content) {
        return new ToolResult(true, content, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error);
    }

    public String getEffectiveContent() {
        return success ? content : "Error: " + error;
    }
}
