package com.pos.system.service;

import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.PointsTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private PointsTransactionRepository pointsTransactionRepository;

    @Captor private ArgumentCaptor<Client> clientCaptor;
    @Captor private ArgumentCaptor<PointsTransaction> txCaptor;

    private LoyaltyService loyaltyService;
    private Client client;

    @BeforeEach
    void setUp() {
        loyaltyService = new LoyaltyService(clientRepository, pointsTransactionRepository);

        client = Client.builder()
                .id(1L).name("Juan Pérez")
                .puntosAcumulados(500L)
                .tier(ClientTier.BRONCE)
                .build();
    }

    @Test
    void acumularPuntos_ShouldAddPoints() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        loyaltyService.acumularPuntos(1L, 100L, BigDecimal.valueOf(10000));

        verify(clientRepository).save(clientCaptor.capture());
        Client saved = clientCaptor.getValue();

        // 10000 * 1 punto por $1 = 10000 puntos
        // BRONCE so no multiplier
        assertTrue(saved.getPuntosAcumulados() > 500);
        assertEquals(saved.getPuntosAcumulados(), 500L + 10000L);

        verify(pointsTransactionRepository).save(txCaptor.capture());
        PointsTransaction tx = txCaptor.getValue();
        assertEquals(PointTransactionType.ACUMULACION, tx.getTipo());
        assertEquals(10000L, tx.getPuntos());
        assertEquals(500L, tx.getSaldoPrevio());
        assertEquals(500L + 10000L, tx.getSaldoPosterior());
    }

    @Test
    void acumularPuntos_WithOroTier_ShouldApplyBonus() {
        client.setTier(ClientTier.ORO);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        loyaltyService.acumularPuntos(1L, 101L, BigDecimal.valueOf(1000));

        verify(clientRepository).save(clientCaptor.capture());
        Client saved = clientCaptor.getValue();

        // ORO: 50% bonus → 1000 * 1.5 = 1500
        assertEquals(500L + 1500L, saved.getPuntosAcumulados());
    }

    @Test
    void acumularPuntos_ShouldRecalculateTier() {
        client.setPuntosAcumulados(0L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // Gain 6000 points → goes from BRONCE to ORO (5000+)
        loyaltyService.acumularPuntos(1L, 102L, BigDecimal.valueOf(6000));

        verify(clientRepository, atLeastOnce()).save(clientCaptor.capture());
        Client saved = clientCaptor.getAllValues().get(clientCaptor.getAllValues().size() - 1);
        assertEquals(ClientTier.ORO, saved.getTier());
    }

    @Test
    void acumularPuntos_WithNullClientId_ShouldDoNothing() {
        loyaltyService.acumularPuntos(null, 1L, BigDecimal.valueOf(1000));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void canjearPuntos_ShouldReduceBalance() {
        client.setPuntosAcumulados(2000L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        BigDecimal descuento = loyaltyService.canjearPuntos(1L, 1000L, 200L);

        verify(clientRepository).save(clientCaptor.capture());
        Client saved = clientCaptor.getValue();

        assertEquals(1000L, saved.getPuntosAcumulados()); // 2000 - 1000
        assertEquals(BigDecimal.valueOf(1000), descuento); // 1000 * $1

        verify(pointsTransactionRepository).save(txCaptor.capture());
        PointsTransaction tx = txCaptor.getValue();
        assertEquals(PointTransactionType.CANJE, tx.getTipo());
        assertEquals(1000L, tx.getPuntos());
        assertEquals(2000L, tx.getSaldoPrevio());
        assertEquals(1000L, tx.getSaldoPosterior());
    }

    @Test
    void canjearPuntos_WithInsufficientBalance_ShouldThrow() {
        client.setPuntosAcumulados(100L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThrows(BadRequestException.class,
                () -> loyaltyService.canjearPuntos(1L, 500L, 300L));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void recalcularTier_ShouldPromoteToPlata() {
        client.setPuntosAcumulados(1500L);
        loyaltyService.recalcularTier(client);
        assertEquals(ClientTier.PLATA, client.getTier());
    }

    @Test
    void recalcularTier_ShouldPromoteToOro() {
        client.setPuntosAcumulados(5000L);
        loyaltyService.recalcularTier(client);
        assertEquals(ClientTier.ORO, client.getTier());
    }

    @Test
    void recalcularTier_ShouldStayBronce() {
        client.setPuntosAcumulados(500L);
        loyaltyService.recalcularTier(client);
        assertEquals(ClientTier.BRONCE, client.getTier());
    }

    @Test
    void getClientPointsInfo_ShouldReturnClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        Client result = loyaltyService.getClientPointsInfo(1L);
        assertEquals(1L, result.getId());
        assertEquals(500L, result.getPuntosAcumulados());
    }

    @Test
    void getClientPointsInfo_WhenNotFound_ShouldThrow() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> loyaltyService.getClientPointsInfo(99L));
    }
}
