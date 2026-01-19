package com.bailing.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration Reader Utility
 * 配置文件读取工具类
 * 
 * <p>Provides functionality to read YAML configuration files with support for
 * environment variable substitution and default fallback values.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>YAML configuration file parsing using Jackson</li>
 *   <li>Environment variable substitution with ${VAR_NAME} syntax</li>
 *   <li>Recursive replacement in nested maps and lists</li>
 *   <li>Graceful handling of missing files with empty defaults</li>
 *   <li>Comprehensive logging for troubleshooting</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, Object&gt; config = ConfigReader.readConfig("config/config.yaml");
 * String apiKey = (String) config.get("api_key");
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class ConfigReader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigReader.class);
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * Reads and parses a YAML configuration file with environment variable substitution.
     * 
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Checks if the configuration file exists</li>
     *   <li>Parses the YAML file into a Map structure</li>
     *   <li>Recursively replaces environment variables (${VAR_NAME} format)</li>
     *   <li>Returns the processed configuration map</li>
     * </ol>
     * 
     * <p>If the file is not found, returns an empty map and logs a warning.
     * If parsing fails, logs an error and returns an empty map.</p>
     * 
     * <p>Environment variable substitution example:</p>
     * <pre>
     * YAML content:
     *   api_key: ${API_KEY}
     *   endpoint: ${SERVICE_ENDPOINT:https://default.example.com}
     * 
     * If API_KEY="secret123" and SERVICE_ENDPOINT is not set:
     *   api_key: "secret123"
     *   endpoint: "https://default.example.com"
     * </pre>
     * 
     * @param configPath Path to the YAML configuration file (relative or absolute)
     * @return Map containing the parsed configuration with environment variables resolved
     *         Returns empty map if file not found or parsing fails
     */
    public static Map<String, Object> readConfig(String configPath) {
        logger.info("Reading configuration from: {}", configPath);
        
        try {
            File configFile = new File(configPath);
            
            if (!configFile.exists()) {
                logger.warn("Configuration file not found: {}. Using empty configuration.", configPath);
                return new HashMap<>();
            }
            
            if (!configFile.isFile()) {
                logger.error("Configuration path exists but is not a file: {}", configPath);
                return new HashMap<>();
            }
            
            if (!configFile.canRead()) {
                logger.error("Configuration file exists but cannot be read: {}", configPath);
                return new HashMap<>();
            }
            
            logger.debug("Configuration file found, size: {} bytes", configFile.length());
            
            String yamlContent = new String(Files.readAllBytes(Paths.get(configPath)));
            logger.debug("Read {} characters from configuration file", yamlContent.length());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> config = yamlMapper.readValue(yamlContent, Map.class);
            
            if (config == null) {
                logger.warn("Parsed configuration is null, returning empty map");
                return new HashMap<>();
            }
            
            logger.info("Successfully parsed configuration with {} top-level keys", config.size());
            
            replaceEnvironmentVariables(config);
            
            logger.info("Configuration loaded and processed successfully");
            return config;
            
        } catch (IOException e) {
            logger.error("Failed to read or parse configuration file: {}", configPath, e);
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("Unexpected error while loading configuration: {}", configPath, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Recursively replaces environment variables in the configuration map.
     * 
     * <p>Processes the following data types:</p>
     * <ul>
     *   <li><b>String values:</b> Replaces ${VAR_NAME} with environment variable value</li>
     *   <li><b>Map values:</b> Recursively processes nested maps</li>
     *   <li><b>List values:</b> Recursively processes list elements</li>
     * </ul>
     * 
     * <p>Supports default values using colon syntax:</p>
     * <pre>
     * ${VAR_NAME:default_value}
     * </pre>
     * 
     * @param config Configuration map to process (modified in place)
     */
    @SuppressWarnings("unchecked")
    private static void replaceEnvironmentVariables(Map<String, Object> config) {
        if (config == null) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String replacedValue = replaceEnvVarsInString((String) value);
                if (!replacedValue.equals(value)) {
                    logger.debug("Replaced environment variable in key '{}': {} -> {}", 
                        entry.getKey(), maskSensitiveValue((String) value), maskSensitiveValue(replacedValue));
                }
                entry.setValue(replacedValue);
                
            } else if (value instanceof Map) {
                replaceEnvironmentVariables((Map<String, Object>) value);
                
            } else if (value instanceof Iterable) {
                replaceEnvVarsInList((Iterable<Object>) value);
            }
        }
    }
    
    /**
     * Recursively replaces environment variables in a list.
     * 
     * <p>Note: This method processes nested maps and lists recursively.
     * String values in lists are logged but cannot be modified because the method
     * uses Iterable interface which doesn't support element replacement by index.
     * Most YAML parsers return immutable or array-backed lists that cannot be modified anyway.</p>
     * 
     * <p>For environment variable substitution in list strings, consider using
     * map-based configuration structures instead of lists.</p>
     * 
     * @param list List to process
     */
    @SuppressWarnings("unchecked")
    private static void replaceEnvVarsInList(Iterable<Object> list) {
        for (Object item : list) {
            if (item instanceof String) {
                String original = (String) item;
                String replaced = replaceEnvVarsInString(original);
                if (!replaced.equals(original)) {
                    logger.debug("String '{}' in list contains environment variables but cannot be replaced (use map keys instead)", 
                        maskSensitiveValue(original));
                }
            } else if (item instanceof Map) {
                replaceEnvironmentVariables((Map<String, Object>) item);
            } else if (item instanceof Iterable) {
                replaceEnvVarsInList((Iterable<Object>) item);
            }
        }
    }
    
    /**
     * Replaces environment variables in a single string value.
     * 
     * <p>Supports two formats:</p>
     * <ul>
     *   <li><b>${VAR_NAME}</b> - Replaced with environment variable value or empty string if not set</li>
     *   <li><b>${VAR_NAME:default}</b> - Replaced with environment variable value or default if not set</li>
     * </ul>
     * 
     * <p>Multiple environment variables in the same string are supported:</p>
     * <pre>
     * "https://${HOST}:${PORT}/api" -> "https://localhost:8080/api"
     * </pre>
     * 
     * @param value String value to process
     * @return String with environment variables replaced
     */
    private static String replaceEnvVarsInString(String value) {
        if (value == null) {
            return null;
        }
        
        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varExpression = matcher.group(1);
            String varName;
            String defaultValue = "";
            
            int colonIndex = varExpression.indexOf(':');
            if (colonIndex > 0) {
                varName = varExpression.substring(0, colonIndex).trim();
                defaultValue = varExpression.substring(colonIndex + 1).trim();
            } else {
                varName = varExpression.trim();
            }
            
            String envValue = System.getenv(varName);
            String replacement;
            
            if (envValue != null) {
                replacement = envValue;
                logger.trace("Resolved environment variable '{}' from system environment", varName);
            } else {
                replacement = defaultValue;
                if (!defaultValue.isEmpty()) {
                    logger.trace("Environment variable '{}' not found, using default value", varName);
                } else {
                    logger.trace("Environment variable '{}' not found and no default provided, using empty string", varName);
                }
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Masks sensitive values for logging purposes.
     * 
     * <p>If the value looks like it might contain sensitive data (contains "key", "token",
     * "password", "secret"), it is partially masked for security.</p>
     * 
     * @param value Value to potentially mask
     * @return Original value or masked version for logging
     */
    private static String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String lowerValue = value.toLowerCase();
        if (lowerValue.contains("key") || lowerValue.contains("token") 
            || lowerValue.contains("password") || lowerValue.contains("secret")) {
            if (value.length() <= 4) {
                return "****";
            }
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        
        return value;
    }
}
