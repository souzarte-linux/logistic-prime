package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.PartProductDto
import retrofit2.http.*

interface PartProductApi {
    @GET("part_products")
    suspend fun getPartProducts(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "brand.asc"
    ): List<PartProductDto>

    @Headers("Prefer: return=representation")
    @POST("part_products")
    suspend fun createPartProduct(
        @Body product: PartProductDto
    ): List<PartProductDto>

    @Headers("Prefer: return=representation")
    @PATCH("part_products")
    suspend fun updatePartProduct(
        @Query("id") idFilter: String,
        @Body product: PartProductDto
    ): List<PartProductDto>

    @DELETE("part_products")
    suspend fun deletePartProduct(
        @Query("id") idFilter: String
    )
}
