package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackchannelFilter
 */
class BackchannelFilterTest {

    private BackchannelFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BackchannelFilter();
    }

    // --- Chinese backchannel tokens ---

    @Test
    void testIsBackchannel_Chinese_En() {
        assertTrue(filter.isBackchannel("嗯"));
    }

    @Test
    void testIsBackchannel_Chinese_EnEn() {
        assertTrue(filter.isBackchannel("嗯嗯"));
    }

    @Test
    void testIsBackchannel_Chinese_Oh() {
        assertTrue(filter.isBackchannel("哦"));
    }

    @Test
    void testIsBackchannel_Chinese_Ah() {
        assertTrue(filter.isBackchannel("啊"));
    }

    @Test
    void testIsBackchannel_Chinese_ShiDe() {
        assertTrue(filter.isBackchannel("是的"));
    }

    @Test
    void testIsBackchannel_Chinese_Dui() {
        assertTrue(filter.isBackchannel("对"));
    }

    @Test
    void testIsBackchannel_Chinese_Hao() {
        assertTrue(filter.isBackchannel("好"));
    }

    @Test
    void testIsBackchannel_Chinese_HaoDe() {
        assertTrue(filter.isBackchannel("好的"));
    }

    @Test
    void testIsBackchannel_Chinese_EnHeng() {
        assertTrue(filter.isBackchannel("嗯哼"));
    }

    @Test
    void testIsBackchannel_Chinese_OhOh() {
        assertTrue(filter.isBackchannel("哦哦"));
    }

    @Test
    void testIsBackchannel_Chinese_DuiDui() {
        assertTrue(filter.isBackchannel("对对"));
    }

    @Test
    void testIsBackchannel_Chinese_HaoHao() {
        assertTrue(filter.isBackchannel("好好"));
    }

    @Test
    void testIsBackchannel_Chinese_Shi() {
        assertTrue(filter.isBackchannel("是"));
    }

    @Test
    void testIsBackchannel_Chinese_Xing() {
        assertTrue(filter.isBackchannel("行"));
    }

    // --- English backchannel tokens ---

    @Test
    void testIsBackchannel_English_Ok() {
        assertTrue(filter.isBackchannel("ok"));
    }

    @Test
    void testIsBackchannel_English_UhHuh() {
        assertTrue(filter.isBackchannel("uh-huh"));
    }

    @Test
    void testIsBackchannel_English_Mm() {
        assertTrue(filter.isBackchannel("mm"));
    }

    @Test
    void testIsBackchannel_English_Hmm() {
        assertTrue(filter.isBackchannel("hmm"));
    }

    @Test
    void testIsBackchannel_English_Yeah() {
        assertTrue(filter.isBackchannel("yeah"));
    }

    @Test
    void testIsBackchannel_English_Yes() {
        assertTrue(filter.isBackchannel("yes"));
    }

    @Test
    void testIsBackchannel_English_Right() {
        assertTrue(filter.isBackchannel("right"));
    }

    // --- Normal text should NOT be filtered ---

    @Test
    void testIsBackchannel_NormalText_Chinese_Weather() {
        assertFalse(filter.isBackchannel("今天天气怎么样"));
    }

    @Test
    void testIsBackchannel_NormalText_Chinese_Flight() {
        assertFalse(filter.isBackchannel("帮我查一下航班"));
    }

    @Test
    void testIsBackchannel_NormalText_English_Weather() {
        assertFalse(filter.isBackchannel("what's the weather"));
    }

    // --- Case insensitivity ---

    @Test
    void testIsBackchannel_CaseInsensitive_OK_Uppercase() {
        assertTrue(filter.isBackchannel("OK"));
    }

    @Test
    void testIsBackchannel_CaseInsensitive_Yeah_Mixed() {
        assertTrue(filter.isBackchannel("Yeah"));
    }

    @Test
    void testIsBackchannel_CaseInsensitive_Hmm_Uppercase() {
        assertTrue(filter.isBackchannel("Hmm"));
    }

    // --- Null and empty ---

    @Test
    void testIsBackchannel_Null_ReturnsTrue() {
        assertTrue(filter.isBackchannel(null));
    }

    @Test
    void testIsBackchannel_EmptyString_ReturnsTrue() {
        assertTrue(filter.isBackchannel(""));
    }

    @Test
    void testIsBackchannel_WhitespaceOnly_ReturnsTrue() {
        assertTrue(filter.isBackchannel("   "));
    }

    // --- Leading/trailing whitespace is trimmed ---

    @Test
    void testIsBackchannel_WithSurroundingWhitespace_TrimsAndMatches() {
        assertTrue(filter.isBackchannel("  嗯  "));
    }

    // --- getBackchannelTokens returns an unmodifiable set ---

    @Test
    void testGetBackchannelTokens_IsUnmodifiable() {
        Set<String> tokens = filter.getBackchannelTokens();
        assertThrows(UnsupportedOperationException.class, () -> tokens.add("test"));
    }

    @Test
    void testGetBackchannelTokens_ContainsExpectedTokens() {
        Set<String> tokens = filter.getBackchannelTokens();
        assertTrue(tokens.contains("嗯"));
        assertTrue(tokens.contains("ok"));
        assertTrue(tokens.contains("hmm"));
        assertFalse(tokens.isEmpty());
    }
}
