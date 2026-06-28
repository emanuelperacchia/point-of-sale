package com.pos.android.shifts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.shifts.data.ShiftRepository
import com.pos.android.shifts.data.model.EmployeeSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShiftUiState(
    val isLoading: Boolean = false,
    val schedule: List<EmployeeSchedule> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = shiftRepository.getSchedule()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        schedule = result.data
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

    fun refresh() {
        loadSchedule()
    }
}
