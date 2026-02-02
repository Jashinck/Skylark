package org.skylark.infrastructure.adapter;

/**
 * VAD (Voice Activity Detection) Interface
 * 语音活动检测接口
 * 
 * <p>Defines the contract for voice activity detection services that
 * identify when speech starts and ends in audio streams.</p>
 * 
 * <p>VAD is essential for:</p>
 * <ul>
 *   <li>Detecting start of speech utterances</li>
 *   <li>Detecting end of speech for segmentation</li>
 *   <li>Filtering silence and non-speech audio</li>
 *   <li>Triggering speech recognition at appropriate times</li>
 * </ul>
 * 
 * <p>The VAD maintains session state to track ongoing speech activity
 * across multiple audio chunks. Each session is identified by a unique
 * sessionId.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * VAD vad = new HttpVADAdapter(config);
 * String sessionId = "user123";
 * 
 * // Process audio chunks
 * String status = vad.detect(audioChunk1, sessionId); // returns "start"
 * status = vad.detect(audioChunk2, sessionId);         // returns null
 * status = vad.detect(audioChunk3, sessionId);         // returns "end"
 * 
 * // Reset for next utterance
 * vad.reset(sessionId);
 * </pre>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface VAD {
    
    /**
     * Detects voice activity in audio data for a specific session.
     * 
     * <p>This method analyzes the provided audio chunk and returns the
     * current voice activity status. The VAD maintains state across
     * multiple calls for the same sessionId to accurately detect
     * speech boundaries.</p>
     * 
     * <p>Return values:</p>
     * <ul>
     *   <li><b>"start"</b>: Speech has started (transition from silence to speech)</li>
     *   <li><b>"end"</b>: Speech has ended (transition from speech to silence)</li>
     *   <li><b>null</b>: No state change (continuing speech or continuing silence)</li>
     * </ul>
     * 
     * <p>Thread Safety: Implementations should handle concurrent calls
     * for different sessionIds safely. Calls with the same sessionId
     * should be processed sequentially to maintain state consistency.</p>
     * 
     * @param audioData Raw audio data as byte array
     * @param sessionId Unique identifier for this detection session
     * @return "start" if speech started, "end" if speech ended, null otherwise
     * @throws Exception if detection fails due to:
     *         <ul>
     *           <li>Invalid audio format</li>
     *           <li>Network errors (for remote services)</li>
     *           <li>Service unavailability</li>
     *           <li>Audio data is null or empty</li>
     *           <li>SessionId is null or empty</li>
     *         </ul>
     */
    String detect(byte[] audioData, String sessionId) throws Exception;
    
    /**
     * Resets the VAD state for a specific session.
     * 
     * <p>This method clears any accumulated state for the given sessionId,
     * allowing the VAD to start fresh for a new audio stream. Call this
     * method when:</p>
     * <ul>
     *   <li>Starting a new conversation</li>
     *   <li>After processing a complete utterance</li>
     *   <li>When switching to a new audio source</li>
     *   <li>Recovering from errors</li>
     * </ul>
     * 
     * <p>After reset, the next call to detect() will treat audio as
     * the beginning of a new speech session.</p>
     * 
     * @param sessionId Unique identifier for the session to reset
     * @throws Exception if reset fails due to:
     *         <ul>
     *           <li>Network errors (for remote services)</li>
     *           <li>Service unavailability</li>
     *           <li>SessionId is null or empty</li>
     *         </ul>
     */
    void reset(String sessionId) throws Exception;
}
