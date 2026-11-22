package com.pos.android.users.data

import com.pos.android.users.data.model.UserCreateRequest
import com.pos.android.users.data.model.UserDto
import com.pos.android.users.data.model.UserPageResponse
import com.pos.android.users.data.model.UserUpdateRequest
import com.pos.android.users.data.model.UserSearchResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null
    ): UserPageResponse

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): UserDto

    @POST("users")
    suspend fun createUser(@Body request: UserCreateRequest): UserDto

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body request: UserUpdateRequest
    ): UserDto

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Unit

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String
    ): List<UserSearchResponse>
}
