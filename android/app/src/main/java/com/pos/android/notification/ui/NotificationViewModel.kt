package com.pos.android.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.notification.data.NotificationEntity
import com.pos.android.notification.data.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        // Observar notificaciones desde Room
        viewModelScope.launch {
            repository.observeAll().collect { list ->
                _state.update { it.copy(notifications = list) }
            }
        }

        // Observar conteo de no leídas
        viewModelScope.launch {
            repository.observeUnreadCount().collect { count ->
                _state.update { it.copy(unreadCount = count) }
            }
        }

        // Arrancar polling
        viewModelScope.launch {
            repository.startPolling()
        }
    }

    fun markAsRead(notification: NotificationEntity) {
        if (!notification.leido) {
            viewModelScope.launch {
                repository.markAsRead(notification.id)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.fetchAndCache()
            _state.update { it.copy(isLoading = false) }
        }
    }
}
