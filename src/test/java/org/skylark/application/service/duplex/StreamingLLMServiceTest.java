package org.skylark.application.service.duplex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.AgentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StreamingLLMService
 */
@ExtendWith(MockitoExtension.class)
class StreamingLLMServiceTest {

    @Mock
    private AgentService agentService;

    private StreamingLLMService streamingLLMService;

    @BeforeEach
    void setUp() {
        streamingLLMService = new StreamingLLMService(agentService);
    }

    @Test
    void testChatStream_CallsAgentServiceAndDeliversSentences() throws Exception {
        // Arrange
        String response = "你好。我是AI助手。";
        when(agentService.chat(eq("session-1"), eq("hello"))).thenReturn(response);

        List<String> sentences = new ArrayList<>();
        AtomicReference<String> fullResponse = new AtomicReference<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, fullResponse);

        // Act
        CompletableFuture<Void> future = streamingLLMService.chatStream("session-1", "hello", callback);
        future.get(5, TimeUnit.SECONDS);

        // Assert
        verify(agentService).chat("session-1", "hello");
        assertEquals(response, fullResponse.get());
        assertEquals(2, sentences.size());
        assertEquals("你好。", sentences.get(0));
        assertEquals("我是AI助手。", sentences.get(1));
    }

    @Test
    void testChatStream_WithNullResponse_CompletesEmpty() throws Exception {
        // Arrange
        when(agentService.chat(eq("session-1"), eq("hello"))).thenReturn(null);

        AtomicReference<String> fullResponse = new AtomicReference<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(new ArrayList<>(), fullResponse);

        // Act
        CompletableFuture<Void> future = streamingLLMService.chatStream("session-1", "hello", callback);
        future.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals("", fullResponse.get());
    }

    @Test
    void testChatStream_WithEmptyResponse_CompletesEmpty() throws Exception {
        // Arrange
        when(agentService.chat(eq("session-1"), eq("hello"))).thenReturn("   ");

        AtomicReference<String> fullResponse = new AtomicReference<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(new ArrayList<>(), fullResponse);

        // Act
        CompletableFuture<Void> future = streamingLLMService.chatStream("session-1", "hello", callback);
        future.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals("", fullResponse.get());
    }

    @Test
    void testSplitAndDeliverSentences_ChinesePunctuation() {
        // Arrange
        List<String> sentences = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, new AtomicReference<>());

        // Act
        streamingLLMService.splitAndDeliverSentences("第一句。第二句！第三句？", callback);

        // Assert
        assertEquals(3, sentences.size());
        assertEquals("第一句。", sentences.get(0));
        assertEquals("第二句！", sentences.get(1));
        assertEquals("第三句？", sentences.get(2));
    }

    @Test
    void testSplitAndDeliverSentences_EnglishPunctuation() {
        // Arrange
        List<String> sentences = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, new AtomicReference<>());

        // Act
        streamingLLMService.splitAndDeliverSentences("Hello. World! Done?", callback);

        // Assert
        assertEquals(3, sentences.size());
        assertEquals("Hello.", sentences.get(0));
        assertEquals("World!", sentences.get(1));
        assertEquals("Done?", sentences.get(2));
    }

    @Test
    void testSplitAndDeliverSentences_MixedLanguages() {
        // Arrange
        List<String> sentences = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, new AtomicReference<>());

        // Act
        streamingLLMService.splitAndDeliverSentences("Hello。你好！OK.", callback);

        // Assert
        assertEquals(3, sentences.size());
        assertEquals("Hello。", sentences.get(0));
        assertEquals("你好！", sentences.get(1));
        assertEquals("OK.", sentences.get(2));
    }

    @Test
    void testSplitAndDeliverSentences_NoBoundary_DeliversAsOneSentence() {
        // Arrange
        List<String> sentences = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, new AtomicReference<>());

        // Act
        streamingLLMService.splitAndDeliverSentences("no punctuation here", callback);

        // Assert
        assertEquals(1, sentences.size());
        assertEquals("no punctuation here", sentences.get(0));
    }

    @Test
    void testSplitAndDeliverSentences_NewlineBoundary() {
        // Arrange
        List<String> sentences = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = createCallback(sentences, new AtomicReference<>());

        // Act
        streamingLLMService.splitAndDeliverSentences("Line1\nLine2", callback);

        // Assert
        assertEquals(2, sentences.size());
    }

    @Test
    void testSplitAndDeliverSentences_TokensDelivered() {
        // Arrange
        List<String> tokens = new ArrayList<>();
        StreamingLLMService.TokenStreamCallback callback = new StreamingLLMService.TokenStreamCallback() {
            @Override public void onToken(String token) { tokens.add(token); }
            @Override public void onSentenceComplete(String sentence) {}
            @Override public void onComplete(String fullResponse) {}
            @Override public void onError(Exception e) {}
        };

        // Act
        streamingLLMService.splitAndDeliverSentences("AB", callback);

        // Assert - each character is a token
        assertEquals(2, tokens.size());
        assertEquals("A", tokens.get(0));
        assertEquals("B", tokens.get(1));
    }

    @Test
    void testIsSentenceBoundary_ChinesePunctuation() {
        assertTrue(StreamingLLMService.isSentenceBoundary('。'));
        assertTrue(StreamingLLMService.isSentenceBoundary('！'));
        assertTrue(StreamingLLMService.isSentenceBoundary('？'));
    }

    @Test
    void testIsSentenceBoundary_EnglishPunctuation() {
        assertTrue(StreamingLLMService.isSentenceBoundary('.'));
        assertTrue(StreamingLLMService.isSentenceBoundary('!'));
        assertTrue(StreamingLLMService.isSentenceBoundary('?'));
    }

    @Test
    void testIsSentenceBoundary_Newline() {
        assertTrue(StreamingLLMService.isSentenceBoundary('\n'));
    }

    @Test
    void testIsSentenceBoundary_NonBoundaryChars() {
        assertFalse(StreamingLLMService.isSentenceBoundary(','));
        assertFalse(StreamingLLMService.isSentenceBoundary('a'));
        assertFalse(StreamingLLMService.isSentenceBoundary(' '));
        assertFalse(StreamingLLMService.isSentenceBoundary('、'));
    }

    @Test
    void testCancelStream_CancelsActiveTask() throws Exception {
        // Arrange
        when(agentService.chat(anyString(), anyString())).thenAnswer(invocation -> {
            Thread.sleep(5000);
            return "delayed response";
        });

        StreamingLLMService.TokenStreamCallback callback = createCallback(new ArrayList<>(), new AtomicReference<>());
        streamingLLMService.chatStream("session-1", "hello", callback);

        // Give time for the task to start
        Thread.sleep(100);

        // Act
        streamingLLMService.cancelStream("session-1");

        // Assert
        assertFalse(streamingLLMService.isStreaming("session-1"));
    }

    @Test
    void testCancelStream_NonExistingSession_NoOp() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> streamingLLMService.cancelStream("nonexistent"));
    }

    @Test
    void testIsStreaming_ActiveTask_ReturnsTrue() throws Exception {
        // Arrange
        when(agentService.chat(anyString(), anyString())).thenAnswer(invocation -> {
            Thread.sleep(5000);
            return "delayed";
        });

        StreamingLLMService.TokenStreamCallback callback = createCallback(new ArrayList<>(), new AtomicReference<>());
        streamingLLMService.chatStream("session-1", "hello", callback);

        // Give time for the task to start
        Thread.sleep(100);

        // Act & Assert
        assertTrue(streamingLLMService.isStreaming("session-1"));

        // Cleanup
        streamingLLMService.cancelStream("session-1");
    }

    @Test
    void testIsStreaming_CompletedTask_ReturnsFalse() throws Exception {
        // Arrange
        when(agentService.chat(eq("session-1"), eq("hi"))).thenReturn("response");
        StreamingLLMService.TokenStreamCallback callback = createCallback(new ArrayList<>(), new AtomicReference<>());

        CompletableFuture<Void> future = streamingLLMService.chatStream("session-1", "hi", callback);
        future.get(5, TimeUnit.SECONDS);

        // Act & Assert
        assertFalse(streamingLLMService.isStreaming("session-1"));
    }

    @Test
    void testIsStreaming_NonExistingSession_ReturnsFalse() {
        assertFalse(streamingLLMService.isStreaming("nonexistent"));
    }

    @Test
    void testChatStream_OnError_CallsErrorCallback() throws Exception {
        // Arrange
        when(agentService.chat(anyString(), anyString())).thenThrow(new RuntimeException("LLM error"));

        AtomicReference<Exception> errorRef = new AtomicReference<>();
        StreamingLLMService.TokenStreamCallback callback = new StreamingLLMService.TokenStreamCallback() {
            @Override public void onToken(String token) {}
            @Override public void onSentenceComplete(String sentence) {}
            @Override public void onComplete(String fullResponse) {}
            @Override public void onError(Exception e) { errorRef.set(e); }
        };

        // Act
        CompletableFuture<Void> future = streamingLLMService.chatStream("session-1", "hello", callback);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Future may complete exceptionally
        }

        // Assert
        assertNotNull(errorRef.get());
        assertEquals("LLM error", errorRef.get().getMessage());
    }

    private StreamingLLMService.TokenStreamCallback createCallback(
            List<String> sentences, AtomicReference<String> fullResponse) {
        return new StreamingLLMService.TokenStreamCallback() {
            @Override public void onToken(String token) {}
            @Override public void onSentenceComplete(String sentence) { sentences.add(sentence); }
            @Override public void onComplete(String response) { fullResponse.set(response); }
            @Override public void onError(Exception e) {}
        };
    }
}
