package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.AppNotification
import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("billing_cycle_id")
    val billingCycleId: String? = null,
    @SerializedName("read")
    val read: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun NotificationDto.toDomain(): AppNotification {
    return AppNotification(
        id = id,
        userId = userId,
        type = type,
        billingCycleId = billingCycleId,
        read = read
    )
}
