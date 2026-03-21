package com.example.inventory_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class MappingLogger {
    private static final Logger log = LoggerFactory.getLogger(MappingLogger.class);

    @Bean
    public CommandLineRunner logMappings(RequestMappingHandlerMapping handlerMapping) {
        return args -> handlerMapping.getHandlerMethods().forEach((info, method) -> {
            log.info("Mapped: {} -> {}", info.getPatternsCondition(), method.toString());
            System.out.println("Mapped: " + info.getPatternsCondition() + " -> " + method.toString());
        });
    }
}

