package com.pos.system.service.impl;

import com.pos.system.dto.response.UserResponse;
import com.pos.system.entity.User;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.UserRepository;
import com.pos.system.service.RefreshTokenService;
import com.pos.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName().name())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("phone", user.getPhone());
        data.put("active", user.getActive());
        data.put("termsAccepted", user.getTermsAccepted());
        data.put("termsAcceptedAt", user.getTermsAcceptedAt());
        data.put("createdAt", user.getCreatedAt());
        data.put("lastLogin", user.getLastLogin());
        data.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList());
        return data;
    }

    @Override
    @Transactional
    public void requestAccountDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalStateException("La cuenta ya está eliminada");
        }

        // Baja lógica: preserva integridad fiscal/contable de registros históricos
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revocar sesiones activas: el usuario borrado no puede seguir operando
        refreshTokenService.revokeAllUserTokens(userId);

        log.info("Solicitud de supresión de datos procesada para usuario {} (deletedAt={})",
                userId, user.getDeletedAt());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .active(user.getActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
