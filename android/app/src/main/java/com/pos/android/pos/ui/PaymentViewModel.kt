package com.pos.android.pos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.local.CartItemEntity
import com.pos.android.pos.data.model.ClientSearchResponse
import com.pos.android.pos.data.model.InvoiceResponse
import com.pos.android.pos.data.model.PaymentRequest
import com.pos.android.pos.data.model.PromotionResponse
import com.pos.android.pos.domain.CalculateChangeUseCase
import com.pos.android.pos.domain.ProcessSaleUseCase
import com.pos.android.pos.domain.SaleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val cartItems: List<CartItemEntity> = emptyList(),
    val total: Double = 0.0,
    val selectedPayment: String = "EFECTIVO",
    val receivedText: String = "",
    val received: Double = 0.0,
    val change: Double = 0.0,
    val isExact: Boolean = false,
    val isInsufficient: Boolean = false,
    val isProcessing: Boolean = false,
    val saleResult: SaleResult? = null,
    val error: String? = null,
    // Client
    val searchQuery: String = "",
    val clientResults: List<ClientSearchResponse> = emptyList(),
    val isSearchingClient: Boolean = false,
    val selectedClient: ClientSearchResponse? = null,
    // Coupon
    val couponCode: String = "",
    // Promotions
    val promotions: List<PromotionResponse> = emptyList(),
    // Invoice
    val invoice: InvoiceResponse? = null,
    val isLoadingInvoice: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val posRepository: PosRepository,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val calculateChangeUseCase: CalculateChangeUseCase
) : ViewModel() {

    private val total: Double = savedStateHandle["total"] ?: 0.0

    private val _uiState = MutableStateFlow(PaymentUiState(total = total))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCart()
        loadPromotions()
    }

    private fun loadCart() {
        viewModelScope.launch {
            val items = posRepository.getCartItems()
            _uiState.value = _uiState.value.copy(cartItems = items)
        }
    }

    private fun loadPromotions() {
        viewModelScope.launch {
            when (val result = posRepository.getActivePromotions()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(promotions = result.data)
                }
                is ApiResult.Error -> {
                    // Silencio — las promos son optativas
                }
            }
        }
    }

    fun onPaymentMethodChanged(method: String) {
        _uiState.value = _uiState.value.copy(
            selectedPayment = method,
            receivedText = if (method != "EFECTIVO") {
                String.format("%.2f", total)
            } else _uiState.value.receivedText
        )
        if (method != "EFECTIVO") {
            onReceivedAmountChanged(String.format("%.2f", total))
        }
    }

    fun onReceivedAmountChanged(text: String) {
        val received = text.toDoubleOrNull() ?: 0.0
        val changeResult = calculateChangeUseCase(total, received)
        _uiState.value = _uiState.value.copy(
            receivedText = text,
            received = received,
            change = changeResult.change,
            isExact = changeResult.isExact,
            isInsufficient = changeResult.isInsufficient
        )
    }

    // ── Client Search ──

    fun onClientSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                clientResults = emptyList(),
                isSearchingClient = false
            )
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.value = _uiState.value.copy(isSearchingClient = true)
            when (val result = posRepository.searchClients(query)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        clientResults = result.data,
                        isSearchingClient = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        clientResults = emptyList(),
                        isSearchingClient = false
                    )
                }
            }
        }
    }

    fun onClientSelected(client: ClientSearchResponse) {
        _uiState.value = _uiState.value.copy(
            selectedClient = client,
            searchQuery = "",
            clientResults = emptyList()
        )
    }

    fun onClearClient() {
        _uiState.value = _uiState.value.copy(
            selectedClient = null
        )
    }

    // ── Coupon ──

    fun onCouponCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(couponCode = code)
    }

    // ── Payment ──

    fun processPayment() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            _uiState.value = state.copy(error = "El carrito está vacío")
            return
        }
        if (state.selectedPayment == "EFECTIVO" && state.isInsufficient) {
            _uiState.value = state.copy(error = "El monto recibido es insuficiente")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)

            val payments = listOf(
                PaymentRequest(
                    paymentMethod = state.selectedPayment,
                    amount = state.total
                )
            )

            val result = processSaleUseCase(
                cartItems = state.cartItems,
                payments = payments,
                clientId = state.selectedClient?.id,
                couponCode = state.couponCode.ifBlank { null }
            )

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                saleResult = result,
                error = result.errorMessage
            )

            // Si la venta fue exitosa online, obtener comprobante
            if (result.isSuccess) {
                result.saleResponse?.id?.let { saleId ->
                    loadInvoice(saleId)
                }
            }
        }
    }

    private suspend fun loadInvoice(saleId: Long) {
        _uiState.value = _uiState.value.copy(isLoadingInvoice = true)
        when (val result = posRepository.getInvoiceBySaleId(saleId)) {
            is ApiResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    invoice = result.data,
                    isLoadingInvoice = false
                )
            }
            is ApiResult.Error -> {
                _uiState.value = _uiState.value.copy(isLoadingInvoice = false)
            }
        }
    }

    fun clearSaleResult() {
        _uiState.value = _uiState.value.copy(
            saleResult = null,
            invoice = null,
            selectedClient = null,
            couponCode = "",
            searchQuery = "",
            clientResults = emptyList(),
            receivedText = "",
            received = 0.0,
            change = 0.0,
            isExact = false,
            isInsufficient = false,
            selectedPayment = "EFECTIVO"
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
