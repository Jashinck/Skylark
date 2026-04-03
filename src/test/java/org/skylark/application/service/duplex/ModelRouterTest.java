package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.infrastructure.adapter.multimodal.QwenAudioAdapter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModelRouter
 */
class ModelRouterTest {

    private ModelRouter modelRouter;

    @BeforeEach
    void setUp() {
        modelRouter = new ModelRouter();
    }

    // --- route() tests (cascade-only default mode) ---

    @Test
    void testRoute_ChineseToolCallingKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "请帮我查询天气"));
    }

    @Test
    void testRoute_ChineseSearchKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "搜索最新新闻"));
    }

    @Test
    void testRoute_ChineseCalculateKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "计算100加200"));
    }

    @Test
    void testRoute_ChineseHelpKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "帮我做一件事"));
    }

    @Test
    void testRoute_ChineseHowKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "怎么做这个"));
    }

    @Test
    void testRoute_ChineseHowToKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "如何学习编程"));
    }

    @Test
    void testRoute_EnglishQueryKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "query the database"));
    }

    @Test
    void testRoute_EnglishSearchKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "please search for files"));
    }

    @Test
    void testRoute_EnglishCalculateKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "calculate the total"));
    }

    @Test
    void testRoute_EnglishCreateKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "create a new project"));
    }

    @Test
    void testRoute_EnglishDeleteKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "delete the old records"));
    }

    @Test
    void testRoute_EnglishModifyKeyword_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "modify the settings"));
    }

    @Test
    void testRoute_NullContext_ReturnsCascade() {
        // Phase 1 default: always CASCADE
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", null));
    }

    @Test
    void testRoute_EmptyContext_ReturnsCascade() {
        // Phase 1 default: always CASCADE
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", ""));
    }

    @Test
    void testRoute_SimpleChat_ReturnsCascade() {
        // Phase 1: even simple chat returns CASCADE (end-to-end not enabled)
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "hello"));
    }

    @Test
    void testRoute_CaseInsensitive_ReturnsCascade() {
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "SEARCH for items"));
        assertEquals(ModelRouter.ModelType.CASCADE, modelRouter.route("s1", "Find something"));
    }

    // --- requiresToolCalling() tests ---

    @Test
    void testRequiresToolCalling_WithKeyword_ReturnsTrue() {
        assertTrue(modelRouter.requiresToolCalling("查询数据"));
        assertTrue(modelRouter.requiresToolCalling("search files"));
        assertTrue(modelRouter.requiresToolCalling("请问这是什么"));
    }

    @Test
    void testRequiresToolCalling_WithoutKeyword_ReturnsFalse() {
        assertFalse(modelRouter.requiresToolCalling("hello world"));
        assertFalse(modelRouter.requiresToolCalling("你好"));
        assertFalse(modelRouter.requiresToolCalling("thanks"));
    }

    @Test
    void testRequiresToolCalling_NullInput_ReturnsFalse() {
        assertFalse(modelRouter.requiresToolCalling(null));
    }

    @Test
    void testRequiresToolCalling_EmptyInput_ReturnsFalse() {
        assertFalse(modelRouter.requiresToolCalling(""));
    }

    // --- isSimpleChat() tests ---

    @Test
    void testIsSimpleChat_ChineseGreeting_ReturnsTrue() {
        assertTrue(modelRouter.isSimpleChat("你好"));
    }

    @Test
    void testIsSimpleChat_EnglishGreeting_ReturnsTrue() {
        assertTrue(modelRouter.isSimpleChat("hello"));
    }

    @Test
    void testIsSimpleChat_ShortUtterance_ReturnsTrue() {
        assertTrue(modelRouter.isSimpleChat("ok")); // ≤ 10 chars
        assertTrue(modelRouter.isSimpleChat("嗯嗯好的")); // ≤ 10 chars
    }

    @Test
    void testIsSimpleChat_LongComplexText_ReturnsFalse() {
        assertFalse(modelRouter.isSimpleChat("请帮我分析这份技术方案的优缺点并给出专业建议"));
    }

    @Test
    void testIsSimpleChat_NullContext_ReturnsTrue() {
        assertTrue(modelRouter.isSimpleChat(null));
    }

    @Test
    void testIsSimpleChat_EmptyContext_ReturnsTrue() {
        assertTrue(modelRouter.isSimpleChat(""));
    }

    // --- isEndToEndEnabled() tests ---

    @Test
    void testIsEndToEndEnabled_DefaultConstructor_ReturnsFalse() {
        assertFalse(modelRouter.isEndToEndEnabled());
    }

    @Test
    void testIsEndToEndEnabled_WithFlag_ReturnsTrue() {
        ModelRouter router = new ModelRouter(true, null);
        assertTrue(router.isEndToEndEnabled());
    }

    @Test
    void testIsEndToEndEnabled_WithFlagFalse_ReturnsFalse() {
        ModelRouter router = new ModelRouter(false, null);
        assertFalse(router.isEndToEndEnabled());
    }

    // --- isEndToEndAvailable() tests ---

    @Test
    void testIsEndToEndAvailable_NullAdapter_ReturnsFalse() {
        ModelRouter router = new ModelRouter(true, null);
        assertFalse(router.isEndToEndAvailable());
    }

    @Test
    void testIsEndToEndAvailable_PlaceholderAdapter_ReturnsFalse() {
        // QwenAudioAdapter is a Phase 3 placeholder — isAvailable() always false
        QwenAudioAdapter adapter = new QwenAudioAdapter();
        ModelRouter router = new ModelRouter(true, adapter);
        assertFalse(router.isEndToEndAvailable());
    }

    // --- END_TO_END routing (requires available adapter) ---

    @Test
    void testRoute_EndToEndEnabled_SimpleChat_ButAdapterUnavailable_ReturnsCascade() {
        // Even if end-to-end is enabled, if model is unavailable → CASCADE
        QwenAudioAdapter unavailableAdapter = new QwenAudioAdapter();
        ModelRouter router = new ModelRouter(true, unavailableAdapter);

        assertEquals(ModelRouter.ModelType.CASCADE, router.route("s1", "hello"));
    }

    @Test
    void testRoute_EndToEndEnabled_ToolCallingContext_AlwaysCascade() {
        QwenAudioAdapter adapter = new QwenAudioAdapter();
        ModelRouter router = new ModelRouter(true, adapter);

        // Tool-calling always → CASCADE regardless of end-to-end config
        assertEquals(ModelRouter.ModelType.CASCADE, router.route("s1", "查询天气"));
    }

    // --- ModelType enum tests ---

    @Test
    void testModelType_AllValuesExist() {
        ModelRouter.ModelType[] types = ModelRouter.ModelType.values();
        assertEquals(2, types.length);
    }

    @Test
    void testModelType_ValueOf() {
        assertEquals(ModelRouter.ModelType.CASCADE, ModelRouter.ModelType.valueOf("CASCADE"));
        assertEquals(ModelRouter.ModelType.END_TO_END, ModelRouter.ModelType.valueOf("END_TO_END"));
    }
}
