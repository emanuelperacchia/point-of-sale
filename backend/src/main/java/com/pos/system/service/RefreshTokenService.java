package com.pos.system.service;

import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.User;

public interface RefreshTokenService {
    /**
     * Crea y persiste un refresh token (la base guarda SOLO el hash SHA-256).
     *
     * @return el token crudo en claro — se entrega al cliente una única vez
     */
    String createRefreshToken(User user);
    RefreshToken findByToken(String token);
    boolean isValid(RefreshToken refreshToken);
    void revoke(RefreshToken refreshToken);
    void revokeAllUserTokens(Long userId);
}