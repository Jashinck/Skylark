package org.skylark.application.service.tool;

import org.skylark.domain.model.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Knowledge Query Tool for Tech-Training Domain
 * 技术培训知识查询工具
 *
 * <p>Example {@link Tool} implementation for the AI tech-training instructor
 * use-case. Given a topic keyword, the tool returns a concise knowledge-base
 * summary that the agent can weave into its answer.</p>
 *
 * <p>In a production deployment this would call an actual knowledge-base API
 * or vector-search service. The built-in responses serve as sensible defaults
 * when no external service is available.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class KnowledgeQueryTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeQueryTool.class);

    private static final Map<String, String> KNOWLEDGE_BASE = Map.ofEntries(
        Map.entry("java",
            "Java是一种面向对象的编程语言，具有平台无关性（JVM）、强类型系统和丰富的生态。" +
            "核心概念包括：类与对象、继承与多态、接口、泛型、Stream API、并发编程等。"),
        Map.entry("spring",
            "Spring Framework是Java企业级开发的主流框架，提供IoC/DI容器、AOP、" +
            "MVC、Security、Data等模块。Spring Boot简化了配置，支持自动装配。"),
        Map.entry("ai",
            "人工智能涵盖机器学习、深度学习、自然语言处理等领域。" +
            "常用框架：TensorFlow、PyTorch；常见任务：分类、回归、生成、推理。"),
        Map.entry("microservices",
            "微服务架构将单体应用拆分为独立可部署的小服务，每个服务负责单一业务能力。" +
            "关键技术：服务注册与发现、API网关、配置中心、链路追踪、熔断降级。"),
        Map.entry("docker",
            "Docker是容器化平台，通过镜像打包应用及其依赖，实现环境一致性。" +
            "核心概念：镜像(Image)、容器(Container)、仓库(Registry)、Dockerfile、docker-compose。")
    );

    @Override
    public String getName() {
        return "knowledge_query";
    }

    @Override
    public String getDescription() {
        return "查询技术培训知识库，获取指定主题的核心概念说明。" +
               "参数：topic（主题关键词，如 java/spring/ai/microservices/docker）";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        if (parameters == null || !parameters.containsKey("topic")) {
            logger.warn("KnowledgeQueryTool called without 'topic' parameter");
            return "请提供查询主题（topic）参数。";
        }

        String topic = String.valueOf(parameters.get("topic")).toLowerCase().trim();
        logger.info("Knowledge query for topic: {}", topic);

        // Exact match
        String result = KNOWLEDGE_BASE.get(topic);
        if (result != null) {
            return result;
        }

        // Partial match
        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (topic.contains(entry.getKey()) || entry.getKey().contains(topic)) {
                return entry.getValue();
            }
        }

        return String.format("暂未找到关于\"%s\"的知识库条目，请尝试：java、spring、ai、microservices、docker。", topic);
    }
}
