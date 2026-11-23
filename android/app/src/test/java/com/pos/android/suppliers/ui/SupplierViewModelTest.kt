package com.pos.android.suppliers.ui

import app.cash.turbine.test
import com.pos.android.suppliers.data.SupplierRepository
import com.pos.android.suppliers.data.model.SupplierDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.After
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.junit.jupiter.api.Assertions.*

class SupplierViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var supplierRepository: SupplierRepository
    private lateinit var viewModel: SupplierViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        supplierRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty suppliers`() = runTest(testDispatcher) {
        // Given: no suppliers
        coEvery { supplierRepository.getSuppliers() } returns MutableStateFlow(emptyList())

        // When: ViewModel is created
        viewModel = SupplierViewModel(supplierRepository)

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.suppliers.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `load suppliers returns list`() = runTest(testDispatcher) {
        // Given: suppliers exist
        val suppliers = listOf(
            SupplierDto(id = 1L, code = "SUP001", taxId = "12345678", businessName = "Proveedor A", tradeName = "Prov A", address = "Calle 1", phone = "12345678", email = "prov@test.com", active = true, createdAt = "2026-01-01", updatedAt = null)
        )
        coEvery { supplierRepository.getSuppliers() } returns MutableStateFlow(suppliers)

        // When: ViewModel is created
        viewModel = SupplierViewModel(supplierRepository)

        // Then: suppliers are loaded
        val state = viewModel.uiState.value
        assertEquals(1, state.suppliers.size)
        assertEquals("Proveedor A", state.suppliers[0].businessName)
    }

    @Test
    fun `create supplier calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { supplierRepository.getSuppliers() } returns MutableStateFlow(emptyList())
        coEvery { supplierRepository.createSupplier(any()) } returns SupplierDto(
            id = 1L,
            code = "SUP001",
            taxId = "12345678",
            businessName = "Proveedor A",
            tradeName = "Prov A",
            address = "Calle 1",
            phone = "12345678",
            email = "prov@test.com",
            active = true,
            createdAt = "2026-01-01",
            updatedAt = null
        )

        viewModel = SupplierViewModel(supplierRepository)

        viewModel.updateFormCode("SUP001")
        viewModel.updateFormTaxId("12345678")
        viewModel.updateFormBusinessName("Proveedor A")
        viewModel.updateFormTradeName("Prov A")
        viewModel.updateFormAddress("Calle 1")
        viewModel.updateFormPhone("12345678")
        viewModel.updateFormEmail("prov@test.com")

        // When: save
        viewModel.saveSupplier()

        // Then: createSupplier is called
        coVerify(exactly = 1) { supplierRepository.createSupplier(any()) }
    }

    @Test
    fun `isEmpty when no suppliers`() = runTest(testDispatcher) {
        // Given
        coEvery { supplierRepository.getSuppliers() } returns MutableStateFlow(emptyList())

        viewModel = SupplierViewModel(supplierRepository)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.suppliers.isEmpty())
    }
}
