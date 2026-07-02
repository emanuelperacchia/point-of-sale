package com.pos.android.notification.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val userId: Long?,
    val titulo: String?,
    val mensaje: String?,
    val leido: Boolean = false,
    val creadoEn: String? = null
)
