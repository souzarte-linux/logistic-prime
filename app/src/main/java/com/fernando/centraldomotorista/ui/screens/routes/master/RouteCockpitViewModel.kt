package com.fernando.centraldomotorista.ui.screens.routes.master

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.preferences.RoutePreferences
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.data.repository.RouteRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

import com.fernando.centraldomotorista.data.repository.GeocodingRepository
import com.fernando.centraldomotorista.util.RouteOptimizationHelper

data class MasterRouteHandOffItem(
    val platformId: String?,
    val platformName: String?,
    val smallPackagesCount: Int,
    val largePackagesCount: Int,
    val totalPackages: Int,
    val proratedKm: BigDecimal,
    val startKm: BigDecimal,
    val endKm: BigDecimal,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val routeDate: LocalDate,
    val origin: String?,
    val masterRouteId: String,
    val notes: String
)

data class RouteCockpitUiState(
    val isLoading: Boolean = true,
    val route: MasterDeliveryRoute? = null,
    val stops: List<MasterRouteStop> = emptyList(),
    val platformName: String? = null,
    val searchQuery: String = "",
    val isFinishing: Boolean = false,
    val error: String? = null,
    // Prompt 5: Estado de expansão dos cards (padrão contraído)
    val expandedStopIds: Set<String> = emptySet(),
    // Prompt 6: Lista de parceiros disponíveis para atribuição
    val deliveryPartners: List<DeliveryPartner> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    // Prompt 9: Mapa osmdroid, ordenação e geocodificação
    val isMapVisible: Boolean = false,
    val isOptimizing: Boolean = false,
    val isGeocoding: Boolean = false
) {
    val totalPackages: Int get() = stops.size
    val deliveredCount: Int get() = stops.count { it.status == StopStatus.ENTREGUE }
    val pendingCount: Int get() = stops.count { it.status == StopStatus.PENDENTE }
    val returnedCount: Int get() = stops.count { it.status == StopStatus.DEVOLVIDO || it.status == StopStatus.AUSENTE }

    // Paradas não confirmadas pelo parceiro (confirmadas somem da navegação do Master)
    val nonConfirmedStops: List<MasterRouteStop>
        get() = stops.filter { it.transferStatus != TransferStatus.CONFIRMADO }

    // Paradas ativas na posse direta do Master
    val activeStops: List<MasterRouteStop>
        get() = nonConfirmedStops.filter { it.transferStatus != TransferStatus.ATRIBUIDO_PENDENTE }

    // Paradas atribuídas que aguardam confirmação física do parceiro
    val awaitingConfirmationStops: List<MasterRouteStop>
        get() = nonConfirmedStops.filter { it.transferStatus == TransferStatus.ATRIBUIDO_PENDENTE }

    private fun filterList(list: List<MasterRouteStop>): List<MasterRouteStop> {
        if (searchQuery.isBlank()) return list
        val query = searchQuery.trim().lowercase()
        return list.filter { stop ->
            stop.barcode.lowercase().contains(query) ||
                    stop.recipientName?.lowercase()?.contains(query) == true ||
                    stop.fullAddress.lowercase().contains(query)
        }
    }

    val filteredActiveStops: List<MasterRouteStop> get() = filterList(activeStops)
    val filteredAwaitingConfirmationStops: List<MasterRouteStop> get() = filterList(awaitingConfirmationStops)
}

class RouteCockpitViewModel @JvmOverloads constructor(
    application: Application,
    private val masterRouteRepository: MasterRouteRepository = MasterRouteRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val deliveryPartnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: RouteRepository = RouteRepository(),
    private val geocodingRepository: GeocodingRepository = GeocodingRepository(),
    private val routePreferences: RoutePreferences = RoutePreferences(application)
) : AndroidViewModel(application) {

    private val tag = "RouteCockpitVM"

    private val _uiState = MutableStateFlow(RouteCockpitUiState())
    val uiState: StateFlow<RouteCockpitUiState> = _uiState.asStateFlow()

    /**
     * Flow com o prazo de retenção das fotos de backup de etiquetas (padrão 3 dias).
     */
    val photoRetentionDays: StateFlow<Int> = routePreferences.photoRetentionDaysFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoutePreferences.DEFAULT_PHOTO_RETENTION_DAYS
        )

    private var cachedLastOdometerKm: BigDecimal? = null

    fun loadCockpit(routeId: String) {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuário não autenticado.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val route = masterRouteRepository.getRouteById(routeId)
                val stops = masterRouteRepository.getRouteStops(routeId)
                val allPlatforms = platformRepository.getActivePlatforms(user.id)
                val platform = route?.platformId?.let { pId ->
                    allPlatforms.firstOrNull { it.id == pId }
                }
                val partners = deliveryPartnerRepository.getDeliveryPartners(user.id)
                cachedLastOdometerKm = routeRepository.getLastOdometerKm(user.id)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        route = route,
                        stops = stops,
                        platformName = platform?.name,
                        deliveryPartners = partners,
                        platforms = allPlatforms,
                        expandedStopIds = emptySet(), // Por padrão todos contraídos (Prompt 5)
                        error = null
                    )
                }

                // Prompt 9: Dispara geocodificação em background para paradas sem coordenadas
                triggerBackgroundGeocoding(stops)
            } catch (e: Exception) {
                Log.e(tag, "Erro ao carregar dados do cockpit $routeId: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar rota: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // --- Prompt 5: Controle de Expansão/Contração de Cards ---

    fun toggleStopExpanded(stopId: String) {
        val current = _uiState.value.expandedStopIds
        _uiState.value = _uiState.value.copy(
            expandedStopIds = if (current.contains(stopId)) current - stopId else current + stopId
        )
    }

    fun expandAllStops() {
        val allIds = _uiState.value.stops.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(expandedStopIds = allIds)
    }

    fun collapseAllStops() {
        _uiState.value = _uiState.value.copy(expandedStopIds = emptySet())
    }

    // --- Prompt 9: Mapa osmdroid, Geocodificação em Background e Otimização Offline ---

    fun toggleMapVisibility() {
        _uiState.value = _uiState.value.copy(isMapVisible = !_uiState.value.isMapVisible)
    }

    fun setMapVisibility(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isMapVisible = visible)
    }

    /**
     * Dispara a geocodificação em segundo plano para paradas que ainda não possuem latitude e longitude.
     * Atualiza o estado local e persiste as coordenadas no backend assim que resolvidas.
     */
    fun triggerBackgroundGeocoding(stopsToGeocode: List<MasterRouteStop>) {
        val pendingGeocode = stopsToGeocode.filter { it.latitude == null || it.longitude == null }
        if (pendingGeocode.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isGeocoding = true)
            try {
                for (stop in pendingGeocode) {
                    val coords = geocodingRepository.geocodeStop(stop)
                    if (coords != null) {
                        val (lat, lng) = coords
                        val saved = masterRouteRepository.updateStopLocation(stop.id, lat, lng)
                        if (saved) {
                            withContext(Dispatchers.Main) {
                                val currentStops = _uiState.value.stops
                                val updated = currentStops.map { current ->
                                    if (current.id == stop.id) {
                                        current.copy(
                                            latitude = BigDecimal.valueOf(lat),
                                            longitude = BigDecimal.valueOf(lng)
                                        )
                                    } else current
                                }
                                _uiState.value = _uiState.value.copy(stops = updated)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Aviso durante geocodificação em segundo plano: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isGeocoding = false)
                }
            }
        }
    }

    /**
     * Reordena as paradas da rota usando o algoritmo heurístico de Vizinho Mais Próximo (100% offline).
     * Salva a nova sequência stop_order no Supabase.
     */
    fun optimizeStopsOrder(startLat: Double? = null, startLng: Double? = null) {
        val currentStops = _uiState.value.stops
        if (currentStops.size <= 1) return

        val route = _uiState.value.route
        val effectiveStartLat = startLat ?: route?.startLatitude?.toDouble()
        val effectiveStartLng = startLng ?: route?.startLongitude?.toDouble()

        _uiState.value = _uiState.value.copy(isOptimizing = true)

        viewModelScope.launch(Dispatchers.Default) {
            val optimized = RouteOptimizationHelper.optimizeStopsByNearestNeighbor(
                startLat = effectiveStartLat,
                startLng = effectiveStartLng,
                stops = currentStops
            )

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(stops = optimized)
            }

            // Persiste sequencialmente no backend
            val orderPairs = optimized.map { it.id to it.stopOrder }
            val success = masterRouteRepository.updateStopsOrder(orderPairs)

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isOptimizing = false)
                if (!success) {
                    Log.w(tag, "Aviso: falha ao sincronizar nova ordem de paradas com o servidor.")
                }
            }
        }
    }

    // --- Prompt 6: Atribuição Cruzada Master -> Parceiro ---

    fun assignStopToPartner(stopId: String, partnerId: String) {
        val currentStops = _uiState.value.stops
        val updatedStops = currentStops.map { stop ->
            if (stop.id == stopId) {
                stop.copy(
                    assignedPartnerId = partnerId,
                    transferStatus = TransferStatus.ATRIBUIDO_PENDENTE,
                    transferredVia = "manual_master"
                )
            } else stop
        }
        _uiState.value = _uiState.value.copy(stops = updatedStops)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.updateStopTransfer(
                stopId = stopId,
                partnerId = partnerId,
                status = TransferStatus.ATRIBUIDO_PENDENTE,
                via = "manual_master"
            )
            if (!success) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(stops = currentStops)
                }
            }
        }
    }

    fun updateStopStatus(stopId: String, newStatus: StopStatus) {
        // Atualização otimista na memória para resposta instantânea na UI
        val currentStops = _uiState.value.stops
        val updatedStops = currentStops.map { stop ->
            if (stop.id == stopId) stop.copy(status = newStatus) else stop
        }
        _uiState.value = _uiState.value.copy(stops = updatedStops)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.updateStopStatus(stopId, newStatus)
            if (!success) {
                // Reverte em caso de erro
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(stops = currentStops)
                }
            }
        }
    }

    /**
     * Exclui um pacote bipado por engano da rota (TASK-DES-08).
     * Não contabiliza devolução nem altera negativamente as métricas do motorista.
     */
    fun deleteStop(
        stop: MasterRouteStop,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentStops = _uiState.value.stops
        val routeId = stop.routeId

        // Atualização otimista local imediata
        val updatedStops = currentStops.filter { it.id != stop.id }
        _uiState.value = _uiState.value.copy(
            stops = updatedStops,
            expandedStopIds = _uiState.value.expandedStopIds - stop.id
        )

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.deleteStop(stopId = stop.id, routeId = routeId)
            withContext(Dispatchers.Main) {
                if (success) {
                    onSuccess()
                } else {
                    // Reverte se falhou no backend
                    _uiState.value = _uiState.value.copy(
                        stops = currentStops,
                        error = "Erro ao remover pacote da rota."
                    )
                    onError("Falha ao excluir parada no servidor.")
                }
            }
        }
    }

    /**
     * Expande especificamente o card de uma parada pelo ID (ex: ao ser localizada via busca ou scanner).
     */
    fun expandStop(stopId: String) {
        _uiState.value = _uiState.value.copy(
            expandedStopIds = _uiState.value.expandedStopIds + stopId
        )
    }

    /**
     * Atualiza os dados cadastrais e logísticos de uma parada (Nome, Endereço, CEP, Tipo, Plataforma, Observações).
     */
    fun updateStop(
        stop: MasterRouteStop,
        recipientName: String?,
        fullAddress: String,
        cep: String?,
        packageType: PackageType,
        platformId: String?,
        notes: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val updatedStop = stop.copy(
            recipientName = recipientName?.trim(),
            fullAddress = fullAddress.trim(),
            cep = cep?.trim(),
            packageType = packageType,
            platformId = platformId,
            notes = notes?.trim()
        )

        val currentStops = _uiState.value.stops
        val updatedList = currentStops.map { if (it.id == stop.id) updatedStop else it }
        _uiState.value = _uiState.value.copy(stops = updatedList)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.updateStop(updatedStop)
            withContext(Dispatchers.Main) {
                if (success) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(stops = currentStops)
                    onError("Falha ao salvar alterações da parada no servidor.")
                }
            }
        }
    }

    // --- Prompt 8: Cálculo de Handoff Multi-Plataforma com Rateio de KM e Tempo ---

    fun calculateMultiPlatformHandoffs(endKmInput: String?): List<MasterRouteHandOffItem> {
        val state = _uiState.value
        val route = state.route ?: return emptyList()

        // Considera apenas as paradas que permaneceram com o Master (exclui transferidas a parceiros)
        val activeMasterStops = state.activeStops
        if (activeMasterStops.isEmpty()) return emptyList()

        val totalPhysicalPackages = activeMasterStops.size

        // Cálculo de KM total da rota física
        val endKmVal = endKmInput?.replace(',', '.')?.trim()?.toBigDecimalOrNull()
        val startKmVal = cachedLastOdometerKm ?: BigDecimal.ZERO
        val totalRouteDistanceKm = if (endKmVal != null && startKmVal > BigDecimal.ZERO && endKmVal > startKmVal) {
            endKmVal.subtract(startKmVal).setScale(1, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Agrupa por platformId (se nulo, usa a plataforma do cabeçalho da rota)
        val grouped = activeMasterStops.groupBy { it.platformId ?: route.platformId }
        val entries = grouped.entries.toList()

        var accumulatedProratedKm = BigDecimal.ZERO
        var currentOdometerKm = startKmVal
        var currentStartTime = LocalTime.now().minusHours(4).withSecond(0).withNano(0)
        val routeDate = route.routeDate

        val result = mutableListOf<MasterRouteHandOffItem>()

        for (i in entries.indices) {
            val (platformId, groupStops) = entries[i]
            val isLast = (i == entries.size - 1)
            val packagesThisPlatform = groupStops.size

            val shareRatio = packagesThisPlatform.toBigDecimal()
                .divide(totalPhysicalPackages.toBigDecimal(), 6, RoundingMode.HALF_UP)

            // Rateio do KM com ajuste de arredondamento no último grupo para garantir soma exata
            val proratedKm = if (isLast) {
                totalRouteDistanceKm.subtract(accumulatedProratedKm).max(BigDecimal.ZERO)
            } else {
                totalRouteDistanceKm.multiply(shareRatio).setScale(1, RoundingMode.HALF_UP)
            }
            accumulatedProratedKm = accumulatedProratedKm.add(proratedKm)

            val groupStartKm = currentOdometerKm
            val groupEndKm = currentOdometerKm.add(proratedKm)
            currentOdometerKm = groupEndKm

            // Rateio do tempo (janela estimada de 240 minutos)
            val proratedWorkedMinutes = (240 * shareRatio.toDouble()).roundToInt()
            val groupEndTime = currentStartTime.plusMinutes(proratedWorkedMinutes.toLong().coerceAtLeast(1))

            // Contagem de pacotinhos e volumosos entregues do grupo
            val anyDelivered = activeMasterStops.any { it.status == StopStatus.ENTREGUE }
            val effectiveStops = if (anyDelivered) {
                groupStops.filter { it.status == StopStatus.ENTREGUE }
            } else {
                groupStops.filter { it.status != StopStatus.DEVOLVIDO && it.status != StopStatus.AUSENTE }
            }

            val smallCount = effectiveStops.count { it.packageType == PackageType.PACOTINHO }
            val largeCount = effectiveStops.count { it.packageType == PackageType.VOLUMOSO }
            val totalDelivered = smallCount + largeCount

            val platName = state.platformName ?: "Plataforma"
            val notes = "Encerrada via Central do Motorista (Rota Master #${route.id})" +
                    if (!platformId.isNullOrBlank()) " - $platName" else ""

            result.add(
                MasterRouteHandOffItem(
                    platformId = platformId,
                    platformName = platName,
                    smallPackagesCount = smallCount,
                    largePackagesCount = largeCount,
                    totalPackages = totalDelivered,
                    proratedKm = proratedKm,
                    startKm = groupStartKm,
                    endKm = groupEndKm,
                    startTime = currentStartTime,
                    endTime = groupEndTime,
                    routeDate = routeDate,
                    origin = route.startLocation.ifBlank { null },
                    masterRouteId = route.id,
                    notes = notes
                )
            )

            currentStartTime = groupEndTime
        }

        return result
    }

    fun finishRoute(
        endKmInput: String? = null,
        onSuccess: (List<MasterRouteHandOffItem>) -> Unit
    ) {
        val routeId = _uiState.value.route?.id ?: return
        val delivered = _uiState.value.deliveredCount
        val returned = _uiState.value.returnedCount
        val retentionDays = photoRetentionDays.value

        val handoffItems = calculateMultiPlatformHandoffs(endKmInput)

        _uiState.value = _uiState.value.copy(isFinishing = true)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.finishRoute(
                routeId = routeId,
                deliveredCount = delivered,
                returnedCount = returned,
                photoRetentionDays = retentionDays
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isFinishing = false)
                if (success) {
                    onSuccess(handoffItems)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Falha ao finalizar rota no banco de dados.")
                }
            }
        }
    }

    /**
     * Descarta e exclui do banco de dados uma rota que foi criada mas não teve nenhum pacote bipado.
     * Evita manter rotas "fantasmas" com 0 pacotes no histórico.
     */
    fun discardEmptyRoute(onSuccess: () -> Unit) {
        val routeId = _uiState.value.route?.id ?: return
        _uiState.value = _uiState.value.copy(isFinishing = true)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.deleteRoute(routeId)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isFinishing = false)
                if (success) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Falha ao descartar rota vazia no banco de dados.")
                }
            }
        }
    }
}
