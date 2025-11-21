package com.example.inventory_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced // Cho phép RestTemplate resolve service name qua Eureka
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
