package org.skylark.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.domain.model.Tool;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolRegistry
 */
class ToolRegistryTest {

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
    }

    private Tool createTool(String name, String description) {
        return new Tool() {
            @Override
            public String getName() { return name; }

            @Override
            public String getDescription() { return description; }

            @Override
            public String execute(Map<String, Object> parameters) {
                return "result-" + name;
            }
        };
    }

    @Test
    void testRegisterAndRetrieve() {
        Tool tool = createTool("test_tool", "A test tool");
        toolRegistry.register(tool);

        Optional<Tool> result = toolRegistry.getTool("test_tool");
        assertTrue(result.isPresent());
        assertEquals("test_tool", result.get().getName());
    }

    @Test
    void testGetToolNotFound() {
        Optional<Tool> result = toolRegistry.getTool("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testHasTools() {
        assertFalse(toolRegistry.hasTools());
        toolRegistry.register(createTool("t1", "desc"));
        assertTrue(toolRegistry.hasTools());
    }

    @Test
    void testGetAll() {
        toolRegistry.register(createTool("tool_a", "desc a"));
        toolRegistry.register(createTool("tool_b", "desc b"));
        assertEquals(2, toolRegistry.getAll().size());
    }

    @Test
    void testGetToolDescriptions_empty() {
        assertEquals("", toolRegistry.getToolDescriptions());
    }

    @Test
    void testGetToolDescriptions_withTools() {
        toolRegistry.register(createTool("my_tool", "Does something"));
        String desc = toolRegistry.getToolDescriptions();
        assertTrue(desc.contains("my_tool"));
        assertTrue(desc.contains("Does something"));
    }

    @Test
    void testRegisterNullToolThrows() {
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.register(null));
    }

    @Test
    void testRegisterOverwritesExisting() {
        toolRegistry.register(createTool("dup", "first"));
        toolRegistry.register(createTool("dup", "second"));
        assertEquals(1, toolRegistry.getAll().size());
        assertEquals("second", toolRegistry.getTool("dup").get().getDescription());
    }
}
