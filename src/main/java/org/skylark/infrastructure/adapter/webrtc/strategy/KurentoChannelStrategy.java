package org.skylark.infrastructure.adapter.webrtc.strategy;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skylark.infrastructure.adapter.webrtc.KurentoClientAdapter;
import org.skylark.infrastructure.adapter.webrtc.WebRTCSession;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurento WebRTC Channel Strategy
 * Kurento WebRTC 通道策略
 * 
 * <p>Uses Kurento Media Server for professional server-side media processing,
 * including Media Pipeline orchestration and WebRTC Endpoint management.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class KurentoChannelStrategy implements WebRTCChannelStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(KurentoChannelStrategy.class);
    
    private final KurentoClientAdapter kurentoClient;
    private final ConcurrentHashMap<String, WebRTCSession> sessions = new ConcurrentHashMap<>();
    
    public KurentoChannelStrategy(KurentoClientAdapter kurentoClient) {
        this.kurentoClient = kurentoClient;
    }
    
    @Override
    public String getStrategyName() {
        return "kurento";
    }
    
    @Override
    public String createSession(String userId) {
        try {
            String sessionId = UUID.randomUUID().toString();
            logger.info("[Kurento] Creating session for user: {}, sessionId: {}", userId, sessionId);
            
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            WebRtcEndpoint webRtcEndpoint = kurentoClient.createWebRTCEndpoint(pipeline);
            
            WebRTCSession session = new WebRTCSession(sessionId, pipeline, webRtcEndpoint);
            sessions.put(sessionId, session);
            
            logger.info("[Kurento] Session created successfully: {}", sessionId);
            return sessionId;
        } catch (Exception e) {
            logger.error("[Kurento] Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Failed to create Kurento WebRTC session", e);
        }
    }
    
    @Override
    public String processOffer(String sessionId, String sdpOffer) {
        WebRTCSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        try {
            String sdpAnswer = session.processOffer(sdpOffer);
            session.gatherCandidates();
            logger.debug("[Kurento] SDP offer processed for session: {}", sessionId);
            return sdpAnswer;
        } catch (Exception e) {
            logger.error("[Kurento] Failed to process SDP offer for session: {}", sessionId, e);
            throw new RuntimeException("Failed to process SDP offer", e);
        }
    }
    
    @Override
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        WebRTCSession session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("[Kurento] Session not found for ICE candidate: {}", sessionId);
            return;
        }
        
        try {
            IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
            session.addIceCandidate(iceCandidate);
            logger.debug("[Kurento] ICE candidate added for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("[Kurento] Failed to add ICE candidate for session: {}", sessionId, e);
        }
    }
    
    @Override
    public void closeSession(String sessionId) {
        try {
            WebRTCSession session = sessions.remove(sessionId);
            if (session != null) {
                session.release();
                logger.info("[Kurento] Session closed: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("[Kurento] Error closing session: {}", sessionId, e);
        }
    }
    
    @Override
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    @Override
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    @Override
    public boolean isAvailable() {
        return kurentoClient.isConnected();
    }
}
