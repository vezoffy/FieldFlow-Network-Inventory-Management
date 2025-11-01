package com.training.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange);
            if (token == null) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception e) {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private String extractToken(ServerWebExchange exchange) {
        List<String> headers = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String header = headers.get(0);
        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties for the filter can be added here
    }
}
