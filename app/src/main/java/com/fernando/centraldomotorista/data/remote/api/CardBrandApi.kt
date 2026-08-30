package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.CardBrandDto
import retrofit2.http.*

interface CardBrandApi {
    @GET("card_brands")
    suspend fun getCardBrands(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<CardBrandDto>

    @Headers("Prefer: return=representation")
    @POST("card_brands")
    suspend fun createCardBrand(
        @Body brand: CardBrandDto
    ): List<CardBrandDto>

    @Headers("Prefer: return=representation")
    @PATCH("card_brands")
    suspend fun updateCardBrand(
        @Query("id") idFilter: String,
        @Body brand: CardBrandDto
    ): List<CardBrandDto>

    @DELETE("card_brands")
    suspend fun deleteCardBrand(
        @Query("id") idFilter: String
    )
}

