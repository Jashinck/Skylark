package org.skylark.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Dialogue History Management
 * 对话历史管理类
 * 
 * <p>Manages the conversation history between user and assistant, including:
 * <ul>
 *   <li>Adding new messages to history</li>
 *   <li>Maintaining a maximum history length</li>
 *   <li>Persisting history to JSON file</li>
 *   <li>Loading history from JSON file</li>
 *   <li>Converting messages to LLM-compatible format</li>
 * </ul>
 * </p>
 * 
 * <p>This class is thread-safe and can be used in concurrent environments.</p>
 * 
 * @author Skylark Team
 * @version 1.0.0
 */
public class Dialogue {
    
    private static final Logger logger = LoggerFactory.getLogger(Dialogue.class);
    private static final int DEFAULT_MAX_HISTORY_LENGTH = 20;
    
    private final List<Message> messages;
    private final ReadWriteLock lock;
    private final ObjectMapper objectMapper;
    
    private int maxHistoryLength;
    private String historyPath;
    
    /**
     * Creates a new Dialogue instance with specified history file path.
     * 
     * @param historyPath Path to the JSON file for persisting dialogue history
     */
    public Dialogue(String historyPath) {
        this.messages = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.objectMapper = createObjectMapper();
        this.maxHistoryLength = DEFAULT_MAX_HISTORY_LENGTH;
        this.historyPath = historyPath;
        
        logger.info("Dialogue instance created with history path: {}", historyPath);
        
        // Attempt to load existing history
        if (historyPath != null && !historyPath.trim().isEmpty()) {
            try {
                loadHistory();
            } catch (Exception e) {
                logger.warn("Could not load existing history from {}: {}", historyPath, e.getMessage());
            }
        }
    }
    
    /**
     * Creates a new Dialogue instance with default settings.
     */
    public Dialogue() {
        this(null);
    }
    
    /**
     * Creates and configures the Jackson ObjectMapper for JSON serialization.
     * 
     * @return Configured ObjectMapper instance
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    /**
     * Adds a message to the dialogue history.
     * 
     * <p>After adding the message, automatically trims history if it exceeds
     * the maximum length and attempts to save to file.</p>
     * 
     * @param message Message to add
     * @throws IllegalArgumentException if message is null or invalid
     */
    public void addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (!message.isValid()) {
            throw new IllegalArgumentException("Message must have non-empty role and content");
        }
        
        lock.writeLock().lock();
        try {
            messages.add(message);
            logger.debug("Added message: {}", message);
            
            // Trim history if needed
            trimHistory();
            
            // Auto-save after adding message
            try {
                saveHistory();
            } catch (IOException e) {
                logger.warn("Failed to auto-save history after adding message: {}", e.getMessage());
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Returns an unmodifiable view of the message history.
     * 
     * @return Unmodifiable list of messages
     */
    public List<Message> getMessages() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(messages));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Returns messages in LLM-compatible format.
     * 
     * <p>Converts Message objects to a list of Maps with "role" and "content" keys,
     * which is the standard format expected by most LLM APIs.</p>
     * 
     * @return List of maps containing role and content for each message
     */
    public List<Map<String, String>> getLLMMessages() {
        lock.readLock().lock();
        try {
            return messages.stream()
                .map(message -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", message.getRole());
                    map.put("content", message.getContent());
                    return map;
                })
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Loads dialogue history from JSON file.
     * 
     * <p>If the file doesn't exist or is empty, initializes with an empty history.
     * Invalid messages in the file are skipped with a warning.</p>
     * 
     * @throws IOException if file reading fails
     */
    public void loadHistory() throws IOException {
        if (historyPath == null || historyPath.trim().isEmpty()) {
            logger.debug("No history path configured, skipping load");
            return;
        }
        
        Path path = Paths.get(historyPath);
        File file = path.toFile();
        
        if (!file.exists()) {
            logger.info("History file does not exist: {}, starting with empty history", historyPath);
            return;
        }
        
        if (file.length() == 0) {
            logger.info("History file is empty: {}, starting with empty history", historyPath);
            return;
        }
        
        lock.writeLock().lock();
        try {
            logger.info("Loading dialogue history from: {}", historyPath);
            
            Message[] loadedMessages = objectMapper.readValue(file, Message[].class);
            messages.clear();
            
            int validCount = 0;
            int invalidCount = 0;
            
            for (Message message : loadedMessages) {
                if (message != null && message.isValid()) {
                    messages.add(message);
                    validCount++;
                } else {
                    invalidCount++;
                    logger.warn("Skipping invalid message during load: {}", message);
                }
            }
            
            logger.info("Loaded {} valid messages from history (skipped {} invalid)", 
                validCount, invalidCount);
            
        } catch (Exception e) {
            logger.error("Failed to load dialogue history from {}: {}", historyPath, e.getMessage());
            throw new IOException("Failed to load dialogue history", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Saves dialogue history to JSON file.
     * 
     * <p>Creates parent directories if they don't exist. If save fails,
     * logs an error but doesn't throw an exception to prevent disrupting
     * the conversation flow.</p>
     * 
     * @throws IOException if file writing fails
     */
    public void saveHistory() throws IOException {
        if (historyPath == null || historyPath.trim().isEmpty()) {
            logger.debug("No history path configured, skipping save");
            return;
        }
        
        lock.readLock().lock();
        try {
            // Create parent directories if they don't exist
            Path path = Paths.get(historyPath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.debug("Created parent directories for history file: {}", parentDir);
            }
            
            logger.debug("Saving {} messages to history file: {}", messages.size(), historyPath);
            
            objectMapper.writeValue(path.toFile(), messages);
            
            logger.info("Successfully saved dialogue history to: {}", historyPath);
            
        } catch (Exception e) {
            logger.error("Failed to save dialogue history to {}: {}", historyPath, e.getMessage());
            throw new IOException("Failed to save dialogue history", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Trims the message history to maintain maximum length.
     * 
     * <p>Removes oldest messages (excluding system messages) when history
     * exceeds the maximum length. System messages are always preserved.</p>
     */
    private void trimHistory() {
        if (messages.size() <= maxHistoryLength) {
            return;
        }
        
        logger.debug("Trimming history from {} to {} messages", messages.size(), maxHistoryLength);
        
        // Separate system messages from conversation messages
        List<Message> systemMessages = messages.stream()
            .filter(msg -> "system".equalsIgnoreCase(msg.getRole()))
            .collect(Collectors.toList());
        
        List<Message> conversationMessages = messages.stream()
            .filter(msg -> !"system".equalsIgnoreCase(msg.getRole()))
            .collect(Collectors.toList());
        
        // Calculate how many conversation messages to keep
        int systemCount = systemMessages.size();
        int maxConversationMessages = Math.max(0, maxHistoryLength - systemCount);
        
        // Keep only the most recent conversation messages
        if (conversationMessages.size() > maxConversationMessages) {
            int startIndex = conversationMessages.size() - maxConversationMessages;
            conversationMessages = conversationMessages.subList(startIndex, conversationMessages.size());
        }
        
        // Rebuild messages list: system messages first, then conversation
        messages.clear();
        messages.addAll(systemMessages);
        messages.addAll(conversationMessages);
        
        logger.debug("History trimmed to {} messages ({} system, {} conversation)", 
            messages.size(), systemMessages.size(), conversationMessages.size());
    }
    
    /**
     * Clears all messages from the dialogue history.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int previousSize = messages.size();
            messages.clear();
            logger.info("Cleared {} messages from dialogue history", previousSize);
            
            // Save empty history
            try {
                saveHistory();
            } catch (IOException e) {
                logger.warn("Failed to save empty history: {}", e.getMessage());
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current number of messages in history.
     * 
     * @return Number of messages
     */
    public int size() {
        lock.readLock().lock();
        try {
            return messages.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Getters and setters
    
    public int getMaxHistoryLength() {
        return maxHistoryLength;
    }
    
    public void setMaxHistoryLength(int maxHistoryLength) {
        if (maxHistoryLength < 1) {
            throw new IllegalArgumentException("Max history length must be at least 1");
        }
        
        lock.writeLock().lock();
        try {
            this.maxHistoryLength = maxHistoryLength;
            logger.info("Max history length set to: {}", maxHistoryLength);
            trimHistory();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public String getHistoryPath() {
        return historyPath;
    }
    
    public void setHistoryPath(String historyPath) {
        lock.writeLock().lock();
        try {
            this.historyPath = historyPath;
            logger.info("History path set to: {}", historyPath);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
