package com.pos.system.service;

import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.PointsTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Programa de fidelización: acumulación, canje y vencimiento de puntos,
 * segmentación por tiers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final ClientRepository clientRepository;
    private final PointsTransactionRepository pointsTransactionRepository;

    // Configuración hardcodeada (idealmente desde SystemConfiguration / US-049)
    private static final long PUNTOS_POR_MONTO = 1;        // 1 punto por cada $1
    private static final long VALOR_PUNTO = 1;              // 1 punto = $1
    private static final long PLAZO_VENCIMIENTO_MESES = 12; // puntos vencen a los 12 meses
    private static final long TIER_BRONCE_MAX = 999;
    private static final long TIER_PLATA_MAX = 4999;
    // BRONCE = 0-999, PLATA = 1000-4999, ORO = 5000+
    private static final double BONUS_ORO = 1.5;            // ORO acumula 50% más

    /**
     * Acumula puntos por una venta completada.
     * Llamado desde SaleService después de confirmar pago.
     */
    @Transactional
    public void acumularPuntos(Long clientId, Long saleId, BigDecimal totalPagado) {
        if (clientId == null || totalPagado == null || totalPagado.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clientId));

        // Calcular puntos: base × bonus según tier
        long puntosBase = totalPagado.longValue() * PUNTOS_POR_MONTO;
        double multiplicador = client.getTier() == ClientTier.ORO ? BONUS_ORO : 1.0;
        long puntosGanados = BigDecimal.valueOf(puntosBase * multiplicador)
                .setScale(0, RoundingMode.HALF_UP).longValue();

        if (puntosGanados <= 0) return;

        Long saldoPrevio = client.getPuntosAcumulados();
        client.setPuntosAcumulados(saldoPrevio + puntosGanados);
        client.setFechaUltimaTransaccion(LocalDate.now());
        clientRepository.save(client);

        // Registrar transacción
        PointsTransaction tx = PointsTransaction.builder()
                .clientId(clientId)
                .saleId(saleId)
                .tipo(PointTransactionType.ACUMULACION)
                .puntos(puntosGanados)
                .saldoPrevio(saldoPrevio)
                .saldoPosterior(client.getPuntosAcumulados())
                .descripcion("Acumulación por venta #" + saleId + " (tier " + client.getTier() + ")")
                .build();
        pointsTransactionRepository.save(tx);

        // Recalcular tier
        recalcularTier(client);

        log.info("Puntos acumulados: cliente={}, venta={}, puntos={}, saldo={}",
                clientId, saleId, puntosGanados, client.getPuntosAcumulados());
    }

    /**
     * Canjea puntos como descuento.
     */
    @Transactional
    public BigDecimal canjearPuntos(Long clientId, Long puntos, Long saleId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clientId));

        if (client.getPuntosAcumulados() < puntos) {
            throw new BadRequestException(
                    "Puntos insuficientes. Saldo: " + client.getPuntosAcumulados() +
                    ", solicitados: " + puntos);
        }

        BigDecimal descuento = BigDecimal.valueOf(puntos * VALOR_PUNTO);
        Long saldoPrevio = client.getPuntosAcumulados();
        client.setPuntosAcumulados(saldoPrevio - puntos);
        client.setFechaUltimaTransaccion(LocalDate.now());
        clientRepository.save(client);

        // Registrar transacción
        PointsTransaction tx = PointsTransaction.builder()
                .clientId(clientId)
                .saleId(saleId)
                .tipo(PointTransactionType.CANJE)
                .puntos(puntos)
                .saldoPrevio(saldoPrevio)
                .saldoPosterior(client.getPuntosAcumulados())
                .descripcion("Canje por venta #" + saleId + " — $" + descuento)
                .build();
        pointsTransactionRepository.save(tx);

        recalcularTier(client);

        log.info("Puntos canjeados: cliente={}, puntos={}, descuento=${}, saldo={}",
                clientId, puntos, descuento, client.getPuntosAcumulados());

        return descuento;
    }

    /**
     * Recalcula el tier del cliente según su acumulado total histórico.
     */
    @Transactional
    public void recalcularTier(Client client) {
        long total = client.getPuntosAcumulados();
        ClientTier nuevoTier;
        if (total > TIER_PLATA_MAX) {
            nuevoTier = ClientTier.ORO;
        } else if (total > TIER_BRONCE_MAX) {
            nuevoTier = ClientTier.PLATA;
        } else {
            nuevoTier = ClientTier.BRONCE;
        }
        client.setTier(nuevoTier);
    }

    /**
     * Tarea programada diaria: vence puntos expirados.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM todos los días
    public void vencerPuntosExpirados() {
        LocalDate corte = LocalDate.now().minusMonths(PLAZO_VENCIMIENTO_MESES);
        List<Client> clients = clientRepository.findAll();

        for (Client client : clients) {
            if (client.getPuntosAcumulados() <= 0) continue;
            if (client.getFechaUltimaTransaccion() == null) continue;
            if (client.getFechaUltimaTransaccion().isAfter(corte)) continue;

            Long puntosVencidos = client.getPuntosAcumulados();
            Long saldoPrevio = puntosVencidos;

            client.setPuntosAcumulados(0L);
            clientRepository.save(client);

            PointsTransaction tx = PointsTransaction.builder()
                    .clientId(client.getId())
                    .tipo(PointTransactionType.VENCIMIENTO)
                    .puntos(puntosVencidos)
                    .saldoPrevio(saldoPrevio)
                    .saldoPosterior(0L)
                    .descripcion("Vencimiento por inactividad > " + PLAZO_VENCIMIENTO_MESES + " meses")
                    .build();
            pointsTransactionRepository.save(tx);

            recalcularTier(client);

            log.info("Puntos vencidos: cliente={}, puntos={}", client.getId(), puntosVencidos);
        }
    }

    /**
     * Obtiene el saldo y tier actual de un cliente.
     */
    @Transactional(readOnly = true)
    public Client getClientPointsInfo(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clientId));
    }
}
