package com.pos.system.service;

import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken findByToken(String token);
    boolean isValid(RefreshToken refreshToken);
    void revoke(RefreshToken refreshToken);
    void revokeAllUserTokens(Long userId);
}
