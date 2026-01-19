package com.bailing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Audio Utility Methods
 * 音频工具类
 * 
 * <p>Provides utility methods for audio processing and file operations,
 * particularly for working with WAV format audio files.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Convert raw audio byte arrays to WAV format files</li>
 *   <li>Generate proper WAV file headers (RIFF format)</li>
 *   <li>Create standard audio format configurations</li>
 *   <li>Automatic directory creation for output files</li>
 *   <li>File path validation and error handling</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * AudioFormat format = AudioUtils.create16kHz16BitMono();
 * byte[] audioData = recordedAudio.getData();
 * AudioUtils.saveAsWav(audioData, "output/recording.wav", format);
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class AudioUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioUtils.class);
    
    /**
     * Saves raw audio data as a WAV file with proper RIFF headers.
     * 
     * <p>This method converts raw PCM audio data into a complete WAV file by:</p>
     * <ol>
     *   <li>Validating input parameters</li>
     *   <li>Creating output directory if needed</li>
     *   <li>Generating WAV file header based on audio format</li>
     *   <li>Writing header and audio data to file</li>
     * </ol>
     * 
     * <p>The WAV file format includes:</p>
     * <ul>
     *   <li>RIFF header with file size</li>
     *   <li>WAVE format chunk</li>
     *   <li>fmt chunk with audio format specifications</li>
     *   <li>data chunk with raw PCM audio samples</li>
     * </ul>
     * 
     * <p>Example:</p>
     * <pre>
     * AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
     * byte[] pcmData = microphone.record();
     * AudioUtils.saveAsWav(pcmData, "recordings/audio.wav", format);
     * </pre>
     * 
     * @param audioData Raw PCM audio data as byte array
     * @param filePath Output file path (directories will be created if needed)
     * @param format AudioFormat specifying sample rate, bit depth, channels, etc.
     * @throws IllegalArgumentException if audioData is null/empty, filePath is invalid, or format is null
     * @throws IOException if file writing fails
     */
    public static void saveAsWav(byte[] audioData, String filePath, AudioFormat format) throws IOException {
        logger.debug("Saving audio data as WAV: {} bytes to {}", 
            audioData != null ? audioData.length : 0, filePath);
        
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        if (format == null) {
            throw new IllegalArgumentException("Audio format cannot be null");
        }
        
        Path outputPath = Paths.get(filePath);
        Path parentDir = outputPath.getParent();
        
        if (parentDir != null && !Files.exists(parentDir)) {
            logger.debug("Creating parent directory: {}", parentDir);
            Files.createDirectories(parentDir);
        }
        
        byte[] wavHeader = createWavHeader(audioData.length, format);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(wavHeader);
        outputStream.write(audioData);
        
        byte[] wavFile = outputStream.toByteArray();
        Files.write(outputPath, wavFile);
        
        logger.info("Successfully saved WAV file: {} ({} bytes, {} samples)", 
            filePath, wavFile.length, audioData.length / format.getFrameSize());
    }
    
    /**
     * Creates a standard 16kHz, 16-bit, mono audio format.
     * 
     * <p>This is a commonly used format for voice applications and speech recognition.
     * The format specifications are:</p>
     * <ul>
     *   <li><b>Sample Rate:</b> 16000 Hz (16 kHz)</li>
     *   <li><b>Sample Size:</b> 16 bits per sample</li>
     *   <li><b>Channels:</b> 1 (mono)</li>
     *   <li><b>Signed:</b> true (signed PCM)</li>
     *   <li><b>Big Endian:</b> false (little-endian byte order)</li>
     * </ul>
     * 
     * @return AudioFormat configured for 16kHz 16-bit mono audio
     */
    public static AudioFormat create16kHz16BitMono() {
        return new AudioFormat(
            16000.0f,  // sample rate (Hz)
            16,        // sample size in bits
            1,         // channels (1 = mono)
            true,      // signed
            false      // bigEndian (false = little-endian)
        );
    }
    
    /**
     * Creates a standard 8kHz, 16-bit, mono audio format.
     * 
     * <p>This format is commonly used for telephony applications.
     * The format specifications are:</p>
     * <ul>
     *   <li><b>Sample Rate:</b> 8000 Hz (8 kHz)</li>
     *   <li><b>Sample Size:</b> 16 bits per sample</li>
     *   <li><b>Channels:</b> 1 (mono)</li>
     *   <li><b>Signed:</b> true (signed PCM)</li>
     *   <li><b>Big Endian:</b> false (little-endian byte order)</li>
     * </ul>
     * 
     * @return AudioFormat configured for 8kHz 16-bit mono audio
     */
    public static AudioFormat create8kHz16BitMono() {
        return new AudioFormat(
            8000.0f,   // sample rate (Hz)
            16,        // sample size in bits
            1,         // channels (1 = mono)
            true,      // signed
            false      // bigEndian (false = little-endian)
        );
    }
    
    /**
     * Creates a WAV file header in RIFF format.
     * 
     * <p>The WAV header structure (44 bytes total):</p>
     * <pre>
     * Bytes 0-3:   "RIFF" chunk descriptor
     * Bytes 4-7:   File size - 8
     * Bytes 8-11:  "WAVE" format
     * Bytes 12-15: "fmt " subchunk
     * Bytes 16-19: Subchunk1 size (16 for PCM)
     * Bytes 20-21: Audio format (1 for PCM)
     * Bytes 22-23: Number of channels
     * Bytes 24-27: Sample rate
     * Bytes 28-31: Byte rate (sample rate * block align)
     * Bytes 32-33: Block align (channels * bits per sample / 8)
     * Bytes 34-35: Bits per sample
     * Bytes 36-39: "data" subchunk
     * Bytes 40-43: Data size
     * </pre>
     * 
     * @param audioDataSize Size of the audio data in bytes
     * @param format AudioFormat with sample rate, bit depth, and channel information
     * @return 44-byte WAV header as byte array
     */
    private static byte[] createWavHeader(int audioDataSize, AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int bitsPerSample = format.getSampleSizeInBits();
        int channels = format.getChannels();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        
        ByteBuffer buffer = ByteBuffer.allocate(44);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + audioDataSize);
        buffer.put("WAVE".getBytes());
        
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        
        buffer.put("data".getBytes());
        buffer.putInt(audioDataSize);
        
        byte[] header = buffer.array();
        
        logger.trace("Created WAV header: sampleRate={}, channels={}, bitsPerSample={}, dataSize={}", 
            sampleRate, channels, bitsPerSample, audioDataSize);
        
        return header;
    }
    
    /**
     * Validates if a file path is valid and can be written to.
     * 
     * <p>Checks performed:</p>
     * <ul>
     *   <li>Path is not null or empty</li>
     *   <li>Parent directory exists and is writable, or can potentially be created</li>
     *   <li>If file exists, it is writable</li>
     * </ul>
     * 
     * <p>Note: This method does not actually attempt to create directories,
     * it only validates if the path structure appears valid.</p>
     * 
     * @param filePath File path to validate
     * @return true if the path is valid and writable
     */
    public static boolean isValidOutputPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("File path is null or empty");
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            
            if (parent != null) {
                if (!Files.exists(parent)) {
                    logger.debug("Parent directory does not exist but may be creatable: {}", parent);
                    return true;
                }
                if (!Files.isWritable(parent)) {
                    logger.warn("Parent directory is not writable: {}", parent);
                    return false;
                }
            }
            
            if (Files.exists(path) && !Files.isWritable(path)) {
                logger.warn("File exists but is not writable: {}", filePath);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating file path: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * Calculates the duration of audio data in seconds.
     * 
     * <p>Duration is calculated as:</p>
     * <pre>
     * duration = audioDataSize / (sampleRate * channels * bytesPerSample)
     * </pre>
     * 
     * @param audioDataSize Size of audio data in bytes
     * @param format AudioFormat with sample rate and format information
     * @return Duration in seconds
     */
    public static double calculateDuration(int audioDataSize, AudioFormat format) {
        int frameSize = format.getFrameSize();
        float frameRate = format.getFrameRate();
        
        if (frameSize <= 0 || frameRate <= 0) {
            logger.warn("Invalid audio format for duration calculation");
            return 0.0;
        }
        
        int frames = audioDataSize / frameSize;
        double duration = frames / frameRate;
        
        logger.trace("Calculated audio duration: {} seconds ({} frames)", duration, frames);
        
        return duration;
    }
}
