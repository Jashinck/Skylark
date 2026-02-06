package org.skylark.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for WebRTC session status
 * WebRTC会话状态的响应DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class SessionStatusResponse {
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("active")
    private Boolean active;
    
    @JsonProperty("message")
    private String message;
    
    public SessionStatusResponse() {
    }
    
    public SessionStatusResponse(String sessionId, String status, Boolean active, String message) {
        this.sessionId = sessionId;
        this.status = status;
        this.active = active;
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
