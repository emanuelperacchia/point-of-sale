package com.pos.android.suppliers.data

import com.pos.android.suppliers.data.model.CategoryDto
import com.pos.android.suppliers.data.model.SupplierCreateRequest
import com.pos.android.suppliers.data.model.SupplierDto
import com.pos.android.suppliers.data.model.SupplierPageResponse
import com.pos.android.suppliers.data.model.SupplierUpdateRequest
import com.pos.android.suppliers.data.model.SupplierSearchResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SupplierApi {

    @GET("suppliers")
    suspend fun getSuppliers(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("activeOnly") activeOnly: Boolean = false,
        @Query("search") search: String? = null
    ): SupplierPageResponse

    @GET("suppliers/{id}")
    suspend fun getSupplierById(@Path("id") id: Long): SupplierDto

    @POST("suppliers")
    suspend fun createSupplier(@Body request: SupplierCreateRequest): SupplierDto

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(
        @Path("id") id: Long,
        @Body request: SupplierUpdateRequest
    ): SupplierDto

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: Long): Unit

    @GET("suppliers/search")
    suspend fun searchSuppliers(
        @Query("term") term: String
    ): List<SupplierSearchResponse>
}
