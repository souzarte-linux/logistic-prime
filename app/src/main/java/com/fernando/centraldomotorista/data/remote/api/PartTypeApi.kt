package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PartTypeDto
import retrofit2.http.*

interface PartTypeApi {
    @GET("part_types")
    suspend fun getPartTypes(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<PartTypeDto>

    @Headers("Prefer: return=representation")
    @POST("part_types")
    suspend fun createPartType(
        @Body partType: PartTypeDto
    ): List<PartTypeDto>

    @Headers("Prefer: return=representation")
    @PATCH("part_types")
    suspend fun updatePartType(
        @Query("id") idFilter: String,
        @Body partType: PartTypeDto
    ): List<PartTypeDto>

    @DELETE("part_types")
    suspend fun deletePartType(
        @Query("id") idFilter: String
    )
}
