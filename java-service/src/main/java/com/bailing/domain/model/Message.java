package com.bailing.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message Entity for Dialogue History
 * 对话消息实体类
 * 
 * <p>Represents a single message in the conversation between user and assistant.
 * This class is used to maintain dialogue history and is serializable to JSON
 * for persistence and LLM API communication.</p>
 * 
 * <p>Typical roles include:</p>
 * <ul>
 *   <li><b>system</b> - System prompts and instructions</li>
 *   <li><b>user</b> - User input messages</li>
 *   <li><b>assistant</b> - AI assistant responses</li>
 * </ul>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    
    /**
     * Role of the message sender (system, user, or assistant)
     * 消息发送者角色
     */
    @JsonProperty("role")
    private String role;
    
    /**
     * Content of the message
     * 消息内容
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * Timestamp when the message was created (Unix timestamp in milliseconds)
     * 消息创建时间戳（毫秒）
     */
    @JsonProperty("timestamp")
    private long timestamp;
    
    /**
     * Creates a new Message with role and content, automatically setting timestamp.
     * 
     * <p>This constructor is convenient for creating messages without manually
     * specifying the timestamp.</p>
     * 
     * @param role Role of the message sender (system, user, or assistant)
     * @param content Content of the message
     */
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Validates if this message has valid required fields.
     * 
     * @return true if role and content are both non-null and non-empty
     */
    public boolean isValid() {
        return role != null && !role.trim().isEmpty() 
            && content != null && !content.trim().isEmpty();
    }
    
    /**
     * Creates a system message.
     * 
     * @param content System message content
     * @return New Message instance with system role
     */
    public static Message system(String content) {
        return new Message("system", content);
    }
    
    /**
     * Creates a user message.
     * 
     * @param content User message content
     * @return New Message instance with user role
     */
    public static Message user(String content) {
        return new Message("user", content);
    }
    
    /**
     * Creates an assistant message.
     * 
     * @param content Assistant message content
     * @return New Message instance with assistant role
     */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }
    
    @Override
    public String toString() {
        return String.format("Message{role='%s', content='%s', timestamp=%d}", 
            role, content != null && content.length() > 50 
                ? content.substring(0, 50) + "..." 
                : content, 
            timestamp);
    }
}
