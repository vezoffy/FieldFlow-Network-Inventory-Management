package com.topology.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable; // <-- Import this
import java.util.Collection;

public class BearerToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final String token;

    // --- SONAR FIX ---
    // Change type from Object to Serializable to enforce the contract
    private final Serializable principal;

    /**
     * Constructor for an UNAUTHENTICATED token.
     */
    public BearerToken(String token) {
        super(null);
        this.token = token;
        this.principal = null; // null is fine
        setAuthenticated(false);
    }

    /**
     * Constructor for an AUTHENTICATED token.
     */
    // --- SONAR FIX ---
    // Update constructor parameter to match the field
    public BearerToken(Serializable principal, String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.principal = principal; // This is now guaranteed to be serializable
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return getToken();
    }

    @Override
    public Object getPrincipal() {
        // This is still correct, as Serializable IS-A Object
        return principal;
    }

    public String getToken() {
        return token;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}