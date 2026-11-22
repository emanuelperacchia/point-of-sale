package com.pos.android.users.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.users.data.UserRepository
import com.pos.android.users.data.model.UserCreateRequest
import com.pos.android.users.data.model.UserDto
import com.pos.android.users.data.model.UserUpdateRequest
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class UserUiState(
    val users: List<UserDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingUser: UserDto? = null,
    val formEmail: String = "",
    val formPassword: String = "",
    val formFirstName: String = "",
    val formLastName: String = "",
    val formPhone: String = "",
    val formRoles: List<String> = emptyList()
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = userRepository.getUsers()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        users = result.data,
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

    fun onSearchQueryChanged(query: String) {
        if (query.isBlank()) {
            loadUsers()
        } else {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = userRepository.searchUsers(query)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            users = result.data.map { c ->
                                UserDto(
                                    id = c.id,
                                    email = c.email,
                                    firstName = "",
                                    lastName = "",
                                    fullName = c.fullName,
                                    phone = "",
                                    active = c.active,
                                    roles = emptyList(),
                                    createdAt = null,
                                    updatedAt = null
                                )
                            },
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
    }

    fun openCreateForm() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingUser = null,
            formEmail = "",
            formPassword = "",
            formFirstName = "",
            formLastName = "",
            formPhone = "",
            formRoles = emptyList()
        )
    }

    fun openEditForm(user: UserDto) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingUser = user,
            formEmail = user.email,
            formPassword = "",
            formFirstName = user.firstName,
            formLastName = user.lastName,
            formPhone = user.phone ?: "",
            formRoles = user.roles
        )
    }

    fun closeForm() {
        _uiState.value = _uiState.value.copy(
            showForm = false,
            editingUser = null,
            formEmail = "",
            formPassword = "",
            formFirstName = "",
            formLastName = "",
            formPhone = "",
            formRoles = emptyList()
        )
    }

    fun updateFormEmail(email: String) {
        _uiState.value = _uiState.value.copy(formEmail = email)
    }

    fun updateFormPassword(password: String) {
        _uiState.value = _uiState.value.copy(formPassword = password)
    }

    fun updateFormFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(formFirstName = firstName)
    }

    fun updateFormLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(formLastName = lastName)
    }

    fun updateFormPhone(phone: String) {
        _uiState.value = _uiState.value.copy(formPhone = phone)
    }

    fun updateFormRoles(roles: List<String>) {
        _uiState.value = _uiState.value.copy(formRoles = roles)
    }

    fun saveUser() {
        if (formEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El email es obligatorio")
            return
        }
        if (formPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "La contraseña es obligatoria")
            return
        }
        if (formFirstName.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El nombre es obligatorio")
            return
        }
        if (formLastName.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El apellido es obligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = UserCreateRequest(
                email = formEmail,
                password = formPassword,
                firstName = formFirstName,
                lastName = formLastName,
                phone = formPhone.takeIf { it.isNotEmpty() },
                roles = formRoles
            )

            when (val result = userRepository.createUser(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formEmail = "",
                        formPassword = "",
                        formFirstName = "",
                        formLastName = "",
                        formPhone = "",
                        formRoles = emptyList(),
                        isLoading = false,
                        message = "Usuario creado correctamente"
                    )
                    loadUsers()
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

    fun updateUser(user: UserDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = UserUpdateRequest(
                firstName = formFirstName.takeIf { it.isNotEmpty() },
                lastName = formLastName.takeIf { it.isNotEmpty() },
                phone = formPhone.takeIf { it.isNotEmpty() },
                active = true,
                roles = formRoles.takeIf { it.isNotEmpty() }
            )

            when (val result = userRepository.updateUser(user.id, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formEmail = "",
                        formPassword = "",
                        formFirstName = "",
                        formLastName = "",
                        formPhone = "",
                        formRoles = emptyList(),
                        isLoading = false,
                        message = "Usuario actualizado correctamente"
                    )
                    loadUsers()
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

    fun deleteUser(user: UserDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = userRepository.deleteUser(user.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Usuario eliminado correctamente"
                    )
                    loadUsers()
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
