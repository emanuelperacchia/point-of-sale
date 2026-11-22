package com.pos.android.cashshift.data

import com.pos.android.cashshift.data.model.ShiftMovementResponse
import com.pos.android.cashshift.data.model.ShiftReportResponse
import com.pos.android.cashshift.data.model.ShiftResponse
import com.pos.android.core.network.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashShiftRepository @Inject constructor(
    private val cashShiftApi: CashShiftApi
) {
    suspend fun openShift(montoApertura: Double): ApiResult<ShiftResponse> {
        return try {
            val response = cashShiftApi.openShift(mapOf("montoApertura" to montoApertura))
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al abrir turno")
        }
    }

    suspend fun closeShift(shiftId: Long, montoCierre: Double): ApiResult<ShiftResponse> {
        return try {
            val response = cashShiftApi.closeShift(shiftId, mapOf("montoCierre" to montoCierre))
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al cerrar turno")
        }
    }

    suspend fun getShiftReport(shiftId: Long): ApiResult<ShiftReportResponse> {
        return try {
            val response = cashShiftApi.getShiftReport(shiftId)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener reporte")
        }
    }

    suspend fun createMovement(
        shiftId: Long,
        tipo: String,
        monto: Double,
        motivo: String
    ): ApiResult<ShiftMovementResponse> {
        return try {
            val response = cashShiftApi.createMovement(
                shiftId,
                mapOf("tipo" to tipo, "monto" to monto, "motivo" to motivo)
            )
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al registrar movimiento")
        }
    }

    suspend fun getActiveShift(): ApiResult<ShiftResponse?> {
        return try {
            val shifts = cashShiftApi.getShifts(estado = "ABIERTO")
            ApiResult.success(shifts.firstOrNull())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener turno activo")
        }
    }
}
