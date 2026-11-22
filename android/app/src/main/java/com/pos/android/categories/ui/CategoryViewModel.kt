package com.pos.android.categories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.categories.data.CategoryRepository
import com.pos.android.categories.data.model.CategoryCreateRequest
import com.pos.android.categories.data.model.CategoryDto
import com.pos.android.categories.data.model.CategoryUpdateRequest
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingCategory: CategoryDto? = null,
    val formName: String = "",
    val formDescription: String = ""
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = categoryRepository.getCategories()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        categories = result.data,
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

    fun loadActiveCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = categoryRepository.getActiveCategories()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        categories = result.data,
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
            loadCategories()
        } else {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = categoryRepository.searchCategories(query)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            categories = result.data.map { c ->
                                CategoryDto(
                                    id = c.id,
                                    name = c.name,
                                    description = c.description,
                                    active = true,
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
            editingCategory = null,
            formName = "",
            formDescription = ""
        )
    }

    fun openEditForm(category: CategoryDto) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingCategory = category,
            formName = category.name,
            formDescription = category.description ?: ""
        )
    }

    fun closeForm() {
        _uiState.value = _uiState.value.copy(
            showForm = false,
            editingCategory = null,
            formName = "",
            formDescription = ""
        )
    }

    fun updateFormName(name: String) {
        _uiState.value = _uiState.value.copy(formName = name)
    }

    fun updateFormDescription(description: String) {
        _uiState.value = _uiState.value.copy(formDescription = description)
    }

    fun saveCategory() {
        if (formName.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El nombre es obligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = CategoryCreateRequest(
                name = formName,
                description = formDescription.takeIf { it.isNotEmpty() },
                active = true
            )

            when (val result = categoryRepository.createCategory(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formName = "",
                        formDescription = "",
                        isLoading = false,
                        message = "Categoría creada correctamente"
                    )
                    loadCategories()
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

    fun updateCategory(category: CategoryDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = CategoryUpdateRequest(
                name = formName,
                description = formDescription.takeIf { it.isNotEmpty() },
                active = true
            )

            when (val result = categoryRepository.updateCategory(category.id, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formName = "",
                        formDescription = "",
                        isLoading = false,
                        message = "Categoría actualizada correctamente"
                    )
                    loadCategories()
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

    fun deleteCategory(category: CategoryDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = categoryRepository.deleteCategory(category.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Categoría eliminada correctamente"
                    )
                    loadCategories()
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
