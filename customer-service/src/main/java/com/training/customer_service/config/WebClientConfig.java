package com.training.customer_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .filter(forwardAuthHeader());
    }

    private ExchangeFilterFunction forwardAuthHeader() {
        return (clientRequest, next) -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authorizationHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                            .build();
                    return next.exchange(authorizedRequest);
                }
            }
            return next.exchange(clientRequest);
        };
    }
}
