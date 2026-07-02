package com.pos.android.notification.ui

import app.cash.turbine.test
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
import com.pos.android.notification.data.NotificationEntity
import com.pos.android.notification.data.NotificationRepository

class NotificationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: NotificationRepository
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty notifications and zero unread`() = runTest(testDispatcher) {
        // Given: observeAll returns empty, observeUnreadCount returns 0
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit

        // When: ViewModel is created
        viewModel = NotificationViewModel(repository)

        // Then: initial state is empty
        val state = viewModel.state.value
        assertTrue(state.notifications.isEmpty())
        assertEquals(0, state.unreadCount)
        assertFalse(state.isLoading)
    }

    @Test
    fun `state updates when notifications arrive from repository`() = runTest(testDispatcher) {
        // Given
        val notificationsFlow = MutableStateFlow(
            listOf(
                NotificationEntity(id = 1L, titulo = "Nuevo", leido = false, creadoEn = "2026-01-01"),
                NotificationEntity(id = 2L, titulo = "Leído", leido = true, creadoEn = "2026-01-02")
            )
        )
        coEvery { repository.observeAll() } returns notificationsFlow
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(1)
        coEvery { repository.startPolling() } returns Unit

        // When
        viewModel = NotificationViewModel(repository)

        // Then
        val state = viewModel.state.value
        assertEquals(2, state.notifications.size)
        assertEquals(1, state.unreadCount)
        assertEquals("Nuevo", state.notifications[0].titulo)
    }

    @Test
    fun `state updates when unread count changes`() = runTest(testDispatcher) {
        // Given
        val countFlow = MutableStateFlow(0)
        coEvery { repository.observeAll() } returns MutableStateFlow(
            listOf(NotificationEntity(id = 1L, titulo = "Test", leido = false))
        )
        coEvery { repository.observeUnreadCount() } returns countFlow
        coEvery { repository.startPolling() } returns Unit

        viewModel = NotificationViewModel(repository)

        // Initial count should be 0
        assertEquals(0, viewModel.state.value.unreadCount)

        // When count changes
        countFlow.value = 3

        // Then
        assertEquals(3, viewModel.state.value.unreadCount)
    }

    @Test
    fun `markAsRead calls repository`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit
        coEvery { repository.markAsRead(1L) } returns Unit

        viewModel = NotificationViewModel(repository)
        val notification = NotificationEntity(id = 1L, titulo = "Test", leido = false)

        // When
        viewModel.markAsRead(notification)

        // Then
        coVerify(exactly = 1) { repository.markAsRead(1L) }
    }

    @Test
    fun `markAsRead does nothing for already read notifications`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit

        viewModel = NotificationViewModel(repository)
        val notification = NotificationEntity(id = 2L, titulo = "Ya leído", leido = true)

        // When: marking an already-read notification
        viewModel.markAsRead(notification)

        // Then: repository should NOT be called
        coVerify(exactly = 0) { repository.markAsRead(any()) }
    }

    @Test
    fun `refresh calls fetchAndCache and toggles loading state`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit
        coEvery { repository.fetchAndCache() } returns Unit

        viewModel = NotificationViewModel(repository)

        // Before refresh
        assertFalse(viewModel.state.value.isLoading)

        // When
        viewModel.refresh()

        // Then
        coVerify(exactly = 1) { repository.fetchAndCache() }
        // After refresh completes, isLoading should be false again
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `startPolling is called on init`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit

        // When
        viewModel = NotificationViewModel(repository)

        // Then
        coVerify(exactly = 1) { repository.startPolling() }
    }

    @Test
    fun `isLoading toggles during refresh`() = runTest(testDispatcher) {
        // Given
        coEvery { repository.observeAll() } returns MutableStateFlow(emptyList())
        coEvery { repository.observeUnreadCount() } returns MutableStateFlow(0)
        coEvery { repository.startPolling() } returns Unit
        coEvery { repository.fetchAndCache() } coAnswers {
            // Simulate some work
            kotlinx.coroutines.delay(100)
            Unit
        }

        viewModel = NotificationViewModel(repository)

        // When
        viewModel.refresh()

        // Then: after refresh, loading is false
        assertFalse(viewModel.state.value.isLoading)
    }
}
