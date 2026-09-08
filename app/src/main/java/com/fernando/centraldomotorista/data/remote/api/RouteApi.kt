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

    @Headers("Prefer: return=representation")
    @retrofit2.http.PATCH("routes")
    suspend fun updateRoute(
        @Query("id") idFilter: String,
        @Body route: RouteDto
    ): List<RouteDto>

    @retrofit2.http.DELETE("routes")
    suspend fun deleteRoute(
        @Query("id") idFilter: String
    )

    @GET("routes")
    suspend fun getRouteById(
        @Query("id") idFilter: String
    ): List<RouteDto>
}
