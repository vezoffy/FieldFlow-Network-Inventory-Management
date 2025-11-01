package com.topology.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .filter(logRequest())
                .filter(reactiveAuthHeaderFilter()); // Use reactive filter
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            logger.info("Outgoing WebClient request: {} {}", request.method(), request.url());
            String authHeader = request.headers().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                logger.info("Authorization Header: {}", authHeader);
            } else {
                logger.info("Authorization Header: (Not Present)");
            }
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction reactiveAuthHeaderFilter() {
        return (clientRequest, next) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getDetails) // Assuming JWT is stored in details
                .cast(String.class) // Cast to String (the JWT)
                .map(jwt -> ClientRequest.from(clientRequest).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).build())
                .defaultIfEmpty(clientRequest) // If no auth, proceed with original request
                .flatMap(next::exchange);
    }
}
