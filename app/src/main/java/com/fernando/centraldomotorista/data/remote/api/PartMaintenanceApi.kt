package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PartMaintenanceDto
import retrofit2.http.*

interface PartMaintenanceApi {
    @GET("part_maintenance")
    suspend fun getPartMaintenances(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): List<PartMaintenanceDto>

    @Headers("Prefer: return=representation")
    @POST("part_maintenance")
    suspend fun createPartMaintenance(
        @Body part: PartMaintenanceDto
    ): List<PartMaintenanceDto>

    @Headers("Prefer: return=representation")
    @PATCH("part_maintenance")
    suspend fun updatePartMaintenance(
        @Query("id") idFilter: String,
        @Body part: PartMaintenanceDto
    ): List<PartMaintenanceDto>

    @DELETE("part_maintenance")
    suspend fun deletePartMaintenance(
        @Query("id") idFilter: String
    )
}
