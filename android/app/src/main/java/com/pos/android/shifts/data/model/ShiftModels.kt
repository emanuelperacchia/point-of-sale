package com.pos.android.shifts.data.model

import com.google.gson.annotations.SerializedName

data class ShiftDefinitionResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    @SerializedName("activo") val activo: Boolean
)

data class ShiftAssignmentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("employeeId") val employeeId: Long,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("shiftDefinitionId") val shiftDefinitionId: Long,
    @SerializedName("shiftName") val shiftName: String?,
    @SerializedName("diaSemana") val diaSemana: Int,
    @SerializedName("fechaInicio") val fechaInicio: String?,
    @SerializedName("fechaFin") val fechaFin: String?
)

data class ShiftScheduleResponse(
    @SerializedName("empleados") val empleados: List<EmployeeSchedule>?
)

data class EmployeeSchedule(
    @SerializedName("employeeId") val employeeId: Long,
    @SerializedName("employeeName") val employeeName: String,
    @SerializedName("shifts") val shifts: List<ShiftInfo>?
)

data class ShiftInfo(
    @SerializedName("diaSemana") val diaSemana: Int,
    @SerializedName("shiftName") val shiftName: String?,
    @SerializedName("horaInicio") val horaInicio: String?,
    @SerializedName("horaFin") val horaFin: String?
)

data class ShiftChangeRequest(
    @SerializedName("employeeId") val employeeId: Long,
    @SerializedName("fechaOrigen") val fechaOrigen: String,
    @SerializedName("turnoOrigenId") val turnoOrigenId: Long?,
    @SerializedName("fechaDestino") val fechaDestino: String?,
    @SerializedName("turnoDestinoId") val turnoDestinoId: Long?,
    @SerializedName("motivo") val motivo: String
)

data class ShiftChangeRequestResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("employeeId") val employeeId: Long?,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("estado") val estado: String?
)
