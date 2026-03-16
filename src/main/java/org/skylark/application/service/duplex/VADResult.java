package org.skylark.application.service.duplex;

/**
 * VAD detection result with confidence
 * 带置信度的VAD检测结果
 *
 * Enhanced result from TripleVADEngine, providing speech probability
 * and timestamp for precise barge-in control.
 */
public class VADResult {

    private final boolean speech;
    private final float probability;
    private final long timestamp;
    private final AudioEventType eventType;

    public VADResult(boolean speech, float probability, long timestamp) {
        this(speech, probability, timestamp, AudioEventType.SPEECH);
    }

    public VADResult(boolean speech, float probability, long timestamp, AudioEventType eventType) {
        this.speech = speech;
        this.probability = probability;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    public static VADResult silence() {
        return new VADResult(false, 0.0f, System.currentTimeMillis(), AudioEventType.SILENCE);
    }

    public static VADResult nonSpeechEvent(AudioEventType eventType) {
        return new VADResult(false, 0.0f, System.currentTimeMillis(), eventType);
    }

    public boolean isSpeech() {
        return speech;
    }

    public float getProbability() {
        return probability;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AudioEventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format("VADResult{speech=%s, probability=%.3f, eventType=%s, timestamp=%d}",
                speech, probability, eventType, timestamp);
    }

    /**
     * Audio event types detected by VAD
     * VAD检测到的音频事件类型
     *
     * FireRedVAD AED capability can distinguish speech from singing/music/echo residuals.
     */
    public enum AudioEventType {
        /** Human speech / 人类语音 */
        SPEECH,
        /** Silence / 静音 */
        SILENCE,
        /** Music / 音乐 */
        MUSIC,
        /** Singing / 歌唱 */
        SINGING,
        /** Echo residual / 回声残留 */
        ECHO_RESIDUAL,
        /** Unknown audio event / 未知音频事件 */
        UNKNOWN
    }
}
