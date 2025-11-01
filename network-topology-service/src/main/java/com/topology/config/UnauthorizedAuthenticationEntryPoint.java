package com.topology.config;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class UnauthorizedAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return Mono.defer(() -> Mono.just(exchange.getResponse()))
                .flatMap(response -> {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    String errorMessage = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}";
                    return response.writeWith(Mono.just(bufferFactory.wrap(errorMessage.getBytes(StandardCharsets.UTF_8))));
                });
    }
}
