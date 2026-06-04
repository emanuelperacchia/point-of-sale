package com.pos.system.service;

import com.pos.system.entity.Role;
import com.pos.system.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.ADMIN);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRoles()).thenReturn(roles);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ShouldReturnCorrectEmail() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.ADMIN);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRoles()).thenReturn(roles);

        String token = jwtService.generateToken(user);
        String username = jwtService.extractUsername(token);

        assertEquals("test@example.com", username);
    }

    @Test
    void isTokenValid_WhenTokenBelongsToUser_ShouldReturnTrue() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.ADMIN);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRoles()).thenReturn(roles);

        String token = jwtService.generateToken(user);
        boolean isValid = jwtService.isTokenValid(token, user);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WhenTokenDoesNotBelongToUser_ShouldReturnFalse() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.ADMIN);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRoles()).thenReturn(roles);

        String token = jwtService.generateToken(user);

        User otherUser = mock(User.class);
        when(otherUser.getEmail()).thenReturn("other@example.com");

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertFalse(isValid);
    }

    @Test
    void extractClaim_ShouldReturnCorrectClaim() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.ADMIN);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRoles()).thenReturn(roles);

        String token = jwtService.generateToken(user);
        
        Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));

        assertEquals(1L, userId);
    }
}