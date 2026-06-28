package com.pos.android.shifts.data

import com.pos.android.shifts.data.model.*
import retrofit2.http.*

interface ShiftApi {

    @GET("shifts/definitions")
    suspend fun getDefinitions(): List<ShiftDefinitionResponse>

    @GET("shifts/schedule")
    suspend fun getSchedule(): ShiftScheduleResponse

    @GET("shifts/employees/{employeeId}")
    suspend fun getEmployeeShifts(@Path("employeeId") employeeId: Long): List<ShiftAssignmentResponse>

    @POST("shifts/change-requests")
    suspend fun createChangeRequest(@Body request: ShiftChangeRequest): ShiftChangeRequestResponse

    @GET("shifts/change-requests/pending")
    suspend fun getPendingRequests(): List<ShiftChangeRequestResponse>
}
