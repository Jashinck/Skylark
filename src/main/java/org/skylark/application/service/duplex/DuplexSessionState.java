package org.skylark.application.service.duplex;

/**
 * Full-duplex session states
 * 全双工会话状态枚举
 *
 * Defines the state machine states for full-duplex voice interaction.
 * Replaces the simple boolean sessionSpeaking map in OrchestrationService.
 */
public enum DuplexSessionState {
    /** Idle state - waiting for user input / 空闲状态 */
    IDLE,
    /** Listening state - streaming ASR in progress / 正在监听用户说话 */
    LISTENING,
    /** Processing state - ASR complete, LLM inference in progress / ASR完成，LLM推理中 */
    PROCESSING,
    /** Speaking state - TTS audio playback, still monitoring VAD / TTS播放中，仍在监听VAD */
    SPEAKING,
    /** Interrupting state - user barge-in, cleaning up and switching / 用户打断，正在清理切换 */
    INTERRUPTING
}
