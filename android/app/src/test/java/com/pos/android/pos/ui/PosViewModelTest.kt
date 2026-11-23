package com.pos.android.pos.ui

import app.cash.turbine.test
import com.pos.android.core.network.ApiResult
import com.pos.android.inventory.data.ProductRepository
import com.pos.android.pos.data.PosRepository
import com.pos.android.inventory.data.model.ProductSearchResponse
import com.pos.android.pos.data.local.CartItemEntity
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

class PosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var posRepository: PosRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var viewModel: PosViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        posRepository = mockk(relaxed = true)
        productRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty cart and zero total`() = runTest(testDispatcher) {
        // Given: cart is empty
        coEvery { posRepository.getCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.getPendingCount() } returns MutableStateFlow(0)

        // When: ViewModel is created
        viewModel = PosViewModel(posRepository, productRepository)

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.cartItems.isEmpty())
        assertEquals(0.0, state.cartTotal)
        assertEquals(0, state.cartItemCount)
        assertTrue(viewModel.isCartEmpty())
    }

    @Test
    fun `cart items are displayed correctly`() = runTest(testDispatcher) {
        // Given: cart has items
        val cartItems = listOf(
            CartItemEntity(id = 1L, productId = 101L, name = "Coca Cola", sku = "COC001", price = 15.0, stock = 100, quantity = 2),
            CartItemEntity(productId = 102L, name = "Agua", sku = "AGU002", price = 10.0, stockDisponible = 50, cantidad = 1)
        )
        coEvery { posRepository.getCart() } returns MutableStateFlow(cartItems)
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        // When: ViewModel is created
        viewModel = PosViewModel(posRepository, productRepository)

        // Then: cart has items with correct total
        val state = viewModel.uiState.value
        assertEquals(2, state.cartItems.size)
        assertEquals(40.0, state.cartTotal) // 15*2 + 10*1 = 40
        assertEquals(3, state.cartItemCount)
    }

    @Test
    fun `addItemToCart calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.getCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)
        coEvery { posRepository.addToCart(any(), any(), any(), any(), any(), any()) } returns Unit

        viewModel = PosViewModel(posRepository, productRepository)

        val product = ProductSearchResult(
            id = 1L,
            name = "Coca Cola",
            sku = "COC001",
            price = 15.0,
            stock = 100,
            categoryName = "Bebidas"
        )

        // When
        viewModel.addToCart(product)

        // Then
        coVerify(exactly = 1) { posRepository.addToCart(1L, "Coca Cola", "COC001", 15.0, 100, 1) }
    }

    @Test
    fun `removeItem calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.getCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)
        coEvery { posRepository.removeFromCart(any()) } returns Unit

        viewModel = PosViewModel(posRepository, productRepository)

        val item = CartItemEntity(productId = 1L, productName = "Coca Cola", sku = "COC001", price = 15.0, stock = 100, quantity = 1)

        // When
        viewModel.removeItem(item)

        // Then
        coVerify(exactly = 1) { posRepository.removeFromCart(item) }
    }

    @Test
    fun `isCartEmpty returns true when cart is empty`() = runTest(testDispatcher) {
        // Given
        coEvery { posRepository.getCart() } returns MutableStateFlow(emptyList())
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        viewModel = PosViewModel(posRepository, productRepository)

        // Then
        assertTrue(viewModel.isCartEmpty())
    }

    @Test
    fun `isCartEmpty returns false when cart has items`() = runTest(testDispatcher) {
        // Given
        val cartItems = listOf(
            CartItemEntity(productId = 1L, productName = "Coca Cola", sku = "COC001", price = 15.0, stockDisponible = 100, cantidad = 1)
        )
        coEvery { posRepository.getCart() } returns MutableStateFlow(cartItems)
        coEvery { posRepository.observePendingCount() } returns MutableStateFlow(0)

        viewModel = PosViewModel(posRepository, productRepository)

        // Then
        assertFalse(viewModel.isCartEmpty())
    }
}
