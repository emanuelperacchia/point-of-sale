package com.pos.android.attendance.data

import com.pos.android.attendance.data.model.*
import retrofit2.http.*

interface AttendanceApi {

    @POST("attendance/check-in")
    suspend fun checkIn(@Body request: CheckInRequest): AttendanceResponse

    @POST("attendance/check-out")
    suspend fun checkOut(@Body request: CheckOutRequest): AttendanceResponse

    @GET("attendance")
    suspend fun getAttendance(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): List<AttendanceResponse>

    @GET("attendance/summary")
    suspend fun getSummary(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): AttendanceSummaryResponse
}
