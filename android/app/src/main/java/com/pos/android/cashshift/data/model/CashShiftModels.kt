package com.pos.android.cashshift.data.model

import com.google.gson.annotations.SerializedName

// ── Responses ──

data class ShiftResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("cajeroId") val cajeroId: Long?,
    @SerializedName("cajeroNombre") val cajeroNombre: String?,
    @SerializedName("sucursalId") val sucursalId: Long?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("montoApertura") val montoApertura: Double?,
    @SerializedName("montoCierre") val montoCierre: Double?,
    @SerializedName("diferencia") val diferencia: Double?,
    @SerializedName("fechaApertura") val fechaApertura: String?,
    @SerializedName("fechaCierre") val fechaCierre: String?
)

data class ShiftReportResponse(
    @SerializedName("shiftId") val shiftId: Long,
    @SerializedName("estado") val estado: String?,
    @SerializedName("montoApertura") val montoApertura: Double?,
    @SerializedName("totalVentasEfectivo") val totalVentasEfectivo: Double?,
    @SerializedName("ventasPorMetodoPago") val ventasPorMetodoPago: Map<String, Double>?,
    @SerializedName("totalIngresos") val totalIngresos: Double?,
    @SerializedName("totalRetiros") val totalRetiros: Double?,
    @SerializedName("movimientos") val movimientos: List<ShiftMovementResponse>?,
    @SerializedName("montoEsperado") val montoEsperado: Double?,
    @SerializedName("montoCierreDeclarado") val montoCierreDeclarado: Double?,
    @SerializedName("diferencia") val diferencia: Double?,
    @SerializedName("fechaApertura") val fechaApertura: String?,
    @SerializedName("fechaCierre") val fechaCierre: String?
)

data class ShiftMovementResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("shiftId") val shiftId: Long?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("monto") val monto: Double?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("usuarioNombre") val usuarioNombre: String?,
    @SerializedName("createdAt") val createdAt: String?
)
