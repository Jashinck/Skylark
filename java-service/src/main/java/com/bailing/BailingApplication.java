package com.bailing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Bailing Voice Assistant - Main Application Entry Point
 * 百聆智能语音助手 - 主应用程序入口
 * 
 * <p>This is the main Spring Boot application class that initializes and starts
 * the Bailing voice assistant service. It handles configuration loading and
 * robot instance management.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@SpringBootApplication
public class BailingApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(BailingApplication.class);
    private static final String DEFAULT_CONFIG_PATH = "config/config.yaml";
    
    /**
     * Main entry point for the Bailing Voice Assistant application.
     * 
     * <p>Accepts command line arguments to specify a custom configuration file path.
     * If no arguments are provided, uses the default configuration path.</p>
     * 
     * <p>Usage:</p>
     * <pre>
     * java -jar bailing-java.jar                          # Use default config
     * java -jar bailing-java.jar config/custom.yaml       # Use custom config
     * </pre>
     * 
     * @param args Command line arguments. First argument is optional config file path.
     */
    public static void main(String[] args) {
        try {
            logger.info("========================================");
            logger.info("  Bailing Voice Assistant Starting");
            logger.info("  百聆智能语音助手启动中");
            logger.info("========================================");
            
            // Determine config file path
            String configPath = getConfigPath(args);
            logger.info("Loading configuration from: {}", configPath);
            
            // Set config path as system property for Spring to access
            System.setProperty("bailing.config.path", configPath);
            
            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(BailingApplication.class, args);
            
            logger.info("Spring Boot application context initialized successfully");
            
            // Start Robot instance in separate thread
            startRobotThread(context, configPath);
            
            logger.info("========================================");
            logger.info("  Bailing Voice Assistant Started");
            logger.info("  百聆智能语音助手已启动");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Failed to start Bailing Voice Assistant", e);
            System.exit(1);
        }
    }
    
    /**
     * Determines the configuration file path from command line arguments.
     * 
     * @param args Command line arguments
     * @return Configuration file path (from args[0] or default)
     */
    private static String getConfigPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            String configPath = args[0].trim();
            logger.info("Using config path from command line argument: {}", configPath);
            return configPath;
        }
        logger.info("No config path provided, using default: {}", DEFAULT_CONFIG_PATH);
        return DEFAULT_CONFIG_PATH;
    }
    
    /**
     * Starts the Robot instance in a separate daemon thread.
     * 
     * <p>This allows the Robot to run independently of the main application thread
     * and ensures it doesn't prevent JVM shutdown.</p>
     * 
     * @param context Spring application context
     * @param configPath Configuration file path
     */
    private static void startRobotThread(ConfigurableApplicationContext context, String configPath) {
        Thread robotThread = new Thread(() -> {
            try {
                logger.info("Starting Robot instance in separate thread");
                
                // TODO: Uncomment when Robot class is implemented
                // Robot robot = context.getBean(Robot.class);
                // robot.start(configPath);
                
                logger.info("Robot thread placeholder - Robot class not yet implemented");
                logger.info("Robot will be started here once Robot class is created");
                
            } catch (Exception e) {
                logger.error("Error in Robot thread", e);
            }
        }, "Robot-Main-Thread");
        
        // Set as daemon thread so it doesn't prevent JVM shutdown
        robotThread.setDaemon(true);
        robotThread.start();
        
        logger.info("Robot thread started successfully");
    }
    
    /**
     * Registers a shutdown hook to gracefully shut down the application.
     * 
     * @param context Spring application context
     */
    private static void registerShutdownHook(ConfigurableApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, closing Bailing Voice Assistant...");
            try {
                context.close();
                logger.info("Bailing Voice Assistant shut down successfully");
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }, "Shutdown-Hook-Thread"));
    }
}
