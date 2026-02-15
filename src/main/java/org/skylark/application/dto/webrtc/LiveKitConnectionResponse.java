package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for LiveKit WebRTC session connection
 * LiveKit WebRTC 会话连接的响应 DTO
 * 
 * <p>Contains the LiveKit access token and server URL needed
 * for client-side connection via LiveKit JS SDK.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class LiveKitConnectionResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    public LiveKitConnectionResponse() {
    }
    
    public LiveKitConnectionResponse(String sessionId, String token, String url, String status, String message) {
        this.sessionId = sessionId;
        this.token = token;
        this.url = url;
        this.status = status;
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
