package com.pos.android.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.ProductRepository
import com.pos.android.inventory.data.model.ProductSearchResponse
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.local.CartItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val cartItems: List<CartItemEntity> = emptyList(),
    val searchResults: List<ProductSearchResponse> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val cartTotal: Double = 0.0,
    val cartItemCount: Int = 0,
    val pendingSaleCount: Int = 0,
    val lastScannedCode: String? = null,
    val message: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        // Observar carrito
        viewModelScope.launch {
            posRepository.observeCart().collect { items ->
                val total = items.sumOf { it.subtotal }
                _uiState.value = _uiState.value.copy(
                    cartItems = items,
                    cartTotal = total,
                    cartItemCount = items.sumOf { it.cantidad }
                )
            }
        }

        // Observar ventas pendientes
        viewModelScope.launch {
            posRepository.observePendingCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingSaleCount = count)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.length >= 2) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSearching = true)
                when (val result = productRepository.searchProducts(query)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            searchResults = result.data,
                            isSearching = false
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            message = result.message
                        )
                    }
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    fun addToCart(product: ProductSearchResponse) {
        viewModelScope.launch {
            posRepository.addToCart(
                productId = product.id,
                nombre = product.name,
                sku = product.sku,
                precio = product.price,
                stock = product.stock
            )
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                searchResults = emptyList(),
                message = "${product.name} agregado al carrito"
            )
        }
    }

    fun updateQuantity(itemId: Long, cantidad: Int) {
        viewModelScope.launch {
            posRepository.updateCartItemQuantity(itemId, cantidad)
        }
    }

    fun removeItem(item: CartItemEntity) {
        viewModelScope.launch {
            posRepository.removeFromCart(item)
        }
    }

    fun onScannedCode(code: String) {
        _uiState.value = _uiState.value.copy(lastScannedCode = code)
        // Buscar producto por código (asumiendo que el código es el SKU)
        viewModelScope.launch {
            when (val result = productRepository.searchProducts(code)) {
                is ApiResult.Success -> {
                    result.data.firstOrNull()?.let { product ->
                        addToCart(product)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            message = "Producto no encontrado: $code"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = "Error al buscar producto: ${result.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun isCartEmpty(): Boolean = _uiState.value.cartItems.isEmpty()
}
