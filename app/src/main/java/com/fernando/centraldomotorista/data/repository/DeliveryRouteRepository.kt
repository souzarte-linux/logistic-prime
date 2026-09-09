package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.DeliveryRouteApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeliveryRouteRepository(
    private val deliveryRouteApi: DeliveryRouteApi = RetrofitClient.deliveryRouteApi
) {
    suspend fun getDeliveryRoutes(userId: String): List<DeliveryRoute> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            deliveryRouteApi.getDeliveryRoutes(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("DeliveryRouteRepo", "Erro ao buscar rotas de entrega: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveDeliveryRoute(route: DeliveryRoute): DeliveryRoute = withContext(Dispatchers.IO) {
        val dto = route.toDto()
        if (route.id.isNotBlank()) {
            val updated = deliveryRouteApi.updateDeliveryRoute("eq.${route.id}", dto)
            updated.firstOrNull()?.toDomain() ?: route
        } else {
            val created = deliveryRouteApi.createDeliveryRoute(dto)
            created.firstOrNull()?.toDomain() ?: route
        }
    }

    suspend fun createSuggestedRoutes(userId: String, names: List<String>): List<DeliveryRoute> = withContext(Dispatchers.IO) {
        try {
            val dtos = names.map { name ->
                DeliveryRoute(userId = userId, name = name).toDto()
            }
            val created = deliveryRouteApi.createDeliveryRoutes(dtos)
            created.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("DeliveryRouteRepo", "Erro ao criar rotas sugeridas em lote: ${e.message}", e)
            // Fallback: tentar criar uma a uma
            val result = mutableListOf<DeliveryRoute>()
            for (name in names) {
                try {
                    val single = saveDeliveryRoute(DeliveryRoute(userId = userId, name = name))
                    result.add(single)
                } catch (ex: Exception) {
                    Log.e("DeliveryRouteRepo", "Erro ao criar rota individual '$name': ${ex.message}")
                }
            }
            result
        }
    }

    suspend fun deleteDeliveryRoute(routeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            deliveryRouteApi.deleteDeliveryRoute("eq.$routeId")
            true
        } catch (e: Exception) {
            Log.e("DeliveryRouteRepo", "Erro ao excluir rota de entrega $routeId: ${e.message}", e)
            false
        }
    }
}
