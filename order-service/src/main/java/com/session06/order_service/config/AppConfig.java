package com.session06.order_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    @LoadBalanced // Enables service discovery resolution (e.g. http://product-service/...) and client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public org.springframework.boot.ApplicationRunner initCircuitBreaker(io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry registry) {
        return args -> registry.circuitBreaker("productService");
    }
}
