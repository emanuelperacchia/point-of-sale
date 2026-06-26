package com.pos.android.inventory.data.model

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("sku") val sku: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("stock") val stock: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("category") val category: CategoryDto?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class CategoryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class ProductSearchResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("sku") val sku: String,
    @SerializedName("price") val price: Double,
    @SerializedName("stock") val stock: Int,
    @SerializedName("categoryName") val categoryName: String?
)

data class PriceResolutionResponse(
    @SerializedName("finalPrice") val finalPrice: Double,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("branchPrice") val branchPrice: Double?,
    @SerializedName("hasBranchPrice") val hasBranchPrice: Boolean,
    @SerializedName("branchName") val branchName: String?,
    @SerializedName("promotionDiscount") val promotionDiscount: Double?,
    @SerializedName("promotionName") val promotionName: String?
)

/**
 * Wrapper para respuestas paginadas de Spring Boot.
 */
data class PageResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("totalElements") val totalElements: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int
)
