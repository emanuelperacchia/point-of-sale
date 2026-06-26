package com.pos.system.service;

import com.pos.system.dto.response.JournalEntryResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.AccountingEntryTemplateRepository;
import com.pos.system.repository.AccountingJournalEntryRepository;
import com.pos.system.repository.AccountingJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingService {

    private final AccountingEntryTemplateRepository templateRepository;
    private final AccountingJournalEntryRepository entryRepository;
    private final AccountingJournalLineRepository lineRepository;
    private final WebhookDispatcherService webhookDispatcherService;

    /**
     * Genera un asiento contable a partir de un template y montos calculados.
     * Es idempotente: si ya existe un asiento para la misma referencia, no lo duplica.
     *
     * @param eventoOrigen   VENTA, COMPRA, GASTO, NOMINA
     * @param referenciaId   ID de la venta/nómina/gasto
     * @param descripcion    Descripción del asiento
     * @param montos         Mapa de valores: TOTAL, IVA, NETO, SUELDOS, CARGAS_SOCIALES
     * @return el asiento generado, o el existente si ya se generó
     */
    @Transactional
    public JournalEntryResponse generateEntry(String eventoOrigen, Long referenciaId,
                                               String descripcion, Map<String, BigDecimal> montos) {

        // Idempotencia: evitar duplicados
        if (entryRepository.existsByReferenciaIdAndReferenciaType(referenciaId, eventoOrigen)) {
            log.info("Asiento ya existe para {} #{}", eventoOrigen, referenciaId);
            return findEntryByReferencia(referenciaId, eventoOrigen);
        }

        AccountingEntryTemplate.EventoOrigen origen;
        try {
            origen = AccountingEntryTemplate.EventoOrigen.valueOf(eventoOrigen);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Evento de asiento inválido: " + eventoOrigen);
        }

        AccountingEntryTemplate template = templateRepository
                .findByEventoOrigenAndActivoTrue(origen)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay template de asiento configurado para " + eventoOrigen));

        // Construir el asiento
        AccountingJournalEntry entry = AccountingJournalEntry.builder()
                .fecha(LocalDate.now())
                .descripcion(descripcion)
                .referenciaId(referenciaId)
                .referenciaType(eventoOrigen)
                .build();
        AccountingJournalEntry finalEntry = entry; // effectively final para lambda

        // Calcular líneas según el template
        List<AccountingJournalLine> lineas = template.getLineas().stream()
                .map(tplLine -> {
                    BigDecimal monto = calcularMonto(tplLine, montos);
                    return AccountingJournalLine.builder()
                            .entry(finalEntry)
                            .cuenta(tplLine.getCuenta())
                            .tipo(AccountingJournalLine.Tipo.valueOf(tplLine.getTipo().name()))
                            .monto(monto)
                            .build();
                })
                .collect(Collectors.toList());

        entry.setLineas(lineas);
        entry = entryRepository.save(entry);

        log.info("Asiento contable generado: {} #{} - {}", eventoOrigen, referenciaId, descripcion);

        // Disparar webhook ACCOUNTING_ENTRY_CREATED
        if (webhookDispatcherService != null) {
            webhookDispatcherService.dispatch("ACCOUNTING_ENTRY_CREATED", Map.of(
                    "entryId", entry.getId(),
                    "eventoOrigen", eventoOrigen,
                    "referenciaId", referenciaId,
                    "descripcion", descripcion,
                    "fecha", entry.getFecha().toString()
            ));
        }

        return mapToResponse(entry);
    }

    /**
     * Calcula el monto de una línea según la fórmula.
     */
    private BigDecimal calcularMonto(AccountingEntryTemplateLine tplLine, Map<String, BigDecimal> montos) {
        return switch (tplLine.getFormula()) {
            case FIJO -> tplLine.getMontoFijo() != null ? tplLine.getMontoFijo() : BigDecimal.ZERO;
            case TOTAL -> montos.getOrDefault("TOTAL", BigDecimal.ZERO);
            case IVA -> montos.getOrDefault("IVA", BigDecimal.ZERO);
            case NETO -> montos.getOrDefault("NETO", BigDecimal.ZERO);
            case SUELDOS -> montos.getOrDefault("SUELDOS", BigDecimal.ZERO);
            case CARGAS_SOCIALES -> montos.getOrDefault("CARGAS_SOCIALES", BigDecimal.ZERO);
        };
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse findEntryByReferencia(Long referenciaId, String referenciaType) {
        AccountingJournalEntry entry = entryRepository
                .findByReferencia(referenciaId, referenciaType)
                .orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado"));
        return mapToResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getJournal(LocalDate desde, LocalDate hasta) {
        List<AccountingJournalEntry> entries = entryRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta);
        return entries.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private JournalEntryResponse mapToResponse(AccountingJournalEntry entry) {
        List<JournalEntryResponse.JournalLineResponse> lineas = entry.getLineas().stream()
                .map(line -> JournalEntryResponse.JournalLineResponse.builder()
                        .id(line.getId())
                        .cuentaId(line.getCuenta().getId())
                        .cuentaCodigo(line.getCuenta().getCodigo())
                        .cuentaNombre(line.getCuenta().getNombre())
                        .tipo(line.getTipo().name())
                        .monto(line.getMonto())
                        .build())
                .collect(Collectors.toList());

        return JournalEntryResponse.builder()
                .id(entry.getId())
                .fecha(entry.getFecha())
                .descripcion(entry.getDescripcion())
                .referenciaId(entry.getReferenciaId())
                .referenciaType(entry.getReferenciaType())
                .estado(entry.getEstado())
                .createdAt(entry.getCreatedAt())
                .lineas(lineas)
                .build();
    }
}
