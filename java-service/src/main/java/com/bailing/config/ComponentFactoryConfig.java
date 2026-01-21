package com.bailing.config;

import com.bailing.utils.ComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Spring Configuration for Component Factory
 * 组件工厂Spring配置
 * 
 * <p>Initializes ComponentFactory with Spring application context
 * to enable dependency injection for dynamically created components.</p>
 * 
 * @author Bailing Team
 * @version 1.0.0
 */
@Configuration
public class ComponentFactoryConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ComponentFactoryConfig.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * Initializes ComponentFactory with Spring context after bean creation.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing ComponentFactory with Spring context");
        ComponentFactory.setSpringContext(applicationContext);
        logger.info("ComponentFactory initialized successfully");
    }
}
