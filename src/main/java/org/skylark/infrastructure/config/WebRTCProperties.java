package org.skylark.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebRTC Configuration Properties
 * WebRTC 配置属性
 * 
 * <p>Type-safe configuration properties for WebRTC, including strategy selection,
 * Kurento, LiveKit, and STUN/TURN server settings.</p>
 * 
 * <p>Supports three pluggable strategies: websocket, kurento, livekit.
 * Use the {@code webrtc.strategy} property to select the active strategy.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "webrtc")
public class WebRTCProperties {
    
    /**
     * Active WebRTC channel strategy: websocket, kurento, or livekit
     * 活动的 WebRTC 通道策略：websocket、kurento 或 livekit
     */
    private String strategy = "websocket";
    
    private final Kurento kurento = new Kurento();
    private final LiveKit livekit = new LiveKit();
    private final Stun stun = new Stun();
    private final Turn turn = new Turn();
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public Kurento getKurento() {
        return kurento;
    }
    
    public LiveKit getLivekit() {
        return livekit;
    }
    
    public Stun getStun() {
        return stun;
    }
    
    public Turn getTurn() {
        return turn;
    }
    
    /**
     * Kurento configuration
     * Kurento 配置
     */
    public static class Kurento {
        private String wsUri = "ws://localhost:8888/kurento";
        
        public String getWsUri() {
            return wsUri;
        }
        
        public void setWsUri(String wsUri) {
            this.wsUri = wsUri;
        }
    }
    
    /**
     * LiveKit configuration
     * LiveKit 配置
     */
    public static class LiveKit {
        private String url = "";
        private String apiKey = "";
        private String apiSecret = "";
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getApiSecret() {
            return apiSecret;
        }
        
        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
    }
    
    /**
     * STUN server configuration
     * STUN 服务器配置
     */
    public static class Stun {
        private String server = "stun:stun.l.google.com:19302";
        
        public String getServer() {
            return server;
        }
        
        public void setServer(String server) {
            this.server = server;
        }
    }
    
    /**
     * TURN server configuration
     * TURN 服务器配置
     */
    public static class Turn {
        private boolean enabled = false;
        private String server = "";
        private String username = "";
        private String password = "";
        private String transport = "udp";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getServer() {
            return server;
        }
        
        public void setServer(String server) {
            this.server = server;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getTransport() {
            return transport;
        }
        
        public void setTransport(String transport) {
            this.transport = transport;
        }
        
        /**
         * Gets the full TURN URL with credentials
         * 获取带凭证的完整 TURN URL
         * 
         * @return TURN URL string in format: turn:server?transport=transport
         */
        public String getTurnUrl() {
            if (server == null || server.trim().isEmpty()) {
                return null;
            }
            
            String url = server;
            if (!url.startsWith("turn:")) {
                url = "turn:" + url;
            }
            
            if (transport != null && !transport.trim().isEmpty()) {
                url += "?transport=" + transport;
            }
            
            return url;
        }
    }
}
