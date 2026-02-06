package org.skylark.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for WebRTC session start
 * WebRTC会话启动的响应DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class SessionStartResponse {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("websocket_url")
    private String websocketUrl;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    public SessionStartResponse() {
    }
    
    public SessionStartResponse(String sessionId, String websocketUrl, String status, String message) {
        this.sessionId = sessionId;
        this.websocketUrl = websocketUrl;
        this.status = status;
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getWebsocketUrl() {
        return websocketUrl;
    }
    
    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
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
