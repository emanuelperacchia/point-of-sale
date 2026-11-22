package com.pos.android.pos.ui.saleshistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.SalesPage
import com.pos.android.pos.data.model.SaleResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesHistoryUiState(
    val sales: List<SaleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false,
    val currentPage: Int = 0,
    val selectedSale: SaleResponse? = null,
    val isLoadingDetail: Boolean = false
)

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState: StateFlow<SalesHistoryUiState> = _uiState.asStateFlow()

    fun loadInitialSales() {
        if (_uiState.value.sales.isEmpty() && !_uiState.value.isLoading) {
            loadSales()
        }
    }

    private fun loadSales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = posRepository.getSales(page = 0)) {
                is ApiResult.Success -> {
                    val page = result.data
                    _uiState.value = SalesHistoryUiState(
                        sales = page.content,
                        hasMore = page.hasMore,
                        currentPage = 0
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1
            when (val result = posRepository.getSales(page = nextPage)) {
                is ApiResult.Success -> {
                    val page = result.data
                    _uiState.value = _uiState.value.copy(
                        sales = _uiState.value.sales + page.content,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                        currentPage = nextPage
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadSaleDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true)
            when (val result = posRepository.getSaleDetail(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        selectedSale = result.data,
                        isLoadingDetail = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(selectedSale = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
