package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.FinancialAdjustmentDto
import retrofit2.http.*

interface FinancialAdjustmentApi {
    @GET("financial_adjustments")
    suspend fun getFinancialAdjustments(
        @Query("user_id") userIdFilter: String,
        @Query("platform_id") platformIdFilter: String? = null,
        @Query("billing_cycle_id") cycleIdFilter: String? = null,
        @Query("occurred_at") sinceFilter: String? = null,
        @Query("occurred_at") untilFilter: String? = null,
        @Query("order") order: String = "occurred_at.desc"
    ): List<FinancialAdjustmentDto>

    @Headers("Prefer: return=representation")
    @POST("financial_adjustments")
    suspend fun createFinancialAdjustment(
        @Body adjustment: FinancialAdjustmentDto
    ): List<FinancialAdjustmentDto>

    @Headers("Prefer: return=representation")
    @PATCH("financial_adjustments")
    suspend fun updateFinancialAdjustment(
        @Query("id") idFilter: String,
        @Body adjustment: FinancialAdjustmentDto
    ): List<FinancialAdjustmentDto>

    @DELETE("financial_adjustments")
    suspend fun deleteFinancialAdjustment(
        @Query("id") idFilter: String
    )
}
