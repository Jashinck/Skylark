package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for SDP answer
 * SDP answer 的响应 DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class SdpAnswerResponse {
    
    @JsonProperty("sdpAnswer")
    private String sdpAnswer;
    
    public SdpAnswerResponse() {
    }
    
    public SdpAnswerResponse(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }
    
    public String getSdpAnswer() {
        return sdpAnswer;
    }
    
    public void setSdpAnswer(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }
}
