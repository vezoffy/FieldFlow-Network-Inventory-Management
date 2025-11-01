package com.training.customer_service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private final JwtForwardingInterceptor jwtForwardingInterceptor;

    public RestTemplateConfig(JwtForwardingInterceptor jwtForwardingInterceptor) {
        this.jwtForwardingInterceptor = jwtForwardingInterceptor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors(jwtForwardingInterceptor).build();
    }
}