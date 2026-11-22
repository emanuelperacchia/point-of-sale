package com.pos.android.suppliers.data.model

import com.google.gson.annotations.SerializedName

data class SupplierDto(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("businessName") val businessName: String,
    @SerializedName("tradeName") val tradeName: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class SupplierCreateRequest(
    @SerializedName("code") val code: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("businessName") val businessName: String,
    @SerializedName("tradeName") val tradeName: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("active") val active: Boolean = true
)

data class SupplierSearchResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("businessName") val businessName: String,
    @SerializedName("taxId") val taxId: String?
)

data class SupplierPageResponse(
    @SerializedName("content") val content: List<SupplierDto>,
    @SerializedName("totalElements") val totalElements: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int
)
