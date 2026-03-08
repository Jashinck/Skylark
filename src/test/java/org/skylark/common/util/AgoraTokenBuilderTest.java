package org.skylark.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgoraTokenBuilder
 * 声网 Token 生成器单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
class AgoraTokenBuilderTest {

    // Test credentials (not real — safe to use in tests)
    private static final String TEST_APP_ID = "970CA35de60c44645bbae8a215061b33";
    private static final String TEST_APP_CERT = "5CFd2fd1755d40ecb72977518be15d3b";

    @Test
    void testBuildTokenWithUserAccount_ReturnsNonEmpty() {
        String token = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testBuildTokenWithUserAccount_StartsWithVersion007() {
        String token = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600);

        assertTrue(token.startsWith("007"), "Token should start with version '007'");
    }

    @Test
    void testBuildTokenWithUserAccount_IsBase64AfterVersion() {
        String token = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600);

        // Remove "007" prefix and verify the rest is valid base64
        String base64Part = token.substring(3);
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(base64Part),
            "Token body after '007' prefix should be valid base64");
    }

    @Test
    void testBuildTokenWithUserAccount_DifferentChannelsProduceDifferentTokens() {
        String token1 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "channel-A", "user-123", 3600, 3600);
        String token2 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "channel-B", "user-123", 3600, 3600);

        assertNotEquals(token1, token2,
            "Tokens for different channels should be different");
    }

    @Test
    void testBuildTokenWithUserAccount_DifferentUsersProduceDifferentTokens() {
        String token1 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-A", 3600, 3600);
        String token2 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-B", 3600, 3600);

        assertNotEquals(token1, token2,
            "Tokens for different users should be different");
    }

    @Test
    void testBuildTokenWithUserAccount_EmptyAppId_Throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AgoraTokenBuilder.buildTokenWithUserAccount(
                "", TEST_APP_CERT, "test-channel", "user-123", 3600, 3600));
    }

    @Test
    void testBuildTokenWithUserAccount_NullAppId_Throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AgoraTokenBuilder.buildTokenWithUserAccount(
                null, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600));
    }

    @Test
    void testBuildTokenWithUserAccount_EmptyAppCertificate_Throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AgoraTokenBuilder.buildTokenWithUserAccount(
                TEST_APP_ID, "", "test-channel", "user-123", 3600, 3600));
    }

    @Test
    void testBuildTokenWithUserAccount_NullChannelName_Throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AgoraTokenBuilder.buildTokenWithUserAccount(
                TEST_APP_ID, TEST_APP_CERT, null, "user-123", 3600, 3600));
    }

    @Test
    void testBuildTokenWithUserAccount_EmptyChannelName_Works() {
        // Empty channel name is valid (for wildcard tokens)
        String token = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "", "user-123", 3600, 3600);

        assertNotNull(token);
        assertTrue(token.startsWith("007"));
    }

    @Test
    void testBuildTokenWithUserAccount_ConsistentLength() {
        // Tokens with similar inputs should have similar length (within margin for salt randomness)
        String token1 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600);
        String token2 = AgoraTokenBuilder.buildTokenWithUserAccount(
            TEST_APP_ID, TEST_APP_CERT, "test-channel", "user-123", 3600, 3600);

        // Tokens have the same structure, so length should be identical
        assertEquals(token1.length(), token2.length(),
            "Tokens with same inputs should have same length");
    }

    @Test
    void testHmacSha256_ProducesConsistentOutput() throws Exception {
        byte[] key = "test-key".getBytes();
        byte[] data = "test-data".getBytes();

        byte[] hash1 = AgoraTokenBuilder.hmacSha256(key, data);
        byte[] hash2 = AgoraTokenBuilder.hmacSha256(key, data);

        assertArrayEquals(hash1, hash2, "HMAC-SHA256 should be deterministic");
        assertEquals(32, hash1.length, "HMAC-SHA256 should produce 32 bytes");
    }
}
