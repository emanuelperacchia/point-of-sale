package com.pos.android.shifts.data

import com.pos.android.core.network.ApiResult
import com.pos.android.shifts.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(
    private val shiftApi: ShiftApi
) {
    suspend fun getSchedule(): ApiResult<List<EmployeeSchedule>> {
        return try {
            val response = shiftApi.getSchedule()
            ApiResult.success(response.empleados ?: emptyList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener horarios")
        }
    }

    suspend fun getDefinitions(): ApiResult<List<ShiftDefinitionResponse>> {
        return try {
            val response = shiftApi.getDefinitions()
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener definiciones de turnos")
        }
    }

    suspend fun createChangeRequest(request: ShiftChangeRequest): ApiResult<ShiftChangeRequestResponse> {
        return try {
            val response = shiftApi.createChangeRequest(request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al crear solicitud de cambio")
        }
    }
}
