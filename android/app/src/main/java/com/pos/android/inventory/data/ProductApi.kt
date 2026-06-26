package com.pos.android.inventory.data

import com.pos.android.inventory.data.model.PageResponse
import com.pos.android.inventory.data.model.PriceResolutionResponse
import com.pos.android.inventory.data.model.ProductDto
import com.pos.android.inventory.data.model.ProductSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "name",
        @Query("sortDir") sortDir: String = "asc",
        @Query("search") search: String? = null,
        @Query("tipo") tipo: String? = null
    ): PageResponse<ProductDto>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Long): ProductDto

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): List<ProductSearchResponse>

    @GET("products/{id}/price")
    suspend fun getProductPrice(@Path("id") id: Long): PriceResolutionResponse
}
