package com.fernando.centraldomotorista.ui.screens.gasstations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.model.GasStationBrand
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.GasStationRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_GAS_STATION_BRANDS = listOf("Shell", "Ipiranga", "Petrobras / Vibra", "Ale", "Texaco", "Repsol", "Boxter", "Bandeira Branca", "Outro")

data class GasStationUiState(
    val stations: List<GasStation> = emptyList(),
    val customBrands: List<GasStationBrand> = emptyList(),
    val allBrands: List<String> = DEFAULT_GAS_STATION_BRANDS,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSearchingCep: Boolean = false,
    val isAddBrandDialogOpen: Boolean = false,
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
    private val repository: GasStationRepository = GasStationRepository(),
    private val viaCepApi: com.fernando.centraldomotorista.data.remote.api.ViaCepApi = com.fernando.centraldomotorista.data.remote.api.ViaCepApi.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(GasStationUiState())
    val uiState: StateFlow<GasStationUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getGasStations(currentUserId)
            val brands = repository.getGasStationBrands(currentUserId)
            val combinedBrands = (DEFAULT_GAS_STATION_BRANDS + brands.map { it.name }).distinct()
            _uiState.update {
                it.copy(
                    stations = list,
                    customBrands = brands,
                    allBrands = combinedBrands,
                    isLoading = false
                )
            }
        }
    }

    fun openAddBrandDialog() = _uiState.update { it.copy(isAddBrandDialogOpen = true) }
    fun closeAddBrandDialog() = _uiState.update { it.copy(isAddBrandDialogOpen = false) }

    fun addBrand(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = repository.createGasStationBrand(currentUserId, name.trim())
                _uiState.update { state ->
                    val updatedCustom = state.customBrands + created
                    val updatedAll = (state.allBrands + created.name).distinct()
                    state.copy(
                        customBrands = updatedCustom,
                        allBrands = updatedAll,
                        brand = created.name,
                        isAddBrandDialogOpen = false,
                        message = "Bandeira '${created.name}' adicionada com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao adicionar bandeira: ${e.message}") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onNicknameChanged(nickname: String) = _uiState.update { it.copy(nickname = nickname) }
    fun onBrandChanged(brand: String) = _uiState.update { it.copy(brand = brand) }

    fun onCepChanged(cep: String) {
        val digits = cep.filter { it.isDigit() }.take(8)
        val formatted = if (digits.length > 5) "${digits.take(5)}-${digits.substring(5)}" else digits
        _uiState.update { it.copy(cep = formatted) }

        if (digits.length == 8) {
            searchCep(digits)
        }
    }

    fun searchCep(cepDigits: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCep = true) }
            try {
                val result = viaCepApi.getAddressByCep(cepDigits)
                if (result.erro != true) {
                    _uiState.update { state ->
                        state.copy(
                            isSearchingCep = false,
                            street = result.logradouro ?: state.street,
                            neighborhood = result.bairro ?: state.neighborhood,
                            city = result.localidade ?: state.city,
                            state = result.uf ?: state.state,
                            message = "Endereço preenchido pelo CEP! Digite o número."
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSearchingCep = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingCep = false) }
            }
        }
    }

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
