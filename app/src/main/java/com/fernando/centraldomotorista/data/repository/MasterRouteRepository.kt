package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageOrigin
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.PartnerSessionPackage
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.MasterRouteApi
import com.fernando.centraldomotorista.data.remote.dto.FinishRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterDeliveryRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterRouteStopDto
import com.fernando.centraldomotorista.data.remote.dto.PartnerSessionPackageDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateRoutePackagesDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopLocationDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopOrderDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopPhotoDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopStatusDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopTransferDto
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
 * Integração com as tabelas master_delivery_routes, master_route_stops e partner_session_packages no Supabase via PostgREST.
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
     * Adiciona uma parada / pacote bipado à rota em andamento com suporte a extensões ADR-003.
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
        longitude: Double? = null,
        platformId: String? = null,
        packageType: PackageType = PackageType.PACOTINHO,
        photoUrl: String? = null,
        photoExpiresAt: OffsetDateTime? = null,
        assignedPartnerId: String? = null,
        transferStatus: TransferStatus? = null,
        transferredVia: String? = null,
        transferredAt: OffsetDateTime? = null
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
            notes = notes?.trim(),
            platformId = platformId?.ifBlank { null },
            packageType = packageType.value,
            photoUrl = photoUrl,
            photoExpiresAt = photoExpiresAt?.toString(),
            assignedPartnerId = assignedPartnerId?.ifBlank { null },
            transferStatus = transferStatus?.value,
            transferredVia = transferredVia,
            transferredAt = transferredAt?.toString()
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
     * Busca uma parada ativa por código de barras (Prompt 1 / ADR-003).
     * Retorna a parada se ela não foi devolvida e ainda não teve transferência confirmada.
     */
    suspend fun findActiveMasterStopByBarcode(barcode: String): MasterRouteStop? = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext null
        try {
            val list = masterRouteApi.findStopsByBarcode(
                userIdFilter = "eq.$userId",
                barcodeFilter = "eq.${barcode.trim()}",
                statusFilter = "neq.${StopStatus.DEVOLVIDO.value}"
            )
            // Filtra paradas onde transfer_status != 'confirmado'
            val activeStopDto = list.firstOrNull { it.transferStatus != TransferStatus.CONFIRMADO.value }
            activeStopDto?.toDomain()
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar parada ativa por barcode '$barcode': ${e.message}", e)
            null
        }
    }

    /**
     * Atualiza o status e detalhes de transferência de uma parada para um parceiro.
     */
    suspend fun updateStopTransfer(
        stopId: String,
        partnerId: String?,
        status: TransferStatus?,
        via: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val transferredAt = if (status != null) OffsetDateTime.now().toString() else null
            val body = UpdateStopTransferDto(
                assignedPartnerId = partnerId,
                transferStatus = status?.value,
                transferredVia = via,
                transferredAt = transferredAt
            )
            val updated = masterRouteApi.updateStopTransfer("eq.$stopId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao atualizar transferência da parada $stopId: ${e.message}", e)
            false
        }
    }

    /**
     * Atualiza a foto e a data de expiração de uma parada.
     */
    suspend fun updateStopPhoto(
        stopId: String,
        photoUrl: String?,
        photoExpiresAt: OffsetDateTime? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = UpdateStopPhotoDto(
                photoUrl = photoUrl,
                photoExpiresAt = photoExpiresAt?.toString()
            )
            val updated = masterRouteApi.updateStopPhoto("eq.$stopId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao atualizar foto da parada $stopId: ${e.message}", e)
            false
        }
    }

    /**
     * Atualiza as coordenadas geográficas (latitude e longitude) de uma parada (Prompt 9).
     */
    suspend fun updateStopLocation(
        stopId: String,
        latitude: Double,
        longitude: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = UpdateStopLocationDto(
                latitude = BigDecimal.valueOf(latitude),
                longitude = BigDecimal.valueOf(longitude)
            )
            val updated = masterRouteApi.updateStopLocation("eq.$stopId", body)
            val success = updated.isNotEmpty()
            if (success) {
                AppDataSync.notifyDataChanged()
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao atualizar localização da parada $stopId: ${e.message}", e)
            false
        }
    }

    /**
     * Atualiza sequencialmente a ordem de entrega (stop_order) das paradas da rota (Prompt 9).
     */
    suspend fun updateStopsOrder(
        stopsWithNewOrder: List<Pair<String, Int>>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            for ((stopId, newOrder) in stopsWithNewOrder) {
                masterRouteApi.updateStopOrder(
                    idFilter = "eq.$stopId",
                    body = UpdateStopOrderDto(stopOrder = newOrder)
                )
            }
            AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e(tag, "Erro ao atualizar ordem das paradas: ${e.message}", e)
            false
        }
    }

    /**
     * Cria um registro granular de pacote na sessão do parceiro.
     */
    suspend fun createPartnerSessionPackage(
        sessionId: String,
        barcode: String,
        origin: PackageOrigin = PackageOrigin.NOVO,
        masterStopId: String? = null,
        status: String = "bipado"
    ): PartnerSessionPackage = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
            ?: throw IllegalStateException("Usuário não autenticado no Supabase")

        val dto = PartnerSessionPackageDto(
            sessionId = sessionId,
            userId = userId,
            barcode = barcode.trim(),
            origin = origin.value,
            masterStopId = masterStopId,
            status = status
        )

        val createdList = masterRouteApi.addPartnerSessionPackage(dto)
        val result = createdList.firstOrNull()?.toDomain()
            ?: throw IllegalStateException("Falha ao registrar pacote da sessão do parceiro")

        AppDataSync.notifyDataChanged()
        result
    }

    /**
     * Importa uma parada do Master para a sessão do parceiro, ou registra como novo pacote.
     * Sincroniza atomicamente a transferência na tabela master_route_stops.
     */
    suspend fun importMasterStopToPartnerSession(
        sessionId: String,
        barcode: String,
        partnerId: String
    ): PartnerSessionPackage = withContext(Dispatchers.IO) {
        val activeMasterStop = findActiveMasterStopByBarcode(barcode)
        if (activeMasterStop != null) {
            updateStopTransfer(
                stopId = activeMasterStop.id,
                partnerId = partnerId,
                status = TransferStatus.CONFIRMADO,
                via = "scan_parceiro"
            )
            createPartnerSessionPackage(
                sessionId = sessionId,
                barcode = barcode,
                origin = PackageOrigin.IMPORTADO_MASTER,
                masterStopId = activeMasterStop.id
            )
        } else {
            createPartnerSessionPackage(
                sessionId = sessionId,
                barcode = barcode,
                origin = PackageOrigin.NOVO,
                masterStopId = null
            )
        }
    }

    /**
     * Retorna a lista de pacotes granulares de uma sessão de parceiro.
     */
    suspend fun getPartnerSessionPackages(sessionId: String): List<PartnerSessionPackage> = withContext(Dispatchers.IO) {
        try {
            val list = masterRouteApi.getPartnerSessionPackages("eq.$sessionId")
            list.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar pacotes da sessão do parceiro $sessionId: ${e.message}", e)
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
     * Retorna a lista de rotas recentes do usuário autenticado no momento.
     */
    suspend fun getRecentRoutes(limit: Int = 10): List<MasterDeliveryRoute> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext emptyList()
        getRecentRoutes(userId, limit)
    }

    /**
     * Retorna a lista de rotas recentes para um determinado userId.
     */
    suspend fun getRecentRoutes(userId: String, limit: Int = 10): List<MasterDeliveryRoute> = withContext(Dispatchers.IO) {
        try {
            val list = masterRouteApi.getRoutes(
                userIdFilter = "eq.$userId",
                statusFilter = null,
                order = "route_date.desc,created_at.desc",
                limit = limit
            )
            list.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao buscar rotas recentes para o usuário $userId: ${e.message}", e)
            emptyList()
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
     * Finaliza a rota master, registrando contagens de entregues e devolvidos,
     * e aplicando a política de retenção de fotos (finished_at + 15 dias).
     */
    suspend fun finishRoute(
        routeId: String,
        deliveredCount: Int,
        returnedCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val total = deliveredCount + returnedCount
            val finishedAt = OffsetDateTime.now()
            val body = FinishRouteDto(
                status = RouteStatus.CONCLUIDA.value,
                deliveredPackages = deliveredCount,
                returnedPackages = returnedCount,
                totalPackages = total,
                finishedAt = finishedAt.toString()
            )

            val updated = masterRouteApi.finishRoute("eq.$routeId", body)
            val success = updated.isNotEmpty()
            if (success) {
                // Aplica política de expiração das fotos (finished_at + 15 dias)
                try {
                    val stops = masterRouteApi.getStopsByRoute("eq.$routeId")
                    val expiresAt = finishedAt.plusDays(15).toString()
                    for (stop in stops) {
                        if (!stop.photoUrl.isNullOrBlank() && stop.photoExpiresAt.isNullOrBlank() && !stop.id.isNullOrBlank()) {
                            masterRouteApi.updateStopPhoto(
                                idFilter = "eq.${stop.id}",
                                body = UpdateStopPhotoDto(photoUrl = stop.photoUrl, photoExpiresAt = expiresAt)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Não foi possível calcular expiração das fotos da rota $routeId: ${e.message}")
                }

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
