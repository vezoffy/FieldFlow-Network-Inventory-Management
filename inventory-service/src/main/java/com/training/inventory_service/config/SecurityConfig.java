package com.training.inventory_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        // This bean definition is correct
        return new AuthTokenFilter();
    }

    @Bean
    // --- SONAR FIX: Dependencies are injected as method parameters ---
    public SecurityFilterChain filterChain(HttpSecurity http, AuthEntryPointJwt unauthorizedHandler, AuthTokenFilter authTokenFilter) throws Exception {

        http.csrf(csrf -> csrf.disable())
                // We now use the 'unauthorizedHandler' parameter
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers("/api/inventory/**").permitAll() // Permit Swagger UI
                                .anyRequest().authenticated()
                );

        // We now use the 'authTokenFilter' parameter instead of calling the method
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}