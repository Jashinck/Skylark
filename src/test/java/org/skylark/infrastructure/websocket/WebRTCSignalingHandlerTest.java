package org.skylark.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skylark.application.service.OrchestrationService;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTCSignalingHandler
 */
@ExtendWith(MockitoExtension.class)
class WebRTCSignalingHandlerTest {

    @Mock
    private OrchestrationService orchestrationService;

    @Mock
    private WebSocketSession session;

    private WebRTCSignalingHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new WebRTCSignalingHandler(orchestrationService);
        objectMapper = new ObjectMapper();
        lenient().when(session.getId()).thenReturn("test-session");
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Test
    void testAfterConnectionEstablished() throws Exception {
        // Act
        handler.afterConnectionEstablished(session);

        // Assert
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_Offer() throws Exception {
        // Arrange
        Map<String, String> offer = new HashMap<>();
        offer.put("type", "offer");
        offer.put("sdp", "test-sdp");
        String jsonMessage = objectMapper.writeValueAsString(offer);
        TextMessage message = new TextMessage(jsonMessage);

        // Act
        handler.handleTextMessage(session, message);

        // Assert
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_TextInput() throws Exception {
        // Arrange
        handler.afterConnectionEstablished(session);
        
        Map<String, String> textInput = new HashMap<>();
        textInput.put("type", "text");
        textInput.put("content", "Hello");
        String jsonMessage = objectMapper.writeValueAsString(textInput);
        TextMessage message = new TextMessage(jsonMessage);

        // Act
        handler.handleTextMessage(session, message);

        // Assert
        verify(orchestrationService, times(1)).processTextInput(
            eq("test-session"),
            eq("Hello"),
            any(OrchestrationService.ResponseCallback.class)
        );
    }

    @Test
    void testHandleBinaryMessage() throws Exception {
        // Arrange
        handler.afterConnectionEstablished(session);
        
        byte[] audioData = new byte[1024];
        ByteBuffer buffer = ByteBuffer.wrap(audioData);
        BinaryMessage message = new BinaryMessage(buffer);

        // Act
        handler.handleBinaryMessage(session, message);

        // Assert
        verify(orchestrationService, times(1)).processAudioStream(
            eq("test-session"),
            any(byte[].class),
            any(OrchestrationService.ResponseCallback.class)
        );
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        // Arrange
        handler.afterConnectionEstablished(session);

        // Act
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        verify(orchestrationService, times(1)).cleanupSession("test-session");
    }

    @Test
    void testHandleTransportError() throws Exception {
        // Arrange
        handler.afterConnectionEstablished(session);
        Exception error = new Exception("Transport error");

        // Act
        handler.handleTransportError(session, error);

        // Assert
        verify(orchestrationService, times(1)).cleanupSession("test-session");
    }

    @Test
    void testHandleTextMessage_InvalidJson() throws Exception {
        // Arrange
        TextMessage message = new TextMessage("invalid json");

        // Act & Assert
        // Should not throw exception, should send error message instead
        handler.handleTextMessage(session, message);
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }
}
