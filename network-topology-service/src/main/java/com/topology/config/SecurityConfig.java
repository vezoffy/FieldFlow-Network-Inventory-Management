package com.topology.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtReactiveAuthenticationManager authenticationManager;

    @Autowired
    private ServerHttpBearerAuthenticationConverter converter;

    @Autowired
    private UnauthorizedAuthenticationEntryPoint entryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) { // Correct method signature
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(converter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec.authenticationEntryPoint(entryPoint))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
