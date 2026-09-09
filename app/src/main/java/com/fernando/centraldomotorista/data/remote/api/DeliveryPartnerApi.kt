package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.DeliveryPartnerDto
import retrofit2.http.*

interface DeliveryPartnerApi {
    @GET("delivery_partners")
    suspend fun getDeliveryPartners(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "full_name.asc"
    ): List<DeliveryPartnerDto>

    @Headers("Prefer: return=representation")
    @POST("delivery_partners")
    suspend fun createDeliveryPartner(
        @Body partner: DeliveryPartnerDto
    ): List<DeliveryPartnerDto>

    @Headers("Prefer: return=representation")
    @PATCH("delivery_partners")
    suspend fun updateDeliveryPartner(
        @Query("id") idFilter: String,
        @Body partner: DeliveryPartnerDto
    ): List<DeliveryPartnerDto>

    @DELETE("delivery_partners")
    suspend fun deleteDeliveryPartner(
        @Query("id") idFilter: String
    )
}
