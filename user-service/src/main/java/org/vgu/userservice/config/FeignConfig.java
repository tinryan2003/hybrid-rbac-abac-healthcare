package org.vgu.userservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Feign client configuration
 * Enables inter-service communication
 */
@Configuration
@EnableFeignClients(basePackages = "org.vgu.userservice.client")
public class FeignConfig {
    // Feign clients can be added here for calling other services if needed
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

