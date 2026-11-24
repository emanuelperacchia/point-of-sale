package com.pos.system.controller.auth;

import com.pos.system.dto.auth.LoginRequest;
import com.pos.system.dto.auth.AuthResponse;
import com.pos.system.entity.system.User;
import com.pos.system.repository.system.UserRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        AuthResponse response = new AuthResponse();
        response.setAccessToken("fake-jwt-token-" + user.getId());
        response.setRefreshToken("refresh-token-" + user.getId());
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setUserId(user.getId());
        response.setBranches(java.util.Collections.emptyList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("El email ya está registrado");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRoles(request.getRoles());
        user.setActive(true);
        user.setEmailVerified(true);
        user.setTermsAccepted(true);

        User saved = userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken("fake-jwt-token-" + saved.getId());
        response.setRefreshToken("refresh-token-" + saved.getId());
        response.setTokenType("Bearer");
        response.setUserId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setFullName(saved.getFullName());
        response.setUserId(saved.getId());
        response.setBranches(java.util.Collections.emptyList());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken("new-jwt-token");
        response.setRefreshToken("new-refresh-token");
        response.setTokenType("Bearer");
        response.setUserId(1L);
        response.setEmail("admin@pos.com");
        response.setFullName("Admin System");
        response.setUserId(1L);
        response.setBranches(java.util.Collections.emptyList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<com.pos.system.dto.response.UserResponse> getCurrentUser() {
        com.pos.system.dto.response.UserResponse response = new com.pos.system.dto.response.UserResponse();
        response.setId(1L);
        response.setEmail("admin@pos.com");
        response.setFullName("Admin System");
        response.setRoles(java.util.Collections.singletonList("ADMIN"));
        response.setActive(true);
        response.setCreatedAt(java.time.LocalDateTime.now());
        response.setUpdatedAt(java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-branch")
    public ResponseEntity<AuthResponse> switchBranch(@RequestBody SwitchBranchRequest request) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken("new-jwt-token");
        response.setRefreshToken("new-refresh-token");
        response.setTokenType("Bearer");
        response.setUserId(1L);
        response.setEmail("admin@pos.com");
        response.setFullName("Admin System");
        response.setUserId(1L);
        response.setBranches(java.util.Collections.emptyList());

        return ResponseEntity.ok(response);
    }
}
