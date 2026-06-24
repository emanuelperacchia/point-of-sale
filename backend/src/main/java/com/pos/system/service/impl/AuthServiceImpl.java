package com.pos.system.service.impl;

import com.pos.system.dto.request.LoginRequest;
import com.pos.system.dto.request.RefreshTokenRequest;
import com.pos.system.dto.request.RegisterRequest;
import com.pos.system.dto.response.AuthResponse;
import com.pos.system.dto.response.UserResponse;
import com.pos.system.entity.Branch;
import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.Role;
import com.pos.system.entity.User;
import com.pos.system.entity.UserBranch;
import com.pos.system.exception.AuthenticationException;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.BranchRepository;
import com.pos.system.repository.RoleRepository;
import com.pos.system.repository.UserBranchRepository;
import com.pos.system.repository.UserRepository;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AuditService;
import com.pos.system.service.AuthService;
import com.pos.system.service.JwtService;
import com.pos.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userDetails.getUser();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditService.logLogin(user.getId(), ipAddress, userAgent);

        List<AuthResponse.BranchInfo> branches = getUserBranches(user);
        Long defaultBranchId = resolveDefaultBranch(user, branches);

        String accessToken = jwtService.generateToken(user, defaultBranchId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .branchId(defaultBranchId)
                .branches(branches)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("El email ya está registrado");
        }

        Role defaultRole = roleRepository.findByName(Role.RoleName.CAJERO)
                .orElseThrow(() -> new BadRequestException("Rol por defecto no encontrado"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .active(true)
                .emailVerified(false)
                .build();
        user.getRoles().add(defaultRole);

        user = userRepository.save(user);

        auditService.log(user.getId(), com.pos.system.entity.AuditLog.AuditAction.USER_CREATE, "USER", user.getId(), null, user.getEmail(), null, null);

        // Asignar a sucursal por defecto si existe
        List<Branch> activeBranches = branchRepository.findByActivaTrue();
        if (!activeBranches.isEmpty()) {
            Branch defaultBranch = activeBranches.get(0);
            userBranchRepository.save(UserBranch.builder()
                    .id(new UserBranch.UserBranchId(user.getId(), defaultBranch.getId()))
                    .activo(true)
                    .build());
        }

        List<AuthResponse.BranchInfo> branches = getUserBranches(user);
        Long defaultBranchId = resolveDefaultBranch(user, branches);

        String accessToken = jwtService.generateToken(user, defaultBranchId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .branchId(defaultBranchId)
                .branches(branches)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, Long userId) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());

        if (!refreshTokenService.isValid(refreshToken)) {
            throw new AuthenticationException("Refresh token inválido o expirado");
        }

        User user = refreshToken.getUser();
        refreshTokenService.revoke(refreshToken);

        List<AuthResponse.BranchInfo> branches = getUserBranches(user);
        Long defaultBranchId = resolveDefaultBranch(user, branches);

        String accessToken = jwtService.generateToken(user, defaultBranchId);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .branchId(defaultBranchId)
                .branches(branches)
                .build();
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request, Long userId, String ipAddress) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        if (userId != null) {
            auditService.logLogout(userId, ipAddress);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("No hay usuario autenticado");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

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

    @Override
    @Transactional
    public AuthResponse switchBranch(Long branchId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("No hay usuario autenticado");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ADMIN);

        if (!isAdmin) {
            boolean hasAccess = userBranchRepository.existsByIdUserIdAndIdBranchId(user.getId(), branchId);
            if (!hasAccess) {
                throw new BadRequestException("No tiene acceso a la sucursal especificada");
            }
        }

        if (!isAdmin && branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new BadRequestException("Sucursal no encontrada"));
            if (!branch.getActiva()) {
                throw new BadRequestException("La sucursal no está activa");
            }
        }

        List<AuthResponse.BranchInfo> branches = getUserBranches(user);
        String accessToken = jwtService.generateToken(user, branchId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .branchId(branchId)
                .branches(branches)
                .build();
    }

    private List<AuthResponse.BranchInfo> getUserBranches(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ADMIN);

        List<Branch> branches;
        if (isAdmin) {
            branches = branchRepository.findAll();
        } else {
            List<Long> branchIds = userBranchRepository.findActiveBranchIdsByUserId(user.getId());
            branches = branchRepository.findAllById(branchIds);
        }

        return branches.stream()
                .map(b -> AuthResponse.BranchInfo.builder()
                        .id(b.getId())
                        .nombre(b.getNombre())
                        .direccion(b.getDireccion())
                        .build())
                .toList();
    }

    private Long resolveDefaultBranch(User user, List<AuthResponse.BranchInfo> branches) {
        if (branches.isEmpty()) {
            return null;
        }

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ADMIN);

        // ADMIN no tiene branchId por defecto (ve todas)
        if (isAdmin) {
            return null;
        }

        // Si solo tiene una sucursal, esa es la default
        if (branches.size() == 1) {
            return branches.get(0).getId();
        }

        // Si tiene varias, la primera (podría mejorarse con preferencia guardada)
        return branches.get(0).getId();
    }
}
