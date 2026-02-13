package org.skylark.application.service;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.AudioProcessor;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.WebRTCSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC Service
 * WebRTC 服务
 * 
 * <p>Manages WebRTC sessions using Kurento Media Server and integrates
 * with the VAD-ASR-LLM-TTS pipeline for real-time voice interaction.</p>
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
    
    private final ConcurrentHashMap<String, WebRTCSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AudioProcessor> audioProcessors = new ConcurrentHashMap<>();
    
    @Autowired
    public WebRTCService(
        KurentoClientAdapter kurentoClient,
        VADService vadService,
        ASRService asrService,
        TTSService ttsService
    ) {
        this.kurentoClient = kurentoClient;
        this.vadService = vadService;
        this.asrService = asrService;
        this.ttsService = ttsService;
    }
    
    /**
     * Creates a new WebRTC session
     * 创建新的 WebRTC 会话
     * 
     * @param userId User identifier
     * @return Session ID
     */
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            logger.info("Creating WebRTC session for user: {}, sessionId: {}", userId, sessionId);
            
            // Create media pipeline
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            logger.debug("Media pipeline created for session: {}", sessionId);
            
            // Create WebRTC endpoint
            WebRtcEndpoint webRtcEndpoint = kurentoClient.createWebRTCEndpoint(pipeline);
            logger.debug("WebRTC endpoint created for session: {}", sessionId);
            
            // Create audio processor for VAD/ASR integration
            AudioProcessor audioProcessor = new AudioProcessor(vadService, asrService, sessionId);
            audioProcessors.put(sessionId, audioProcessor);
            
            // Create WebRTC session
            WebRTCSession session = new WebRTCSession(sessionId, pipeline, webRtcEndpoint);
            sessions.put(sessionId, session);
            
            logger.info("✅ WebRTC session created successfully: {}", sessionId);
            return sessionId;
            
        } catch (Exception e) {
            logger.error("Failed to create WebRTC session for user: {}", userId, e);
            throw new RuntimeException("Failed to create WebRTC session", e);
        }
    }
    
    /**
     * Processes SDP offer from client
     * 处理来自客户端的 SDP offer
     * 
     * @param sessionId Session identifier
     * @param sdpOffer SDP offer from client
     * @return SDP answer
     */
    public String processOffer(String sessionId, String sdpOffer) {
        WebRTCSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        try {
            logger.debug("Processing SDP offer for session: {}", sessionId);
            
            // Process offer and get answer
            String sdpAnswer = session.processOffer(sdpOffer);
            
            // Start gathering ICE candidates
            session.gatherCandidates();
            
            logger.info("SDP offer processed successfully for session: {}", sessionId);
            return sdpAnswer;
            
        } catch (Exception e) {
            logger.error("Failed to process SDP offer for session: {}", sessionId, e);
            throw new RuntimeException("Failed to process SDP offer", e);
        }
    }
    
    /**
     * Adds ICE candidate to session
     * 向会话添加 ICE candidate
     * 
     * @param sessionId Session identifier
     * @param candidate ICE candidate string
     * @param sdpMid SDP media ID
     * @param sdpMLineIndex SDP media line index
     */
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        WebRTCSession session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("Session not found for ICE candidate: {}", sessionId);
            return;
        }
        
        try {
            IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
            session.addIceCandidate(iceCandidate);
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
     * Closes a WebRTC session
     * 关闭 WebRTC 会话
     * 
     * @param sessionId Session identifier
     */
    public void closeSession(String sessionId) {
        try {
            // Remove and release WebRTC session
            WebRTCSession session = sessions.remove(sessionId);
            if (session != null) {
                session.release();
                logger.info("WebRTC session closed: {}", sessionId);
            }
            
            // Remove audio processor
            AudioProcessor processor = audioProcessors.remove(sessionId);
            if (processor != null) {
                processor.reset();
                logger.debug("Audio processor cleaned up for session: {}", sessionId);
            }
            
            // Clean up VAD state
            vadService.reset(sessionId);
            
        } catch (Exception e) {
            logger.error("Error closing session: {}", sessionId, e);
        }
    }
    
    /**
     * Gets a WebRTC session
     * 获取 WebRTC 会话
     * 
     * @param sessionId Session identifier
     * @return WebRTC session or null if not found
     */
    public WebRTCSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Checks if a session exists
     * 检查会话是否存在
     * 
     * @param sessionId Session identifier
     * @return true if session exists
     */
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    /**
     * Gets the number of active sessions
     * 获取活动会话数
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
