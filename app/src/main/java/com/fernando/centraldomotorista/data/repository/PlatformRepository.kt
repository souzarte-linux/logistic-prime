package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PlatformApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlatformRepository(
    private val platformApi: PlatformApi = RetrofitClient.platformApi
) {
    suspend fun getActivePlatforms(userId: String): List<Platform> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            platformApi.getPlatforms(userFilter, "eq.true").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao buscar plataformas ativas: ${e.message}", e)
            emptyList()
        }
    }
}
