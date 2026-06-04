package com.pos.system.service.impl;

import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.User;
import com.pos.system.repository.RefreshTokenRepository;
import com.pos.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expirationDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    @Override
    public boolean isValid(RefreshToken refreshToken) {
        return refreshToken != null && refreshToken.isValid();
    }

    @Override
    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevokeAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
