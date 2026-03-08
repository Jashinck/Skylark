package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Agora WebRTC session connection
 * 声网 WebRTC 会话连接的响应 DTO
 * 
 * <p>Contains the Agora RTC Token, Channel Name, and App ID needed
 * for client-side connection via Agora Web SDK.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class AgoraConnectionResponse {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("appId")
    private String appId;

    @JsonProperty("channelName")
    private String channelName;

    @JsonProperty("token")
    private String token;

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    public AgoraConnectionResponse() {
    }

    public AgoraConnectionResponse(String sessionId, String appId, String channelName,
                                    String token, String uid, String status, String message) {
        this.sessionId = sessionId;
        this.appId = appId;
        this.channelName = channelName;
        this.token = token;
        this.uid = uid;
        this.status = status;
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
