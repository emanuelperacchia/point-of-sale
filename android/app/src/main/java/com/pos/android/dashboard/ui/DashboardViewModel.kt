package com.pos.android.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.core.network.ApiResult
import com.pos.android.core.security.TokenStorage
import com.pos.android.dashboard.data.DashboardRepository
import com.pos.android.dashboard.data.model.ExecutiveDashboardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val data: ExecutiveDashboardDto? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val cachedAt: String? = null,
    val selectedPeriod: String = "HOY",
    val branches: List<com.pos.android.auth.data.model.BranchInfo>? = null,
    val selectedBranchId: Long? = null,
    val isAdmin: Boolean = false,
    val error: String? = null
) {
    val periods: List<PeriodOption> = listOf(
        PeriodOption("HOY", "Hoy"),
        PeriodOption("SEMANA", "Semana"),
        PeriodOption("MES", "Mes")
    )

    data class PeriodOption(val value: String, val label: String)
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        // Cargar datos del usuario
        val branchId = tokenStorage.activeBranchId
        val branchName = tokenStorage.activeBranchName
        val isAdmin = tokenStorage.isAdmin
        _state.update {
            it.copy(
                selectedBranchId = if (branchId > 0) branchId else null,
                isAdmin = isAdmin
            )
        }
        load()
    }

    fun selectPeriod(period: String) {
        _state.update { it.copy(selectedPeriod = period) }
        load()
    }

    fun selectBranch(branchId: Long?) {
        _state.update { it.copy(selectedBranchId = branchId) }
        load()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        load(forceRemote = true)
    }

    private fun load(forceRemote: Boolean = false) {
        viewModelScope.launch {
            val period = _state.value.selectedPeriod
            val branchId = _state.value.selectedBranchId

            // 1. Mostrar caché primero (si no es refresh forzado)
            if (!forceRemote) {
                repository.getCached(period, branchId)?.let { cached ->
                    _state.update {
                        it.copy(
                            data = cached.data,
                            isFromCache = true,
                            cachedAt = cached.relativeTime,
                            error = null
                        )
                    }
                }
            }

            // 2. Cargar de API
            _state.update { it.copy(isLoading = !forceRemote) }

            when (val result = repository.fetch(period, branchId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            data = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            isFromCache = false,
                            cachedAt = null,
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = if (it.data != null) null else result.message
                        )
                    }
                }
            }
        }
    }
}
