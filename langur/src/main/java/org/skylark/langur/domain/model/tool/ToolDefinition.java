package org.skylark.langur.domain.model.tool;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ToolDefinition {
    private final String name;
    private final String description;
    private final Map<String, Object> parametersSchema;

    public static ToolDefinition of(String name, String description, Map<String, Object> schema) {
        return ToolDefinition.builder()
                .name(name)
                .description(description)
                .parametersSchema(schema)
                .build();
    }
}
