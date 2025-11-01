package com.training.authservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username")
       })
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "password_hash")
    private String password;

    @Enumerated(EnumType.STRING)
    private ERole role;

    private LocalDateTime lastLogin;

    public User(String username, String password, ERole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
