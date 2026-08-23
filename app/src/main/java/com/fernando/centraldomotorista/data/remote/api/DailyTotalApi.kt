package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.DailyTotalDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface DailyTotalApi {
    @GET("daily_totals")
    suspend fun getDailyTotals(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "occurred_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): List<DailyTotalDto>

    @Headers("Prefer: return=representation")
    @POST("daily_totals")
    suspend fun createDailyTotal(
        @Body dailyTotal: DailyTotalDto
    ): List<DailyTotalDto>
}
