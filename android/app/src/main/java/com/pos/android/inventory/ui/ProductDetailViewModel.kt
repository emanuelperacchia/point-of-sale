package com.pos.android.inventory.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.domain.GetProductDetailUseCase
import com.pos.android.inventory.domain.ProductDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val detail: ProductDetail? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductDetailUseCase: GetProductDetailUseCase
) : ViewModel() {

    private val productId: Long = savedStateHandle["productId"] ?: -1L

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        if (productId > 0) {
            loadProduct()
        } else {
            _uiState.value = ProductDetailUiState(error = "ID de producto inválido")
        }
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState(isLoading = true)

            when (val result = getProductDetailUseCase(productId)) {
                is ApiResult.Success -> {
                    _uiState.value = ProductDetailUiState(detail = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = ProductDetailUiState(error = result.message)
                }
            }
        }
    }
}
