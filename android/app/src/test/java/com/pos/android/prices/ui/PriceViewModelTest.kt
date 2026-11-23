package com.pos.android.prices.ui

import app.cash.turbine.test
import com.pos.android.prices.data.PriceRepository
import com.pos.android.prices.data.model.PriceDto
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

class PriceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var priceRepository: PriceRepository
    private lateinit var viewModel: PriceViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        priceRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty prices`() = runTest(testDispatcher) {
        // Given: no prices
        coEvery { priceRepository.getPricesByBranch(anyLong()) } returns MutableStateFlow(emptyList())

        // When: ViewModel is created
        viewModel = PriceViewModel(priceRepository)

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.prices.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `load prices returns list`() = runTest(testDispatcher) {
        // Given: prices exist
        val prices = listOf(
            PriceDto(id = 1L, productId = 1L, productName = "Coca Cola", productSku = "COC001", precio = 15.0, precioGlobal = 15.0, precioLocal = null, vigenciaDesde = null, vigenciaHasta = null, activo = true, createdAt = "2026-01-01")
        )
        coEvery { priceRepository.getPricesByBranch(anyLong()) } returns MutableStateFlow(prices)

        // When: ViewModel is created
        viewModel = PriceViewModel(priceRepository)

        // Then: prices are loaded
        val state = viewModel.uiState.value
        assertEquals(1, state.prices.size)
        assertEquals("Coca Cola", state.prices[0].productName)
    }

    @Test
    fun `create price calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { priceRepository.getPricesByBranch(anyLong()) } returns MutableStateFlow(emptyList())
        coEvery { priceRepository.createPrice(anyLong(), any()) } returns PriceDto(
            id = 1L,
            productId = 1L,
            productName = "Coca Cola",
            productSku = "COC001",
            precio = 15.0,
            precioGlobal = 15.0,
            precioLocal = null,
            vigenciaDesde = null,
            vigenciaHasta = null,
            activo = true,
            createdAt = "2026-01-01"
        )

        viewModel = PriceViewModel(priceRepository)

        viewModel.updateFormProductId(1L)
        viewModel.updateFormPrecio(15.0)

        // When: save
        viewModel.savePrice(1L)

        // Then: createPrice is called
        coVerify(exactly = 1) { priceRepository.createPrice(anyLong(), any()) }
    }

    @Test
    fun `isEmpty when no prices`() = runTest(testDispatcher) {
        // Given
        coEvery { priceRepository.getPricesByBranch(anyLong()) } returns MutableStateFlow(emptyList())

        viewModel = PriceViewModel(priceRepository)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.prices.isEmpty())
    }
}
