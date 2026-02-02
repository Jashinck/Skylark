package com.bailing.infrastructure.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM interface for streaming chat interactions.
 * Implementations should support streaming responses with chunk-by-chunk delivery.
 */
public interface LLM {
    
    /**
     * Performs a streaming chat interaction with the LLM.
     * 
     * @param messages List of message objects, each containing role and content.
     *                 Example: [{"role": "user", "content": "Hello"}]
     * @param onChunk Callback invoked for each text chunk received from the stream.
     *                Receives the content string for each chunk.
     * @param onComplete Callback invoked when the stream completes successfully.
     * @throws Exception if an error occurs during the streaming process
     */
    void chat(List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onComplete) throws Exception;
}
