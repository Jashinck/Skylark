package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for ICE candidate
 * ICE candidate 的请求 DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class IceCandidateRequest {
    
    @JsonProperty("candidate")
    private String candidate;
    
    @JsonProperty("sdpMid")
    private String sdpMid;
    
    @JsonProperty("sdpMLineIndex")
    private int sdpMLineIndex;
    
    public IceCandidateRequest() {
    }
    
    public IceCandidateRequest(String candidate, String sdpMid, int sdpMLineIndex) {
        this.candidate = candidate;
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
    }
    
    public String getCandidate() {
        return candidate;
    }
    
    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }
    
    public String getSdpMid() {
        return sdpMid;
    }
    
    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }
    
    public int getSdpMLineIndex() {
        return sdpMLineIndex;
    }
    
    public void setSdpMLineIndex(int sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }
}
