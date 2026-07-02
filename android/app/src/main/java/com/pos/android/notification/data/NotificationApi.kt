package com.pos.android.notification.data

import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationApi {

    @GET("notifications/count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @GET("notifications/unread")
    suspend fun getUnread(): List<NotificationResponse>

    @GET("notifications")
    suspend fun getAll(): List<NotificationResponse>

    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long)
}
