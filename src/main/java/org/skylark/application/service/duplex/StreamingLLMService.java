package org.skylark.application.service.duplex;

import org.skylark.application.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming LLM Service — enhances AgentService with streaming output
 * 流式LLM服务 —— 增强AgentService，支持流式token输出
 *
 * <p>Phase 1: Wraps existing AgentService.chat() with async interface + sentence splitting.
 * Phase 2: Uses OpenAI-compatible Streaming API (SSE) for token-by-token output.</p>
 *
 * <p>Sentence splitting strategy: accumulate tokens until sentence boundary
 * (period/question mark/exclamation mark) then send complete sentence to TTS.</p>
 *
 * @author Skylark Team
 * @version 1.0.0
 */
public class StreamingLLMService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingLLMService.class);

    /** Sentence boundary characters (Chinese + English) */
    private static final String SENTENCE_BOUNDARIES = "。！？.!?\n";

    private final AgentService agentService;
    private final Map<String, CompletableFuture<Void>> activeTasks = new ConcurrentHashMap<>();

    /**
     * Token stream callback interface
     * Token流回调接口
     */
    public interface TokenStreamCallback {
        /** Called for each LLM token / 每个LLM token的回调 */
        void onToken(String token);
        /** Called when a complete sentence is formed / 完整句子形成时的回调 */
        void onSentenceComplete(String sentence);
        /** Called when the complete response is done / 完整回复完成时的回调 */
        void onComplete(String fullResponse);
        /** Called on error / 错误回调 */
        void onError(Exception e);
    }

    public StreamingLLMService(AgentService agentService) {
        this.agentService = agentService;
        logger.info("StreamingLLMService initialized (Phase 1: sentence-splitting wrapper)");
    }

    /**
     * Streaming chat — async version with sentence splitting
     * 流式对话 —— 带分句策略的异步版本
     *
     * <p>Phase 1: Gets full response from AgentService.chat(), then splits
     * into sentences and delivers them via callback to simulate streaming.</p>
     *
     * @param sessionId session identifier
     * @param text user input text
     * @param callback token stream callback
     * @return cancellable future
     */
    public CompletableFuture<Void> chatStream(String sessionId, String text, TokenStreamCallback callback) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting streaming chat for session {}: {}", sessionId, text);

                // Phase 1: Get full response from existing AgentService
                String fullResponse = agentService.chat(sessionId, text);

                if (Thread.currentThread().isInterrupted()) {
                    logger.info("Streaming chat cancelled for session {}", sessionId);
                    return;
                }

                if (fullResponse == null || fullResponse.trim().isEmpty()) {
                    callback.onComplete("");
                    return;
                }

                // Split response into sentences and deliver via callback
                splitAndDeliverSentences(fullResponse, callback);

                callback.onComplete(fullResponse);
                logger.info("Completed streaming chat for session {}", sessionId);

            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    logger.error("Error in streaming chat for session {}", sessionId, e);
                    callback.onError(e);
                }
            }
        });

        activeTasks.put(sessionId, future);
        return future;
    }

    /**
     * Cancel streaming for a session (barge-in)
     * 取消会话的流式处理（打断时调用）
     *
     * @param sessionId session identifier
     */
    public void cancelStream(String sessionId) {
        CompletableFuture<Void> task = activeTasks.remove(sessionId);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            logger.info("Cancelled streaming LLM for session {}", sessionId);
        }
    }

    /**
     * Check if a session has an active streaming task
     */
    public boolean isStreaming(String sessionId) {
        CompletableFuture<Void> task = activeTasks.get(sessionId);
        return task != null && !task.isDone();
    }

    /**
     * Split full response text into sentences and deliver via callback
     * 将完整回复分句并通过回调传递
     */
    void splitAndDeliverSentences(String text, TokenStreamCallback callback) {
        StringBuilder sentenceBuffer = new StringBuilder();

        for (char c : text.toCharArray()) {
            sentenceBuffer.append(c);
            callback.onToken(String.valueOf(c));

            if (isSentenceBoundary(c) && sentenceBuffer.length() > 0) {
                String sentence = sentenceBuffer.toString().trim();
                if (!sentence.isEmpty()) {
                    callback.onSentenceComplete(sentence);
                }
                sentenceBuffer.setLength(0);
            }
        }

        // Deliver remaining text as last sentence
        if (sentenceBuffer.length() > 0) {
            String remaining = sentenceBuffer.toString().trim();
            if (!remaining.isEmpty()) {
                callback.onSentenceComplete(remaining);
            }
        }
    }

    /**
     * Check if a character is a sentence boundary
     * 检查字符是否为句子边界
     */
    static boolean isSentenceBoundary(char c) {
        return SENTENCE_BOUNDARIES.indexOf(c) >= 0;
    }
}
