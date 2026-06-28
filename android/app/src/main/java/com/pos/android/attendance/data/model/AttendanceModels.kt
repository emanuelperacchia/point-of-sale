package com.pos.android.attendance.data.model

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class CheckOutRequest(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class AttendanceResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("employeeId") val employeeId: Long?,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("entrada") val entrada: String?,
    @SerializedName("salida") val salida: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("horasTrabajadas") val horasTrabajadas: Double?,
    @SerializedName("createdAt") val createdAt: String?
)

data class AttendanceSummaryResponse(
    @SerializedName("totalAsistencias") val totalAsistencias: Int,
    @SerializedName("totalAusencias") val totalAusencias: Int,
    @SerializedName("totalTardanzas") val totalTardanzas: Int,
    @SerializedName("horasTotales") val horasTotales: Double,
    @SerializedName("registros") val registros: List<AttendanceResponse>?
)

data class AbsenceRequest(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("justificacion") val justificacion: String?,
    @SerializedName("empleadoId") val empleadoId: Long
)
