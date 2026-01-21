package com.bailing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Component Factory
 * 组件工厂类
 * 
 * <p>Factory class for dynamically creating component instances based on
 * configuration. Uses reflection to instantiate classes specified in the
 * configuration files.</p>
 * 
 * <p>Supported component types:</p>
 * <ul>
 *   <li><b>ASR</b> - Automatic Speech Recognition</li>
 *   <li><b>VAD</b> - Voice Activity Detection</li>
 *   <li><b>TTS</b> - Text-to-Speech</li>
 *   <li><b>LLM</b> - Large Language Model</li>
 *   <li><b>Recorder</b> - Audio Recording</li>
 *   <li><b>Player</b> - Audio Playback</li>
 * </ul>
 * 
 * <p>Configuration format example:</p>
 * <pre>
 * asr:
 *   class_name: "com.bailing.asr.AlibabaASR"
 *   api_key: "${ASR_API_KEY}"
 *   sample_rate: 16000
 * </pre>
 * 
 * <p>Usage example:</p>
 * <pre>
 * Map&lt;String, Object&gt; config = ConfigReader.readConfig("config.yaml");
 * Map&lt;String, Object&gt; asrConfig = (Map) config.get("asr");
 * Object asr = ComponentFactory.createASR(asrConfig);
 * </pre>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
public class ComponentFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ComponentFactory.class);
    
    private static final String CLASS_NAME_KEY = "class_name";
    private static ApplicationContext springContext;
    
    /**
     * Sets the Spring application context for dependency injection.
     * 
     * @param context Spring application context
     */
    public static void setSpringContext(ApplicationContext context) {
        springContext = context;
        logger.info("Spring context set in ComponentFactory");
    }
    
    /**
     * Creates an ASR (Automatic Speech Recognition) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.asr.AlibabaASR",
     *   "api_key": "your-api-key",
     *   "sample_rate": 16000
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return ASR instance, or null if creation fails
     */
    public static Object createASR(Map<String, Object> config) {
        return createComponent("ASR", config);
    }
    
    /**
     * Creates a VAD (Voice Activity Detection) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.vad.WebRTCVAD",
     *   "sample_rate": 16000,
     *   "frame_duration_ms": 30
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return VAD instance, or null if creation fails
     */
    public static Object createVAD(Map<String, Object> config) {
        return createComponent("VAD", config);
    }
    
    /**
     * Creates a TTS (Text-to-Speech) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.tts.AlibabaTTS",
     *   "api_key": "your-api-key",
     *   "voice": "xiaoyun"
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return TTS instance, or null if creation fails
     */
    public static Object createTTS(Map<String, Object> config) {
        return createComponent("TTS", config);
    }
    
    /**
     * Creates an LLM (Large Language Model) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.llm.OpenAILLM",
     *   "api_key": "your-api-key",
     *   "model": "gpt-4",
     *   "temperature": 0.7
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return LLM instance, or null if creation fails
     */
    public static Object createLLM(Map<String, Object> config) {
        return createComponent("LLM", config);
    }
    
    /**
     * Creates a Recorder (Audio Recording) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.audio.JavaSoundRecorder",
     *   "sample_rate": 16000,
     *   "channels": 1
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return Recorder instance, or null if creation fails
     */
    public static Object createRecorder(Map<String, Object> config) {
        return createComponent("Recorder", config);
    }
    
    /**
     * Creates a Player (Audio Playback) instance from configuration.
     * 
     * <p>The configuration map should contain a "class_name" key specifying the
     * fully qualified class name to instantiate. The class should have a constructor
     * that accepts a Map&lt;String, Object&gt; parameter.</p>
     * 
     * <p>Example configuration:</p>
     * <pre>
     * {
     *   "class_name": "com.bailing.audio.JavaSoundPlayer",
     *   "sample_rate": 16000,
     *   "buffer_size": 4096
     * }
     * </pre>
     * 
     * @param config Configuration map containing class_name and component parameters
     * @return Player instance, or null if creation fails
     */
    public static Object createPlayer(Map<String, Object> config) {
        return createComponent("Player", config);
    }
    
    /**
     * Generic component creation method using reflection.
     * 
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Validates configuration and extracts class_name</li>
     *   <li>Loads the class using reflection</li>
     *   <li>Finds a constructor that accepts Map&lt;String, Object&gt;</li>
     *   <li>Instantiates the class with the configuration</li>
     *   <li>Returns the created instance</li>
     * </ol>
     * 
     * <p>Error handling:</p>
     * <ul>
     *   <li>Null or empty configuration returns null</li>
     *   <li>Missing class_name returns null</li>
     *   <li>Class not found logs error and returns null</li>
     *   <li>Constructor not found logs error and returns null</li>
     *   <li>Instantiation errors log error and returns null</li>
     * </ul>
     * 
     * @param componentType Type name for logging purposes (e.g., "ASR", "TTS")
     * @param config Configuration map containing class_name and parameters
     * @return Component instance, or null if creation fails
     */
    private static Object createComponent(String componentType, Map<String, Object> config) {
        logger.debug("Creating {} component from configuration", componentType);
        
        if (config == null || config.isEmpty()) {
            logger.warn("Cannot create {} component: configuration is null or empty", componentType);
            return null;
        }
        
        String className = (String) config.get(CLASS_NAME_KEY);
        
        if (className == null || className.trim().isEmpty()) {
            logger.error("Cannot create {} component: '{}' not specified in configuration", 
                componentType, CLASS_NAME_KEY);
            return null;
        }
        
        className = className.trim();
        logger.info("Creating {} component with class: {}", componentType, className);
        
        try {
            Class<?> componentClass = Class.forName(className);
            logger.debug("Successfully loaded class: {}", className);
            
            Constructor<?> constructor = findMapConstructor(componentClass);
            
            if (constructor == null) {
                logger.error("No suitable constructor found for class: {}. " +
                    "Class must have a constructor accepting Map<String, Object>", className);
                return null;
            }
            
            Object instance = constructor.newInstance(config);
            logger.info("Successfully created {} component: {}", componentType, className);
            
            // Inject Spring dependencies if available
            if (springContext != null) {
                injectSpringDependencies(instance, componentType);
            }
            
            return instance;
            
        } catch (ClassNotFoundException e) {
            logger.error("Failed to create {} component: class not found: {}", 
                componentType, className, e);
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to create {} component: {}", componentType, className, e);
            return null;
        }
    }
    
    /**
     * Finds a constructor that accepts a Map parameter.
     * 
     * <p>Looks for constructors in the following order:</p>
     * <ol>
     *   <li>Constructor with Map&lt;String, Object&gt; parameter</li>
     *   <li>Constructor with Map parameter (raw type)</li>
     * </ol>
     * 
     * @param clazz Class to search for constructor
     * @return Constructor accepting Map parameter, or null if not found
     */
    @SuppressWarnings("unchecked")
    private static Constructor<?> findMapConstructor(Class<?> clazz) throws NoSuchMethodException {
        try {
            return clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException e) {
            logger.debug("No constructor with Map parameter found, checking all constructors");
            
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == 1 && Map.class.isAssignableFrom(paramTypes[0])) {
                    logger.debug("Found compatible Map constructor: {}", constructor);
                    return constructor;
                }
            }
            
            throw new NoSuchMethodException(
                "No constructor found that accepts Map parameter in class: " + clazz.getName());
        }
    }
    
    /**
     * Injects Spring dependencies into a component instance.
     * 
     * <p>Looks for setter methods and attempts to inject Spring beans.</p>
     * 
     * @param instance Component instance
     * @param componentType Component type for logging
     */
    private static void injectSpringDependencies(Object instance, String componentType) {
        try {
            String className = instance.getClass().getSimpleName();
            
            // For Direct adapters, inject the appropriate service
            if ("DirectASRAdapter".equals(className)) {
                injectService(instance, "setAsrService", "ASRService");
            } else if ("DirectVADAdapter".equals(className)) {
                injectService(instance, "setVadService", "VADService");
            } else if ("DirectTTSAdapter".equals(className)) {
                injectService(instance, "setTtsService", "TTSService");
            }
            
        } catch (Exception e) {
            logger.warn("Failed to inject Spring dependencies into {}: {}", componentType, e.getMessage());
        }
    }
    
    /**
     * Injects a specific service into an instance via setter method.
     * 
     * @param instance Instance to inject into
     * @param setterName Name of setter method
     * @param serviceName Name of service class (simple name)
     */
    private static void injectService(Object instance, String setterName, String serviceName) {
        if (springContext == null) {
            logger.debug("Spring context not available, skipping dependency injection");
            return;
        }
        
        try {
            // Use fully qualified class name for security
            String fullClassName = "com.bailing.service." + serviceName;
            Class<?> serviceClass = Class.forName(fullClassName);
            Object service = springContext.getBean(serviceClass);
            
            // Try to find and invoke the setter method
            Class<?>[] interfaces = serviceClass.getInterfaces();
            Method setter = null;
            
            // Try with interface if available
            if (interfaces != null && interfaces.length > 0) {
                try {
                    setter = instance.getClass().getMethod(setterName, interfaces[0]);
                } catch (NoSuchMethodException e) {
                    // Continue to try with concrete class
                }
            }
            
            // If no interface setter found, try with concrete class
            if (setter == null) {
                setter = instance.getClass().getMethod(setterName, serviceClass);
            }
            
            setter.invoke(instance, service);
            logger.info("Injected {} into {}", serviceName, instance.getClass().getSimpleName());
            
        } catch (ClassNotFoundException e) {
            logger.warn("Service class not found: {}", serviceName);
        } catch (NoSuchMethodException e) {
            logger.warn("Setter method {} not found in {}", setterName, instance.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to inject {}: {}", serviceName, e.getMessage());
        }
    }
    
    /**
     * Checks if a component class is available in the classpath.
     * 
     * <p>Useful for validating configuration before attempting to create components.</p>
     * 
     * @param className Fully qualified class name to check
     * @return true if class exists and can be loaded
     */
    public static boolean isClassAvailable(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        
        try {
            Class.forName(className.trim());
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug("Class not available: {}", className);
            return false;
        }
    }
    
    /**
     * Validates component configuration.
     * 
     * <p>Checks that:</p>
     * <ul>
     *   <li>Configuration is not null or empty</li>
     *   <li>class_name key exists and is not empty</li>
     *   <li>Specified class is available in classpath</li>
     * </ul>
     * 
     * @param config Configuration map to validate
     * @param componentType Component type name for error messages
     * @return true if configuration is valid
     */
    public static boolean isValidConfig(Map<String, Object> config, String componentType) {
        if (config == null || config.isEmpty()) {
            logger.warn("{} configuration is null or empty", componentType);
            return false;
        }
        
        String className = (String) config.get(CLASS_NAME_KEY);
        if (className == null || className.trim().isEmpty()) {
            logger.warn("{} configuration missing '{}' key", componentType, CLASS_NAME_KEY);
            return false;
        }
        
        if (!isClassAvailable(className)) {
            logger.warn("{} class not available: {}", componentType, className);
            return false;
        }
        
        return true;
    }
}
