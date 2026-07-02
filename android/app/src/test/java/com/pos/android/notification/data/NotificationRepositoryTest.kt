package com.pos.android.notification.data

import com.pos.android.core.network.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    private lateinit var api: NotificationApi
    private lateinit var dao: NotificationDao
    private lateinit var repository: NotificationRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk()
        repository = NotificationRepository(api, dao)
    }

    // ── Flow delegation tests ──

    @Test
    fun `observeAll delegates to DAO`() = testScope.runTest {
        val expected = listOf(
            NotificationEntity(id = 1L, titulo = "Notif 1"),
            NotificationEntity(id = 2L, titulo = "Notif 2")
        )
        val flow = MutableStateFlow(expected)
        coEvery { dao.observeAll() } returns flow

        val result = repository.observeAll().first()

        assertEquals(expected, result)
    }

    @Test
    fun `observeUnread delegates to DAO`() = testScope.runTest {
        val expected = listOf(NotificationEntity(id = 1L, leido = false))
        val flow = MutableStateFlow(expected)
        coEvery { dao.observeUnread() } returns flow

        val result = repository.observeUnread().first()

        assertEquals(expected, result)
    }

    @Test
    fun `observeUnreadCount delegates to DAO`() = testScope.runTest {
        val flow = MutableStateFlow(5)
        coEvery { dao.observeUnreadCount() } returns flow

        val result = repository.observeUnreadCount().first()

        assertEquals(5, result)
    }

    // ── fetchUnreadCount ──

    @Test
    fun `fetchUnreadCount returns count on success`() = testScope.runTest {
        coEvery { api.getUnreadCount() } returns UnreadCountResponse(count = 3L)

        val result = repository.fetchUnreadCount()

        assertTrue(result is ApiResult.Success)
        assertEquals(3, (result as ApiResult.Success).data)
    }

    @Test
    fun `fetchUnreadCount returns error on exception`() = testScope.runTest {
        coEvery { api.getUnreadCount() } throws RuntimeException("Network error")

        val result = repository.fetchUnreadCount()

        assertTrue(result is ApiResult.Error)
        assertNotNull((result as ApiResult.Error).message)
    }

    // ── fetchAndCache ──

    @Test
    fun `fetchAndCache calls API and upserts to DAO`() = testScope.runTest {
        val response = listOf(
            NotificationResponse(id = 1L, userId = 10L, titulo = "A", mensaje = "Msg A", leido = false, creadoEn = "2026-01-01"),
            NotificationResponse(id = 2L, userId = 10L, titulo = "B", mensaje = "Msg B", leido = true, creadoEn = "2026-01-02")
        )
        coEvery { api.getUnread() } returns response
        coEvery { dao.upsertAll(any()) } returns Unit

        repository.fetchAndCache()

        coVerify(exactly = 1) { api.getUnread() }
        coVerify(exactly = 1) {
            dao.upsertAll(match { entities ->
                entities.size == 2 &&
                    entities[0].id == 1L &&
                    entities[0].titulo == "A" &&
                    entities[1].id == 2L &&
                    entities[1].leido == true
            })
        }
    }

    @Test
    fun `fetchAndCache handles API exception gracefully`() = testScope.runTest {
        coEvery { api.getUnread() } throws RuntimeException("Timeout")

        // Should not throw
        repository.fetchAndCache()

        coVerify(exactly = 0) { dao.upsertAll(any()) }
    }

    // ── markAsRead ──

    @Test
    fun `markAsRead calls API and DAO`() = testScope.runTest {
        coEvery { api.markRead(1L) } returns Unit
        coEvery { dao.markRead(1L) } returns Unit

        repository.markAsRead(1L)

        coVerify(exactly = 1) { api.markRead(1L) }
        coVerify(exactly = 1) { dao.markRead(1L) }
    }

    @Test
    fun `markAsRead handles API exception by still marking local`() = testScope.runTest {
        coEvery { api.markRead(1L) } throws RuntimeException("API down")
        coEvery { dao.markRead(1L) } returns Unit

        repository.markAsRead(1L)

        // DAO should still be called even if API fails
        coVerify(exactly = 1) { dao.markRead(1L) }
    }

    // ── markLocalAsRead ──

    @Test
    fun `markLocalAsRead only marks in DAO`() = testScope.runTest {
        coEvery { dao.markRead(5L) } returns Unit

        repository.markLocalAsRead(5L)

        coVerify(exactly = 0) { api.markRead(any()) }
        coVerify(exactly = 1) { dao.markRead(5L) }
    }

    // ── DTO mapping ──

    @Test
    fun `NotificationResponse toEntity mapping`() = testScope.runTest {
        val response = NotificationResponse(
            id = 10L,
            userId = 5L,
            titulo = "Test",
            mensaje = "Mensaje test",
            leido = false,
            creadoEn = "2026-06-28T12:00:00"
        )

        coEvery { api.getUnread() } returns listOf(response)
        coEvery { dao.upsertAll(any()) } answers {
            val entities = firstArg<List<NotificationEntity>>()
            assertEquals(1, entities.size)
            val entity = entities[0]
            assertEquals(10L, entity.id)
            assertEquals(5L, entity.userId)
            assertEquals("Test", entity.titulo)
            assertEquals("Mensaje test", entity.mensaje)
            assertEquals(false, entity.leido)
            assertEquals("2026-06-28T12:00:00", entity.creadoEn)
        }

        repository.fetchAndCache()
    }
}
