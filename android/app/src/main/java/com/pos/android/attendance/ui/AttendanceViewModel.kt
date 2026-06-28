package com.pos.android.attendance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.attendance.data.AttendanceRepository
import com.pos.android.attendance.data.model.AttendanceSummaryResponse
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val summary: AttendanceSummaryResponse? = null,
    val lastCheck: String? = null, // "in" o "out"
    val error: String? = null
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = attendanceRepository.getSummary()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        summary = result.data
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

    fun checkIn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = attendanceRepository.checkIn()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastCheck = "in"
                    )
                    loadSummary()
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

    fun checkOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = attendanceRepository.checkOut()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastCheck = "out"
                    )
                    loadSummary()
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
