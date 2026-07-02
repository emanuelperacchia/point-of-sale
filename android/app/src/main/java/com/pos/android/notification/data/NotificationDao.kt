package com.pos.android.notification.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY creadoEn DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE leido = 0 ORDER BY creadoEn DESC")
    fun observeUnread(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE leido = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM notifications ORDER BY creadoEn DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE leido = 0 ORDER BY creadoEn DESC")
    suspend fun getUnread(): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE leido = 0")
    suspend fun getUnreadCount(): Int

    @Upsert
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET leido = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
