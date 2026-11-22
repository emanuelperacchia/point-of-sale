package com.pos.android.suppliers.data

import com.pos.android.core.network.ApiResult
import com.pos.android.suppliers.data.model.SupplierCreateRequest
import com.pos.android.suppliers.data.model.SupplierDto
import com.pos.android.suppliers.data.model.SupplierPageResponse
import com.pos.android.suppliers.data.model.SupplierSearchResponse
import com.pos.android.suppliers.data.model.SupplierUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val supplierApi: SupplierApi
) {

    /**
     * Obtiene proveedores paginados.
     */
    suspend fun getSuppliers(
        page: Int = 0,
        size: Int = 20,
        activeOnly: Boolean = false,
        search: String? = null
    ): ApiResult<List<SupplierDto>> {
        return try {
            val response = supplierApi.getSuppliers(
                page = page,
                size = size,
                activeOnly = activeOnly,
                search = search
            )
            ApiResult.success(response.content)
        } catch (e: Exception) {
            ApiResult.Error("Error al cargar proveedores: ${e.message}")
        }
    }

    /**
     * Obtiene un proveedor por ID.
     */
    suspend fun getSupplierById(id: Long): ApiResult<SupplierDto> {
        return try {
            val response = supplierApi.getSupplierById(id)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Proveedor no encontrado")
        }
    }

    /**
     * Crea un nuevo proveedor.
     */
    suspend fun createSupplier(request: SupplierCreateRequest): ApiResult<SupplierDto> {
        return try {
            val response = supplierApi.createSupplier(request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al crear proveedor: ${e.message}")
        }
    }

    /**
     * Actualiza un proveedor existente.
     */
    suspend fun updateSupplier(
        id: Long,
        request: SupplierUpdateRequest
    ): ApiResult<SupplierDto> {
        return try {
            val response = supplierApi.updateSupplier(id, request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al actualizar proveedor: ${e.message}")
        }
    }

    /**
     * Elimina un proveedor.
     */
    suspend fun deleteSupplier(id: Long): ApiResult<Unit> {
        return try {
            supplierApi.deleteSupplier(id)
            ApiResult.success(Unit)
        } catch (e: Exception) {
            ApiResult.Error("Error al eliminar proveedor: ${e.message}")
        }
    }

    /**
     * Busca proveedores por nombre.
     */
    suspend fun searchSuppliers(query: String): ApiResult<List<SupplierSearchResponse>> {
        return try {
            val response = supplierApi.searchSuppliers(query)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al buscar proveedores: ${e.message}")
        }
    }
}
