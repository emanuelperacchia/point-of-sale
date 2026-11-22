package com.pos.android.pos.data

import com.pos.android.pos.data.model.CartValidationRequest
import com.pos.android.pos.data.model.CartValidationResponse
import com.pos.android.pos.data.model.ClientSearchResponse
import com.pos.android.pos.data.model.InvoiceResponse
import com.pos.android.pos.data.model.PromotionResponse
import com.pos.android.pos.data.model.SaleRequest
import com.pos.android.pos.data.model.SaleResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PosApi {

    @POST("sales")
    suspend fun createSale(@Body request: SaleRequest): SaleResponse

    @POST("sales/validate-cart")
    suspend fun validateCart(@Body request: CartValidationRequest): CartValidationResponse

    // ── Sales History ──

    @GET("sales")
    suspend fun getSales(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Map<String, @JvmSuppressWildcards Any>

    @GET("sales/{id}")
    suspend fun getSaleDetail(@Path("id") id: Long): SaleResponse

    // ── Clients ──

    @GET("clients/search")
    suspend fun searchClients(@Query("q") query: String): List<ClientSearchResponse>

    // ── Invoices ──

    @GET("invoices")
    suspend fun getInvoicesBySaleId(@Query("saleId") saleId: Long): List<InvoiceResponse>

    // ── Promotions ──

    @GET("promotions")
    suspend fun getPromotions(): List<PromotionResponse>
}
