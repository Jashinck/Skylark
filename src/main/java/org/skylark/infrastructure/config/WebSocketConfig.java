package org.skylark.infrastructure.config;

import org.skylark.infrastructure.websocket.WebRTCSignalingHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration for WebRTC Signaling
 * WebRTC信令的WebSocket配置
 * 
 * <p>Configures WebSocket endpoints for WebRTC signaling and audio streaming.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebRTCSignalingHandler webRTCSignalingHandler;

    public WebSocketConfig(WebRTCSignalingHandler webRTCSignalingHandler) {
        this.webRTCSignalingHandler = webRTCSignalingHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebRTC signaling endpoint
        // TODO: In production, restrict CORS to specific origins for security
        // Consider making this configurable via application.properties
        registry.addHandler(webRTCSignalingHandler, "/ws/webrtc")
                .setAllowedOrigins("*");  // WARNING: This allows all origins - configure for production!
    }
}
