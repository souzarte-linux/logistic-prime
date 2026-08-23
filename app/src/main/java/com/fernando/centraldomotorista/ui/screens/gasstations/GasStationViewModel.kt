package com.fernando.centraldomotorista.ui.screens.gasstations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.repository.GasStationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GasStationUiState(
    val stations: List<GasStation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // Form state
    val editingStationId: String? = null,
    val name: String = "",
    val nickname: String = "",
    val brand: String = "Shell",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val selectedFuelTypes: Set<String> = setOf("Gasolina Comum", "Etanol")
)

class GasStationViewModel(
    private val repository: GasStationRepository = GasStationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GasStationUiState())
    val uiState: StateFlow<GasStationUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    init {
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getGasStations(currentUserId)
            _uiState.update { it.copy(stations = list, isLoading = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onNicknameChanged(nickname: String) = _uiState.update { it.copy(nickname = nickname) }
    fun onBrandChanged(brand: String) = _uiState.update { it.copy(brand = brand) }
    fun onCepChanged(cep: String) = _uiState.update { it.copy(cep = cep) }
    fun onStreetChanged(street: String) = _uiState.update { it.copy(street = street) }
    fun onNumberChanged(number: String) = _uiState.update { it.copy(number = number) }
    fun onNeighborhoodChanged(neighborhood: String) = _uiState.update { it.copy(neighborhood = neighborhood) }
    fun onCityChanged(city: String) = _uiState.update { it.copy(city = city) }
    fun onStateChanged(state: String) = _uiState.update { it.copy(state = state) }

    fun toggleFuelType(fuelType: String) {
        _uiState.update { state ->
            val updated = state.selectedFuelTypes.toMutableSet()
            if (updated.contains(fuelType)) {
                updated.remove(fuelType)
            } else {
                updated.add(fuelType)
            }
            state.copy(selectedFuelTypes = updated)
        }
    }

    fun startEditing(station: GasStation) {
        _uiState.update {
            it.copy(
                editingStationId = station.id,
                name = station.name,
                nickname = station.nickname ?: "",
                brand = station.brand,
                cep = station.cep ?: "",
                street = station.street ?: "",
                number = station.number ?: "",
                neighborhood = station.neighborhood ?: "",
                city = station.city ?: "",
                state = station.state ?: "",
                selectedFuelTypes = station.fuelTypes.toSet()
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingStationId = null,
                name = "",
                nickname = "",
                brand = "Shell",
                cep = "",
                street = "",
                number = "",
                neighborhood = "",
                city = "",
                state = "",
                selectedFuelTypes = setOf("Gasolina Comum", "Etanol")
            )
        }
    }

    fun saveStation(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome do posto.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val fullAddress = listOfNotNull(
                state.street.ifBlank { null }?.let { if (state.number.isNotBlank()) "$it, ${state.number}" else it },
                state.neighborhood.ifBlank { null },
                state.city.ifBlank { null }?.let { if (state.state.isNotBlank()) "$it - ${state.state}" else it },
                state.cep.ifBlank { null }?.let { "CEP: $it" }
            ).joinToString(", ")

            val station = GasStation(
                id = state.editingStationId ?: "",
                userId = currentUserId,
                name = state.name.trim(),
                nickname = state.nickname.trim().ifBlank { null },
                brand = state.brand.trim(),
                address = fullAddress.ifBlank { null },
                cep = state.cep.trim().ifBlank { null },
                street = state.street.trim().ifBlank { null },
                number = state.number.trim().ifBlank { null },
                neighborhood = state.neighborhood.trim().ifBlank { null },
                city = state.city.trim().ifBlank { null },
                state = state.state.trim().ifBlank { null },
                fuelTypes = state.selectedFuelTypes.toList()
            )

            try {
                repository.saveGasStation(station)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = if (state.editingStationId != null) "Posto atualizado com sucesso!" else "Posto cadastrado com sucesso!"
                    )
                }
                cancelEditing()
                loadStations()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar posto: ${e.message}") }
            }
        }
    }

    fun deleteStation(stationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deleteGasStation(stationId)
            if (success) {
                _uiState.update { it.copy(message = "Posto excluído!") }
                loadStations()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir posto.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
