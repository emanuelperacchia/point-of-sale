package com.pos.android.auth.data

import com.pos.android.auth.data.model.AuthResponse
import com.pos.android.auth.data.model.LoginRequest
import com.pos.android.auth.data.model.RegisterRequest
import com.pos.android.auth.data.model.SwitchBranchRequest
import com.pos.android.auth.data.model.UserResponse
import com.pos.android.core.network.ApiResult
import com.pos.android.core.security.TokenStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage
) {

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            saveSession(response)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.error(
                message = parseErrorMessage(e),
                code = parseErrorCode(e)
            )
        }
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?
    ): ApiResult<AuthResponse> {
        return try {
            val response = authApi.register(
                RegisterRequest(email, password, firstName, lastName, phone)
            )
            saveSession(response)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.error(
                message = parseErrorMessage(e),
                code = parseErrorCode(e)
            )
        }
    }

    suspend fun refreshToken(): ApiResult<AuthResponse> {
        val currentRefresh = tokenStorage.refreshToken ?: return ApiResult.error("No refresh token available")
        return try {
            val response = authApi.refreshToken(RefreshTokenRequest(currentRefresh))
            saveSession(response)
            ApiResult.success(response)
        } catch (e: Exception) {
            tokenStorage.clear()
            ApiResult.error(
                message = parseErrorMessage(e),
                code = parseErrorCode(e)
            )
        }
    }

    suspend fun switchBranch(branchId: Long): ApiResult<AuthResponse> {
        return try {
            val response = authApi.switchBranch(SwitchBranchRequest(branchId))
            saveSession(response)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.error(
                message = parseErrorMessage(e),
                code = parseErrorCode(e)
            )
        }
    }

    suspend fun getMe(): ApiResult<UserResponse> {
        return try {
            val response = authApi.getMe()
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.error(
                message = parseErrorMessage(e),
                code = parseErrorCode(e)
            )
        }
    }

    suspend fun logout() {
        try {
            val refreshToken = tokenStorage.refreshToken
            if (refreshToken != null) {
                authApi.logout(RefreshTokenRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Ignorar errores de logout
        } finally {
            tokenStorage.clear()
        }
    }

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn

    // ── Helpers ──

    private fun saveSession(response: AuthResponse) {
        tokenStorage.accessToken = response.accessToken
        tokenStorage.refreshToken = response.refreshToken
        tokenStorage.userId = response.userId
        tokenStorage.userEmail = response.email
        tokenStorage.userName = response.fullName
        response.branchId?.let { tokenStorage.activeBranchId = it }
        response.branches?.firstOrNull { it.id == response.branchId }?.let {
            tokenStorage.activeBranchName = it.nombre
        }
    }

    private fun parseErrorMessage(e: Exception): String {
        return e.message ?: "Error de conexión"
    }

    private fun parseErrorCode(e: Exception): Int? {
        return if (e is retrofit2.HttpException) {
            e.code()
        } else {
            null
        }
    }
}
