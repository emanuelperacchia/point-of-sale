package com.pos.android.dashboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    @PrimaryKey val key: String, // "{branchId}_{periodo}"
    val json: String, // JSON serializado del ExecutiveDashboardDto
    val cachedAt: Long = System.currentTimeMillis()
)
