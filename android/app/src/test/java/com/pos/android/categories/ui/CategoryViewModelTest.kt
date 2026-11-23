package com.pos.android.categories.ui

import app.cash.turbine.test
import com.pos.android.categories.data.CategoryRepository
import com.pos.android.categories.data.model.CategoryDto
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

class CategoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: CategoryViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty categories`() = runTest(testDispatcher) {
        // Given: no categories
        coEvery { categoryRepository.getCategories() } returns MutableStateFlow(emptyList())

        // When: ViewModel is created
        viewModel = CategoryViewModel(categoryRepository)

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.categories.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `load categories returns list`() = runTest(testDispatcher) {
        // Given: categories exist
        val categories = listOf(
            CategoryDto(id = 1L, name = "Bebidas", description = "Bebidas varias", active = true, createdAt = "2026-01-01", updatedAt = null)
        )
        coEvery { categoryRepository.getCategories() } returns MutableStateFlow(categories)

        // When: ViewModel is created
        viewModel = CategoryViewModel(categoryRepository)

        // Then: categories are loaded
        val state = viewModel.uiState.value
        assertEquals(1, state.categories.size)
        assertEquals("Bebidas", state.categories[0].name)
    }

    @Test
    fun `create category calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { categoryRepository.getCategories() } returns MutableStateFlow(emptyList())
        coEvery { categoryRepository.createCategory(any()) } returns CategoryDto(
            id = 1L,
            name = "Bebidas",
            description = "Bebidas varias",
            active = true,
            createdAt = "2026-01-01",
            updatedAt = null
        )

        viewModel = CategoryViewModel(categoryRepository)

        viewModel.updateFormName("Bebidas")
        viewModel.updateFormDescription("Bebidas varias")

        // When: save
        viewModel.saveCategory()

        // Then: createCategory is called
        coVerify(exactly = 1) { categoryRepository.createCategory(any()) }
    }

    @Test
    fun `isEmpty when no categories`() = runTest(testDispatcher) {
        // Given
        coEvery { categoryRepository.getCategories() } returns MutableStateFlow(emptyList())

        viewModel = CategoryViewModel(categoryRepository)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.categories.isEmpty())
    }
}
