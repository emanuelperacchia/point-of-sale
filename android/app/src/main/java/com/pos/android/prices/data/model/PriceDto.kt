package com.pos.android.prices.data.model

import com.google.gson.annotations.SerializedName

data class PriceDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("productId") val productId: Long,
    @SerializedName("productName") val productName: String?,
    @SerializedName("productSku") val productSku: String?,
    @SerializedName("precio") val precio: Double,
    @SerializedName("precioGlobal") val precioGlobal: Double?,
    @SerializedName("vigenciaDesde") val vigenciaDesde: String?,
    @SerializedName("vigenciaHasta") val vigenciaHasta: String?,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)

data class PriceCreateRequest(
    @SerializedName("productId") val productId: Long,
    @SerializedName("precio") val precio: Double,
    @SerializedName("vigenciaDesde") val vigenciaDesde: String?,
    @SerializedName("vigenciaHasta") val vigenciaHasta: String?
)

data class PriceUpdateRequest(
    @SerializedName("productId") val productId: Long,
    @SerializedName("precio") val precio: Double,
    @SerializedName("vigenciaDesde") val vigenciaDesde: String?,
    @SerializedName("vigenciaHasta") val vigenciaHasta: String?
)

data class PriceResolutionResponse(
    @SerializedName("productId") val productId: Long,
    @SerializedName("productName") val productName: String?,
    @SerializedName("productSku") val productSku: String?,
    @SerializedName("precioFinal") val precioFinal: Double,
    @SerializedName("precioGlobal") val precioGlobal: Double?,
    @SerializedName("precioLocal") val precioLocal: Double?,
    @SerializedName("esLocal") val esLocal: Boolean,
    @SerializedName("sucursalId") val sucursalId: Long?,
    @SerializedName("vigenciaHasta") val vigenciaHasta: String?
)
