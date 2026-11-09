package com.pos.system.service;

import com.pos.system.entity.RefreshToken;
import com.pos.system.entity.User;
import com.pos.system.repository.RefreshTokenRepository;
import com.pos.system.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);
    }

    @Test
    void createRefreshToken_ShouldPersistHashNotRawToken() {
        User user = mock(User.class);

        String rawToken = refreshTokenService.createRefreshToken(user);

        assertNotNull(rawToken);
        assertFalse(rawToken.isEmpty());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken persisted = captor.getValue();
        // El token crudo NUNCA se persiste: la DB guarda el hash SHA-256 (64 hex chars)
        assertNotEquals(rawToken, persisted.getToken());
        assertEquals(64, persisted.getToken().length());
        assertTrue(persisted.getToken().matches("[0-9a-f]{64}"));
        assertEquals(user, persisted.getUser());
        assertNotNull(persisted.getExpirationDate());
    }

    @Test
    void findByToken_ShouldLookUpByHash() {
        String rawToken = "abc-123-raw-token";
        RefreshToken stored = RefreshToken.builder().token("some-hash").build();

        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));

        RefreshToken result = refreshTokenService.findByToken(rawToken);

        assertNotNull(result);
        // Se busca por hash (64 hex chars), nunca por el token crudo
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).findByToken(captor.capture());
        assertNotEquals(rawToken, captor.getValue());
        assertEquals(64, captor.getValue().length());
        assertTrue(captor.getValue().matches("[0-9a-f]{64}"));
    }

    @Test
    void findByToken_WhenTokenNotFound_ShouldReturnNull() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        RefreshToken result = refreshTokenService.findByToken("no-existe");

        assertNull(result);
    }

    @Test
    void revoke_ShouldMarkRevoked() {
        RefreshToken refreshToken = RefreshToken.builder().build();

        refreshTokenService.revoke(refreshToken);

        assertNotNull(refreshToken.getRevokeAt());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeAllUserTokens_ShouldDeleteByUserId() {
        refreshTokenService.revokeAllUserTokens(42L);

        verify(refreshTokenRepository).deleteByUserId(42L);
    }
}