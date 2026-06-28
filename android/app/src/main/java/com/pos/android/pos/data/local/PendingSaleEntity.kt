package com.pos.android.pos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sales")
data class PendingSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "body_json") val bodyJson: String,
    @ColumnInfo(name = "estado") val estado: String = "PENDIENTE", // PENDIENTE, SINCRONIZANDO, COMPLETADA, ERROR
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "intentos") val intentos: Int = 0,
    @ColumnInfo(name = "creado_en") val creadoEn: Long = System.currentTimeMillis()
)
