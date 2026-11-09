package com.pos.system.service;

import com.pos.system.entity.Role;
import com.pos.system.entity.User;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.UserRepository;
import com.pos.system.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, refreshTokenService);
    }

    private User buildActiveUser() {
        return User.builder()
                .id(1L)
                .email("empleado@pos.com")
                .password("encoded")
                .firstName("Juan")
                .lastName("Pérez")
                .phone("1155554444")
                .active(true)
                .termsAccepted(true)
                .termsAcceptedAt(LocalDateTime.now().minusDays(30))
                .createdAt(LocalDateTime.now().minusDays(30))
                .roles(new HashSet<>())
                .build();
    }

    // --- Exportación (Derecho de Acceso / Portabilidad) ---

    @Test
    void exportUserData_ShouldIncludePersonalDataAndConsent() {
        User user = buildActiveUser();
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.RoleName.CAJERO);
        user.getRoles().add(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Map<String, Object> data = userService.exportUserData(1L);

        assertEquals("empleado@pos.com", data.get("email"));
        assertEquals("Juan", data.get("firstName"));
        assertEquals("Pérez", data.get("lastName"));
        assertEquals("1155554444", data.get("phone"));
        assertEquals(true, data.get("termsAccepted"));
        assertNotNull(data.get("termsAcceptedAt"));
        assertNotNull(data.get("createdAt"));
        assertEquals("CAJERO", ((java.util.List<?>) data.get("roles")).get(0));
        // Nunca exporta el hash de la password
        assertFalse(data.containsKey("password"));
    }

    @Test
    void exportUserData_WhenUserNotFound_ShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.exportUserData(99L));
    }

    // --- Supresión (Derecho al Olvido) ---

    @Test
    void requestAccountDeletion_ShouldDeactivateAndRevokeTokens() {
        User user = buildActiveUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.requestAccountDeletion(1L);

        assertFalse(user.getActive());
        assertNotNull(user.getDeletedAt());
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(1L);
    }

    @Test
    void requestAccountDeletion_WhenAlreadyDeleted_ShouldThrow() {
        User user = buildActiveUser();
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> userService.requestAccountDeletion(1L));
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    void requestAccountDeletion_WhenUserNotFound_ShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.requestAccountDeletion(99L));
    }
}