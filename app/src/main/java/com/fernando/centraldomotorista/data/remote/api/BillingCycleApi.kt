package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.BillingCycleDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BillingCycleApi {
    @GET("billing_cycles")
    suspend fun getBillingCycles(
        @Query("user_id") userIdFilter: String,
        @Query("status") statusFilter: String = "eq.pending"
    ): List<BillingCycleDto>
}
