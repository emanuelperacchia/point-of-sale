package com.pos.android.users.data

import com.pos.android.core.network.ApiResult
import com.pos.android.users.data.model.UserCreateRequest
import com.pos.android.users.data.model.UserDto
import com.pos.android.users.data.model.UserPageResponse
import com.pos.android.users.data.model.UserSearchResponse
import com.pos.android.users.data.model.UserUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {

    /**
     * Obtiene usuarios paginados.
     */
    suspend fun getUsers(
        page: Int = 0,
        size: Int = 20,
        search: String? = null
    ): ApiResult<List<UserDto>> {
        return try {
            val response = userApi.getUsers(
                page = page,
                size = size,
                search = search
            )
            ApiResult.success(response.content)
        } catch (e: Exception) {
            ApiResult.Error("Error al cargar usuarios: ${e.message}")
        }
    }

    /**
     * Obtiene un usuario por ID.
     */
    suspend fun getUserById(id: Long): ApiResult<UserDto> {
        return try {
            val response = userApi.getUserById(id)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Usuario no encontrado")
        }
    }

    /**
     * Crea un nuevo usuario.
     */
    suspend fun createUser(request: UserCreateRequest): ApiResult<UserDto> {
        return try {
            val response = userApi.createUser(request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al crear usuario: ${e.message}")
        }
    }

    /**
     * Actualiza un usuario existente.
     */
    suspend fun updateUser(
        id: Long,
        request: UserUpdateRequest
    ): ApiResult<UserDto> {
        return try {
            val response = userApi.updateUser(id, request)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al actualizar usuario: ${e.message}")
        }
    }

    /**
     * Elimina un usuario.
     */
    suspend fun deleteUser(id: Long): ApiResult<Unit> {
        return try {
            userApi.deleteUser(id)
            ApiResult.success(Unit)
        } catch (e: Exception) {
            ApiResult.Error("Error al eliminar usuario: ${e.message}")
        }
    }

    /**
     * Busca usuarios por nombre o email.
     */
    suspend fun searchUsers(query: String): ApiResult<List<UserSearchResponse>> {
        return try {
            val response = userApi.searchUsers(query)
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error("Error al buscar usuarios: ${e.message}")
        }
    }
}
