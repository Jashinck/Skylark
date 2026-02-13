package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a WebRTC session
 * 创建 WebRTC 会话的请求 DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class CreateSessionRequest {
    
    @JsonProperty("userId")
    private String userId;
    
    public CreateSessionRequest() {
    }
    
    public CreateSessionRequest(String userId) {
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
