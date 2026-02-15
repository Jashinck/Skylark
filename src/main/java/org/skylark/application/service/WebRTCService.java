package org.skylark.application.service;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.AudioProcessor;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.WebRTCSession;
import org.skylark.infrastructure.adapter.webrtc.strategy.WebRTCChannelStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC Service
 * WebRTC 服务
 * 
 * <p>Manages WebRTC sessions using pluggable channel strategies (WebSocket, Kurento, LiveKit).
 * Integrates with the VAD-ASR-LLM-TTS pipeline for real-time voice interaction.</p>
 * 
 * <p>The active strategy is selected via {@code webrtc.strategy} configuration property.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
@Service
public class WebRTCService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCService.class);
    
    private final KurentoClientAdapter kurentoClient;
    private final VADService vadService;
    private final ASRService asrService;
    private final TTSService ttsService;
    private final WebRTCChannelStrategy channelStrategy;
    
    private final ConcurrentHashMap<String, WebRTCSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AudioProcessor> audioProcessors = new ConcurrentHashMap<>();
    
    @Autowired
    public WebRTCService(
        KurentoClientAdapter kurentoClient,
        VADService vadService,
        ASRService asrService,
        TTSService ttsService,
        WebRTCChannelStrategy channelStrategy
    ) {
        this.kurentoClient = kurentoClient;
        this.vadService = vadService;
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.channelStrategy = channelStrategy;
        logger.info("WebRTCService initialized with strategy: {}", channelStrategy.getStrategyName());
    }
    
    /**
     * Creates a new WebRTC session using the active channel strategy
     * 使用活动通道策略创建新的 WebRTC 会话
     * 
     * @param userId User identifier
     * @return Session ID
     */
    public String createSession(String userId) {
        try {
            String sessionId = channelStrategy.createSession(userId);
            logger.info("Session created via {} strategy: {}", channelStrategy.getStrategyName(), sessionId);
            
            // Create audio processor for VAD/ASR integration
            AudioProcessor audioProcessor = new AudioProcessor(vadService, asrService, sessionId);
            audioProcessors.put(sessionId, audioProcessor);
            
            return sessionId;
        } catch (Exception e) {
            logger.error("Failed to create WebRTC session for user: {}", userId, e);
            throw new RuntimeException("Failed to create WebRTC session", e);
        }
    }
    
    /**
     * Processes SDP offer from client using the active channel strategy
     * 使用活动通道策略处理来自客户端的 SDP offer
     * 
     * @param sessionId Session identifier
     * @param sdpOffer SDP offer from client
     * @return SDP answer or connection info
     */
    public String processOffer(String sessionId, String sdpOffer) {
        try {
            logger.debug("Processing SDP offer for session: {} via {} strategy",
                sessionId, channelStrategy.getStrategyName());
            String result = channelStrategy.processOffer(sessionId, sdpOffer);
            logger.info("SDP offer processed successfully for session: {}", sessionId);
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process SDP offer for session: {}", sessionId, e);
            throw new RuntimeException("Failed to process SDP offer", e);
        }
    }
    
    /**
     * Adds ICE candidate to session via the active channel strategy
     * 通过活动通道策略向会话添加 ICE candidate
     * 
     * @param sessionId Session identifier
     * @param candidate ICE candidate string
     * @param sdpMid SDP media ID
     * @param sdpMLineIndex SDP media line index
     */
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        try {
            channelStrategy.addIceCandidate(sessionId, candidate, sdpMid, sdpMLineIndex);
            logger.debug("ICE candidate added for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Failed to add ICE candidate for session: {}", sessionId, e);
        }
    }
    
    /**
     * Processes audio data from WebRTC stream
     * 处理来自 WebRTC 流的音频数据
     * 
     * @param sessionId Session identifier
     * @param audioData Raw PCM audio data
     * @return VAD detection status
     */
    public String processAudioData(String sessionId, byte[] audioData) {
        AudioProcessor processor = audioProcessors.get(sessionId);
        if (processor == null) {
            logger.warn("Audio processor not found for session: {}", sessionId);
            return null;
        }
        
        try {
            return processor.processAudioChunk(audioData);
        } catch (Exception e) {
            logger.error("Failed to process audio data for session: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Gets recognized text for a session
     * 获取会话的识别文本
     * 
     * @param sessionId Session identifier
     * @param audioData Audio data to recognize
     * @return Recognized text
     */
    public String recognizeSpeech(String sessionId, byte[] audioData) {
        AudioProcessor processor = audioProcessors.get(sessionId);
        if (processor == null) {
            logger.warn("Audio processor not found for session: {}", sessionId);
            return null;
        }
        
        try {
            return processor.recognizeSpeech(audioData);
        } catch (Exception e) {
            logger.error("Failed to recognize speech for session: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Closes a WebRTC session via the active channel strategy
     * 通过活动通道策略关闭 WebRTC 会话
     * 
     * @param sessionId Session identifier
     */
    public void closeSession(String sessionId) {
        try {
            // Close session via strategy
            channelStrategy.closeSession(sessionId);
            
            // Remove audio processor
            AudioProcessor processor = audioProcessors.remove(sessionId);
            if (processor != null) {
                processor.reset();
                logger.debug("Audio processor cleaned up for session: {}", sessionId);
            }
            
            // Clean up VAD state
            vadService.reset(sessionId);
            
            logger.info("WebRTC session closed: {}", sessionId);
        } catch (Exception e) {
            logger.error("Error closing session: {}", sessionId, e);
        }
    }
    
    /**
     * Gets the active WebRTC channel strategy
     * 获取活动的 WebRTC 通道策略
     * 
     * @return Active channel strategy
     */
    public WebRTCChannelStrategy getChannelStrategy() {
        return channelStrategy;
    }
    
    /**
     * Checks if a session exists
     * 检查会话是否存在
     * 
     * @param sessionId Session identifier
     * @return true if session exists
     */
    public boolean sessionExists(String sessionId) {
        return channelStrategy.sessionExists(sessionId);
    }
    
    /**
     * Gets the number of active sessions
     * 获取活动会话数
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return channelStrategy.getActiveSessionCount();
    }
}
