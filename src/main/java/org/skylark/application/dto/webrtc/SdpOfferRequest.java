package org.skylark.application.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for SDP offer
 * SDP offer 的请求 DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class SdpOfferRequest {
    
    @JsonProperty("sdpOffer")
    private String sdpOffer;
    
    public SdpOfferRequest() {
    }
    
    public SdpOfferRequest(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }
    
    public String getSdpOffer() {
        return sdpOffer;
    }
    
    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }
}
