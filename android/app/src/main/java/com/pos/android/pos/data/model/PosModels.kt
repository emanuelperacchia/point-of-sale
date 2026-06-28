package com.pos.android.pos.data.model

import com.google.gson.annotations.SerializedName

// ── Request ──

data class SaleRequest(
    @SerializedName("items") val items: List<SaleItemRequest>,
    @SerializedName("payments") val payments: List<PaymentRequest>,
    @SerializedName("clientId") val clientId: Long? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("couponCode") val couponCode: String? = null,
    @SerializedName("puntosCanje") val puntosCanje: Long? = null
)

data class SaleItemRequest(
    @SerializedName("productId") val productId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("discount") val discount: Double? = null
)

data class PaymentRequest(
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("reference") val reference: String? = null
)

data class CartValidationRequest(
    @SerializedName("items") val items: List<CartValidationItem>
)

data class CartValidationItem(
    @SerializedName("productId") val productId: Long,
    @SerializedName("quantity") val quantity: Int
)

// ── Response ──

data class SaleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("status") val status: String?,
    @SerializedName("subtotal") val subtotal: Double?,
    @SerializedName("taxAmount") val taxAmount: Double?,
    @SerializedName("discount") val discount: Double?,
    @SerializedName("total") val total: Double?,
    @SerializedName("items") val items: List<SaleItemResponse>?,
    @SerializedName("payments") val payments: List<PaymentResponse>?,
    @SerializedName("createdAt") val createdAt: String?
)

data class SaleItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("productId") val productId: Long?,
    @SerializedName("productName") val productName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unitPrice") val unitPrice: Double?,
    @SerializedName("subtotal") val subtotal: Double?
)

data class PaymentResponse(
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("amount") val amount: Double
)

data class CartValidationResponse(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("errors") val errors: List<String>?,
    @SerializedName("items") val items: List<CartValidationItemResult>?
)

data class CartValidationItemResult(
    @SerializedName("productId") val productId: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("availableStock") val availableStock: Int,
    @SerializedName("requestedQuantity") val requestedQuantity: Int,
    @SerializedName("error") val error: String?
)

// ── Error ──

data class ErrorResponse(
    @SerializedName("status") val status: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)
