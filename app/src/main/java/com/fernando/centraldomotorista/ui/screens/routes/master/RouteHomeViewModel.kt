package com.fernando.centraldomotorista.ui.screens.routes.master

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.preferences.RoutePreferences
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RouteHomeUiState(
    val isLoading: Boolean = false,
    val activeRoute: MasterDeliveryRoute? = null,
    val recentRoutes: List<MasterDeliveryRoute> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val isCreatingRoute: Boolean = false,
    val error: String? = null
)

class RouteHomeViewModel @JvmOverloads constructor(
    application: Application,
    private val masterRouteRepository: MasterRouteRepository = MasterRouteRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val routePreferences: RoutePreferences = RoutePreferences(application)
) : AndroidViewModel(application) {

    private val tag = "RouteHomeVM"

    private val _uiState = MutableStateFlow(RouteHomeUiState(isLoading = true))
    val uiState: StateFlow<RouteHomeUiState> = _uiState.asStateFlow()

    /**
     * Flow com o prazo configurado de retenção das fotos de backup de etiquetas (padrão 3 dias).
     */
    val photoRetentionDays: StateFlow<Int> = routePreferences.photoRetentionDaysFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoutePreferences.DEFAULT_PHOTO_RETENTION_DAYS
        )

    /**
     * Atualiza a preferência do usuário com relação ao prazo de retenção das fotos de etiquetas.
     */
    fun updatePhotoRetentionDays(days: Int) {
        viewModelScope.launch {
            routePreferences.setPhotoRetentionDays(days)
        }
    }

    init {
        loadData()
    }

    fun loadData() {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuário não autenticado.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val active = masterRouteRepository.getActiveRoute(user.id)
                val recents = masterRouteRepository.getRecentRoutes(limit = 10)
                val platforms = platformRepository.getActivePlatforms(user.id)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeRoute = active,
                        recentRoutes = recents,
                        platforms = platforms,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro ao carregar dados do hub de rotas: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar rotas: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun createNewRoute(
        platformId: String?,
        startLocation: String,
        startLat: Double?,
        startLng: Double?,
        onSuccess: (String) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(isCreatingRoute = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val created = masterRouteRepository.createRoute(
                    platformId = platformId,
                    startLocation = startLocation,
                    startLat = startLat,
                    startLng = startLng
                )
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingRoute = false,
                        activeRoute = created
                    )
                    onSuccess(created.id)
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro ao criar nova rota master: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingRoute = false,
                        error = "Falha ao criar rota: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
