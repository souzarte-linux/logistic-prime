package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.RouteApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RouteRepository(
    private val routeApi: RouteApi = RetrofitClient.routeApi
) {
    suspend fun createRoute(route: Route): Route = withContext(Dispatchers.IO) {
        val dto = route.toDto()
        val createdList = routeApi.createRoute(dto)
        createdList.firstOrNull()?.toDomain() ?: route
    }

    suspend fun getRoutes(userId: String): List<Route> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            routeApi.getRoutes(userFilter, "occurred_at.desc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Erro ao buscar rotas: ${e.message}", e)
            emptyList()
        }
    }
}
