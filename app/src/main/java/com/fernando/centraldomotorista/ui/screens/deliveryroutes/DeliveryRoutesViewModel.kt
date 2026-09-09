package com.fernando.centraldomotorista.ui.screens.deliveryroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val SUGGESTED_DELIVERY_ROUTES = listOf(
    "Av. Hilda",
    "Final de Linha",
    "Tomaz Gonzaga Alta",
    "Tomaz Gonzaga Centro",
    "Tomaz Gonzaga Baixa",
    "Jardim Brasília",
    "Saramandaia"
)

data class DeliveryRoutesUiState(
    val routes: List<DeliveryRoute> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAddingSuggestions: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields
    val editingRouteId: String? = null,
    val name: String = ""
)

class DeliveryRoutesViewModel(
    private val repository: DeliveryRouteRepository = DeliveryRouteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryRoutesUiState())
    val uiState: StateFlow<DeliveryRoutesUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadRoutes()
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getDeliveryRoutes(currentUserId)
            _uiState.update {
                it.copy(
                    routes = list,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddDialog(prefillName: String? = null) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingRouteId = null,
                name = prefillName ?: "",
                error = null
            )
        }
    }

    fun startEditing(route: DeliveryRoute) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingRouteId = route.id,
                name = route.name,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingRouteId = null,
                name = "",
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun saveRoute(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da rota.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val route = DeliveryRoute(
                id = state.editingRouteId ?: "",
                userId = currentUserId,
                name = state.name.trim()
            )

            try {
                repository.saveDeliveryRoute(route)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingRouteId != null) "Rota atualizada com sucesso!" else "Rota cadastrada com sucesso!"
                    )
                }
                loadRoutes()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar rota: ${e.message}") }
            }
        }
    }

    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deleteDeliveryRoute(routeId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingRouteId = null,
                        message = "Rota excluída com sucesso!"
                    )
                }
                loadRoutes()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir rota.") }
            }
        }
    }

    fun addSuggestedRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSuggestions = true, error = null) }
            try {
                // Filtra nomes que já possam existir para evitar duplicados exatos
                val existingNames = _uiState.value.routes.map { it.name.trim().lowercase() }.toSet()
                val routesToCreate = SUGGESTED_DELIVERY_ROUTES.filter { it.trim().lowercase() !in existingNames }
                
                if (routesToCreate.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isAddingSuggestions = false,
                            message = "Todas as rotas sugeridas já estão cadastradas!"
                        )
                    }
                    return@launch
                }

                repository.createSuggestedRoutes(currentUserId, routesToCreate)
                _uiState.update {
                    it.copy(
                        isAddingSuggestions = false,
                        message = "${routesToCreate.size} rotas sugeridas adicionadas com sucesso!"
                    )
                }
                loadRoutes()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAddingSuggestions = false,
                        error = "Erro ao adicionar rotas sugeridas: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
