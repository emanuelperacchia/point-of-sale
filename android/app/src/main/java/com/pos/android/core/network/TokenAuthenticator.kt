package com.pos.android.core.network

import com.pos.android.auth.data.TokenRefreshApi
import com.pos.android.core.security.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticator que intenta renovar el token automáticamente ante un 401.
 * Si el refresh también falla, limpia el storage y fuerza el login.
 * Usa AuthApi desde un Retrofit SIN authenticator para evitar ciclo.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val tokenRefreshApi: TokenRefreshApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenStorage.refreshToken ?: return null

        // Evita bucles infinitos si el refresh también falla
        if (response.request.header("X-Refresh") != null) {
            tokenStorage.clear()
            return null
        }

        return try {
            val result = runBlocking {
                tokenRefreshApi.refreshToken(
                    com.pos.android.auth.data.model.RefreshTokenRequest(refreshToken)
                )
            }

            tokenStorage.accessToken = result.accessToken
            tokenStorage.refreshToken = result.refreshToken

            response.request.newBuilder()
                .header("Authorization", "Bearer ${result.accessToken}")
                .header("X-Refresh", "true")
                .build()
        } catch (e: Exception) {
            tokenStorage.clear()
            null
        }
    }
}
