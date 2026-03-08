package org.skylark.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.OrchestrationService;
import org.skylark.infrastructure.adapter.webrtc.AgoraClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.LiveKitClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.strategy.AgoraChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.KurentoChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.LiveKitChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebRTCChannelStrategy;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebSocketChannelStrategy;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebRTCStrategyConfig
 * WebRTCStrategyConfig 单元测试
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WebRTCStrategyConfigTest {
    
    @Mock
    private KurentoClientAdapter kurentoClientAdapter;
    
    @Mock
    private LiveKitClientAdapter liveKitClientAdapter;
    
    @Mock
    private AgoraClientAdapter agoraClientAdapter;
    
    @Mock
    private OrchestrationService orchestrationService;
    
    private WebRTCStrategyConfig createConfig(String strategyName) throws Exception {
        WebRTCProperties properties = new WebRTCProperties();
        properties.setStrategy(strategyName);
        
        WebRTCStrategyConfig config = new WebRTCStrategyConfig();
        
        Field propsField = WebRTCStrategyConfig.class.getDeclaredField("webRTCProperties");
        propsField.setAccessible(true);
        propsField.set(config, properties);
        
        Field kurentoField = WebRTCStrategyConfig.class.getDeclaredField("kurentoClientAdapter");
        kurentoField.setAccessible(true);
        kurentoField.set(config, kurentoClientAdapter);
        
        Field liveKitField = WebRTCStrategyConfig.class.getDeclaredField("liveKitClientAdapter");
        liveKitField.setAccessible(true);
        liveKitField.set(config, liveKitClientAdapter);
        
        Field agoraField = WebRTCStrategyConfig.class.getDeclaredField("agoraClientAdapter");
        agoraField.setAccessible(true);
        agoraField.set(config, agoraClientAdapter);
        
        Field orchField = WebRTCStrategyConfig.class.getDeclaredField("orchestrationService");
        orchField.setAccessible(true);
        orchField.set(config, orchestrationService);
        
        return config;
    }
    
    @Test
    void testWebSocketStrategySelected() throws Exception {
        WebRTCStrategyConfig config = createConfig("websocket");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(WebSocketChannelStrategy.class, strategy);
        assertEquals("websocket", strategy.getStrategyName());
    }
    
    @Test
    void testKurentoStrategySelected() throws Exception {
        WebRTCStrategyConfig config = createConfig("kurento");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(KurentoChannelStrategy.class, strategy);
        assertEquals("kurento", strategy.getStrategyName());
    }
    
    @Test
    void testLiveKitStrategySelected() throws Exception {
        WebRTCStrategyConfig config = createConfig("livekit");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(LiveKitChannelStrategy.class, strategy);
        assertEquals("livekit", strategy.getStrategyName());
    }
    
    @Test
    void testAgoraStrategySelected() throws Exception {
        WebRTCStrategyConfig config = createConfig("agora");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(AgoraChannelStrategy.class, strategy);
        assertEquals("agora", strategy.getStrategyName());
    }
    
    @Test
    void testDefaultStrategyIsWebSocket() throws Exception {
        WebRTCStrategyConfig config = createConfig("unknown");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(WebSocketChannelStrategy.class, strategy);
    }
    
    @Test
    void testStrategyCaseInsensitive() throws Exception {
        WebRTCStrategyConfig config = createConfig("KURENTO");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(KurentoChannelStrategy.class, strategy);
    }
    
    @Test
    void testAgoraStrategyCaseInsensitive() throws Exception {
        WebRTCStrategyConfig config = createConfig("AGORA");
        
        WebRTCChannelStrategy strategy = config.webRTCChannelStrategy();
        
        assertNotNull(strategy);
        assertInstanceOf(AgoraChannelStrategy.class, strategy);
    }
}
