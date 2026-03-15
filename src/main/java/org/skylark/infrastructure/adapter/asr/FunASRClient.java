package org.skylark.infrastructure.adapter.asr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FunASR WebSocket Client
 * FunASR WebSocket客户端
 *
 * <p>Connects to FunASR Server (Alibaba DAMO Academy) for real-time
 * streaming speech recognition. Uses Paraformer-large real-time model
 * with punctuation restoration and hotword support.</p>
 *
 * <p>Integration architecture:
 * <pre>
 *   Skylark Server ←──WebSocket──→ FunASR Server (Docker)
 *   StreamingASRService              Paraformer-large (real-time)
 *   (PCM stream →)                   + punctuation restoration
 *   (← JSON results)                + hotword support
 * </pre></p>
 *
 * <p>Phase 2 component: WebSocket client for FunASR server.
 * Current implementation is a placeholder with interface definition.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://github.com/modelscope/FunASR">FunASR GitHub</a>
 */
public class FunASRClient {

    private static final Logger logger = LoggerFactory.getLogger(FunASRClient.class);

    private static final String DEFAULT_FUNASR_URL = "ws://localhost:10095";

    private final String serverUrl;
    private volatile boolean connected = false;

    /**
     * ASR result callback
     */
    public interface ASRCallback {
        /** Partial (intermediate) recognition result / 中间识别结果 */
        void onPartialResult(String text, boolean isStable);
        /** Final recognition result / 最终识别结果 */
        void onFinalResult(String text);
        /** Connection error / 连接错误 */
        void onError(Exception e);
        /** Connection closed / 连接关闭 */
        void onClose();
    }

    public FunASRClient() {
        this(DEFAULT_FUNASR_URL);
    }

    public FunASRClient(String serverUrl) {
        this.serverUrl = serverUrl;
        logger.info("FunASRClient initialized (Phase 2 placeholder). Server URL: {}", serverUrl);
    }

    /**
     * Connect to FunASR server and start streaming recognition
     * 连接FunASR服务器并开始流式识别
     *
     * @param callback ASR result callback
     */
    public void connect(ASRCallback callback) {
        logger.info("Connecting to FunASR server: {} (placeholder)", serverUrl);
        // Phase 2: Establish WebSocket connection to FunASR server
    }

    /**
     * Send audio chunk for recognition
     * 发送音频块进行识别
     *
     * @param audioChunk PCM audio chunk (16kHz, 16-bit, mono)
     */
    public void sendAudio(byte[] audioChunk) {
        if (!connected) {
            logger.debug("FunASR client not connected, audio chunk discarded");
            return;
        }
        // Phase 2: Send binary audio data via WebSocket
    }

    /**
     * Signal end of audio stream
     */
    public void endStream() {
        // Phase 2: Send end-of-stream signal to FunASR
        logger.debug("FunASR end stream signal sent (placeholder)");
    }

    /**
     * Disconnect from FunASR server
     */
    public void disconnect() {
        connected = false;
        logger.info("Disconnected from FunASR server");
    }

    /**
     * Check if connected to FunASR server
     */
    public boolean isConnected() {
        return connected;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
