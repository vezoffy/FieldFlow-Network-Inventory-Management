package com.topology.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)
                .cast(BearerToken.class)
                .flatMap(bearerToken -> {
                    try {
                        Claims claims = Jwts.parserBuilder().setSigningKey(key()).build()
                                .parseClaimsJws(bearerToken.getToken())
                                .getBody();

                        String username = claims.getSubject();
                        List<String> roles = claims.get("roles", List.class);

                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        // Store the raw JWT in the details for propagation
                        UsernamePasswordAuthenticationToken authenticatedToken = 
                                new UsernamePasswordAuthenticationToken(username, null, authorities);
                        authenticatedToken.setDetails(bearerToken.getToken());

                        return Mono.just(authenticatedToken);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Invalid or expired JWT", e));
                    }
                });
    }
}
