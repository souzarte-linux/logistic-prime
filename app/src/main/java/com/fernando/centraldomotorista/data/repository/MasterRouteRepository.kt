package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.MasterRouteApi
import com.fernando.centraldomotorista.data.remote.dto.FinishRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterDeliveryRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterRouteStopDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateRoutePackagesDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopStatusDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Repositório responsável pelo gerenciamento de rotas e paradas do Usuário Master.
 * Integração com as tabelas master_delivery_routes e master_route_stops no Supabase via PostgREST.
 */
open class MasterRouteRepository(
    private val masterRouteApi: MasterRouteApi = RetrofitClient.masterRouteApi,
    private val userIdProvider: () -> String? = { supabase.auth.currentUserOrNull()?.id }
) {
    private val tag = "MasterRouteRepo"

    private fun getCurrentUserId(): String? {
        return userIdProvider()
    }

    /**
     * Cria uma nova rota diária para o motorista Master.
     */
    suspend fun createRoute(
        platformId: String?,
        startLocation: String,
        startLat: Double? = null,
        startLng: Double? = null
    ): MasterDeliveryRoute = createRoute(
        platformId = platformId,
        startLocation = startLocation,
        startLat = startLat?.let { BigDecimal.valueOf(it) },
        startLng = startLng?.let { BigDecimal.valueOf(it) }
    )

    /**
     * Sobrecarga de criação de rota com coordenadas em BigDecimal.
     */
    suspend fun createRoute(
        platformId: String?,
        startLocation: String,
        startLat: BigDecimal?,
        startLng: BigDecimal?
    ): MasterDeliveryRoute = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
            ?: throw IllegalStateException("Usuário não autenticado no Supabase")

        val dto = MasterDeliveryRouteDto(
            userId = userId,
            platformId = platformId?.ifBlank { null },
            routeDate = LocalDate.now().toString(),
            startLocation = startLocation,
            startLatitude = startLat,
            startLongitude = startLng,
            status = RouteStatus.EM_ANDAMENTO.value,
            totalPackages = 0,
            deliveredPackages = 0,
            returnedPackages = 0
        )

        val created = masterRouteApi.createRoute(dto)
        val result = created.firstOrNull()?.toDomain()
            ?: throw IllegalStateException("Falha ao registrar rota master no Supabase")

        AppDataSync.notifyDataChanged()
        result
    }

    /**
     * Adiciona uma parada / pacote bipado à rota em andamento.
     */
    suspend fun addStop(
        routeId: String,
        barcode: String,
        recipientName: String?,
        fullAddress: String,
        cep: String?,
        stopOrder: Int = 1,
        notes: String? = null,
        street: String? = null,
        number: String? = null,
        neighborhood: String? = null,
        city: String? = null,
        state: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): MasterRouteStop = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
            ?: throw IllegalStateException("Usuário não autenticado no Supabase")

        val stopDto = MasterRouteStopDto(
            routeId = routeId,
            userId = userId,
            barcode = barcode.trim(),
            recipientName = recipientName?.trim(),
            fullAddress = fullAddress.trim(),
            street = street?.trim(),
            number = number?.trim(),
            neighborhood = neighborhood?.trim(),
            city = city?.trim(),
            state = state?.trim(),
            cep = cep?.trim(),
            stopOrder = stopOrder,
            status = StopStatus.PENDENTE.value,
            latitude = latitude?.let { BigDecimal.valueOf(it) },
            longitude = longitude?.let { BigDecimal.valueOf(it) },
            notes = notes?.trim()
        )

        val createdList = masterRouteApi.addStop(stopDto)
        val createdStop = createdList.firstOrNull()?.toDomain()
            ?: throw IllegalStateException("Falha ao adicionar parada $barcode à rota $routeId")

        // Atualiza a contagem total de pacotes na rota de cabeçalho
        try {
            val allStops = masterRouteApi.getStopsByRoute("eq.$routeId")
            masterRouteApi.updateRouteTotalPackages(
                idFilter = "eq.$routeId",
                body = UpdateRoutePackagesDto(totalPackages = allStops.size)
            )
        } catch (e: Exception) {
            Log.w(tag, "Não foi possível atualizar total_packages na rota $routeId: ${e.message}")
        }

        AppDataSync.notifyDataChanged()
        createdStop
    }

    /**
     * Adiciona múltiplas paradas em lote para a rota.
     */
    suspend fun addStopsBatch(stops: List<MasterRouteStop>): List<MasterRouteStop> = withContext(Dispatchers.IO) {
        if (stops.isEmpty()) return@withContext emptyList()
        val userId = getCurrentUserId()
            ?: throw IllegalStateException("Usuário não autenticado no Supabase")

        val dtos = stops.map {
            it.copy(userId = userId).toDto()
        }

        try {
            val resultDtos = masterRouteApi.addStopsBatch(dtos)
            val result = resultDtos.map { it.toDomain() }

            val routeId = stops.first().routeId
            if (routeId.isNotBlank()) {
                val allStops = masterRouteApi.getStopsByRoute("eq.$routeId")
                masterRouteApi.updateRouteTotalPackages(
                    idFilter = "eq.$routeId",
                    body = UpdateRoutePackagesDto(totalPackages = allStops.size)
                )
            }

            AppDataSync.notifyDataChanged()
            result
        } catch (e: Exception) {
            Log.e(tag, "Erro ao adicionar paradas em lote: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Atualiza o status de entrega de uma parada específica.
     */
    suspend fun updateStopStatus(
        stopId: String,
        status: StopStatus,
        notes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deliveredAt = if (status == StopStatus.ENTREGUE) {
                OffsetDateTime.now().toString()
            } else null

            val body = UpdateStopStatusDto(
                status = status.value,
                notes = notes,
                deliveredAt = deliveredAt
            )

            val updated = masterRouteApi.updateStopStatus("eq.$stopId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao atualizar status da parada $stopId: ${e.message}", e)
            false
        }
    }

    /**
     * Retorna a rota ativa (status = 'em_andamento') do usuário autenticado no momento.
     */
    suspend fun getActiveRoute(): MasterDeliveryRoute? = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext null
        getActiveRoute(userId)
    }

    /**
     * Retorna a rota ativa para um determinado userId.
     */
    suspend fun getActiveRoute(userId: String): MasterDeliveryRoute? = withContext(Dispatchers.IO) {
        try {
            val list = masterRouteApi.getRoutes(
                userIdFilter = "eq.$userId",
                statusFilter = "eq.${RouteStatus.EM_ANDAMENTO.value}",
                order = "created_at.desc",
                limit = 1
            )
            list.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar rota ativa para o usuário $userId: ${e.message}", e)
            null
        }
    }

    /**
     * Retorna uma rota específica pelo seu identificador único.
     */
    suspend fun getRouteById(routeId: String): MasterDeliveryRoute? = withContext(Dispatchers.IO) {
        try {
            val list = masterRouteApi.getRouteById("eq.$routeId")
            list.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar rota por id $routeId: ${e.message}", e)
            null
        }
    }

    /**
     * Retorna a lista de rotas mais recentes do usuário ordenadas por data de criação descrescente.
     */
    suspend fun getRecentRoutes(limit: Int = 10): List<MasterDeliveryRoute> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext emptyList()
        try {
            val list = masterRouteApi.getRoutes(
                userIdFilter = "eq.$userId",
                order = "created_at.desc",
                limit = limit
            )
            list.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar rotas recentes do usuário $userId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Retorna todas as paradas cadastradas de uma rota, ordenadas por ordem de entrega.
     */
    suspend fun getRouteStops(routeId: String): List<MasterRouteStop> = withContext(Dispatchers.IO) {
        try {
            val list = masterRouteApi.getStopsByRoute(
                routeIdFilter = "eq.$routeId",
                order = "stop_order.asc,scanned_at.asc"
            )
            list.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar paradas da rota $routeId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Finaliza a rota master, registrando contagens de entregues e devolvidos.
     */
    suspend fun finishRoute(
        routeId: String,
        deliveredCount: Int,
        returnedCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val total = deliveredCount + returnedCount
            val body = FinishRouteDto(
                status = RouteStatus.CONCLUIDA.value,
                deliveredPackages = deliveredCount,
                returnedPackages = returnedCount,
                totalPackages = total,
                finishedAt = OffsetDateTime.now().toString()
            )

            val updated = masterRouteApi.finishRoute("eq.$routeId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao finalizar rota $routeId: ${e.message}", e)
            false
        }
    }

    /**
     * Cancela uma rota master em andamento.
     */
    suspend fun cancelRoute(routeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = FinishRouteDto(
                status = RouteStatus.CANCELADA.value,
                deliveredPackages = 0,
                returnedPackages = 0,
                totalPackages = 0,
                finishedAt = OffsetDateTime.now().toString()
            )
            val updated = masterRouteApi.finishRoute("eq.$routeId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao cancelar rota $routeId: ${e.message}", e)
            false
        }
    }

    /**
     * Exclui uma rota e todas as suas paradas (via cascade do banco).
     */
    suspend fun deleteRoute(routeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            masterRouteApi.deleteRoute("eq.$routeId")
            AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e(tag, "Erro ao excluir rota $routeId: ${e.message}", e)
            false
        }
    }
}
