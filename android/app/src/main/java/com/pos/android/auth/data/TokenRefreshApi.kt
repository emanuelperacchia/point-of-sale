package com.pos.android.auth.data

import com.pos.android.auth.data.model.AuthResponse
import com.pos.android.auth.data.model.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API mínima para refresh de token.
 * Separada de AuthApi para romper la dependencia circular:
 * TokenAuthenticator → TokenRefreshApi → AuthRetrofit (sin authenticator) → Retrofit → OkHttpClient
 * En lugar de: TokenAuthenticator → AuthApi → Retrofit (con authenticator) → OkHttpClient → TokenAuthenticator
 */
interface TokenRefreshApi {

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthResponse
}
