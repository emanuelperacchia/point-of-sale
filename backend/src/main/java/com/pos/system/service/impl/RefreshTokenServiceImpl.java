package com.pos.system.service.impl;

import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.User;
import com.pos.system.repository.RefreshTokenRepository;
import com.pos.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Gestión de refresh tokens.
 * <p>
 * En base de datos solo se persiste el hash SHA-256 del token: si el store se
 * compromete, los tokens almacenados no son utilizables. El token crudo se
 * retorna al cliente únicamente en el momento de creación.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hash(rawToken))
                .expirationDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(hash(token)).orElse(null);
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

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}