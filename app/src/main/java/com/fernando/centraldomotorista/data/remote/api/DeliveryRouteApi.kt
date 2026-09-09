package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.DeliveryRouteDto
import retrofit2.http.*

interface DeliveryRouteApi {
    @GET("delivery_routes")
    suspend fun getDeliveryRoutes(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<DeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @POST("delivery_routes")
    suspend fun createDeliveryRoute(
        @Body route: DeliveryRouteDto
    ): List<DeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @POST("delivery_routes")
    suspend fun createDeliveryRoutes(
        @Body routes: List<DeliveryRouteDto>
    ): List<DeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @PATCH("delivery_routes")
    suspend fun updateDeliveryRoute(
        @Query("id") idFilter: String,
        @Body route: DeliveryRouteDto
    ): List<DeliveryRouteDto>

    @DELETE("delivery_routes")
    suspend fun deleteDeliveryRoute(
        @Query("id") idFilter: String
    )
}
