package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.ExpenseApi
import com.fernando.centraldomotorista.data.remote.api.RouteApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class RouteRepository(
    private val routeApi: RouteApi = RetrofitClient.routeApi,
    private val expenseApi: ExpenseApi = RetrofitClient.expenseApi
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

    suspend fun getLastOdometerKm(userId: String): BigDecimal? = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val routes = routeApi.getRoutes(userFilter, "occurred_at.desc")
            val latestRouteEndKm = routes.firstOrNull { it.endKm != null && it.endKm > BigDecimal.ZERO }?.endKm

            val expenses = expenseApi.getExpenses(userFilter, "occurred_at.desc")
            val latestExpenseKm = expenses.firstOrNull { it.odometerKm != null && it.odometerKm > BigDecimal.ZERO }?.odometerKm

            when {
                latestRouteEndKm != null && latestExpenseKm != null -> maxOf(latestRouteEndKm, latestExpenseKm)
                latestRouteEndKm != null -> latestRouteEndKm
                latestExpenseKm != null -> latestExpenseKm
                else -> null
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Erro ao buscar último KM do odômetro: ${e.message}", e)
            null
        }
    }
}
