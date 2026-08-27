package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.model.GasStationBrand
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.GasStationApi
import com.fernando.centraldomotorista.data.remote.api.GasStationBrandApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GasStationRepository(
    private val gasStationApi: GasStationApi = RetrofitClient.gasStationApi,
    private val gasStationBrandApi: GasStationBrandApi = RetrofitClient.gasStationBrandApi
) {
    suspend fun getGasStations(userId: String): List<GasStation> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            gasStationApi.getGasStations(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("GasStationRepo", "Erro ao buscar postos: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveGasStation(station: GasStation): GasStation = withContext(Dispatchers.IO) {
        val dto = station.toDto()
        if (station.id.isNotBlank()) {
            val updated = gasStationApi.updateGasStation("eq.${station.id}", dto)
            updated.firstOrNull()?.toDomain() ?: station
        } else {
            val created = gasStationApi.createGasStation(dto)
            created.firstOrNull()?.toDomain() ?: station
        }
    }

    suspend fun deleteGasStation(stationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            gasStationApi.deleteGasStation("eq.$stationId")
            true
        } catch (e: Exception) {
            Log.e("GasStationRepo", "Erro ao excluir posto $stationId: ${e.message}", e)
            false
        }
    }

    suspend fun getGasStationBrands(userId: String): List<GasStationBrand> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            gasStationBrandApi.getGasStationBrands(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("GasStationRepo", "Erro ao buscar bandeiras de posto: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createGasStationBrand(userId: String, name: String): GasStationBrand = withContext(Dispatchers.IO) {
        val brand = GasStationBrand(id = "", userId = userId, name = name)
        val dto = brand.toDto()
        Log.d("DiagnosticoBandeira", ">> [POST gas_station_brands] userId='$userId', name='$name', payload: $dto")
        try {
            val created = gasStationBrandApi.createGasStationBrand(dto)
            Log.d("DiagnosticoBandeira", "<< [POST gas_station_brands SUCESSO]: $created")
            created.firstOrNull()?.toDomain() ?: brand
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("DiagnosticoBandeira", "!! [POST gas_station_brands ERRO HTTP $code]: $errorBody", e)
            throw Exception("HTTP $code: $errorBody", e)
        } catch (e: Exception) {
            Log.e("DiagnosticoBandeira", "!! [POST gas_station_brands ERRO GERAL]: ${e.message}", e)
            throw e
        }
    }
}
