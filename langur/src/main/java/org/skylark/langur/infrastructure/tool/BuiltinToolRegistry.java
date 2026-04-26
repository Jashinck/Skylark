package org.skylark.langur.infrastructure.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.skylark.langur.domain.model.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置工具注册表 - 管理所有可用工具的注册与查找
 */
@Component
@RequiredArgsConstructor
public class BuiltinToolRegistry {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public List<Tool> getBuiltinTools() {
        List<Tool> tools = new ArrayList<>();
        tools.add(new HttpCallTool(webClientBuilder.build(), objectMapper));
        return tools;
    }
}
