package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PlatformDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlatformApi {
    @GET("platforms")
    suspend fun getPlatforms(
        @Query("user_id") userIdFilter: String,
        @Query("active") activeFilter: String = "eq.true",
        @Query("order") order: String = "name.asc"
    ): List<PlatformDto>
}
