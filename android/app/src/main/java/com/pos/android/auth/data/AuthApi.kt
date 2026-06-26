package com.pos.android.auth.data

import com.pos.android.auth.data.model.AuthResponse
import com.pos.android.auth.data.model.LoginRequest
import com.pos.android.auth.data.model.RefreshTokenRequest
import com.pos.android.auth.data.model.RegisterRequest
import com.pos.android.auth.data.model.SwitchBranchRequest
import com.pos.android.auth.data.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest)

    @GET("auth/me")
    suspend fun getMe(): UserResponse

    @POST("auth/switch-branch")
    suspend fun switchBranch(@Body request: SwitchBranchRequest): AuthResponse
}
