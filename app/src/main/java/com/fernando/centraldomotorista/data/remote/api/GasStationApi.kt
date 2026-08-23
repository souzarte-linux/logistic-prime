package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.GasStationDto
import retrofit2.http.*

interface GasStationApi {
    @GET("gas_stations")
    suspend fun getGasStations(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<GasStationDto>

    @Headers("Prefer: return=representation")
    @POST("gas_stations")
    suspend fun createGasStation(
        @Body station: GasStationDto
    ): List<GasStationDto>

    @Headers("Prefer: return=representation")
    @PATCH("gas_stations")
    suspend fun updateGasStation(
        @Query("id") idFilter: String,
        @Body station: GasStationDto
    ): List<GasStationDto>

    @DELETE("gas_stations")
    suspend fun deleteGasStation(
        @Query("id") idFilter: String
    )
}
