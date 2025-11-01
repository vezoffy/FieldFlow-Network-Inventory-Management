package com.deploymentservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .filter(reactiveAuthHeaderFilter());
    }

    @Bean
    public ExchangeFilterFunction reactiveAuthHeaderFilter() {
        return (clientRequest, next) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getDetails)
                .cast(String.class)
                .map(jwt -> ClientRequest.from(clientRequest).header("Authorization", "Bearer " + jwt).build())
                .defaultIfEmpty(clientRequest)
                .flatMap(next::exchange);
    }
}
