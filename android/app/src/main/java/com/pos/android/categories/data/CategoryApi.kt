package com.pos.android.categories.data

import com.pos.android.categories.data.model.CategoryCreateRequest
import com.pos.android.categories.data.model.CategoryDto
import com.pos.android.categories.data.model.CategoryPageResponse
import com.pos.android.categories.data.model.CategoryUpdateRequest
import com.pos.android.categories.data.model.CategorySearchResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CategoryApi {

    @GET("categories")
    suspend fun getCategories(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "name",
        @Query("sortDir") sortDir: String = "asc",
        @Query("search") search: String? = null
    ): CategoryPageResponse

    @GET("categories/active")
    suspend fun getActiveCategories(): List<CategoryDto>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): CategoryDto

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryCreateRequest): CategoryDto

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: CategoryUpdateRequest
    ): CategoryDto

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Unit

    @GET("categories/search")
    suspend fun searchCategories(
        @Query("q") query: String
    ): List<CategorySearchResponse>
}
