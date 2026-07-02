package com.pos.android.notification.data

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long?,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("leido") val leido: Boolean?,
    @SerializedName("creadoEn") val creadoEn: String?
)

data class UnreadCountResponse(
    @SerializedName("count") val count: Long
)
