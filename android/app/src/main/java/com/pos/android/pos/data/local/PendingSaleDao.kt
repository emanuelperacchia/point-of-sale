package com.pos.android.pos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSaleDao {

    @Query("SELECT * FROM pending_sales WHERE estado = 'PENDIENTE' OR estado = 'ERROR' ORDER BY creado_en ASC")
    suspend fun getPending(): List<PendingSaleEntity>

    @Query("SELECT * FROM pending_sales WHERE estado = 'PENDIENTE' OR estado = 'ERROR' ORDER BY creado_en ASC")
    fun observePending(): Flow<List<PendingSaleEntity>>

    @Query("SELECT COUNT(*) FROM pending_sales WHERE estado = 'PENDIENTE' OR estado = 'SINCRONIZANDO'")
    fun countPending(): Flow<Int>

    @Insert
    suspend fun insert(sale: PendingSaleEntity): Long

    @Update
    suspend fun update(sale: PendingSaleEntity)

    @Query("UPDATE pending_sales SET estado = :estado, intentos = intentos + 1 WHERE id = :id")
    suspend fun markSyncing(id: Long, estado: String = "SINCRONIZANDO")

    @Query("UPDATE pending_sales SET estado = 'COMPLETADA' WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("UPDATE pending_sales SET estado = 'ERROR', error_message = :error WHERE id = :id")
    suspend fun markError(id: Long, error: String)

    @Delete
    suspend fun delete(sale: PendingSaleEntity)
}
