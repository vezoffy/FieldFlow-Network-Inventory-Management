package com.training.inventory_service.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Imports for Logger and LoggerFactory are removed
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    // --- FIX 1: This line has been DELETED ---
    // The "logger" field from the parent class (OncePerRequestFilter) will be used instead.


    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && validateJwtToken(jwt)) {
                String username = getUserNameFromJwtToken(jwt);
                List<String> roles = getRolesFromJwtToken(jwt);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, roles.stream().map(SimpleGrantedAuthority::new).toList());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // This "logger" call now uses the inherited logger from OncePerRequestFilter
            logger.error("Cannot set user authentication: {}");
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public List<String> getRolesFromJwtToken(String token) {
        List<?> rolesObject = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody().get("roles", List.class);
        if (rolesObject != null) {
            return rolesObject.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of(); // Return empty list if roles are null
    }

    public boolean validateJwtToken(String authToken) {
        try {
            // --- FIX 2: Changed .parse() to .parseClaimsJws() ---
            // This forces the parser to validate the signature (JWS)
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            // Pass 'e' as the last argument
            logger.error("Invalid JWT token: {}");
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}");
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}");
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}");
        }
        return false;
    }
}