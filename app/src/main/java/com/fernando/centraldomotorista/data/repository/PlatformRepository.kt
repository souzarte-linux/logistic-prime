package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PlatformApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlatformRepository(
    private val platformApi: PlatformApi = RetrofitClient.platformApi
) {
    suspend fun getPlatforms(userId: String): List<Platform> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            platformApi.getPlatforms(userIdFilter = userFilter, activeFilter = null, order = "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao buscar todas as plataformas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getActivePlatforms(userId: String): List<Platform> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            platformApi.getPlatforms(userIdFilter = userFilter, activeFilter = "eq.true", order = "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao buscar plataformas ativas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun savePlatform(platform: Platform): Platform = withContext(Dispatchers.IO) {
        val dto = platform.toDto()
        if (platform.id.isNotBlank()) {
            val updated = platformApi.updatePlatform("eq.${platform.id}", dto)
            updated.firstOrNull()?.toDomain() ?: platform
        } else {
            val created = platformApi.createPlatform(dto)
            created.firstOrNull()?.toDomain() ?: platform
        }
    }

    suspend fun deletePlatform(platformId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            platformApi.deletePlatform("eq.$platformId")
            true
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao excluir plataforma $platformId: ${e.message}", e)
            false
        }
    }
}

