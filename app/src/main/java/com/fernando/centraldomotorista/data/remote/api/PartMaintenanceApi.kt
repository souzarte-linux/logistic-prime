package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PartMaintenanceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PartMaintenanceApi {
    @GET("part_maintenance")
    suspend fun getPartMaintenances(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): List<PartMaintenanceDto>
}
