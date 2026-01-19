package com.bailing.tts;

/**
 * TTS (Text-To-Speech) Interface
 * 文本转语音接口
 * 
 * <p>Defines the contract for text-to-speech services that convert
 * text into synthesized speech audio.</p>
 * 
 * <p>Implementations may use different TTS engines such as:</p>
 * <ul>
 *   <li>HTTP-based TTS services</li>
 *   <li>WebSocket streaming TTS</li>
 *   <li>Local TTS engines</li>
 *   <li>Cloud-based TTS APIs</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * TTS tts = new HttpTTSAdapter(config);
 * String audioFilePath = tts.synthesize("Hello, world!");
 * playAudioFile(audioFilePath);
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public interface TTS {
    
    /**
     * Synthesizes speech from text and returns the audio file path.
     * 
     * <p>This method processes the provided text and generates a
     * synthesized audio file. The audio format and encoding
     * characteristics are implementation-specific.</p>
     * 
     * <p>Common audio formats produced:</p>
     * <ul>
     *   <li>WAV (PCM 16-bit, 16kHz or 24kHz, mono)</li>
     *   <li>MP3 (compressed audio)</li>
     *   <li>Other formats as specified by implementation</li>
     * </ul>
     * 
     * <p>The returned file path points to a valid audio file that
     * can be played back or transmitted to clients. Callers are
     * responsible for cleaning up the file when no longer needed.</p>
     * 
     * <p>Thread Safety: Implementations should be thread-safe to allow
     * concurrent synthesis requests.</p>
     * 
     * @param text Text to synthesize into speech
     * @return Absolute path to the generated audio file
     * @throws Exception if synthesis fails due to:
     *         <ul>
     *           <li>Text is null or empty</li>
     *           <li>Network errors (for remote services)</li>
     *           <li>Service unavailability</li>
     *           <li>File system errors</li>
     *           <li>TTS engine errors</li>
     *           <li>Invalid voice configuration</li>
     *         </ul>
     */
    String synthesize(String text) throws Exception;
}
