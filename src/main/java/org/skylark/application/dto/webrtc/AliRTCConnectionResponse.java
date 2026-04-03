package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for AliRTC WebRTC session connection
 * 阿里云 ARTC WebRTC 会话连接的响应 DTO
 *
 * <p>Contains the AliRTC App ID, Channel ID, User ID, and Auth Info (JSON string)
 * needed for client-side connection via the AliRTC Web SDK.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class AliRTCConnectionResponse {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("appId")
    private String appId;

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("userId")
    private String userId;

    /** AuthInfo JSON string containing nonce, timestamp, and HMAC token */
    @JsonProperty("authInfo")
    private String authInfo;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    public AliRTCConnectionResponse() {
    }

    public AliRTCConnectionResponse(String sessionId, String appId, String channelId,
                                     String userId, String authInfo,
                                     String status, String message) {
        this.sessionId = sessionId;
        this.appId = appId;
        this.channelId = channelId;
        this.userId = userId;
        this.authInfo = authInfo;
        this.status = status;
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAuthInfo() { return authInfo; }
    public void setAuthInfo(String authInfo) { this.authInfo = authInfo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
