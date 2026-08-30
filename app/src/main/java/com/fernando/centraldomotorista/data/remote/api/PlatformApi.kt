package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PlatformDto
import retrofit2.http.*

interface PlatformApi {
    @GET("platforms")
    suspend fun getPlatforms(
        @Query("user_id") userIdFilter: String,
        @Query("active") activeFilter: String? = null,
        @Query("order") order: String = "name.asc"
    ): List<PlatformDto>

    @Headers("Prefer: return=representation")
    @POST("platforms")
    suspend fun createPlatform(
        @Body platform: PlatformDto
    ): List<PlatformDto>

    @Headers("Prefer: return=representation")
    @PATCH("platforms")
    suspend fun updatePlatform(
        @Query("id") idFilter: String,
        @Body platform: PlatformDto
    ): List<PlatformDto>

    @DELETE("platforms")
    suspend fun deletePlatform(
        @Query("id") idFilter: String
    )
}

