package com.pos.android.auth.data.model

import com.google.gson.annotations.SerializedName

// ── Request DTOs ──

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("phone") val phone: String? = null
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class SwitchBranchRequest(
    @SerializedName("branchId") val branchId: Long
)

// ── Response DTOs ──

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("userId") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("branchId") val branchId: Long?,
    @SerializedName("branches") val branches: List<BranchInfo>?
)

data class BranchInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("direccion") val direccion: String?
)

data class ErrorResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String,
    @SerializedName("path") val path: String?,
    @SerializedName("timestamp") val timestamp: String?
)

// ── User Me DTO ──

data class UserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("roles") val roles: List<String>,
    @SerializedName("lastLogin") val lastLogin: String?
)
