package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.GasStationBrandDto
import retrofit2.http.*

interface GasStationBrandApi {
    @GET("gas_station_brands")
    suspend fun getGasStationBrands(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "name.asc"
    ): List<GasStationBrandDto>

    @POST("gas_station_brands")
    @Headers("Prefer: return=representation")
    suspend fun createGasStationBrand(
        @Body brand: GasStationBrandDto
    ): List<GasStationBrandDto>
}
