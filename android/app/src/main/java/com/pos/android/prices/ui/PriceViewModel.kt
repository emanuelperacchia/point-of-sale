package com.pos.android.prices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.prices.data.PriceRepository
import com.pos.android.prices.data.model.PriceCreateRequest
import com.pos.android.prices.data.model.PriceDto
import com.pos.android.prices.data.model.PriceResolutionResponse
import com.pos.android.prices.data.model.PriceUpdateRequest
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class PriceUiState(
    val prices: List<PriceDto> = emptyList(),
    val priceResolution: PriceResolutionResponse? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingPrice: PriceDto? = null,
    val formProductId: Long = 0,
    val formPrecio: Double = 0.0,
    val formVigenciaDesde: String = "",
    val formVigenciaHasta: String = ""
)

@HiltViewModel
class PriceViewModel @Inject constructor(
    private val priceRepository: PriceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceUiState())
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    init {
        loadBranchPrices(1L) // Por defecto sucursal 1
    }

    fun loadBranchPrices(branchId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = priceRepository.getPricesByBranch(branchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        prices = result.data,
                        isLoading = false
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

    fun resolveProductPrice(productId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = priceRepository.resolvePrice(productId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        priceResolution = result.data,
                        isLoading = false
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

    fun openCreateForm() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingPrice = null,
            formProductId = 0,
            formPrecio = 0.0,
            formVigenciaDesde = "",
            formVigenciaHasta = ""
        )
    }

    fun openEditForm(price: PriceDto) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingPrice = price,
            formProductId = price.productId,
            formPrecio = price.precio,
            formVigenciaDesde = price.vigenciaDesde ?: "",
            formVigenciaHasta = price.vigenciaHasta ?: ""
        )
    }

    fun closeForm() {
        _uiState.value = _uiState.value.copy(
            showForm = false,
            editingPrice = null,
            formProductId = 0,
            formPrecio = 0.0,
            formVigenciaDesde = "",
            formVigenciaHasta = ""
        )
    }

    fun updateFormProductId(productId: Long) {
        _uiState.value = _uiState.value.copy(formProductId = productId)
    }

    fun updateFormPrecio(precio: Double) {
        _uiState.value = _uiState.value.copy(formPrecio = precio)
    }

    fun updateFormVigenciaDesde(vigenciaDesde: String) {
        _uiState.value = _uiState.value.copy(formVigenciaDesde = vigenciaDesde)
    }

    fun updateFormVigenciaHasta(vigenciaHasta: String) {
        _uiState.value = _uiState.value.copy(formVigenciaHasta = vigenciaHasta)
    }

    fun savePrice(branchId: Long) {
        if (formProductId <= 0) {
            _uiState.value = _uiState.value.copy(message = "Seleccione un producto")
            return
        }
        if (formPrecio <= 0) {
            _uiState.value = _uiState.value.copy(message = "El precio debe ser mayor a 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = PriceCreateRequest(
                productId = formProductId,
                precio = formPrecio,
                vigenciaDesde = formVigenciaDesde.takeIf { it.isNotEmpty() },
                vigenciaHasta = formVigenciaHasta.takeIf { it.isNotEmpty() }
            )

            when (val result = priceRepository.createPrice(branchId, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formProductId = 0,
                        formPrecio = 0.0,
                        formVigenciaDesde = "",
                        formVigenciaHasta = "",
                        isLoading = false,
                        message = "Precio creado correctamente"
                    )
                    loadBranchPrices(branchId)
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

    fun updatePrice(branchId: Long, priceId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = PriceUpdateRequest(
                productId = formProductId,
                precio = formPrecio,
                vigenciaDesde = formVigenciaDesde.takeIf { it.isNotEmpty() },
                vigenciaHasta = formVigenciaHasta.takeIf { it.isNotEmpty() }
            )

            when (val result = priceRepository.updatePrice(branchId, priceId, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formProductId = 0,
                        formPrecio = 0.0,
                        formVigenciaDesde = "",
                        formVigenciaHasta = "",
                        isLoading = false,
                        message = "Precio actualizado correctamente"
                    )
                    loadBranchPrices(branchId)
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

    fun deletePrice(branchId: Long, priceId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = priceRepository.deletePrice(branchId, priceId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Precio eliminado correctamente"
                    )
                    loadBranchPrices(branchId)
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

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
