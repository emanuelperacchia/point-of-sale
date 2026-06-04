package com.pos.system.service;

import com.pos.system.dto.response.ShiftMovementResponse;
import com.pos.system.dto.response.ShiftReportResponse;
import com.pos.system.dto.response.ShiftResponse;
import com.pos.system.entity.ShiftStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de gestión de turnos de caja.
 */
public interface CashShiftService {

    /**
     * Abre un turno de caja para un cajero.
     *
     * @param cajeroId     ID del usuario que abre el turno
     * @param sucursalId   ID de la sucursal
     * @param montoApertura monto inicial declarado en caja
     * @return turno creado
     * @throws IllegalStateException si ya hay un turno ABIERTO para este cajero
     */
    ShiftResponse openShift(Long cajeroId, Long sucursalId, BigDecimal montoApertura);

    /**
     * Cierra el turno de caja activo del cajero.
     *
     * @param shiftId     ID del turno a cerrar
     * @param montoCierre monto final declarado en caja
     * @return turno cerrado con diferencia calculada
     * @throws IllegalStateException si el turno no está ABIERTO
     */
    ShiftResponse closeShift(Long shiftId, BigDecimal montoCierre);

    /**
     * Registra un movimiento manual de caja (retiro o ingreso).
     *
     * @param shiftId ID del turno
     * @param tipo    RETIRO o INGRESO
     * @param monto   monto del movimiento
     * @param motivo  motivo del movimiento
     * @param usuarioId ID del usuario que registra el movimiento
     * @return movimiento creado
     */
    ShiftMovementResponse addMovement(Long shiftId, String tipo, BigDecimal monto, String motivo, Long usuarioId);

    /**
     * Genera el reporte detallado de un turno.
     *
     * @param shiftId ID del turno
     * @return reporte con ventas, movimientos y diferencia
     */
    ShiftReportResponse getReport(Long shiftId);

    /**
     * Obtiene un turno por ID.
     */
    ShiftResponse getById(Long shiftId);

    /**
     * Busca turnos con filtros opcionales.
     */
    List<ShiftResponse> findByFilters(Long cajeroId, ShiftStatus estado);
}
