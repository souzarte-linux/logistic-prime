package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.DeliveryPartnerSessionDto
import retrofit2.http.*

interface DeliveryPartnerSessionApi {
    @GET("delivery_partner_sessions")
    suspend fun getSessions(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "start_time.desc,created_at.desc"
    ): List<DeliveryPartnerSessionDto>

    @GET("delivery_partner_sessions")
    suspend fun getSessionsForPartner(
        @Query("user_id") userIdFilter: String,
        @Query("partner_id") partnerIdFilter: String,
        @Query("order") order: String = "start_time.desc,created_at.desc"
    ): List<DeliveryPartnerSessionDto>

    @GET("delivery_partner_sessions")
    suspend fun getSessionById(
        @Query("id") idFilter: String
    ): List<DeliveryPartnerSessionDto>

    @Headers("Prefer: return=representation")
    @POST("delivery_partner_sessions")
    suspend fun createSession(
        @Body session: DeliveryPartnerSessionDto
    ): List<DeliveryPartnerSessionDto>

    @Headers("Prefer: return=representation")
    @PATCH("delivery_partner_sessions")
    suspend fun updateSession(
        @Query("id") idFilter: String,
        @Body session: DeliveryPartnerSessionDto
    ): List<DeliveryPartnerSessionDto>

    @DELETE("delivery_partner_sessions")
    suspend fun deleteSession(
        @Query("id") idFilter: String
    )
}
