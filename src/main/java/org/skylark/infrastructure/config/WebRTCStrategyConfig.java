package org.skylark.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.LiveKitClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.strategy.KurentoChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.LiveKitChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebRTCChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebSocketChannelStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebRTC Strategy Configuration
 * WebRTC 策略配置
 * 
 * <p>Configures the active WebRTC channel strategy based on the
 * {@code webrtc.strategy} property. Supports: websocket, kurento, livekit.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Configuration
public class WebRTCStrategyConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCStrategyConfig.class);
    
    @Autowired
    private WebRTCProperties webRTCProperties;
    
    @Autowired
    private KurentoClientAdapter kurentoClientAdapter;
    
    @Autowired
    private LiveKitClientAdapter liveKitClientAdapter;
    
    /**
     * Creates the active WebRTC channel strategy bean based on configuration
     * 根据配置创建活动的 WebRTC 通道策略 Bean
     * 
     * @return Active WebRTCChannelStrategy implementation
     */
    @Bean
    public WebRTCChannelStrategy webRTCChannelStrategy() {
        String strategyName = webRTCProperties.getStrategy();
        logger.info("Configuring WebRTC channel strategy: {}", strategyName);
        
        WebRTCChannelStrategy strategy;
        
        switch (strategyName.toLowerCase()) {
            case "kurento":
                strategy = new KurentoChannelStrategy(kurentoClientAdapter);
                logger.info("✅ Kurento WebRTC strategy activated");
                break;
            case "livekit":
                strategy = new LiveKitChannelStrategy(liveKitClientAdapter);
                logger.info("✅ LiveKit WebRTC strategy activated");
                break;
            case "websocket":
            default:
                strategy = new WebSocketChannelStrategy();
                logger.info("✅ WebSocket WebRTC strategy activated");
                break;
        }
        
        return strategy;
    }
}
