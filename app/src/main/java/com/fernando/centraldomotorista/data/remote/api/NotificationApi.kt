package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationApi {
    @GET("notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String,
        @Query("read") readFilter: String = "eq.false"
    ): List<NotificationDto>
}
