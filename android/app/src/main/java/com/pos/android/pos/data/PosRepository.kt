package com.pos.android.pos.data

import com.google.gson.Gson
import com.pos.android.core.network.ApiResult
import com.pos.android.core.network.connectivity.ConnectivityObserver
import com.pos.android.core.network.connectivity.ConnectivityStatus
import com.pos.android.pos.data.local.CartItemDao
import com.pos.android.pos.data.local.CartItemEntity
import com.pos.android.pos.data.local.PendingSaleDao
import com.pos.android.pos.data.local.PendingSaleEntity
import com.pos.android.pos.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepository @Inject constructor(
    private val posApi: PosApi,
    private val cartItemDao: CartItemDao,
    private val pendingSaleDao: PendingSaleDao,
    private val connectivityObserver: ConnectivityObserver,
    private val gson: Gson
) {
    // ── Cart ──

    fun observeCart() = cartItemDao.observeAll()

    fun observeCartCount() = cartItemDao.count()

    suspend fun getCartItems(): List<CartItemEntity> = cartItemDao.getAll()

    suspend fun addToCart(productId: Long, nombre: String, sku: String, precio: Double, stock: Int, cantidad: Int = 1) {
        val existing = cartItemDao.getByProductId(productId)
        if (existing != null) {
            cartItemDao.updateQuantity(existing.id, existing.cantidad + cantidad)
        } else {
            cartItemDao.upsert(
                CartItemEntity(
                    productId = productId,
                    nombre = nombre,
                    sku = sku,
                    precio = precio,
                    stockDisponible = stock,
                    cantidad = cantidad
                )
            )
        }
    }

    suspend fun updateCartItemQuantity(id: Long, cantidad: Int) {
        if (cantidad <= 0) {
            cartItemDao.delete(cartItemDao.getByProductId(id) ?: return)
        } else {
            cartItemDao.updateQuantity(id, cantidad)
        }
    }

    suspend fun removeFromCart(item: CartItemEntity) = cartItemDao.delete(item)

    suspend fun clearCart() = cartItemDao.clearAll()

    // ── Sale Processing ──

    suspend fun processSale(saleRequest: SaleRequest): ApiResult<SaleResponse> {
        return try {
            val response = posApi.createSale(saleRequest)
            clearCart()
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al procesar venta")
        }
    }

    suspend fun processOfflineSale(bodyJson: String): ApiResult<SaleResponse> {
        return try {
            val request = gson.fromJson(bodyJson, SaleRequest::class.java)
            val response = posApi.createSale(request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al sincronizar venta offline")
        }
    }

    suspend fun saveOfflineSale(saleRequest: SaleRequest): Long {
        val json = gson.toJson(saleRequest)
        return pendingSaleDao.insert(
            PendingSaleEntity(bodyJson = json)
        )
    }

    suspend fun hasPendingSales(): Boolean {
        return pendingSaleDao.getPending().isNotEmpty()
    }

    fun observePendingCount() = pendingSaleDao.countPending()

    suspend fun validateCart(items: List<CartValidationItem>): ApiResult<CartValidationResponse> {
        return try {
            val response = posApi.validateCart(CartValidationRequest(items))
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al validar carrito")
        }
    }

    fun isOnline(): Boolean = connectivityObserver.isCurrentlyConnected()

    // ── Clients ──

    suspend fun searchClients(query: String): ApiResult<List<ClientSearchResponse>> {
        return try {
            val response = posApi.searchClients(query)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al buscar clientes")
        }
    }

    // ── Invoices ──

    suspend fun getInvoiceBySaleId(saleId: Long): ApiResult<InvoiceResponse?> {
        return try {
            val invoices = posApi.getInvoicesBySaleId(saleId)
            ApiResult.success(invoices.firstOrNull())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener comprobante")
        }
    }

    // ── Promotions ──

    suspend fun getActivePromotions(): ApiResult<List<PromotionResponse>> {
        return try {
            val promotions = posApi.getPromotions()
            ApiResult.success(promotions.filter { it.activa == true })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener promociones")
        }
    }

    // ── Sales History ──

    suspend fun getSales(page: Int = 0, size: Int = 20): ApiResult<SalesPage> {
        return try {
            val response = posApi.getSales(page, size)
            val contentList = (response["content"] as? List<Map<String, Any>>)?.mapNotNull { map ->
                gson.fromJson(gson.toJson(map), SaleResponse::class.java)
            } ?: emptyList()
            val totalPages = (response["totalPages"] as? Double)?.toInt() ?: 0
            val totalElements = (response["totalElements"] as? Double)?.toLong() ?: 0L
            val currentPage = (response["number"] as? Double)?.toInt() ?: 0
            ApiResult.success(SalesPage(contentList, totalPages, totalElements, currentPage))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener historial de ventas")
        }
    }

    suspend fun getSaleDetail(id: Long): ApiResult<SaleResponse> {
        return try {
            val response = posApi.getSaleDetail(id)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al obtener detalle de venta")
        }
    }
}

data class SalesPage(
    val content: List<SaleResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val currentPage: Int
) {
    val hasMore: Boolean get() = currentPage < totalPages - 1
}
