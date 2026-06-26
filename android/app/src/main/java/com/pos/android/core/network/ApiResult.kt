package com.pos.android.core.network

/**
 * Envoltorio genérico para resultados de API.
 * Tipado seguro: Success<T> contiene el DTO, Error contiene mensaje.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        fun <T> success(data: T): ApiResult<T> = Success(data)
        fun error(message: String, code: Int? = null): ApiResult<Nothing> = Error(message, code)
    }
}
