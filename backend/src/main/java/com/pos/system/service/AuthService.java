package com.pos.system.service;

import com.pos.system.dto.request.LoginRequest;
import com.pos.system.dto.request.RefreshTokenRequest;
import com.pos.system.dto.request.RegisterRequest;
import com.pos.system.dto.response.AuthResponse;
import com.pos.system.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request, Long userId);
    void logout(RefreshTokenRequest request, Long userId, String ipAddress);
    UserResponse getCurrentUser();
    AuthResponse switchBranch(Long branchId);
}
