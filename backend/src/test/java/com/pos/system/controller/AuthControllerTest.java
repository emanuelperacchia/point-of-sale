package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pos.system.dto.request.LoginRequest;
import com.pos.system.dto.request.RefreshTokenRequest;
import com.pos.system.dto.request.RegisterRequest;
import com.pos.system.dto.response.AuthResponse;
import com.pos.system.dto.response.UserResponse;
import com.pos.system.entity.User;
import com.pos.system.exception.AuthenticationException;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private AuthService authService;

    private AuthController authController;

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_WithValidCredentials_ShouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@pos.com");
        request.setPassword("admin123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiJ9...")
                .refreshToken("refresh-token-123")
                .tokenType("Bearer")
                .userId(1L)
                .email("admin@pos.com")
                .fullName("Admin System")
                .build();

        when(authService.login(any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiJ9..."))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("admin@pos.com"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@pos.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(), any(), any()))
                .thenThrow(new AuthenticationException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithValidData_ShouldReturnTokens() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@pos.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhone("123456789");

        AuthResponse response = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiJ9...")
                .refreshToken("refresh-token-456")
                .tokenType("Bearer")
                .userId(2L)
                .email("newuser@pos.com")
                .fullName("New User")
                .build();

        when(authService.register(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void register_WithExistingEmail_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@pos.com");
        request.setPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");

        when(authService.register(any()))
                .thenThrow(new BadRequestException("El email ya está registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_WithAuthenticatedUser_ShouldReturnUserData() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("admin@pos.com")
                .firstName("Admin")
                .lastName("System")
                .fullName("Admin System")
                .phone("123456789")
                .active(true)
                .roles(List.of("ADMIN"))
                .createdAt(LocalDateTime.now())
                .lastLogin(null)
                .build();

        when(authService.getCurrentUser())
                .thenReturn(response);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("admin@pos.com"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void getCurrentUser_WithoutAuthentication_ShouldReturn401() throws Exception {
        when(authService.getCurrentUser())
                .thenThrow(new AuthenticationException("No hay usuario autenticado"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Set up security context for @AuthenticationPrincipal
        User user = User.builder()
                .id(1L)
                .email("admin@pos.com")
                .password("encoded")
                .active(true)
                .roles(new HashSet<>())
                .build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .email("admin@pos.com")
                .fullName("Admin System")
                .build();

        when(authService.refreshToken(any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_WithValidRefreshToken_ShouldReturn200() throws Exception {
        // Set up security context for @AuthenticationPrincipal
        User user = User.builder()
                .id(1L)
                .email("admin@pos.com")
                .password("encoded")
                .active(true)
                .roles(new HashSet<>())
                .build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        doNothing().when(authService).logout(any(), any(), any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}