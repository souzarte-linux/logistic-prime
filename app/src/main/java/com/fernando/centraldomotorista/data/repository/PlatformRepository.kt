package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PlatformApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class PlatformRepository(
    private val platformApi: PlatformApi = RetrofitClient.platformApi,
    private val routeRepository: RouteRepository = RouteRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository()
) {
    suspend fun getPlatforms(userId: String, partnerId: String? = null): List<Platform> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val partnerFilter = if (partnerId != null) "eq.$partnerId" else "is.null"
            platformApi.getPlatforms(
                userIdFilter = userFilter,
                partnerIdFilter = partnerFilter,
                activeFilter = null,
                order = "name.asc"
            ).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao buscar todas as plataformas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getActivePlatforms(userId: String, partnerId: String? = null): List<Platform> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val partnerFilter = if (partnerId != null) "eq.$partnerId" else "is.null"
            platformApi.getPlatforms(
                userIdFilter = userFilter,
                partnerIdFilter = partnerFilter,
                activeFilter = "eq.true",
                order = "name.asc"
            ).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao buscar plataformas ativas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMonthEarningsByPlatform(userId: String): Map<String, BigDecimal> = withContext(Dispatchers.IO) {
        try {
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()

            val routes = routeRepository.getRoutes(userId).filter {
                it.platformId != null && !it.occurredAt.isBefore(startOfMonth)
            }
            val dailyTotals = dailyTotalRepository.getDailyTotals(userId).filter {
                it.platformId != null && !it.occurredAt.isBefore(startOfMonth)
            }

            val map = mutableMapOf<String, BigDecimal>()
            routes.forEach { r ->
                val platId = r.platformId ?: return@forEach
                val current = map.getOrDefault(platId, BigDecimal.ZERO)
                map[platId] = current.add(r.amount).add(r.tip)
            }
            dailyTotals.forEach { dt ->
                val platId = dt.platformId ?: return@forEach
                val current = map.getOrDefault(platId, BigDecimal.ZERO)
                map[platId] = current.add(dt.amount)
            }
            map
        } catch (e: Exception) {
            Log.e("PlatformRepository", "Erro ao calcular ganhos do mês por plataforma: ${e.message}", e)
            emptyMap()
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

