package com.pos.android.notification.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val userId: Long? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
    val leido: Boolean = false,
    val creadoEn: String? = null
)
