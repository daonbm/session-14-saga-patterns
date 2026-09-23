package com.session03.category_service.configuration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfiguration {

    @Bean
    @LoadBalanced // Tự động tích hợp Eureka + Load Balancer khi gọi service khác bằng tên (http://service-name/...)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
