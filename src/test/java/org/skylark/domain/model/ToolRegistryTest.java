package org.skylark.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolRegistry
 */
class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
    }

    @Test
    void testRegisterAndGetTool() {
        Tool tool = createTestTool("search", "Search knowledge base");
        registry.register(tool);

        Tool retrieved = registry.getTool("search");
        assertNotNull(retrieved);
        assertEquals("search", retrieved.getName());
        assertEquals("Search knowledge base", retrieved.getDescription());
    }

    @Test
    void testRegisterNullTool() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }

    @Test
    void testRegisterToolWithNullName() {
        Tool tool = new Tool() {
            @Override public String getName() { return null; }
            @Override public String getDescription() { return "desc"; }
            @Override public String execute(Map<String, Object> parameters) { return ""; }
        };
        assertThrows(IllegalArgumentException.class, () -> registry.register(tool));
    }

    @Test
    void testUnregisterTool() {
        Tool tool = createTestTool("search", "Search");
        registry.register(tool);

        Tool removed = registry.unregister("search");
        assertNotNull(removed);
        assertNull(registry.getTool("search"));
    }

    @Test
    void testUnregisterNonExistentTool() {
        Tool removed = registry.unregister("non-existent");
        assertNull(removed);
    }

    @Test
    void testHasTool() {
        assertFalse(registry.hasTool("search"));

        registry.register(createTestTool("search", "Search"));
        assertTrue(registry.hasTool("search"));
    }

    @Test
    void testGetAllTools() {
        registry.register(createTestTool("tool1", "Tool 1"));
        registry.register(createTestTool("tool2", "Tool 2"));

        Collection<Tool> allTools = registry.getAllTools();
        assertEquals(2, allTools.size());
    }

    @Test
    void testSize() {
        assertEquals(0, registry.size());
        registry.register(createTestTool("tool1", "Tool 1"));
        assertEquals(1, registry.size());
        registry.register(createTestTool("tool2", "Tool 2"));
        assertEquals(2, registry.size());
    }

    @Test
    void testBuildToolDescriptions_Empty() {
        assertEquals("", registry.buildToolDescriptions());
    }

    @Test
    void testBuildToolDescriptions_WithTools() {
        registry.register(createTestTool("search", "Search knowledge base"));
        registry.register(createTestTool("calculate", "Perform calculations"));

        String descriptions = registry.buildToolDescriptions();
        assertNotNull(descriptions);
        assertTrue(descriptions.contains("search"));
        assertTrue(descriptions.contains("Search knowledge base"));
        assertTrue(descriptions.contains("calculate"));
        assertTrue(descriptions.contains("Perform calculations"));
    }

    @Test
    void testToolExecution() {
        Tool tool = new Tool() {
            @Override public String getName() { return "greet"; }
            @Override public String getDescription() { return "Greet a user"; }
            @Override public String execute(Map<String, Object> parameters) {
                return "Hello, " + parameters.get("name") + "!";
            }
        };
        registry.register(tool);

        String result = registry.getTool("greet").execute(Map.of("name", "World"));
        assertEquals("Hello, World!", result);
    }

    private Tool createTestTool(String name, String description) {
        return new Tool() {
            @Override public String getName() { return name; }
            @Override public String getDescription() { return description; }
            @Override public String execute(Map<String, Object> parameters) { return "result"; }
        };
    }
}
