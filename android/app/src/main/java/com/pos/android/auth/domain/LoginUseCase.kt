package com.pos.android.auth.domain

import com.pos.android.auth.data.AuthRepository
import com.pos.android.core.network.ApiResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<Boolean> {
        val result = authRepository.login(email, password)
        return result.map { true }
    }
}
