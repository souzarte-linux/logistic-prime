package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.ExpenseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ExpenseApi {
    @GET("expenses")
    suspend fun getExpenses(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "occurred_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): List<ExpenseDto>

    @Headers("Prefer: return=representation")
    @POST("expenses")
    suspend fun createExpense(
        @Body expense: ExpenseDto
    ): List<ExpenseDto>
}
