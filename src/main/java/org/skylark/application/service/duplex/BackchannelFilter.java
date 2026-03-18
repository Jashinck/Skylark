package org.skylark.application.service.duplex;

import java.util.Collections;
import java.util.Set;

/**
 * Backchannel Filter — 过滤语气词，避免误打断
 *
 * <p>在 L3 全双工模式下，用户在系统说话时发出的 "嗯"、"嗯嗯"、"哦" 等语气词
 * 不应触发打断，而应被识别为 backchannel（反馈信号）并被过滤。</p>
 *
 * <p>In L3 full-duplex mode, filler words uttered by the user while the system is speaking
 * (such as "嗯", "嗯嗯", "哦") should not trigger a barge-in. They are treated as
 * backchannel signals and filtered out.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class BackchannelFilter {

    // 中英文 backchannel 词典 / Chinese and English backchannel dictionary
    private static final Set<String> BACKCHANNEL_TOKENS = Set.of(
            "嗯", "嗯嗯", "哦", "啊", "是的", "对", "好", "好的",
            "ok", "uh-huh", "mm", "hmm", "yeah", "yes", "right",
            "嗯哼", "哦哦", "对对", "好好", "是", "行"
    );

    /**
     * 判断 ASR 识别结果是否是 backchannel 语气词
     *
     * <p>Determine whether the ASR transcript is a backchannel filler word.</p>
     *
     * @param asrText ASR 识别出的文本 / ASR transcript text
     * @return true 如果是语气词（应被过滤，不触发打断）/
     *         true if the text is a backchannel filler (should be filtered, not trigger barge-in)
     */
    public boolean isBackchannel(String asrText) {
        if (asrText == null || asrText.trim().isEmpty()) {
            return true;  // 空文本视为无效输入 / empty text treated as invalid input
        }
        String normalized = asrText.trim().toLowerCase();
        return BACKCHANNEL_TOKENS.contains(normalized);
    }

    /**
     * 获取 backchannel 词典（用于测试和配置）
     *
     * <p>Return an unmodifiable view of the backchannel token dictionary.</p>
     *
     * @return unmodifiable set of backchannel tokens
     */
    public Set<String> getBackchannelTokens() {
        return Collections.unmodifiableSet(BACKCHANNEL_TOKENS);
    }
}
