package com.pos.android.dashboard.data.local

import androidx.room.*

@Dao
interface DashboardCacheDao {

    @Query("SELECT * FROM dashboard_cache WHERE key = :key")
    suspend fun get(key: String): DashboardCacheEntity?

    @Upsert
    suspend fun save(entity: DashboardCacheEntity)

    @Query("DELETE FROM dashboard_cache WHERE cachedAt < :threshold")
    suspend fun evictOld(threshold: Long)
}
