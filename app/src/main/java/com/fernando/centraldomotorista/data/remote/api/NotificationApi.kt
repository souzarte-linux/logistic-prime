package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.NotificationDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.Query

interface NotificationApi {
    @GET("notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String,
        @Query("read") readFilter: String = "eq.false"
    ): List<NotificationDto>

    @Headers("Prefer: return=representation")
    @PATCH("notifications")
    suspend fun updateNotification(
        @Query("id") idFilter: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any>
    ): List<NotificationDto>
}
