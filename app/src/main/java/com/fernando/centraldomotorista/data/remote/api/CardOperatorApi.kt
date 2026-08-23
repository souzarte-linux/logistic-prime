package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.CardOperatorDto
import retrofit2.http.*

interface CardOperatorApi {
    @GET("card_operators")
    suspend fun getCardOperators(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<CardOperatorDto>

    @Headers("Prefer: return=representation")
    @POST("card_operators")
    suspend fun createCardOperator(
        @Body operator: CardOperatorDto
    ): List<CardOperatorDto>
}
