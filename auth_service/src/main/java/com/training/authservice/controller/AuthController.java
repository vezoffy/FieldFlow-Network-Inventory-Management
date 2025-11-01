package com.training.authservice.controller;

import com.training.authservice.config.JwtUtils;
import com.training.authservice.dto.JwtResponse;
import com.training.authservice.dto.LoginRequest;
import com.training.authservice.dto.MessageResponse;
import com.training.authservice.dto.SignupRequest;
import com.training.authservice.entity.ERole;
import com.training.authservice.entity.User;
import com.training.authservice.exception.RoleNotFoundException;
import com.training.authservice.repository.UserRepository;
import com.training.authservice.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Update last login time
        User user = userRepository.findById(userDetails.getId()).get();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        ERole role;
        try {
            role = ERole.valueOf("ROLE_" + signUpRequest.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RoleNotFoundException("Role not found: " + signUpRequest.getRole());
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                encoder.encode(signUpRequest.getPassword()),
                role);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> testAdminAccess() {
        return ResponseEntity.ok(new MessageResponse("Admin content access successful!"));
    }
}
