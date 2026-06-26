package com.pos.android.inventory.data

import com.pos.android.core.database.PosDatabase
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.local.ProductEntity
import com.pos.android.inventory.data.model.PriceResolutionResponse
import com.pos.android.inventory.data.model.ProductDto
import com.pos.android.inventory.data.model.ProductSearchResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productApi: ProductApi,
    private val database: PosDatabase
) {
    private val productDao = database.productDao()

    /**
     * Obtiene productos paginados. Cachea en Room para offline.
     */
    suspend fun getProducts(
        page: Int = 0,
        search: String? = null
    ): ApiResult<List<ProductDto>> {
        return try {
            val response = productApi.getProducts(
                page = page,
                search = search
            )
            // Cachear en Room solo si es la primera página
            if (page == 0 && search.isNullOrBlank()) {
                cacheProducts(response.content)
            }
            ApiResult.success(response.content)
        } catch (e: Exception) {
            // Fallback a cache local
            val cached = productDao.getAllProducts()
            // Convertir Flow a List para respuesta única
            ApiResult.Error("Error de conexión: ${e.message}")
        }
    }

    /**
     * Obtiene detalle de un producto por ID.
     */
    suspend fun getProductById(id: Long): ApiResult<ProductDto> {
        return try {
            val response = productApi.getProductById(id)
            // Cachear
            productDao.insert(response.toEntity())
            ApiResult.success(response)
        } catch (e: Exception) {
            // Fallback a cache local
            val cached = productDao.getProductById(id)
            if (cached != null) {
                ApiResult.success(cached.toDto())
            } else {
                ApiResult.Error("Producto no encontrado")
            }
        }
    }

    /**
     * Búsqueda rápida de productos (autocomplete).
     */
    suspend fun searchProducts(query: String): ApiResult<List<ProductSearchResponse>> {
        return try {
            val response = productApi.searchProducts(query)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error de búsqueda: ${e.message}")
        }
    }

    /**
     * Obtiene el precio resuelto de un producto (con precio de sucursal y promociones).
     */
    suspend fun getProductPrice(id: Long): ApiResult<PriceResolutionResponse> {
        return try {
            val response = productApi.getProductPrice(id)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al obtener precio: ${e.message}")
        }
    }

    // ── Helpers ──

    private suspend fun cacheProducts(products: List<ProductDto>) {
        productDao.deleteAll()
        productDao.insertAll(products.map { it.toEntity() })
    }

    private fun ProductDto.toEntity() = ProductEntity(
        id = id,
        name = name,
        sku = sku,
        description = description,
        price = price,
        stock = stock,
        tipo = tipo,
        categoryName = category?.name,
        active = active,
        updatedAt = updatedAt
    )

    private fun ProductEntity.toDto() = ProductDto(
        id = id,
        name = name,
        sku = sku,
        description = description,
        price = price,
        stock = stock,
        tipo = tipo,
        active = active,
        category = null,
        createdAt = null,
        updatedAt = updatedAt
    )
}
