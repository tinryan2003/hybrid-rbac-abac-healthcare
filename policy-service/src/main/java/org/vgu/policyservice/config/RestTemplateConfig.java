package org.vgu.policyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * REST Template Configuration for OPA communication.
 * Uses SimpleClientHttpRequestFactory to avoid dependency on RestTemplateBuilder (Spring Boot 4 moved it to optional module).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5 seconds
        factory.setReadTimeout(10000);     // 10 seconds
        return new RestTemplate(factory);
    }
}
