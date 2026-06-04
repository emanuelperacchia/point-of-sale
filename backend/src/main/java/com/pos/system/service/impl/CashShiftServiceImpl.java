package com.pos.system.service.impl;

import com.pos.system.dto.response.ShiftMovementResponse;
import com.pos.system.dto.response.ShiftReportResponse;
import com.pos.system.dto.response.ShiftResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.CashShiftService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashShiftServiceImpl implements CashShiftService {

    private static final Logger log = LoggerFactory.getLogger(CashShiftServiceImpl.class);

    private final CashShiftRepository cashShiftRepository;
    private final ShiftMovementRepository shiftMovementRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShiftResponse openShift(Long cajeroId, Long sucursalId, BigDecimal montoApertura) {
        // Validar que no exista un turno ABIERTO para este cajero
        Optional<CashShift> activeShift = cashShiftRepository
                .findByCajeroIdAndEstado(cajeroId, ShiftStatus.ABIERTO);
        if (activeShift.isPresent()) {
            throw new BadRequestException(
                    "El cajero ya tiene un turno abierto (ID: " + activeShift.get().getId() + ")");
        }

        User cajero = userRepository.findById(cajeroId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + cajeroId));

        CashShift shift = CashShift.builder()
                .cajero(cajero)
                .sucursalId(sucursalId)
                .estado(ShiftStatus.ABIERTO)
                .montoApertura(montoApertura)
                .build();

        shift = cashShiftRepository.save(shift);

        log.info("Turno abierto: ID={}, cajero={}, monto={}",
                shift.getId(), cajeroId, montoApertura);

        return mapToResponse(shift);
    }

    @Override
    @Transactional
    public ShiftResponse closeShift(Long shiftId, BigDecimal montoCierre) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + shiftId));

        if (shift.getEstado() != ShiftStatus.ABIERTO) {
            throw new BadRequestException(
                    "El turno ya está cerrado. Estado actual: " + shift.getEstado());
        }

        // Calcular monto esperado = apertura + ventas_efectivo + ingresos - retiros
        BigDecimal ventasEfectivo = calculateCashSalesTotal(shiftId);
        BigDecimal totalIngresos = calculateMovementsTotal(shiftId, ShiftMovementType.INGRESO);
        BigDecimal totalRetiros = calculateMovementsTotal(shiftId, ShiftMovementType.RETIRO);

        BigDecimal montoEsperado = shift.getMontoApertura()
                .add(ventasEfectivo)
                .add(totalIngresos)
                .subtract(totalRetiros);

        BigDecimal diferencia = montoEsperado.subtract(montoCierre);

        shift.setMontoCierre(montoCierre);
        shift.setDiferencia(diferencia);
        shift.setEstado(ShiftStatus.CERRADO);
        shift.setFechaCierre(LocalDateTime.now());

        shift = cashShiftRepository.save(shift);

        log.info("Turno cerrado: ID={}, esperado={}, declarado={}, diferencia={}",
                shiftId, montoEsperado, montoCierre, diferencia);

        return mapToResponse(shift);
    }

    @Override
    @Transactional
    public ShiftMovementResponse addMovement(Long shiftId, String tipo,
                                              BigDecimal monto, String motivo, Long usuarioId) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + shiftId));

        if (shift.getEstado() != ShiftStatus.ABIERTO) {
            throw new BadRequestException("No se pueden registrar movimientos en un turno cerrado");
        }

        ShiftMovementType movementType;
        try {
            movementType = ShiftMovementType.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de movimiento inválido: " + tipo);
        }

        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        ShiftMovement movement = ShiftMovement.builder()
                .shiftId(shiftId)
                .usuario(usuario)
                .tipo(movementType)
                .monto(monto)
                .motivo(motivo)
                .build();

        movement = shiftMovementRepository.save(movement);

        log.info("Movimiento registrado: turno={}, tipo={}, monto={}, motivo={}",
                shiftId, tipo, monto, motivo);

        return mapMovementToResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftReportResponse getReport(Long shiftId) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + shiftId));

        // Ventas agrupadas por método de pago
        List<Payment> shiftPayments = paymentRepository.findByShiftId(shiftId);
        Map<String, BigDecimal> ventasPorMetodo = new LinkedHashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            ventasPorMetodo.put(method.name(), BigDecimal.ZERO);
        }
        BigDecimal totalVentasEfectivo = BigDecimal.ZERO;
        for (Payment payment : shiftPayments) {
            String method = payment.getPaymentMethod().name();
            ventasPorMetodo.merge(method, payment.getAmount(), BigDecimal::add);
            if (payment.getPaymentMethod() == PaymentMethod.CASH) {
                totalVentasEfectivo = totalVentasEfectivo.add(payment.getAmount());
            }
        }

        // Movimientos manuales
        List<ShiftMovement> movements = shiftMovementRepository
                .findByShiftIdOrderByCreatedAtAsc(shiftId);
        List<ShiftMovementResponse> movementResponses = movements.stream()
                .map(this::mapMovementToResponse)
                .toList();

        BigDecimal totalIngresos = movements.stream()
                .filter(m -> m.getTipo() == ShiftMovementType.INGRESO)
                .map(ShiftMovement::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetiros = movements.stream()
                .filter(m -> m.getTipo() == ShiftMovementType.RETIRO)
                .map(ShiftMovement::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoEsperado = shift.getMontoApertura()
                .add(totalVentasEfectivo)
                .add(totalIngresos)
                .subtract(totalRetiros);

        return ShiftReportResponse.builder()
                .shiftId(shift.getId())
                .estado(shift.getEstado())
                .montoApertura(shift.getMontoApertura())
                .totalVentasEfectivo(totalVentasEfectivo)
                .ventasPorMetodoPago(ventasPorMetodo)
                .totalIngresos(totalIngresos)
                .totalRetiros(totalRetiros)
                .movimientos(movementResponses)
                .montoEsperado(montoEsperado)
                .montoCierreDeclarado(shift.getMontoCierre())
                .diferencia(shift.getDiferencia())
                .fechaApertura(shift.getFechaApertura())
                .fechaCierre(shift.getFechaCierre())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getById(Long shiftId) {
        CashShift shift = cashShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + shiftId));
        return mapToResponse(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> findByFilters(Long cajeroId, ShiftStatus estado) {
        return cashShiftRepository.findByFilters(cajeroId, estado)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private BigDecimal calculateCashSalesTotal(Long shiftId) {
        List<Payment> cashPayments = paymentRepository.findByShiftId(shiftId)
                .stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH)
                .toList();
        return cashPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateMovementsTotal(Long shiftId, ShiftMovementType tipo) {
        return shiftMovementRepository.findByShiftIdOrderByCreatedAtAsc(shiftId)
                .stream()
                .filter(m -> m.getTipo() == tipo)
                .map(ShiftMovement::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShiftResponse mapToResponse(CashShift shift) {
        return ShiftResponse.builder()
                .id(shift.getId())
                .cajeroId(shift.getCajero().getId())
                .cajeroNombre(shift.getCajero().getFullName())
                .sucursalId(shift.getSucursalId())
                .estado(shift.getEstado())
                .montoApertura(shift.getMontoApertura())
                .montoCierre(shift.getMontoCierre())
                .diferencia(shift.getDiferencia())
                .fechaApertura(shift.getFechaApertura())
                .fechaCierre(shift.getFechaCierre())
                .build();
    }

    private ShiftMovementResponse mapMovementToResponse(ShiftMovement movement) {
        return ShiftMovementResponse.builder()
                .id(movement.getId())
                .shiftId(movement.getShiftId())
                .tipo(movement.getTipo())
                .monto(movement.getMonto())
                .motivo(movement.getMotivo())
                .usuarioNombre(movement.getUsuario().getFullName())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
