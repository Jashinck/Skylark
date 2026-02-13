package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for WebRTC session creation
 * WebRTC 会话创建的响应 DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class WebRTCSessionResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    public WebRTCSessionResponse() {
    }
    
    public WebRTCSessionResponse(String sessionId, String status, String message) {
        this.sessionId = sessionId;
        this.status = status;
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
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
