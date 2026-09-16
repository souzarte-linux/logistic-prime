package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.BillingCycleDto
import retrofit2.http.*

interface BillingCycleApi {
    @GET("billing_cycles")
    suspend fun getBillingCycles(
        @Query("user_id") userIdFilter: String,
        @Query("status") statusFilter: String? = null,
        @Query("platform_id") platformFilter: String? = null,
        @Query("order") order: String = "period_start.desc"
    ): List<BillingCycleDto>

    @Headers("Prefer: return=representation")
    @POST("billing_cycles")
    suspend fun createBillingCycle(
        @Body cycle: BillingCycleDto
    ): List<BillingCycleDto>

    @Headers("Prefer: return=representation")
    @PATCH("billing_cycles")
    suspend fun updateBillingCycle(
        @Query("id") idFilter: String,
        @Body cycle: Map<String, @JvmSuppressWildcards Any?>
    ): List<BillingCycleDto>

    @DELETE("billing_cycles")
    suspend fun deleteBillingCycle(
        @Query("id") idFilter: String
    )
}

