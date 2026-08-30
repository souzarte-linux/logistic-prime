package com.fernando.centraldomotorista.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val PLATFORM_SEGMENTS = listOf(
    "logistica" to "Logística",
    "delivery" to "Delivery"
)

val PAYMENT_CYCLES = listOf(
    "semanal" to "Semanal",
    "quinzenal" to "Quinzenal",
    "misto" to "Misto",
    "mensal" to "Mensal",
    "diario" to "Diário"
)

val COMMON_PAYMENT_DAYS = listOf(
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado",
    "Domingo",
    "Dia 5 e 20",
    "Dia 15 e 30",
    "Último dia do mês",
    "D+1 (Diário)"
)

val PAYMENT_MODELS = listOf(
    "producao" to "Produção / Por Entrega",
    "taxa_fixa" to "Taxa Fixa",
    "diaria" to "Diária Fixa",
    "km" to "Por KM Rodado"
)

val POPULAR_PLATFORMS = listOf(
    Triple("Mercado Envios", "logistica", "semanal"),
    Triple("iFood", "delivery", "semanal"),
    Triple("Loggi", "logistica", "semanal"),
    Triple("Lalamove", "logistica", "semanal"),
    Triple("Rappi", "delivery", "semanal"),
    Triple("Shopee Entregas", "logistica", "quinzenal"),
    Triple("Uber Direct", "delivery", "semanal"),
    Triple("Zé Delivery", "delivery", "semanal")
)

data class PlatformsUiState(
    val platforms: List<Platform> = emptyList(),
    val searchQuery: String = "",
    val selectedSegmentFilter: String? = null, // null = all, "logistica", "delivery"
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    
    // Form fields
    val editingPlatformId: String? = null,
    val name: String = "",
    val segment: String = "logistica",
    val cycle: String = "semanal",
    val paymentDay: String = "Quarta-feira",
    val paymentModel: String = "producao",
    val active: Boolean = true
)

class PlatformsViewModel(
    private val repository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatformsUiState())
    val uiState: StateFlow<PlatformsUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadPlatforms()
    }

    fun loadPlatforms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getPlatforms(currentUserId)
            _uiState.update {
                it.copy(
                    platforms = list,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSegmentFilterChanged(segment: String?) {
        _uiState.update { it.copy(selectedSegmentFilter = segment) }
    }

    fun openAddDialog(prefillName: String? = null, prefillSegment: String? = null, prefillCycle: String? = null) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPlatformId = null,
                name = prefillName ?: "",
                segment = prefillSegment ?: "logistica",
                cycle = prefillCycle ?: "semanal",
                paymentDay = "Quarta-feira",
                paymentModel = "producao",
                active = true,
                error = null
            )
        }
    }

    fun startEditing(platform: Platform) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPlatformId = platform.id,
                name = platform.name,
                segment = platform.segment,
                cycle = platform.cycle,
                paymentDay = platform.paymentDay ?: "Quarta-feira",
                paymentModel = platform.paymentModel,
                active = platform.active,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingPlatformId = null,
                name = "",
                segment = "logistica",
                cycle = "semanal",
                paymentDay = "Quarta-feira",
                paymentModel = "producao",
                active = true,
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onSegmentChanged(segment: String) = _uiState.update { it.copy(segment = segment) }
    fun onCycleChanged(cycle: String) = _uiState.update { it.copy(cycle = cycle) }
    fun onPaymentDayChanged(paymentDay: String) = _uiState.update { it.copy(paymentDay = paymentDay) }
    fun onPaymentModelChanged(model: String) = _uiState.update { it.copy(paymentModel = model) }
    fun onActiveChanged(active: Boolean) = _uiState.update { it.copy(active = active) }

    fun togglePlatformActive(platform: Platform) {
        viewModelScope.launch {
            val updatedPlatform = platform.copy(active = !platform.active)
            try {
                repository.savePlatform(updatedPlatform)
                _uiState.update { state ->
                    val updatedList = state.platforms.map {
                        if (it.id == platform.id) updatedPlatform else it
                    }
                    val statusText = if (updatedPlatform.active) "ativada" else "inativada"
                    state.copy(
                        platforms = updatedList,
                        message = "Plataforma '${platform.name}' $statusText!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao alterar status: ${e.message}") }
            }
        }
    }

    fun savePlatform(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da plataforma.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val platform = Platform(
                id = state.editingPlatformId ?: "",
                userId = currentUserId,
                name = state.name.trim(),
                segment = state.segment,
                cycle = state.cycle,
                paymentDay = state.paymentDay.trim().ifBlank { null },
                paymentModel = state.paymentModel,
                active = state.active
            )

            try {
                repository.savePlatform(platform)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingPlatformId != null) "Plataforma atualizada com sucesso!" else "Plataforma adicionada com sucesso!"
                    )
                }
                loadPlatforms()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar plataforma: ${e.message}") }
            }
        }
    }

    fun deletePlatform(platformId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deletePlatform(platformId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingPlatformId = null,
                        message = "Plataforma excluída com sucesso!"
                    )
                }
                loadPlatforms()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir plataforma.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
