package com.pos.android.cashshift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.cashshift.data.CashShiftRepository
import com.pos.android.cashshift.data.model.ShiftMovementResponse
import com.pos.android.cashshift.data.model.ShiftReportResponse
import com.pos.android.cashshift.data.model.ShiftResponse
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CashShiftUiState(
    val activeShift: ShiftResponse? = null,
    val report: ShiftReportResponse? = null,
    val movements: List<ShiftMovementResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
) {
    val isOpen: Boolean get() = activeShift?.estado == "ABIERTO"
}

@HiltViewModel
class CashShiftViewModel @Inject constructor(
    private val repository: CashShiftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashShiftUiState())
    val uiState: StateFlow<CashShiftUiState> = _uiState.asStateFlow()

    init {
        loadActiveShift()
    }

    fun loadActiveShift() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getActiveShift()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeShift = result.data,
                        isLoading = false
                    )
                    if (result.data != null) {
                        loadReport(result.data.id)
                    }
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

    fun openShift(montoApertura: Double) {
        if (montoApertura <= 0) {
            _uiState.value = _uiState.value.copy(error = "El monto debe ser mayor a cero")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            when (val result = repository.openShift(montoApertura)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeShift = result.data,
                        isProcessing = false,
                        successMessage = "Turno abierto exitosamente"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun closeShift(montoCierre: Double) {
        val shiftId = _uiState.value.activeShift?.id ?: return
        if (montoCierre < 0) {
            _uiState.value = _uiState.value.copy(error = "El monto de cierre no puede ser negativo")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            when (val result = repository.closeShift(shiftId, montoCierre)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeShift = result.data,
                        report = null,
                        movements = emptyList(),
                        isProcessing = false,
                        successMessage = "Turno cerrado exitosamente"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun createMovement(tipo: String, monto: Double, motivo: String) {
        val shiftId = _uiState.value.activeShift?.id ?: return
        if (monto <= 0) {
            _uiState.value = _uiState.value.copy(error = "El monto debe ser mayor a cero")
            return
        }
        if (motivo.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Debe ingresar un motivo")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            when (val result = repository.createMovement(shiftId, tipo, monto, motivo)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        successMessage = "Movimiento registrado"
                    )
                    loadReport(shiftId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private fun loadReport(shiftId: Long) {
        viewModelScope.launch {
            when (val result = repository.getShiftReport(shiftId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        report = result.data,
                        movements = result.data.movimientos ?: emptyList()
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

