package org.skylark.application.service.duplex;

/**
 * VAD event types for the duplex state machine
 * 全双工状态机的VAD事件类型
 */
public enum VADEvent {
    /** User started speaking / 用户开始说话 */
    SPEECH_START,
    /** User stopped speaking / 用户停止说话 */
    SPEECH_END,
    /** Continued silence detected / 持续静音 */
    SILENCE_TIMEOUT
}
