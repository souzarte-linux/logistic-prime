package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.OilChangeDto
import retrofit2.http.*

interface OilChangeApi {
    @GET("oil_changes")
    suspend fun getOilChanges(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "changed_at.desc"
    ): List<OilChangeDto>

    @Headers("Prefer: return=representation")
    @POST("oil_changes")
    suspend fun createOilChange(
        @Body oilChange: OilChangeDto
    ): List<OilChangeDto>

    @Headers("Prefer: return=representation")
    @PATCH("oil_changes")
    suspend fun updateOilChange(
        @Query("id") idFilter: String,
        @Body oilChange: OilChangeDto
    ): List<OilChangeDto>

    @DELETE("oil_changes")
    suspend fun deleteOilChange(
        @Query("id") idFilter: String
    )
}
