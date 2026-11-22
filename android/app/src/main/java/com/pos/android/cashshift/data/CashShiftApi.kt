package com.pos.android.cashshift.data

import com.pos.android.cashshift.data.model.ShiftMovementResponse
import com.pos.android.cashshift.data.model.ShiftReportResponse
import com.pos.android.cashshift.data.model.ShiftResponse
import retrofit2.http.*

interface CashShiftApi {

    @POST("shifts/open")
    suspend fun openShift(@Body request: Map<String, @JvmSuppressWildcards Any>): ShiftResponse

    @POST("shifts/{id}/close")
    suspend fun closeShift(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ShiftResponse

    @GET("shifts/{id}/report")
    suspend fun getShiftReport(@Path("id") id: Long): ShiftReportResponse

    @POST("shifts/{id}/movements")
    suspend fun createMovement(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ShiftMovementResponse

    @GET("shifts")
    suspend fun getShifts(
        @Query("estado") estado: String? = null
    ): List<ShiftResponse>
}
