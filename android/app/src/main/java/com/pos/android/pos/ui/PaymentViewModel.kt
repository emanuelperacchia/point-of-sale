package com.pos.android.pos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.local.CartItemEntity
import com.pos.android.pos.data.model.PaymentRequest
import com.pos.android.pos.domain.CalculateChangeUseCase
import com.pos.android.pos.domain.ProcessSaleUseCase
import com.pos.android.pos.domain.SaleResult
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
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

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            val items = posRepository.getCartItems()
            _uiState.value = _uiState.value.copy(cartItems = items)
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
                payments = payments
            )

            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                saleResult = result,
                error = result.errorMessage
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
