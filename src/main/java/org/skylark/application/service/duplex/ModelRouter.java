package org.skylark.application.service.duplex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intelligent Model Router — routes between cascade and end-to-end models
 * 智能模型路由 —— 根据对话场景选择最佳模型
 *
 * <p>Routing strategy:
 * <ol>
 *   <li>Detect tool-calling requirements → cascade mode (AgentScope ReAct)</li>
 *   <li>Simple chat / emotional interaction → end-to-end mode (Moshi / GLM-4-Voice)</li>
 *   <li>User preference → follow user setting</li>
 * </ol></p>
 *
 * <p>Phase 1: Always returns CASCADE (current behavior).
 * Phase 3: Adds END_TO_END routing with Moshi/GLM-4-Voice integration.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class ModelRouter {

    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);

    /**
     * Model type for routing decisions
     */
    public enum ModelType {
        /** Cascade mode: ASR → LLM (AgentScope ReAct) → TTS */
        CASCADE,
        /** End-to-end mode: Audio → Moshi/GLM-4-Voice → Audio */
        END_TO_END
    }

    // Phase 3: end-to-end model adapters
    // private final MoshiAdapter moshiAdapter;
    // private final GLM4VoiceAdapter glm4VoiceAdapter;

    /**
     * Tool-calling indicator keywords
     */
    private static final String[] TOOL_CALLING_INDICATORS = {
            "查询", "搜索", "计算", "查找", "设置", "修改", "删除", "创建",
            "query", "search", "calculate", "find", "set", "modify", "delete", "create",
            "帮我", "请问", "怎么", "如何"
    };

    public ModelRouter() {
        logger.info("ModelRouter initialized (Phase 1: cascade-only mode)");
    }

    /**
     * Route to the best model based on context
     * 根据上下文路由到最佳模型
     *
     * @param sessionId session identifier
     * @param context conversation context or user text
     * @return model type to use
     */
    public ModelType route(String sessionId, String context) {
        // Phase 1: Always use cascade mode (existing AgentScope pipeline)
        // Phase 3: Add heuristic routing based on context analysis

        if (context != null && requiresToolCalling(context)) {
            logger.debug("Session {} routed to CASCADE (tool-calling detected)", sessionId);
            return ModelType.CASCADE;
        }

        // Phase 1: Default to CASCADE even for simple chat
        // Phase 3: Route simple conversations to END_TO_END
        return ModelType.CASCADE;
    }

    /**
     * Check if the context requires tool calling
     * 检查上下文是否需要工具调用
     */
    boolean requiresToolCalling(String context) {
        if (context == null || context.isEmpty()) {
            return false;
        }

        String lowerContext = context.toLowerCase();
        for (String indicator : TOOL_CALLING_INDICATORS) {
            if (lowerContext.contains(indicator.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
