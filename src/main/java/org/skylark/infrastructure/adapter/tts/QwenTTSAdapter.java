package org.skylark.infrastructure.adapter.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.skylark.infrastructure.adapter.TTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Alibaba Tongyi Qwen TTS Adapter (CosyVoice / Tongyi Speech Synthesis)
 * 阿里云通义千问TTS适配器（CosyVoice / 通义语音合成）
 *
 * <p>Implements the TTS interface using Alibaba Cloud DashScope
 * CosyVoice speech synthesis API. Delivers high-fidelity, low-latency
 * audio output with rich voice selection and emotion control.</p>
 *
 * <p>Key advantages over MaryTTS:
 * <ul>
 *   <li>First-package latency under 150ms (vs MaryTTS ~800ms)</li>
 *   <li>Natural prosody with emotion support</li>
 *   <li>Zero-shot voice cloning (CosyVoice 2)</li>
 *   <li>20+ Chinese dialects support</li>
 * </ul></p>
 *
 * <p>Configuration parameters:
 * <ul>
 *   <li><b>apiKey</b> (required): Alibaba Cloud DashScope API key</li>
 *   <li><b>model</b> (optional): TTS model (default: "cosyvoice-v1")</li>
 *   <li><b>voice</b> (optional): voice identifier (default: "longxiaochun")</li>
 *   <li><b>serviceUrl</b> (optional): TTS service URL (default: DashScope endpoint)</li>
 *   <li><b>outputFile</b> (optional): output directory for audio files</li>
 *   <li><b>timeout</b> (optional): request timeout in seconds (default: 60)</li>
 *   <li><b>format</b> (optional): audio format (default: "wav")</li>
 *   <li><b>sampleRate</b> (optional): sample rate (default: 22050)</li>
 * </ul></p>
 *
 * <p>Phase 2 component ([B2] in the full-duplex upgrade roadmap).
 * Provides natural-sounding speech that is a significant quality improvement
 * over MaryTTS mechanical output.</p>
 *
 * <p>Example config.yaml usage:
 * <pre>
 * tts:
 *   class_name: org.skylark.infrastructure.adapter.tts.QwenTTSAdapter
 *   apiKey: ${DASHSCOPE_API_KEY}
 *   voice: longxiaochun
 *   model: cosyvoice-v1
 * </pre></p>
 *
 * @author Skylark Team
 * @version 1.0.0
 * @see <a href="https://help.aliyun.com/zh/dashscope/developer-reference/cosyvoice-api">
 *      CosyVoice API</a>
 */
public class QwenTTSAdapter implements TTS {

    private static final Logger logger = LoggerFactory.getLogger(QwenTTSAdapter.class);

    private static final String DEFAULT_SERVICE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2audiores/generation";
    private static final String DEFAULT_MODEL = "cosyvoice-v1";
    private static final String DEFAULT_VOICE = "longxiaochun";
    private static final String DEFAULT_OUTPUT_BASE = "tmp/tts";
    private static final String DEFAULT_FORMAT = "wav";
    private static final int DEFAULT_SAMPLE_RATE = 22050;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final String FILE_EXTENSION = ".wav";

    private final String apiKey;
    private final String model;
    private final String voice;
    private final String serviceUrl;
    private final String outputFile;
    private final String format;
    private final int sampleRate;
    private final int timeout;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a QwenTTSAdapter from configuration map.
     *
     * @param config Configuration map (must contain "apiKey")
     * @throws IllegalArgumentException if apiKey is missing
     */
    public QwenTTSAdapter(Map<String, Object> config) {
        if (config == null || !config.containsKey("apiKey")) {
            throw new IllegalArgumentException("apiKey is required in QwenTTSAdapter configuration");
        }

        this.apiKey = config.get("apiKey").toString();
        this.model = config.getOrDefault("model", DEFAULT_MODEL).toString();
        this.voice = config.getOrDefault("voice", DEFAULT_VOICE).toString();
        this.serviceUrl = config.getOrDefault("serviceUrl", DEFAULT_SERVICE_URL).toString();
        this.outputFile = config.getOrDefault("outputFile", DEFAULT_OUTPUT_BASE).toString();
        this.format = config.getOrDefault("format", DEFAULT_FORMAT).toString();
        this.sampleRate = config.containsKey("sampleRate")
                ? Integer.parseInt(config.get("sampleRate").toString())
                : DEFAULT_SAMPLE_RATE;
        this.timeout = config.containsKey("timeout")
                ? Integer.parseInt(config.get("timeout").toString())
                : DEFAULT_TIMEOUT_SECONDS;

        this.webClient = WebClient.builder()
                .baseUrl(this.serviceUrl)
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .defaultHeader("X-DashScope-Async", "disable")
                .build();
        this.objectMapper = new ObjectMapper();

        logger.info("QwenTTSAdapter initialized: model={}, voice={}, serviceUrl={}, timeout={}s",
                model, voice, serviceUrl, timeout);
    }

    /**
     * Synthesizes speech using Tongyi CosyVoice TTS.
     *
     * <p>Sends text to DashScope CosyVoice API and saves the returned
     * audio stream to a WAV file. Supports streaming download for
     * low-latency first-packet delivery.</p>
     *
     * @param text Text to synthesize
     * @return Absolute path to the generated WAV audio file
     * @throws Exception if synthesis fails
     */
    @Override
    public String synthesize(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("QwenTTS: null or empty text");
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        logger.debug("QwenTTS: synthesizing {} characters with voice={}", text.length(), voice);

        try {
            String outputPath = generateOutputPath();
            downloadAudioData(text, outputPath);

            logger.info("QwenTTS: synthesis complete → {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            logger.error("QwenTTS: synthesis failed", e);
            throw new Exception("Tongyi Qwen TTS synthesis failed: " + e.getMessage(), e);
        }
    }

    private String generateOutputPath() throws IOException {
        String uniqueId = UUID.randomUUID().toString();
        Path dirPath = Paths.get(outputFile);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path filePath = dirPath.resolve(uniqueId + FILE_EXTENSION);
        return filePath.toAbsolutePath().toString();
    }

    private void downloadAudioData(String text, String outputPath) throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("text", text);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("voice", voice);
        parameters.put("format", format);
        parameters.put("sample_rate", sampleRate);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);

        try {
            Flux<DataBuffer> audioFlux = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofSeconds(timeout));

            Path filePath = Paths.get(outputPath);
            Mono<Void> writeMono = DataBufferUtils.write(
                    audioFlux,
                    filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            writeMono.block();

            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                throw new IOException("QwenTTS: received empty audio response from CosyVoice API");
            }

            logger.debug("QwenTTS: saved {} bytes to {}", Files.size(filePath), outputPath);
        } catch (Exception e) {
            // Clean up incomplete file on error
            try {
                Path filePath = Paths.get(outputPath);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException cleanupEx) {
                logger.warn("QwenTTS: failed to clean up incomplete file: {}", outputPath, cleanupEx);
            }
            throw new Exception("QwenTTS: HTTP request failed: " + e.getMessage(), e);
        }
    }

    public String getModel() {
        return model;
    }

    public String getVoice() {
        return voice;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }
}
