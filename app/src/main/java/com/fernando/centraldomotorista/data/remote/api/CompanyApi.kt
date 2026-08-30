package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.CompanyDto
import retrofit2.http.*

interface CompanyApi {
    @GET("companies")
    suspend fun getCompanies(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<CompanyDto>

    @Headers("Prefer: return=representation")
    @POST("companies")
    suspend fun createCompany(
        @Body company: CompanyDto
    ): List<CompanyDto>

    @Headers("Prefer: return=representation")
    @PATCH("companies")
    suspend fun updateCompany(
        @Query("id") idFilter: String,
        @Body company: CompanyDto
    ): List<CompanyDto>

    @DELETE("companies")
    suspend fun deleteCompany(
        @Query("id") idFilter: String
    )
}
