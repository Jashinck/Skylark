package org.skylark.infrastructure.adapter;

/**
 * ASR (Automatic Speech Recognition) Interface
 * 自动语音识别接口
 * 
 * <p>Defines the contract for speech recognition services that convert
 * audio data into text transcriptions.</p>
 * 
 * <p>Implementations may use different ASR engines such as:</p>
 * <ul>
 *   <li>HTTP-based ASR services</li>
 *   <li>WebSocket streaming ASR</li>
 *   <li>Local ASR engines</li>
 *   <li>Cloud-based ASR APIs</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * ASR asr = new HttpASRAdapter(config);
 * byte[] audioData = recordAudio();
 * String text = asr.recognize(audioData);
 * System.out.println("Recognized: " + text);
 * </pre>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public interface ASR {
    
    /**
     * Recognizes speech from audio data and returns the transcribed text.
     * 
     * <p>This method processes the provided audio data and returns the
     * recognized text transcription. The audio format and encoding
     * requirements are implementation-specific.</p>
     * 
     * <p>Common audio formats supported:</p>
     * <ul>
     *   <li>WAV (PCM 16-bit, 16kHz, mono)</li>
     *   <li>Raw PCM audio data</li>
     *   <li>Other formats as specified by implementation</li>
     * </ul>
     * 
     * <p>Thread Safety: Implementations should be thread-safe to allow
     * concurrent recognition requests.</p>
     * 
     * @param audioData Raw audio data as byte array
     * @return Recognized text transcription, or empty string if no speech detected
     * @throws Exception if recognition fails due to:
     *         <ul>
     *           <li>Invalid audio format</li>
     *           <li>Network errors (for remote services)</li>
     *           <li>Service unavailability</li>
     *           <li>Audio data is null or empty</li>
     *           <li>ASR engine errors</li>
     *         </ul>
     */
    String recognize(byte[] audioData) throws Exception;
}
