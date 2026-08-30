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

    @Headers("Prefer: return=representation")
    @PATCH("card_operators")
    suspend fun updateCardOperator(
        @Query("id") idFilter: String,
        @Body operator: CardOperatorDto
    ): List<CardOperatorDto>

    @DELETE("card_operators")
    suspend fun deleteCardOperator(
        @Query("id") idFilter: String
    )
}

