package org.skylark.infrastructure.adapter.webrtc;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import livekit.LivekitModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.config.WebRTCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LiveKit Client Adapter Implementation
 * LiveKit 客户端适配器实现
 * 
 * <p>Manages connection to LiveKit Server and provides operations
 * for room management and token generation.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Component
public class LiveKitClientAdapterImpl implements LiveKitClientAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveKitClientAdapterImpl.class);
    
    private final WebRTCProperties webRTCProperties;
    
    private RoomServiceClient roomServiceClient;
    private volatile boolean connected = false;
    
    @Autowired
    public LiveKitClientAdapterImpl(WebRTCProperties webRTCProperties) {
        this.webRTCProperties = webRTCProperties;
    }
    
    /**
     * Initializes LiveKit client connection
     * 初始化 LiveKit 客户端连接
     */
    @PostConstruct
    public void init() {
        connectToLiveKit();
    }
    
    /**
     * Connects to LiveKit Server
     * 连接到 LiveKit 服务器
     */
    private void connectToLiveKit() {
        try {
            WebRTCProperties.LiveKit liveKitConfig = webRTCProperties.getLivekit();
            String serverUrl = liveKitConfig.getUrl();
            String apiKey = liveKitConfig.getApiKey();
            String apiSecret = liveKitConfig.getApiSecret();
            
            if (serverUrl == null || serverUrl.trim().isEmpty()) {
                logger.warn("LiveKit server URL not configured. LiveKit features will not be available.");
                return;
            }
            
            if (apiKey == null || apiKey.trim().isEmpty() || apiSecret == null || apiSecret.trim().isEmpty()) {
                logger.warn("LiveKit API credentials not configured. LiveKit features will not be available.");
                return;
            }
            
            logger.info("Connecting to LiveKit Server: {}", serverUrl);
            roomServiceClient = RoomServiceClient.Companion.createClient(serverUrl, apiKey, apiSecret);
            connected = true;
            logger.info("✅ LiveKit Client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to connect to LiveKit Server", e);
            logger.warn("LiveKit WebRTC features will not be available.");
            connected = false;
            roomServiceClient = null;
        }
    }
    
    @Override
    public String createRoom(String roomName) {
        if (roomServiceClient == null) {
            throw new IllegalStateException("LiveKit Client is not initialized. Check LiveKit server configuration.");
        }
        
        try {
            LivekitModels.Room room = roomServiceClient.createRoom(roomName).execute().body();
            logger.info("[LiveKit] Room created: {}", roomName);
            return room != null ? room.getName() : roomName;
        } catch (Exception e) {
            logger.error("[LiveKit] Failed to create room: {}", roomName, e);
            throw new RuntimeException("Failed to create LiveKit room", e);
        }
    }
    
    @Override
    public void deleteRoom(String roomName) {
        if (roomServiceClient == null) {
            logger.warn("[LiveKit] Client not initialized, cannot delete room: {}", roomName);
            return;
        }
        
        try {
            roomServiceClient.deleteRoom(roomName).execute();
            logger.info("[LiveKit] Room deleted: {}", roomName);
        } catch (Exception e) {
            logger.error("[LiveKit] Failed to delete room: {}", roomName, e);
        }
    }
    
    @Override
    public String generateToken(String roomName, String participantIdentity) {
        WebRTCProperties.LiveKit liveKitConfig = webRTCProperties.getLivekit();
        String apiKey = liveKitConfig.getApiKey();
        String apiSecret = liveKitConfig.getApiSecret();
        
        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("LiveKit API credentials are not configured");
        }
        
        try {
            AccessToken token = new AccessToken(apiKey, apiSecret);
            token.setName(participantIdentity);
            token.setIdentity(participantIdentity);
            token.addGrants(new RoomJoin(true), new RoomName(roomName));
            
            String jwt = token.toJwt();
            logger.debug("[LiveKit] Token generated for participant: {} in room: {}", participantIdentity, roomName);
            return jwt;
        } catch (Exception e) {
            logger.error("[LiveKit] Failed to generate token for room: {}", roomName, e);
            throw new RuntimeException("Failed to generate LiveKit access token", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected && roomServiceClient != null;
    }
    
    @Override
    public String getServerUrl() {
        return webRTCProperties.getLivekit().getUrl();
    }
    
    /**
     * Cleans up LiveKit client resources
     * 清理 LiveKit 客户端资源
     */
    @PreDestroy
    public void destroy() {
        connected = false;
        roomServiceClient = null;
        logger.info("LiveKit Client destroyed");
    }
}
