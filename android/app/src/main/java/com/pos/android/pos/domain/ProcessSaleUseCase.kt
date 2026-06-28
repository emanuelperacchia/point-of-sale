package com.pos.android.pos.domain

import com.pos.android.core.network.ApiResult
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.model.*
import com.pos.android.pos.data.local.CartItemEntity
import javax.inject.Inject

data class SaleResult(
    val saleResponse: SaleResponse?,
    val wasOffline: Boolean,
    val errorMessage: String?
) {
    val isSuccess: Boolean get() = saleResponse != null && errorMessage == null
    val isPendingSync: Boolean get() = wasOffline && saleResponse == null
}

class ProcessSaleUseCase @Inject constructor(
    private val posRepository: PosRepository
) {
    suspend operator fun invoke(
        cartItems: List<CartItemEntity>,
        payments: List<PaymentRequest>,
        clientId: Long? = null,
        notes: String? = null,
        couponCode: String? = null
    ): SaleResult {
        // Validar carrito vacío
        if (cartItems.isEmpty()) {
            return SaleResult(null, false, "El carrito está vacío")
        }

        // Construir request
        val saleItems = cartItems.map { item ->
            SaleItemRequest(
                productId = item.productId,
                quantity = item.cantidad,
                discount = if (item.descuento > 0) item.descuento else null
            )
        }

        val saleRequest = SaleRequest(
            items = saleItems,
            payments = payments,
            clientId = clientId,
            notes = notes,
            couponCode = couponCode
        )

        // Verificar conectividad
        return if (posRepository.isOnline()) {
            processOnline(saleRequest)
        } else {
            processOffline(saleRequest)
        }
    }

    private suspend fun processOnline(request: SaleRequest): SaleResult {
        return when (val result = posRepository.processSale(request)) {
            is ApiResult.Success -> {
                SaleResult(
                    saleResponse = result.data,
                    wasOffline = false,
                    errorMessage = null
                )
            }
            is ApiResult.Error -> {
                // Si falla por conectividad (no por negocio), guardar offline
                if (isConnectionError(result)) {
                    saveAndReturnOffline(request)
                } else {
                    SaleResult(null, false, result.message)
                }
            }
        }
    }

    private suspend fun processOffline(request: SaleRequest): SaleResult {
        return saveAndReturnOffline(request)
    }

    private suspend fun saveAndReturnOffline(request: SaleRequest): SaleResult {
        posRepository.saveOfflineSale(request)
        posRepository.clearCart()
        return SaleResult(
            saleResponse = null,
            wasOffline = true,
            errorMessage = null
        )
    }

    private fun isConnectionError(result: ApiResult.Error): Boolean {
        val msg = result.message.lowercase()
        return msg.contains("timeout") ||
                msg.contains("unable to resolve host") ||
                msg.contains("failed to connect") ||
                msg.contains("network") ||
                msg.contains("eof") ||
                msg.contains("connection")
    }
}
