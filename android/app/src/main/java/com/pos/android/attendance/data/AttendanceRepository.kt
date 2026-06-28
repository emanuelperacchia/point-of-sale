package com.pos.android.attendance.data

import com.pos.android.attendance.data.model.*
import com.pos.android.core.network.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceApi: AttendanceApi
) {
    suspend fun checkIn(): ApiResult<AttendanceResponse> {
        return try {
            val response = attendanceApi.checkIn(CheckInRequest())
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al marcar entrada")
        }
    }

    suspend fun checkOut(): ApiResult<AttendanceResponse> {
        return try {
            val response = attendanceApi.checkOut(CheckOutRequest())
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al marcar salida")
        }
    }

    suspend fun getSummary(from: String? = null, to: String? = null): ApiResult<AttendanceSummaryResponse> {
        return try {
            val response = attendanceApi.getSummary(from, to)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener resumen")
        }
    }
}
