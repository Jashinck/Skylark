package org.skylark.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for starting a WebRTC session
 * 启动WebRTC会话的请求DTO
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class SessionStartRequest {
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("audio_config")
    private AudioConfig audioConfig;
    
    public SessionStartRequest() {
    }
    
    public SessionStartRequest(String clientId, AudioConfig audioConfig) {
        this.clientId = clientId;
        this.audioConfig = audioConfig;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public AudioConfig getAudioConfig() {
        return audioConfig;
    }
    
    public void setAudioConfig(AudioConfig audioConfig) {
        this.audioConfig = audioConfig;
    }
    
    /**
     * Audio configuration for the session
     */
    public static class AudioConfig {
        @JsonProperty("sample_rate")
        private Integer sampleRate = 16000;
        
        @JsonProperty("channels")
        private Integer channels = 1;
        
        @JsonProperty("bit_depth")
        private Integer bitDepth = 16;
        
        public AudioConfig() {
        }
        
        public Integer getSampleRate() {
            return sampleRate;
        }
        
        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }
        
        public Integer getChannels() {
            return channels;
        }
        
        public void setChannels(Integer channels) {
            this.channels = channels;
        }
        
        public Integer getBitDepth() {
            return bitDepth;
        }
        
        public void setBitDepth(Integer bitDepth) {
            this.bitDepth = bitDepth;
        }
    }
}
