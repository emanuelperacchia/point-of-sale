package com.pos.android.users.ui

import app.cash.turbine.test
import com.pos.android.users.data.UserRepository
import com.pos.android.users.data.model.UserDto
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

class UserViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: UserViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty users`() = runTest(testDispatcher) {
        // Given: no users
        coEvery { userRepository.getUsers() } returns MutableStateFlow(emptyList())

        // When: ViewModel is created
        viewModel = UserViewModel(userRepository)

        // Then: initial state
        val state = viewModel.uiState.value
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `load users returns list`() = runTest(testDispatcher) {
        // Given: users exist
        val users = listOf(
            UserDto(id = 1L, email = "admin@pos.com", firstName = "Admin", lastName = "System", fullName = "Admin System", phone = null, active = true, roles = listOf("ADMIN"), createdAt = "2026-01-01", updatedAt = null)
        )
        coEvery { userRepository.getUsers() } returns MutableStateFlow(users)

        // When: ViewModel is created
        viewModel = UserViewModel(userRepository)

        // Then: users are loaded
        val state = viewModel.uiState.value
        assertEquals(1, state.users.size)
        assertEquals("Admin System", state.users[0].fullName)
    }

    @Test
    fun `create user calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { userRepository.getUsers() } returns MutableStateFlow(emptyList())
        coEvery { userRepository.createUser(any()) } returns UserDto(
            id = 1L,
            email = "test@test.com",
            firstName = "Test",
            lastName = "User",
            fullName = "Test User",
            phone = null,
            active = true,
            roles = listOf("CAJERO"),
            createdAt = "2026-01-01",
            updatedAt = null
        )

        viewModel = UserViewModel(userRepository)

        viewModel.updateFormEmail("test@test.com")
        viewModel.updateFormPassword("password123")
        viewModel.updateFormFirstName("Test")
        viewModel.updateFormLastName("User")
        viewModel.updateFormRoles(listOf("CAJERO"))

        // When: save
        viewModel.saveUser()

        // Then: createUser is called
        coVerify(exactly = 1) { userRepository.createUser(any()) }
    }

    @Test
    fun `isEmpty when no users`() = runTest(testDispatcher) {
        // Given
        coEvery { userRepository.getUsers() } returns MutableStateFlow(emptyList())

        viewModel = UserViewModel(userRepository)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.users.isEmpty())
    }
}
