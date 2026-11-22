package com.pos.android.pos.ui

import app.cash.turbine.test
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.model.ProductSearchResponse
import com.pos.android.pos.data.PosRepository
import com.pos.android.pos.data.local.CartItemEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var posRepository: PosRepository
    private lateinit var viewModel: PosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        posRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty cart and zero total`() = runTest(testDispatcher) {
        // Given: cart returns empty list
        coEvery { posRepository.observeCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        // When: ViewModel is created
        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.cartItems.isEmpty())
        assertEquals(0.0, state.cartTotal)
        assertEquals(0, state.cartItemCount)
        assertFalse(viewModel.isCartEmpty())
    }

    @Test
    fun `cart items are observed and total is calculated`() = runTest(testDispatcher) {
        // Given: cart returns items
        val cartItems = listOf(
            CartItemEntity(productId = 1L, nombre = "Producto A", sku = "A001", precio = 10.0, stockDisponible = 10, cantidad = 2),
            CartItemEntity(productId = 2L, nombre = "Producto B", sku = "B002", precio = 20.0, stockDisponible = 5, cantidad = 1)
        )
        coEvery { posRepository.observeCart() } returns MutableStateFlow(cartItems)
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        // When: ViewModel is created
        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        // Then: cart has items and total is calculated
        val state = viewModel.uiState.value
        assertEquals(2, state.cartItems.size)
        assertEquals(30.0, state.cartTotal) // 10*2 + 20*1 = 30
        assertEquals(3, state.cartItemCount)
    }

    @Test
    fun `addToCart calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.observeCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)
        coEvery { posRepository.addToCart(any(), any(), any(), any(), any(), any()) } returns Unit

        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        val product = ProductSearchResponse(id = 1L, name = "Producto Test", sku = "TEST001", price = 15.0, stock = 10, categoryName = null)

        // When
        viewModel.addToCart(product)

        // Then
        coVerify(exactly = 1) { posRepository.addToCart(1L, "Producto Test", "TEST001", 15.0, 10, 1) }
    }

    @Test
    fun `removeItem calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.observeCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)
        coEvery { posRepository.removeFromCart(any()) } returns Unit

        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        val item = CartItemEntity(productId = 1L, nombre = "Producto", sku = "SKU001", precio = 10.0, stockDisponible = 10, cantidad = 1)

        // When
        viewModel.removeItem(item)

        // Then
        coVerify(exactly = 1) { posRepository.removeFromCart(item) }
    }

    @Test
    fun `isCartEmpty returns true when cart is empty`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.observeCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        // Then
        assertTrue(viewModel.isCartEmpty())
    }

    @Test
    fun `isCartEmpty returns false when cart has items`() = runTest(testDispatcher) {
        // Given
        val cartItems = listOf(CartItemEntity(productId = 1L, nombre = "Producto", sku = "SKU001", precio = 10.0, stockDisponible = 10, cantidad = 1))
        coEvery { posRepository.observeCart() } returns MutableStateFlow(cartItems)
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        viewModel = PosViewModel(posRepository, mockk(relaxed = true))

        // Then
        assertFalse(viewModel.isCartEmpty())
    }
}
