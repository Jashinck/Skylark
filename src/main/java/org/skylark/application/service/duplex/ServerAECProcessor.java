package org.skylark.application.service.duplex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side Acoustic Echo Cancellation (AEC) Processor
 * 服务端回声消除处理器
 *
 * <p>Removes TTS playback echo from user microphone input,
 * enabling VAD to detect only genuine user speech.</p>
 *
 * <p>AEC position in the full-duplex pipeline:
 * <pre>
 *   RTC uplink audio ──┐
 *                      ├──→ AEC process ──→ clean audio ──→ VAD ──→ ASR
 *   RTC downlink audio ─┘   (reference)
 *   (TTS playback)
 * </pre></p>
 *
 * <p>Phase 1: Pass-through (no actual echo cancellation, relies on client-side AEC).
 * Phase 3: Integrate SpeexDSP or WebRTC AEC3 for server-side processing.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class ServerAECProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ServerAECProcessor.class);

    // Phase 3: SpeexDSP JNI integration
    // private final SpeexAEC speexAEC;

    public ServerAECProcessor() {
        logger.info("ServerAECProcessor initialized (Phase 1: pass-through mode, relies on client-side AEC)");
    }

    /**
     * Process echo cancellation
     * 处理回声消除
     *
     * @param micAudio   uplink microphone audio (may contain echo)
     * @param refAudio   downlink reference audio (TTS playback content), null if not playing
     * @return           clean audio with echo removed
     */
    public float[] process(float[] micAudio, float[] refAudio) {
        if (micAudio == null || micAudio.length == 0) {
            return new float[0];
        }

        if (refAudio == null || refAudio.length == 0) {
            // System not playing, no AEC needed
            // 系统未在播放，无需AEC
            return micAudio;
        }

        // Phase 1: Pass-through (client-side AEC via PAAS RTC SDK)
        // Phase 3: Apply SpeexDSP echo cancellation
        // return speexAEC.cancelEcho(micAudio, refAudio);

        logger.debug("AEC pass-through: {} mic samples, {} ref samples", micAudio.length, refAudio.length);
        return micAudio;
    }

    /**
     * Convert PCM byte array to float array normalized to [-1, 1]
     * 将PCM字节数组转换为归一化到[-1, 1]的浮点数组
     *
     * @param pcmBytes PCM audio bytes (16-bit little-endian)
     * @return normalized float array
     */
    public static float[] pcmBytesToFloatArray(byte[] pcmBytes) {
        if (pcmBytes == null || pcmBytes.length < 2) {
            return new float[0];
        }

        int numSamples = pcmBytes.length / 2;
        float[] samples = new float[numSamples];

        for (int i = 0; i < numSamples; i++) {
            int low = pcmBytes[2 * i] & 0xFF;
            int high = pcmBytes[2 * i + 1];
            short sample = (short) (low | (high << 8));
            samples[i] = sample / 32768.0f;
        }

        return samples;
    }
}
