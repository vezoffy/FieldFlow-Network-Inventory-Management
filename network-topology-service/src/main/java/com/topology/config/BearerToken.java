package com.topology.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class BearerToken extends AbstractAuthenticationToken {

    private final String token;

    public BearerToken(String token) {
        super(null);
        this.token = token;
        setAuthenticated(false);
    }

    public BearerToken(String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    public String getToken() {
        return token;
    }
}
