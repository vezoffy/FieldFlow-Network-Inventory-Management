package com.training.authservice.service;

import com.training.authservice.entity.ERole;
import com.training.authservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {

    @Test
    void build_ShouldCorrectlyMapUserToUserDetails() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("testpassword");
        user.setRole(ERole.ROLE_ADMIN); // Corrected to use an existing role

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertEquals(user.getId(), userDetails.getId());
        assertEquals(user.getUsername(), userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());

        // Assert default UserDetails properties
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void equals_ShouldReturnTrueForSameId() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setRole(ERole.ROLE_ADMIN); // Corrected to use an existing role

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("user2"); // Different username
        user2.setRole(ERole.ROLE_ADMIN); // Corrected to use an existing role

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(user1);
        UserDetailsImpl userDetails2 = UserDetailsImpl.build(user2);

        // Act & Assert
        assertEquals(userDetails1, userDetails2);
    }

    @Test
    void equals_ShouldReturnFalseForDifferentId() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user");
        user1.setRole(ERole.ROLE_ADMIN); // Corrected to use an existing role

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user");
        user2.setRole(ERole.ROLE_ADMIN); // Corrected to use an existing role

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(user1);
        UserDetailsImpl userDetails2 = UserDetailsImpl.build(user2);

        // Act & Assert
        assertNotEquals(userDetails1, userDetails2);
    }
}
