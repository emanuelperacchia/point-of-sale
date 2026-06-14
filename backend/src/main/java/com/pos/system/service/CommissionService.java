package com.pos.system.service;

import com.pos.system.dto.request.CommissionSchemeRequest;
import com.pos.system.dto.response.CommissionResultResponse;
import com.pos.system.dto.response.CommissionSchemeResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionSchemeRepository schemeRepository;
    private final CommissionTierRepository tierRepository;
    private final EmployeeCommissionAssignmentRepository assignmentRepository;
    private final SalesTargetRepository targetRepository;
    private final CommissionResultRepository resultRepository;
    private final SaleRepository saleRepository;
    private final EmployeeRepository employeeRepository;

    // ── Scheme CRUD ─────────────────────────────────────────────────

    @Transactional
    public CommissionSchemeResponse crearEsquema(CommissionSchemeRequest request) {
        CommissionScheme scheme = CommissionScheme.builder()
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .activo(true)
                .vigenciaDesde(request.getVigenciaDesde())
                .vigenciaHasta(request.getVigenciaHasta())
                .valor(request.getValor())
                .build();
        CommissionScheme saved = schemeRepository.save(scheme);

        if (request.getTipo() == CommissionScheme.Tipo.ESCALONADO && request.getTiers() != null) {
            List<CommissionTier> tiers = request.getTiers().stream()
                    .map(t -> CommissionTier.builder()
                            .schemeId(saved.getId())
                            .montoDesde(t.getMontoDesde())
                            .montoHasta(t.getMontoHasta())
                            .porcentaje(t.getPorcentaje())
                            .build())
                    .toList();
            tierRepository.saveAll(tiers);
        }

        return mapToSchemeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommissionSchemeResponse> listarEsquemas() {
        return schemeRepository.findByActivoTrue().stream()
                .map(this::mapToSchemeResponse)
                .toList();
    }

    // ── Calculation ─────────────────────────────────────────────────

    @Transactional
    public CommissionResultResponse calculate(Long employeeId, int mes, int anio) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + employeeId));

        if (employee.getUserId() == null) {
            throw new BadRequestException("El empleado no está vinculado a un usuario para calcular comisiones");
        }

        // Find active scheme assignment for this period
        LocalDate periodStart = LocalDate.of(anio, mes, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        EmployeeCommissionAssignment assignment = assignmentRepository
                .findVigente(employeeId, periodStart)
                .orElseThrow(() -> new BadRequestException("El empleado no tiene un esquema de comisión vigente"));

        CommissionScheme scheme = schemeRepository.findById(assignment.getSchemeId())
                .orElseThrow(() -> new ResourceNotFoundException("Esquema no encontrado"));

        // Get sales total for the period
        LocalDateTime desde = periodStart.atStartOfDay();
        LocalDateTime hasta = periodEnd.atTime(LocalTime.MAX);
        BigDecimal totalVentas = saleRepository.sumTotalByUserAndDateRange(employee.getUserId(), desde, hasta);

        // Calculate commission
        BigDecimal comision = calcularComision(totalVentas, scheme);

        // Check sales target / bonus
        Optional<SalesTarget> targetOpt = targetRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio);
        boolean metaAlcanzada = false;
        BigDecimal bonoAplicado = BigDecimal.ZERO;

        if (targetOpt.isPresent()) {
            SalesTarget target = targetOpt.get();
            if (totalVentas.compareTo(target.getMetaMonto()) >= 0) {
                metaAlcanzada = true;
                if (target.getBonoPorSuperacion() == SalesTarget.TipoBono.FIJO) {
                    bonoAplicado = target.getValorBono();
                } else if (target.getBonoPorSuperacion() == SalesTarget.TipoBono.PORCENTAJE) {
                    BigDecimal excedente = totalVentas.subtract(target.getMetaMonto());
                    bonoAplicado = excedente.multiply(target.getValorBono())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }

        // Save result
        CommissionResult result = CommissionResult.builder()
                .employeeId(employeeId)
                .mes(mes)
                .anio(anio)
                .totalVentas(totalVentas)
                .comisionCalculada(comision.add(bonoAplicado))
                .metaAlcanzada(metaAlcanzada)
                .bonoAplicado(bonoAplicado)
                .esquemaUsado(scheme.getNombre())
                .build();
        result = resultRepository.save(result);

        return mapToResultResponse(result);
    }

    @Transactional(readOnly = true)
    public CommissionResultResponse getSummary(Long employeeId, int mes, int anio) {
        CommissionResult result = resultRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay comisiones calculadas para el empleado " + employeeId + " en " + mes + "/" + anio));
        return mapToResultResponse(result);
    }

    @Transactional(readOnly = true)
    public List<CommissionResultResponse> getRanking(int mes, int anio) {
        return resultRepository.findByMesAndAnioOrderByTotalVentasDesc(mes, anio)
                .stream().map(this::mapToResultResponse).toList();
    }

    // ── Core calculation logic ──────────────────────────────────────

    private BigDecimal calcularComision(BigDecimal totalVentas, CommissionScheme scheme) {
        return switch (scheme.getTipo()) {
            case PORCENTAJE_VENTA -> {
                if (scheme.getValor() == null) yield BigDecimal.ZERO;
                yield totalVentas.multiply(scheme.getValor())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case MONTO_FIJO_POR_VENTA -> {
                yield scheme.getValor() != null ? scheme.getValor() : BigDecimal.ZERO;
            }
            case ESCALONADO -> {
                List<CommissionTier> tiers = tierRepository.findBySchemeIdOrderByMontoDesdeAsc(scheme.getId());
                yield calcularEscalonado(totalVentas, tiers);
            }
        };
    }

    private BigDecimal calcularEscalonado(BigDecimal totalVentas, List<CommissionTier> tiers) {
        BigDecimal comision = BigDecimal.ZERO;
        for (CommissionTier tier : tiers) {
            if (totalVentas.compareTo(tier.getMontoDesde()) <= 0) {
                break; // below this tier
            }
            BigDecimal tierBase;
            if (tier.getMontoHasta() == null || totalVentas.compareTo(tier.getMontoHasta()) <= 0) {
                tierBase = totalVentas.subtract(tier.getMontoDesde());
            } else {
                tierBase = tier.getMontoHasta().subtract(tier.getMontoDesde());
            }
            if (tierBase.compareTo(BigDecimal.ZERO) > 0) {
                comision = comision.add(tierBase.multiply(tier.getPorcentaje())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            if (tier.getMontoHasta() != null && totalVentas.compareTo(tier.getMontoHasta()) <= 0) {
                break; // within this tier range
            }
            if (tier.getMontoHasta() == null) {
                break; // last open-ended tier
            }
        }
        return comision;
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private CommissionSchemeResponse mapToSchemeResponse(CommissionScheme s) {
        List<CommissionTier> tiers = tierRepository.findBySchemeIdOrderByMontoDesdeAsc(s.getId());
        return CommissionSchemeResponse.builder()
                .id(s.getId()).nombre(s.getNombre())
                .tipo(s.getTipo()).activo(s.getActivo())
                .vigenciaDesde(s.getVigenciaDesde()).vigenciaHasta(s.getVigenciaHasta())
                .valor(s.getValor())
                .tiers(tiers.stream()
                        .map(t -> CommissionSchemeResponse.TierResponse.builder()
                                .id(t.getId()).montoDesde(t.getMontoDesde())
                                .montoHasta(t.getMontoHasta()).porcentaje(t.getPorcentaje())
                                .build())
                        .toList())
                .build();
    }

    private CommissionResultResponse mapToResultResponse(CommissionResult r) {
        return CommissionResultResponse.builder()
                .id(r.getId()).employeeId(r.getEmployeeId())
                .mes(r.getMes()).anio(r.getAnio())
                .totalVentas(r.getTotalVentas())
                .comisionCalculada(r.getComisionCalculada())
                .metaAlcanzada(r.getMetaAlcanzada())
                .bonoAplicado(r.getBonoAplicado())
                .esquemaUsado(r.getEsquemaUsado())
                .build();
    }
}
