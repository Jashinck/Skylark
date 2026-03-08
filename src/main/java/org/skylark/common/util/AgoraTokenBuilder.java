package org.skylark.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.TreeMap;

/**
 * Agora RTC Token Builder (AccessToken2 / "007" format)
 * 声网 RTC Token 生成器（AccessToken2 / "007" 格式）
 *
 * <p>Pure Java implementation of Agora's RTC token generation algorithm.
 * Generates valid AccessToken2 tokens using HMAC-SHA256 without requiring
 * the Agora RTC Server SDK.</p>
 *
 * <p>Based on Agora's open-source token generation specification:
 * https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class AgoraTokenBuilder {

    private static final String VERSION = "007";
    private static final int SERVICE_TYPE_RTC = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // RTC privileges
    private static final int PRIVILEGE_JOIN_CHANNEL = 1;
    private static final int PRIVILEGE_PUBLISH_AUDIO = 2;
    private static final int PRIVILEGE_PUBLISH_VIDEO = 3;
    private static final int PRIVILEGE_PUBLISH_DATA = 4;

    private AgoraTokenBuilder() {
        // Utility class
    }

    /**
     * Builds an RTC Token for a user account (string UID)
     * 生成基于用户账号的 RTC Token
     *
     * @param appId           Agora App ID
     * @param appCertificate  Agora App Certificate
     * @param channelName     Channel name
     * @param account         User account (string UID)
     * @param tokenExpireSeconds  Token expiration in seconds
     * @param privilegeExpireSeconds  Privilege expiration in seconds
     * @return Generated RTC token string
     */
    public static String buildTokenWithUserAccount(String appId, String appCertificate,
                                                    String channelName, String account,
                                                    int tokenExpireSeconds, int privilegeExpireSeconds) {
        if (appId == null || appId.isEmpty()) {
            throw new IllegalArgumentException("appId must not be null or empty");
        }
        if (appCertificate == null || appCertificate.isEmpty()) {
            throw new IllegalArgumentException("appCertificate must not be null or empty");
        }
        if (channelName == null) {
            throw new IllegalArgumentException("channelName must not be null");
        }

        int now = (int) (System.currentTimeMillis() / 1000);
        int expire = now + tokenExpireSeconds;
        int salt = SECURE_RANDOM.nextInt();
        int ts = now;

        int privilegeExpire = privilegeExpireSeconds > 0 ? now + privilegeExpireSeconds : 0;

        // Build RTC service with privileges
        TreeMap<Short, Integer> privileges = new TreeMap<>();
        privileges.put((short) PRIVILEGE_JOIN_CHANNEL, privilegeExpire);
        privileges.put((short) PRIVILEGE_PUBLISH_AUDIO, privilegeExpire);
        privileges.put((short) PRIVILEGE_PUBLISH_VIDEO, privilegeExpire);
        privileges.put((short) PRIVILEGE_PUBLISH_DATA, privilegeExpire);

        try {
            return generateAccessToken2(appId, appCertificate, channelName, account,
                expire, salt, ts, privileges);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Agora RTC token", e);
        }
    }

    private static String generateAccessToken2(String appId, String appCertificate,
                                                String channelName, String account,
                                                int expire, int salt, int ts,
                                                TreeMap<Short, Integer> privileges) throws Exception {
        // Build the message to sign
        ByteArrayOutputStream signing = new ByteArrayOutputStream();
        writeString(signing, channelName);
        writeInt(signing, 0); // uid=0 for string account mode
        writeString(signing, account);
        byte[] signingContent = signing.toByteArray();

        // Generate signing key: HMAC-SHA256(appCertificate, salt_bytes)
        byte[] saltBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(salt).array();
        byte[] tsBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ts).array();

        byte[] signingKey = hmacSha256(appCertificate.getBytes(), saltBytes);

        // Sign the content
        byte[] signature = hmacSha256(signingKey, signingContent);

        // Build the service
        ByteArrayOutputStream serviceContent = new ByteArrayOutputStream();
        writeShort(serviceContent, (short) SERVICE_TYPE_RTC);
        writeMap(serviceContent, privileges);

        // Build the full content
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        writeBytes(content, signature);
        writeInt(content, salt);
        writeInt(content, ts);
        writeInt(content, expire);
        // services count = 1
        writeShort(content, (short) 1);
        content.write(serviceContent.toByteArray());

        // Final token: VERSION + base64(appId + content)
        ByteArrayOutputStream tokenContent = new ByteArrayOutputStream();
        writeString(tokenContent, appId);
        tokenContent.write(content.toByteArray());

        return VERSION + Base64.getEncoder().encodeToString(tokenContent.toByteArray());
    }

    // ========== Pack helpers (little-endian) ==========

    static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static void writeShort(ByteArrayOutputStream out, short value) throws IOException {
        out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) throws IOException {
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private static void writeString(ByteArrayOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes();
        writeShort(out, (short) bytes.length);
        out.write(bytes);
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] value) throws IOException {
        writeShort(out, (short) value.length);
        out.write(value);
    }

    private static void writeMap(ByteArrayOutputStream out, TreeMap<Short, Integer> map) throws IOException {
        writeShort(out, (short) map.size());
        for (var entry : map.entrySet()) {
            writeShort(out, entry.getKey());
            writeInt(out, entry.getValue());
        }
    }
}
