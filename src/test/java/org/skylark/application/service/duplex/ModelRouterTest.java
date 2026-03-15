package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    // --- route() tests ---

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
        // Phase 1: even simple chat returns CASCADE
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
