package com.pos.android.dashboard.data

import com.google.gson.Gson
import com.pos.android.core.network.ApiResult
import com.pos.android.dashboard.data.local.DashboardCacheDao
import com.pos.android.dashboard.data.local.DashboardCacheEntity
import com.pos.android.dashboard.data.model.ExecutiveDashboardDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val cacheDao: DashboardCacheDao,
    private val gson: Gson
) {
    suspend fun fetch(periodo: String, sucursalId: Long?): ApiResult<ExecutiveDashboardDto> {
        return try {
            val response = dashboardApi.getExecutiveDashboard(periodo, sucursalId)
            // Guardar en caché
            val key = cacheKey(sucursalId, periodo)
            cacheDao.save(DashboardCacheEntity(key = key, json = gson.toJson(response)))
            ApiResult.success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Error al cargar dashboard")
        }
    }

    suspend fun getCached(periodo: String, sucursalId: Long?): CachedDashboard? {
        val key = cacheKey(sucursalId, periodo)
        val entity = cacheDao.get(key) ?: return null
        val data = gson.fromJson(entity.json, ExecutiveDashboardDto::class.java)
        return CachedDashboard(
            data = data,
            cachedAt = entity.cachedAt
        )
    }

    private fun cacheKey(sucursalId: Long?, periodo: String): String {
        val branchPart = sucursalId?.toString() ?: "all"
        return "${branchPart}_${periodo}"
    }

    data class CachedDashboard(
        val data: ExecutiveDashboardDto,
        val cachedAt: Long
    ) {
        val relativeTime: String
            get() {
                val diff = System.currentTimeMillis() - cachedAt
                val minutes = diff / 60_000
                return when {
                    minutes < 1 -> "Ahora"
                    minutes < 60 -> "Hace $minutes min"
                    else -> "Hace ${minutes / 60}h ${minutes % 60}m"
                }
            }
    }
}
