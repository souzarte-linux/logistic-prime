package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.ProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ProfileApi {
    @GET("profiles")
    suspend fun getProfile(
        @Query("id") idFilter: String
    ): List<ProfileDto>

    @Headers("Prefer: return=representation")
    @POST("profiles")
    suspend fun createProfile(
        @Body profile: ProfileDto
    ): List<ProfileDto>

    @Headers("Prefer: return=representation")
    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any>
    ): List<ProfileDto>
}
