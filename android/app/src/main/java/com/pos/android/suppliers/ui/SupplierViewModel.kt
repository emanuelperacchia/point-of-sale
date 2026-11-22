package com.pos.android.suppliers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.android.suppliers.data.SupplierRepository
import com.pos.android.suppliers.data.model.SupplierCreateRequest
import com.pos.android.suppliers.data.model.SupplierDto
import com.pos.android.suppliers.data.model.SupplierUpdateRequest
import com.pos.android.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SupplierUiState(
    val suppliers: List<SupplierDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingSupplier: SupplierDto? = null,
    val formCode: String = "",
    val formTaxId: String = "",
    val formBusinessName: String = "",
    val formTradeName: String = "",
    val formAddress: String = "",
    val formPhone: String = "",
    val formEmail: String = ""
)

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = supplierRepository.getSuppliers()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        suppliers = result.data,
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
            loadSuppliers()
        } else {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = supplierRepository.searchSuppliers(query)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            suppliers = result.data.map { c ->
                                SupplierDto(
                                    id = c.id,
                                    code = c.code,
                                    taxId = c.taxId ?: "",
                                    businessName = c.businessName,
                                    tradeName = c.tradeName,
                                    address = c.address,
                                    phone = c.phone,
                                    email = c.email,
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
            editingSupplier = null,
            formCode = "",
            formTaxId = "",
            formBusinessName = "",
            formTradeName = "",
            formAddress = "",
            formPhone = "",
            formEmail = ""
        )
    }

    fun openEditForm(supplier: SupplierDto) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            editingSupplier = supplier,
            formCode = supplier.code,
            formTaxId = supplier.taxId,
            formBusinessName = supplier.businessName,
            formTradeName = supplier.tradeName ?: "",
            formAddress = supplier.address ?: "",
            formPhone = supplier.phone ?: "",
            formEmail = supplier.email ?: ""
        )
    }

    fun closeForm() {
        _uiState.value = _uiState.value.copy(
            showForm = false,
            editingSupplier = null,
            formCode = "",
            formTaxId = "",
            formBusinessName = "",
            formTradeName = "",
            formAddress = "",
            formPhone = "",
            formEmail = ""
        )
    }

    fun updateFormCode(code: String) {
        _uiState.value = _uiState.value.copy(formCode = code)
    }

    fun updateFormTaxId(taxId: String) {
        _uiState.value = _uiState.value.copy(formTaxId = taxId)
    }

    fun updateFormBusinessName(businessName: String) {
        _uiState.value = _uiState.value.copy(formBusinessName = businessName)
    }

    fun updateFormTradeName(tradeName: String) {
        _uiState.value = _uiState.value.copy(formTradeName = tradeName)
    }

    fun updateFormAddress(address: String) {
        _uiState.value = _uiState.value.copy(formAddress = address)
    }

    fun updateFormPhone(phone: String) {
        _uiState.value = _uiState.value.copy(formPhone = phone)
    }

    fun updateFormEmail(email: String) {
        _uiState.value = _uiState.value.copy(formEmail = email)
    }

    fun saveSupplier() {
        if (formCode.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El código es obligatorio")
            return
        }
        if (formTaxId.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "El RUT/CUIT es obligatorio")
            return
        }
        if (formBusinessName.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "La razón social es obligatoria")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = SupplierCreateRequest(
                code = formCode,
                taxId = formTaxId,
                businessName = formBusinessName,
                tradeName = formTradeName.takeIf { it.isNotEmpty() },
                address = formAddress.takeIf { it.isNotEmpty() },
                phone = formPhone.takeIf { it.isNotEmpty() },
                email = formEmail.takeIf { it.isNotEmpty() },
                active = true
            )

            when (val result = supplierRepository.createSupplier(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formCode = "",
                        formTaxId = "",
                        formBusinessName = "",
                        formTradeName = "",
                        formAddress = "",
                        formPhone = "",
                        formEmail = "",
                        isLoading = false,
                        message = "Proveedor creado correctamente"
                    )
                    loadSuppliers()
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

    fun updateSupplier(supplier: SupplierDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val request = SupplierUpdateRequest(
                code = formCode,
                taxId = formTaxId,
                businessName = formBusinessName,
                tradeName = formTradeName.takeIf { it.isNotEmpty() },
                address = formAddress.takeIf { it.isNotEmpty() },
                phone = formPhone.takeIf { it.isNotEmpty() },
                email = formEmail.takeIf { it.isNotEmpty() },
                active = true
            )

            when (val result = supplierRepository.updateSupplier(supplier.id, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showForm = false,
                        formCode = "",
                        formTaxId = "",
                        formBusinessName = "",
                        formTradeName = "",
                        formAddress = "",
                        formPhone = "",
                        formEmail = "",
                        isLoading = false,
                        message = "Proveedor actualizado correctamente"
                    )
                    loadSuppliers()
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

    fun deleteSupplier(supplier: SupplierDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = supplierRepository.deleteSupplier(supplier.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Proveedor eliminado correctamente"
                    )
                    loadSuppliers()
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
