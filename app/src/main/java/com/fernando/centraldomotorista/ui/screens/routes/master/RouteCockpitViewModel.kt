package com.fernando.centraldomotorista.ui.screens.routes.master

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RouteCockpitUiState(
    val isLoading: Boolean = true,
    val route: MasterDeliveryRoute? = null,
    val stops: List<MasterRouteStop> = emptyList(),
    val platformName: String? = null,
    val searchQuery: String = "",
    val isFinishing: Boolean = false,
    val error: String? = null
) {
    val totalPackages: Int get() = stops.size
    val deliveredCount: Int get() = stops.count { it.status == StopStatus.ENTREGUE }
    val pendingCount: Int get() = stops.count { it.status == StopStatus.PENDENTE }
    val returnedCount: Int get() = stops.count { it.status == StopStatus.DEVOLVIDO || it.status == StopStatus.AUSENTE }

    val filteredStops: List<MasterRouteStop>
        get() {
            if (searchQuery.isBlank()) return stops
            val query = searchQuery.trim().lowercase()
            return stops.filter { stop ->
                stop.barcode.lowercase().contains(query) ||
                        stop.recipientName?.lowercase()?.contains(query) == true ||
                        stop.fullAddress.lowercase().contains(query)
            }
        }
}

class RouteCockpitViewModel(
    private val masterRouteRepository: MasterRouteRepository = MasterRouteRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val tag = "RouteCockpitVM"

    private val _uiState = MutableStateFlow(RouteCockpitUiState())
    val uiState: StateFlow<RouteCockpitUiState> = _uiState.asStateFlow()

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
                val platform = route?.platformId?.let { pId ->
                    platformRepository.getActivePlatforms(user.id).firstOrNull { it.id == pId }
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        route = route,
                        stops = stops,
                        platformName = platform?.name,
                        error = null
                    )
                }
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

    fun finishRoute(
        onSuccess: () -> Unit
    ) {
        val routeId = _uiState.value.route?.id ?: return
        val delivered = _uiState.value.deliveredCount
        val returned = _uiState.value.returnedCount

        _uiState.value = _uiState.value.copy(isFinishing = true)

        viewModelScope.launch(Dispatchers.IO) {
            val success = masterRouteRepository.finishRoute(
                routeId = routeId,
                deliveredCount = delivered,
                returnedCount = returned
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isFinishing = false)
                if (success) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Falha ao finalizar rota no banco de dados.")
                }
            }
        }
    }
}
