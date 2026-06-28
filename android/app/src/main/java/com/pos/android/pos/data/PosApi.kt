package com.pos.android.pos.data

import com.pos.android.pos.data.model.CartValidationRequest
import com.pos.android.pos.data.model.CartValidationResponse
import com.pos.android.pos.data.model.SaleRequest
import com.pos.android.pos.data.model.SaleResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PosApi {

    @POST("sales")
    suspend fun createSale(@Body request: SaleRequest): SaleResponse

    @POST("sales/validate-cart")
    suspend fun validateCart(@Body request: CartValidationRequest): CartValidationResponse

    @GET("sales/{id}")
    suspend fun getSaleById(@Path("id") id: Long): SaleResponse
}
