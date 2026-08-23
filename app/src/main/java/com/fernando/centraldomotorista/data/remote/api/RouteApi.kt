package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.RouteDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface RouteApi {
    @GET("routes")
    suspend fun getRoutes(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "occurred_at.desc"
    ): List<RouteDto>

    @Headers("Prefer: return=representation")
    @POST("routes")
    suspend fun createRoute(
        @Body route: RouteDto
    ): List<RouteDto>
}
