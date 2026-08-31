package com.fernando.centraldomotorista.ui.screens.gasstations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.model.GasStationBrand
import com.fernando.centraldomotorista.data.remote.api.OverpassApi
import com.fernando.centraldomotorista.data.remote.dto.NearbyGasStation
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.GasStationRepository
import com.fernando.centraldomotorista.util.LocationHelper
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
    // GPS & Mapa de Postos Próximos
    val isLocatingGps: Boolean = false,
    val showNearbyStationsDialog: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val nearbyStations: List<NearbyGasStation> = emptyList(),
    val selectedNearbyStation: NearbyGasStation? = null,
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
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val selectedFuelTypes: Set<String> = setOf("Gasolina Comum", "Etanol")
)

class GasStationViewModel(
    private val repository: GasStationRepository = GasStationRepository(),
    private val viaCepApi: com.fernando.centraldomotorista.data.remote.api.ViaCepApi = com.fernando.centraldomotorista.data.remote.api.ViaCepApi.instance,
    private val overpassApi: OverpassApi = OverpassApi.instance
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

    // -------------------------------------------------------------
    // GPS & Busca de Postos Próximos
    // -------------------------------------------------------------
    fun findNearbyGasStations(context: Context) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLocatingGps = true,
                    error = null
                )
            }

            val location = LocationHelper.getCurrentLocation(context)
            if (location == null) {
                _uiState.update {
                    it.copy(
                        isLocatingGps = false,
                        error = "Não foi possível obter sua localização GPS. Verifique se o GPS está ativo e com permissão concedida."
                    )
                }
                return@launch
            }

            val userLat = location.latitude
            val userLon = location.longitude

            _uiState.update {
                it.copy(
                    userLatitude = userLat,
                    userLongitude = userLon,
                    showNearbyStationsDialog = true
                )
            }

            try {
                // Query Overpass: Raio de 5000 metros (5km)
                val query = """
                    [out:json][timeout:25];
                    (
                      node["amenity"="fuel"](around:5000,$userLat,$userLon);
                      way["amenity"="fuel"](around:5000,$userLat,$userLon);
                    );
                    out center tags;
                """.trimIndent()

                val response = overpassApi.getNearbyGasStations(query)
                val elements = response.elements ?: emptyList()

                val mappedList = elements.mapNotNull { element ->
                    val lat = element.latitude ?: return@mapNotNull null
                    val lon = element.longitude ?: return@mapNotNull null
                    val tags = element.tags ?: emptyMap()

                    val rawName = tags["name"] ?: tags["operator"] ?: tags["brand"] ?: "Posto de Combustível"
                    val brand = LocationHelper.normalizeBrand(tags["brand"], rawName, tags["operator"])
                    val street = tags["addr:street"]
                    val number = tags["addr:housenumber"]
                    val neighborhood = tags["addr:suburb"] ?: tags["addr:district"]
                    val city = tags["addr:city"]
                    val state = tags["addr:state"]
                    val cep = tags["addr:postcode"]
                    val distance = LocationHelper.calculateDistanceMeters(userLat, userLon, lat, lon)
                    val fuelTypes = LocationHelper.detectFuelTypes(tags)

                    val addressParts = listOfNotNull(
                        street?.let { if (!number.isNullOrBlank()) "$it, $number" else it },
                        neighborhood,
                        city?.let { if (!state.isNullOrBlank()) "$it - $state" else it }
                    ).joinToString(", ")

                    NearbyGasStation(
                        id = "${element.type}_${element.id}",
                        name = rawName,
                        brand = brand,
                        street = street,
                        number = number,
                        neighborhood = neighborhood,
                        city = city,
                        state = state,
                        cep = cep,
                        fullAddress = addressParts.ifBlank { "Coordenadas: ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lon)}" },
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = distance,
                        fuelTypes = fuelTypes
                    )
                }.sortedBy { it.distanceMeters }

                _uiState.update {
                    it.copy(
                        isLocatingGps = false,
                        nearbyStations = mappedList,
                        message = if (mappedList.isEmpty()) "Nenhum posto encontrado no raio de 5km." else "${mappedList.size} postos encontrados próximos a você!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLocatingGps = false,
                        error = "Erro ao buscar postos no mapa: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectNearbyStation(context: Context, station: NearbyGasStation) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedNearbyStation = station,
                    name = station.name,
                    brand = station.brand,
                    street = station.street ?: it.street,
                    number = station.number ?: it.number,
                    neighborhood = station.neighborhood ?: it.neighborhood,
                    city = station.city ?: it.city,
                    state = station.state ?: it.state,
                    cep = station.cep ?: it.cep,
                    latitude = BigDecimal.valueOf(station.latitude),
                    longitude = BigDecimal.valueOf(station.longitude),
                    selectedFuelTypes = if (station.fuelTypes.isNotEmpty()) station.fuelTypes.toSet() else it.selectedFuelTypes,
                    showNearbyStationsDialog = false,
                    message = "Dados do posto '${station.name}' carregados no formulário!"
                )
            }

            // Se o endereço estiver incompleto (ex: sem rua ou bairro), tenta enriquecer com o Geocoder reverso
            if (station.street.isNullOrBlank() || station.city.isNullOrBlank()) {
                val geocoded = LocationHelper.reverseGeocode(context, station.latitude, station.longitude)
                if (geocoded != null) {
                    _uiState.update { state ->
                        state.copy(
                            street = if (state.street.isBlank()) geocoded.thoroughfare ?: state.street else state.street,
                            number = if (state.number.isBlank()) geocoded.subThoroughfare ?: state.number else state.number,
                            neighborhood = if (state.neighborhood.isBlank()) geocoded.subLocality ?: state.neighborhood else state.neighborhood,
                            city = if (state.city.isBlank()) geocoded.locality ?: state.city else state.city,
                            state = if (state.state.isBlank()) geocoded.adminArea ?: state.state else state.state,
                            cep = if (state.cep.isBlank()) geocoded.postalCode ?: state.cep else state.cep
                        )
                    }
                }
            }
        }
    }

    fun closeNearbyStationsDialog() {
        _uiState.update { it.copy(showNearbyStationsDialog = false) }
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
                latitude = station.latitude,
                longitude = station.longitude,
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
                latitude = null,
                longitude = null,
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
                latitude = state.latitude,
                longitude = state.longitude,
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

