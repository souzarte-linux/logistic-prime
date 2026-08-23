package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.CreditCardDto
import retrofit2.http.*

interface CreditCardApi {
    @GET("credit_cards")
    suspend fun getCreditCards(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "nickname.asc"
    ): List<CreditCardDto>

    @Headers("Prefer: return=representation")
    @POST("credit_cards")
    suspend fun createCreditCard(
        @Body card: CreditCardDto
    ): List<CreditCardDto>

    @Headers("Prefer: return=representation")
    @PATCH("credit_cards")
    suspend fun updateCreditCard(
        @Query("id") idFilter: String,
        @Body card: CreditCardDto
    ): List<CreditCardDto>

    @DELETE("credit_cards")
    suspend fun deleteCreditCard(
        @Query("id") idFilter: String
    )
}
