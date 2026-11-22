package com.pos.android.categories.data

import com.pos.android.core.network.ApiResult
import com.pos.android.categories.data.model.CategoryDto
import com.pos.android.categories.data.model.CategoryCreateRequest
import com.pos.android.categories.data.model.CategoryPageResponse
import com.pos.android.categories.data.model.CategorySearchResponse
import com.pos.android.categories.data.model.CategoryUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {

    /**
     * Obtiene categorías paginadas.
     */
    suspend fun getCategories(
        page: Int = 0,
        size: Int = 20,
        search: String? = null
    ): ApiResult<List<CategoryDto>> {
        return try {
            val response = categoryApi.getCategories(
                page = page,
                size = size,
                search = search
            )
            ApiResult.success(response.content)
        } catch (e: Exception) {
            ApiResult.Error("Error al cargar categorías: ${e.message}")
        }
    }

    /**
     * Obtiene categorías activas (para selects).
     */
    suspend fun getActiveCategories(): ApiResult<List<CategoryDto>> {
        return try {
            val response = categoryApi.getActiveCategories()
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al cargar categorías activas: ${e.message}")
        }
    }

    /**
     * Obtiene una categoría por ID.
     */
    suspend fun getCategoryById(id: Long): ApiResult<CategoryDto> {
        return try {
            val response = categoryApi.getCategoryById(id)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Categoría no encontrada")
        }
    }

    /**
     * Crea una nueva categoría.
     */
    suspend fun createCategory(request: CategoryCreateRequest): ApiResult<CategoryDto> {
        return try {
            val response = categoryApi.createCategory(request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al crear categoría: ${e.message}")
        }
    }

    /**
     * Actualiza una categoría existente.
     */
    suspend fun updateCategory(
        id: Long,
        request: CategoryUpdateRequest
    ): ApiResult<CategoryDto> {
        return try {
            val response = categoryApi.updateCategory(id, request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al actualizar categoría: ${e.message}")
        }
    }

    /**
     * Elimina una categoría.
     */
    suspend fun deleteCategory(id: Long): ApiResult<Unit> {
        return try {
            categoryApi.deleteCategory(id)
            ApiResult.success(Unit)
        } catch (e: Exception) {
            ApiResult.Error("Error al eliminar categoría: ${e.message}")
        }
    }

    /**
     * Busca categorías por nombre.
     */
    suspend fun searchCategories(query: String): ApiResult<List<CategorySearchResponse>> {
        return try {
            val response = categoryApi.searchCategories(query)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al buscar categorías: ${e.message}")
        }
    }
}
