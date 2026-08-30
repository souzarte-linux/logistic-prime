package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.ExpenseDto
import retrofit2.http.*

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

    @Headers("Prefer: return=representation")
    @PATCH("expenses")
    suspend fun updateExpense(
        @Query("id") idFilter: String,
        @Body expense: ExpenseDto
    ): List<ExpenseDto>

    @DELETE("expenses")
    suspend fun deleteExpense(
        @Query("id") idFilter: String
    )
}
