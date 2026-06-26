package com.pos.android.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.ProductRepository
import com.pos.android.inventory.data.model.ProductDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductSearchUiState(
    val query: String = "",
    val products: List<ProductDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class ProductSearchViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductSearchUiState())
    val uiState: StateFlow<ProductSearchUiState> = _uiState.asStateFlow()

    private var currentQuery: String? = null

    init {
        loadProducts()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        currentQuery = query

        if (query.length >= 2) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSearching = true, error = null)
                when (val result = productRepository.searchProducts(query)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            products = emptyList() // No mostrar search results en grid view
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadProducts(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = productRepository.getProducts(page = page, search = currentQuery)) {
                is ApiResult.Success -> {
                    val products = if (page == 0) {
                        result.data
                    } else {
                        _uiState.value.products + result.data
                    }
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false,
                        currentPage = page,
                        hasMore = result.data.isNotEmpty()
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadProducts(_uiState.value.currentPage + 1)
        }
    }

    fun refresh() {
        loadProducts()
    }
}
