package com.pos.android.prices.data

import com.pos.android.core.network.ApiResult
import com.pos.android.prices.data.model.PriceCreateRequest
import com.pos.android.prices.data.model.PriceDto
import com.pos.android.prices.data.model.PriceResolutionResponse
import com.pos.android.prices.data.model.PriceUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepository @Inject constructor(
    private val priceApi: PriceApi
) {

    /**
     * Resuelve el precio de un producto según la sucursal activa.
     */
    suspend fun resolvePrice(productId: Long): ApiResult<PriceResolutionResponse> {
        return try {
            val response = priceApi.resolvePrice(productId)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al resolver precio: ${e.message}")
        }
    }

    /**
     * Obtiene los precios locales de una sucursal.
     */
    suspend fun getPricesByBranch(branchId: Long): ApiResult<List<PriceDto>> {
        return try {
            val response = priceApi.getPricesByBranch(branchId)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al cargar precios: ${e.message}")
        }
    }

    /**
     * Crea un precio local.
     */
    suspend fun createPrice(
        branchId: Long,
        request: PriceCreateRequest
    ): ApiResult<PriceDto> {
        return try {
            val response = priceApi.createPrice(branchId, request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al crear precio: ${e.message}")
        }
    }

    /**
     * Actualiza un precio local.
     */
    suspend fun updatePrice(
        branchId: Long,
        priceId: Long,
        request: PriceUpdateRequest
    ): ApiResult<PriceDto> {
        return try {
            val response = priceApi.updatePrice(branchId, priceId, request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al actualizar precio: ${e.message}")
        }
    }

    /**
     * Elimina un precio local.
     */
    suspend fun deletePrice(
        branchId: Long,
        priceId: Long
    ): ApiResult<Unit> {
        return try {
            priceApi.deletePrice(branchId, priceId)
            ApiResult.success(Unit)
        } catch (e: Exception) {
            ApiResult.Error("Error al eliminar precio: ${e.message}")
        }
    }
}
