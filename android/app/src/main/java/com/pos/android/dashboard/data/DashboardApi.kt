package com.pos.android.dashboard.data

import com.pos.android.dashboard.data.model.ExecutiveDashboardDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {

    @GET("dashboard/executive")
    suspend fun getExecutiveDashboard(
        @Query("periodo") periodo: String = "MONTH",
        @Query("sucursalId") sucursalId: Long? = null
    ): ExecutiveDashboardDto
}
