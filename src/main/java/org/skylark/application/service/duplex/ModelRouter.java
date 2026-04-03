package org.skylark.application.service.duplex;

import org.skylark.infrastructure.adapter.multimodal.QwenAudioAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intelligent Model Router — routes between cascade and end-to-end models
 * 智能模型路由 —— 根据对话场景选择最佳模型
 *
 * <p>Routing strategy:
 * <ol>
 *   <li>Detect tool-calling requirements → cascade mode (AgentScope ReAct)</li>
 *   <li>Simple chat / emotional interaction → end-to-end mode (Qwen2-Audio / Moshi / GLM-4-Voice)</li>
 *   <li>User preference → follow user setting</li>
 * </ol></p>
 *
 * <p>Phase 1: Always returns CASCADE (existing behavior, half/barge-in modes).
 * Phase 2/3: Activates END_TO_END routing for simple-chat in streaming/full modes
 * when an end-to-end model adapter (Qwen2-Audio / Moshi / GLM-4-Voice) is available.</p>
 *
 * <p>Priority matrix from DUPLEX_COMPARISON_ANALYSIS.md:
 * <pre>
 *   [C1] Qwen2-Audio  — ⭐⭐⭐ high priority, open-source, Chinese-optimized
 *   [C2] GLM-4-Voice  — ⭐⭐⭐ high priority, open-source, Chinese dialog
 *   [Moshi]           — ⭐⭐  medium, full-duplex native
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.1.0
 */
public class ModelRouter {

    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);

    /**
     * Model type for routing decisions
     */
    public enum ModelType {
        /** Cascade mode: ASR → LLM (AgentScope ReAct) → TTS */
        CASCADE,
        /** End-to-end mode: Audio → Qwen2-Audio / Moshi / GLM-4-Voice → Audio */
        END_TO_END
    }

    /**
     * Tool-calling indicator keywords
     */
    private static final String[] TOOL_CALLING_INDICATORS = {
            "查询", "搜索", "计算", "查找", "设置", "修改", "删除", "创建",
            "query", "search", "calculate", "find", "set", "modify", "delete", "create",
            "帮我", "请问", "怎么", "如何"
    };

    /**
     * Simple-chat patterns that benefit from end-to-end low-latency processing.
     * 适合端到端处理的简单对话模式（无需工具调用）
     */
    private static final String[] SIMPLE_CHAT_PATTERNS = {
            "你好", "嗯", "对", "好的", "谢谢", "再见", "哈哈", "厉害",
            "hello", "hi", "yes", "no", "ok", "thanks", "bye", "cool"
    };

    /** Whether end-to-end routing is enabled (Phase 3: requires model adapter) */
    private final boolean endToEndEnabled;

    /** Phase 3: Qwen2-Audio adapter for end-to-end processing */
    private final QwenAudioAdapter qwenAudioAdapter;

    /**
     * Default constructor — cascade-only mode (Phase 1 behavior).
     */
    public ModelRouter() {
        this(false, null);
    }

    /**
     * Creates a ModelRouter with optional end-to-end routing support.
     *
     * @param endToEndEnabled  whether to enable END_TO_END routing for simple chat
     * @param qwenAudioAdapter Qwen2-Audio adapter (null if not available)
     */
    public ModelRouter(boolean endToEndEnabled, QwenAudioAdapter qwenAudioAdapter) {
        this.endToEndEnabled = endToEndEnabled;
        this.qwenAudioAdapter = qwenAudioAdapter;

        if (endToEndEnabled && qwenAudioAdapter != null) {
            logger.info("ModelRouter initialized: END_TO_END routing ENABLED (Qwen2-Audio)");
        } else {
            logger.info("ModelRouter initialized: cascade-only mode");
        }
    }

    /**
     * Route to the best model based on context and duplex mode.
     * 根据上下文和全双工模式路由到最佳模型
     *
     * <p>Routing logic:
     * <ol>
     *   <li>Tool-calling detected → always CASCADE (AgentScope ReAct required)</li>
     *   <li>End-to-end enabled + model available + simple chat → END_TO_END</li>
     *   <li>Default → CASCADE</li>
     * </ol></p>
     *
     * @param sessionId session identifier
     * @param context   conversation context or user text
     * @return model type to use
     */
    public ModelType route(String sessionId, String context) {
        if (context != null && requiresToolCalling(context)) {
            logger.debug("Session {} → CASCADE (tool-calling detected)", sessionId);
            return ModelType.CASCADE;
        }

        if (endToEndEnabled && isEndToEndAvailable() && isSimpleChat(context)) {
            logger.debug("Session {} → END_TO_END (simple chat, Qwen2-Audio available)", sessionId);
            return ModelType.END_TO_END;
        }

        logger.debug("Session {} → CASCADE (default)", sessionId);
        return ModelType.CASCADE;
    }

    /**
     * Check if the context requires tool calling.
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

    /**
     * Check if the context is a simple conversational exchange
     * that does not require tool calling or complex reasoning.
     * 检查是否为不需要工具调用的简单对话交互
     */
    boolean isSimpleChat(String context) {
        if (context == null || context.isEmpty()) {
            return true;
        }
        // Short utterances (≤10 chars) are typically simple chat
        if (context.trim().length() <= 10) {
            return true;
        }
        String lowerContext = context.toLowerCase();
        for (String pattern : SIMPLE_CHAT_PATTERNS) {
            if (lowerContext.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the end-to-end model adapter is ready.
     * 返回端到端模型适配器是否就绪
     */
    boolean isEndToEndAvailable() {
        return qwenAudioAdapter != null && qwenAudioAdapter.isAvailable();
    }

    /**
     * Returns whether end-to-end routing is configured.
     */
    public boolean isEndToEndEnabled() {
        return endToEndEnabled;
    }
}
