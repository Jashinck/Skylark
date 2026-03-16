package org.skylark.application.service.duplex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Full-duplex feature configuration
 * 全双工特性配置
 *
 * <p>Feature toggle for backward compatibility:
 * <pre>
 *   duplex.mode = half      → existing OrchestrationService (default)
 *   duplex.mode = barge-in  → Phase 1: barge-in support
 *   duplex.mode = streaming → Phase 2: streaming pipeline
 *   duplex.mode = full      → Phase 3: full-duplex with multimodal
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
@Configuration
public class DuplexConfig {

    private static final Logger logger = LoggerFactory.getLogger(DuplexConfig.class);

    public static final String MODE_HALF = "half";
    public static final String MODE_BARGE_IN = "barge-in";
    public static final String MODE_STREAMING = "streaming";
    public static final String MODE_FULL = "full";

    @Value("${duplex.mode:half}")
    private String duplexMode;

    @Bean
    public DuplexMode duplexMode() {
        DuplexMode mode = DuplexMode.fromString(duplexMode);
        logger.info("Duplex mode configured: {} ({})", mode, mode.getDescription());
        return mode;
    }

    @Bean
    public ServerAECProcessor serverAECProcessor() {
        return new ServerAECProcessor();
    }

    @Bean
    public ModelRouter modelRouter() {
        return new ModelRouter();
    }

    @Bean
    public BackchannelFilter backchannelFilter() {
        return new BackchannelFilter();
    }

    /**
     * Duplex mode enum with feature flags
     * 全双工模式枚举及特性标志
     */
    public enum DuplexMode {
        /** Half-duplex: existing behavior, no interruption / 半双工：现有行为 */
        HALF(MODE_HALF, "Half-duplex (existing behavior)", false, false, false),
        /** Barge-in: TTS can be interrupted / 可打断：TTS播放时可被打断 */
        BARGE_IN(MODE_BARGE_IN, "Barge-in support (Phase 1)", true, false, false),
        /** Streaming: full streaming pipeline / 流式：全链路流式处理 */
        STREAMING(MODE_STREAMING, "Streaming pipeline (Phase 2)", true, true, false),
        /** Full-duplex: simultaneous speaking and listening / 全双工：同时说听 */
        FULL(MODE_FULL, "Full-duplex with multimodal (Phase 3)", true, true, true);

        private final String value;
        private final String description;
        private final boolean bargeInEnabled;
        private final boolean streamingEnabled;
        private final boolean fullDuplexEnabled;

        DuplexMode(String value, String description, boolean bargeInEnabled,
                   boolean streamingEnabled, boolean fullDuplexEnabled) {
            this.value = value;
            this.description = description;
            this.bargeInEnabled = bargeInEnabled;
            this.streamingEnabled = streamingEnabled;
            this.fullDuplexEnabled = fullDuplexEnabled;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
        public boolean isBargeInEnabled() { return bargeInEnabled; }
        public boolean isStreamingEnabled() { return streamingEnabled; }
        public boolean isFullDuplexEnabled() { return fullDuplexEnabled; }

        public static DuplexMode fromString(String value) {
            if (value == null) return HALF;
            for (DuplexMode mode : values()) {
                if (mode.value.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return HALF;
        }
    }
}
