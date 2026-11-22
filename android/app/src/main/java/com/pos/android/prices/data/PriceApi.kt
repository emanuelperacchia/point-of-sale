package com.pos.android.prices.data

import com.pos.android.prices.data.model.PriceCreateRequest
import com.pos.android.prices.data.model.PriceDto
import com.pos.android.prices.data.model.PriceResolutionResponse
import com.pos.android.prices.data.model.PriceUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PriceApi {

    @Get("/api/products/{id}/price")
    suspend fun resolvePrice(
        @Path("id") productId: Long
    ): PriceResolutionResponse

    @GET("branches/{branchId}/prices")
    suspend fun getPricesByBranch(
        @Path("branchId") branchId: Long
    ): List<PriceDto>

    @POST("branches/{branchId}/prices")
    suspend fun createPrice(
        @Path("branchId") branchId: Long,
        @Body request: PriceCreateRequest
    ): PriceDto

    @PUT("branches/{branchId}/prices/{priceId}")
    suspend fun updatePrice(
        @Path("branchId") branchId: Long,
        @Path("priceId") priceId: Long,
        @Body request: PriceUpdateRequest
    ): PriceDto

    @DELETE("branches/{branchId}/prices/{priceId}")
    suspend fun deletePrice(
        @Path("branchId") branchId: Long,
        @Path("priceId") priceId: Long
    ): Unit
}
