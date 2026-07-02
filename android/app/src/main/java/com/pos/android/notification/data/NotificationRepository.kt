package com.pos.android.notification.data

import android.util.Log
import com.pos.android.core.network.ApiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val notificationDao: NotificationDao
) {
    companion object {
        private const val TAG = "NotificationRepo"
        private const val POLL_INTERVAL_MS = 30_000L // 30 segundos
    }

    // ── Observables desde Room ──

    fun observeAll(): Flow<List<NotificationEntity>> = notificationDao.observeAll()

    fun observeUnread(): Flow<List<NotificationEntity>> = notificationDao.observeUnread()

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    // ── Remote ──

    suspend fun fetchUnreadCount(): ApiResult<Int> {
        return try {
            val response = notificationApi.getUnreadCount()
            ApiResult.success(response.count.toInt())
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching unread count: ${e.message}")
            ApiResult.Error(e.message ?: "Error")
        }
    }

    suspend fun fetchAndCache() {
        try {
            val unread = notificationApi.getUnread()
            val entities = unread.map { it.toEntity() }
            notificationDao.upsertAll(entities)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching notifications: ${e.message}")
        }
    }

    suspend fun markAsRead(id: Long) {
        try {
            notificationApi.markRead(id)
            notificationDao.markRead(id)
        } catch (e: Exception) {
            Log.w(TAG, "Error marking read: ${e.message}")
        }
    }

    suspend fun markLocalAsRead(id: Long) {
        notificationDao.markRead(id)
    }

    // ── Polling ──

    /**
     * Loop infinito de polling cada 30 segundos.
     * Lanzar en un viewModelScope independiente.
     */
    suspend fun startPolling() {
        while (true) {
            fetchAndCache()
            delay(POLL_INTERVAL_MS)
        }
    }

    // ── Helpers ──

    private fun NotificationResponse.toEntity() = NotificationEntity(
        id = id,
        userId = userId,
        titulo = titulo,
        mensaje = mensaje,
        leido = leido ?: false,
        creadoEn = creadoEn
    )
}
