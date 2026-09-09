package com.fernando.centraldomotorista.ui.screens.deliverypartners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.remote.api.ViaCepApi
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import com.fernando.centraldomotorista.ui.utils.isValidCpf
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Locale

enum class PartnerStatusFilter {
    ALL, ACTIVE, INACTIVE
}

data class DeliveryPartnerFormData(
    val id: String? = null,
    val fullName: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val phone: String = "",
    val isWhatsapp: Boolean = false,
    val socialMedia: String = "",
    val pixKey: String = "",
    val pixBank: String = "",
    val cpf: String = "",
    val preferredRouteId: String? = null,
    val packageRateText: String = "0,00",
    val defaultBonusText: String = "0,00",
    val deliveryType: String = "moto", // "a_pe", "bike", "moto", "carro", "utilitario"
    val rating: Int = 3,
    val paymentCycleType: String = "fixed", // "fixed" | "variable"
    val paymentCycleFixed: String = "semanal", // "semanal" | "quinzenal" | "mensal"
    val paymentCycleVariableDays: List<Int> = listOf(7, 7, 15, 15),
    val active: Boolean = true
) {
    val isDirty: Boolean
        get() = this != DeliveryPartnerFormData()
}

data class DeliveryPartnersUiState(
    val partners: List<DeliveryPartner> = emptyList(),
    val routes: List<DeliveryRoute> = emptyList(),
    val partnerSessions: List<DeliveryPartnerSession> = emptyList(),
    val activeSessionsMap: Map<String, DeliveryPartnerSession> = emptyMap(),
    val isLoadingSessions: Boolean = false,
    val searchQuery: String = "",
    val statusFilter: PartnerStatusFilter = PartnerStatusFilter.ALL,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSearchingCep: Boolean = false,
    val isFormOpen: Boolean = false,
    val showDiscardAlert: Boolean = false,
    val formData: DeliveryPartnerFormData = DeliveryPartnerFormData(),
    val initialFormData: DeliveryPartnerFormData = DeliveryPartnerFormData(),
    val cpfError: String? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isFormDirty: Boolean
        get() = formData != initialFormData
}

class DeliveryPartnersViewModel(
    private val partnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: DeliveryRouteRepository = DeliveryRouteRepository(),
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository(),
    private val viaCepApi: ViaCepApi = ViaCepApi.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryPartnersUiState())
    val uiState: StateFlow<DeliveryPartnersUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadData()
        viewModelScope.launch {
            AppDataSync.dataChangedEvents.collect {
                loadData()
                _uiState.value.formData.id?.let { partnerId ->
                    loadPartnerSessions(partnerId)
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val partners = partnerRepository.getDeliveryPartners(currentUserId)
            val routes = routeRepository.getDeliveryRoutes(currentUserId)
            val sessions = sessionRepository.getSessions(currentUserId)
            val activeSessions = sessions.filter { it.endTime == null }.associateBy { it.partnerId }

            _uiState.update {
                it.copy(
                    partners = partners,
                    routes = routes,
                    activeSessionsMap = activeSessions,
                    isLoading = false
                )
            }
        }
    }

    fun loadPartnerSessions(partnerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true) }
            val sessions = sessionRepository.getSessionsForPartner(currentUserId, partnerId)
            _uiState.update {
                it.copy(
                    partnerSessions = sessions,
                    isLoadingSessions = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChanged(filter: PartnerStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun openCreateForm() {
        val initial = DeliveryPartnerFormData()
        _uiState.update {
            it.copy(
                isFormOpen = true,
                formData = initial,
                initialFormData = initial,
                showDiscardAlert = false,
                cpfError = null,
                error = null
            )
        }
    }

    fun openEditForm(partner: DeliveryPartner) {
        val initial = DeliveryPartnerFormData(
            id = partner.id,
            fullName = partner.fullName,
            cep = partner.cep?.filter { it.isDigit() } ?: "",
            street = partner.street ?: "",
            number = partner.number ?: "",
            neighborhood = partner.neighborhood ?: "",
            city = partner.city ?: "",
            state = partner.state ?: "",
            phone = partner.phone?.filter { it.isDigit() } ?: "",
            isWhatsapp = partner.isWhatsapp,
            socialMedia = partner.socialMedia ?: "",
            pixKey = partner.pixKey ?: "",
            pixBank = partner.pixBank ?: "",
            cpf = partner.cpf?.filter { it.isDigit() } ?: "",
            preferredRouteId = partner.preferredRouteId,
            packageRateText = String.format(Locale("pt", "BR"), "%.2f", partner.packageRate),
            defaultBonusText = String.format(Locale("pt", "BR"), "%.2f", partner.defaultBonus),
            deliveryType = partner.deliveryType,
            rating = partner.rating,
            paymentCycleType = partner.paymentCycleType,
            paymentCycleFixed = partner.paymentCycleFixed ?: "semanal",
            paymentCycleVariableDays = partner.paymentCycleVariableDays ?: listOf(7, 7, 15, 15),
            active = partner.active
        )
        _uiState.update {
            it.copy(
                isFormOpen = true,
                formData = initial,
                initialFormData = initial,
                showDiscardAlert = false,
                cpfError = null,
                error = null,
                partnerSessions = emptyList()
            )
        }
        loadPartnerSessions(partner.id)
    }

    fun requestCloseForm() {
        val state = _uiState.value
        if (state.isFormDirty) {
            _uiState.update { it.copy(showDiscardAlert = true) }
        } else {
            forceCloseForm()
        }
    }

    fun forceCloseForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                showDiscardAlert = false,
                formData = DeliveryPartnerFormData(),
                initialFormData = DeliveryPartnerFormData(),
                cpfError = null,
                error = null
            )
        }
    }

    fun dismissDiscardAlert() {
        _uiState.update { it.copy(showDiscardAlert = false) }
    }

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(formData = it.formData.copy(fullName = name)) }
    }

    fun onCepChanged(cepInput: String) {
        val digits = cepInput.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(formData = it.formData.copy(cep = digits)) }

        if (digits.length == 8) {
            searchCep(digits)
        }
    }

    private fun searchCep(cepDigits: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCep = true) }
            try {
                val result = viaCepApi.getAddressByCep(cepDigits)
                if (result.erro != true) {
                    _uiState.update { state ->
                        state.copy(
                            isSearchingCep = false,
                            formData = state.formData.copy(
                                street = result.logradouro ?: state.formData.street,
                                neighborhood = result.bairro ?: state.formData.neighborhood,
                                city = result.localidade ?: state.formData.city,
                                state = result.uf ?: state.formData.state
                            )
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

    fun onStreetChanged(street: String) = _uiState.update { it.copy(formData = it.formData.copy(street = street)) }
    fun onNumberChanged(number: String) = _uiState.update { it.copy(formData = it.formData.copy(number = number)) }
    fun onNeighborhoodChanged(neighborhood: String) = _uiState.update { it.copy(formData = it.formData.copy(neighborhood = neighborhood)) }
    fun onCityChanged(city: String) = _uiState.update { it.copy(formData = it.formData.copy(city = city)) }
    fun onStateChanged(state: String) = _uiState.update { it.copy(formData = it.formData.copy(state = state)) }

    fun onPhoneChanged(phone: String) {
        val digits = phone.filter { it.isDigit() }.take(11)
        _uiState.update { it.copy(formData = it.formData.copy(phone = digits)) }
    }

    fun onIsWhatsappChanged(isWhatsapp: Boolean) {
        _uiState.update { it.copy(formData = it.formData.copy(isWhatsapp = isWhatsapp)) }
    }

    fun onSocialMediaChanged(socialMedia: String) {
        _uiState.update { it.copy(formData = it.formData.copy(socialMedia = socialMedia)) }
    }

    fun onPixKeyChanged(pixKey: String) {
        _uiState.update { it.copy(formData = it.formData.copy(pixKey = pixKey)) }
    }

    fun onPixBankChanged(pixBank: String) {
        _uiState.update { it.copy(formData = it.formData.copy(pixBank = pixBank)) }
    }

    fun onCpfChanged(cpfInput: String) {
        val digits = cpfInput.filter { it.isDigit() }.take(11)
        val error = if (digits.length == 11 && !isValidCpf(digits)) {
            "CPF inválido"
        } else {
            null
        }
        _uiState.update {
            it.copy(
                formData = it.formData.copy(cpf = digits),
                cpfError = error
            )
        }
    }

    fun onPreferredRouteChanged(routeId: String?) {
        _uiState.update { it.copy(formData = it.formData.copy(preferredRouteId = routeId)) }
    }

    fun onPackageRateChanged(rateText: String) {
        val clean = rateText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(formData = it.formData.copy(packageRateText = clean)) }
    }

    fun onDefaultBonusChanged(bonusText: String) {
        val clean = bonusText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(formData = it.formData.copy(defaultBonusText = clean)) }
    }

    fun onDeliveryTypeChanged(type: String) {
        _uiState.update { it.copy(formData = it.formData.copy(deliveryType = type)) }
    }

    fun onRatingChanged(rating: Int) {
        _uiState.update { it.copy(formData = it.formData.copy(rating = rating.coerceIn(1, 5))) }
    }

    fun onPaymentCycleTypeChanged(type: String) {
        _uiState.update { it.copy(formData = it.formData.copy(paymentCycleType = type)) }
    }

    fun onPaymentCycleFixedChanged(cycle: String) {
        _uiState.update { it.copy(formData = it.formData.copy(paymentCycleFixed = cycle)) }
    }

    fun addVariableCycleDay(days: Int) {
        if (days <= 0) return
        val currentList = _uiState.value.formData.paymentCycleVariableDays.toMutableList()
        currentList.add(days)
        _uiState.update { it.copy(formData = it.formData.copy(paymentCycleVariableDays = currentList)) }
    }

    fun removeVariableCycleDay(index: Int) {
        val currentList = _uiState.value.formData.paymentCycleVariableDays.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.update { it.copy(formData = it.formData.copy(paymentCycleVariableDays = currentList)) }
        }
    }

    fun moveVariableCycleDay(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.formData.paymentCycleVariableDays.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.update { it.copy(formData = it.formData.copy(paymentCycleVariableDays = currentList)) }
        }
    }

    fun onActiveChanged(active: Boolean) {
        _uiState.update { it.copy(formData = it.formData.copy(active = active)) }
    }

    fun togglePartnerActive(partner: DeliveryPartner) {
        viewModelScope.launch {
            val newStatus = !partner.active
            // Atualização otimista
            _uiState.update { state ->
                state.copy(
                    partners = state.partners.map {
                        if (it.id == partner.id) it.copy(active = newStatus) else it
                    }
                )
            }
            val success = partnerRepository.updateActiveStatus(partner, newStatus)
            if (!success) {
                // Reverter se falhar
                _uiState.update { state ->
                    state.copy(
                        partners = state.partners.map {
                            if (it.id == partner.id) it.copy(active = partner.active) else it
                        },
                        error = "Erro ao alterar status do entregador."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(message = "Status de ${partner.fullName} atualizado!")
                }
            }
        }
    }

    fun savePartner(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        val form = state.formData

        if (form.fullName.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome completo do entregador.") }
            return
        }

        if (form.cpf.isNotBlank() && !isValidCpf(form.cpf)) {
            _uiState.update { it.copy(error = "CPF inválido. Verifique os números digitados.", cpfError = "CPF inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val packageRate = parseCurrency(form.packageRateText)
            val defaultBonus = parseCurrency(form.defaultBonusText)

            val partner = DeliveryPartner(
                id = form.id ?: "",
                userId = currentUserId,
                fullName = form.fullName.trim(),
                cep = form.cep.ifBlank { null },
                street = form.street.ifBlank { null },
                number = form.number.ifBlank { null },
                neighborhood = form.neighborhood.ifBlank { null },
                city = form.city.ifBlank { null },
                state = form.state.ifBlank { null },
                phone = form.phone.ifBlank { null },
                isWhatsapp = form.isWhatsapp,
                socialMedia = form.socialMedia.ifBlank { null },
                pixKey = form.pixKey.ifBlank { null },
                pixBank = form.pixBank.ifBlank { null },
                cpf = form.cpf.ifBlank { null },
                preferredRouteId = form.preferredRouteId?.ifBlank { null },
                packageRate = packageRate,
                defaultBonus = defaultBonus,
                deliveryType = form.deliveryType,
                rating = form.rating,
                paymentCycleType = form.paymentCycleType,
                paymentCycleFixed = if (form.paymentCycleType == "fixed") form.paymentCycleFixed else null,
                paymentCycleVariableDays = if (form.paymentCycleType == "variable") form.paymentCycleVariableDays else null,
                active = form.active
            )

            try {
                partnerRepository.saveDeliveryPartner(partner)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (form.id != null) "Entregador atualizado com sucesso!" else "Entregador cadastrado com sucesso!"
                    )
                }
                loadData()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar entregador: ${e.message}") }
            }
        }
    }

    fun deletePartner(partnerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = partnerRepository.deleteDeliveryPartner(partnerId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        message = "Entregador excluído com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir entregador.") }
            }
        }
    }

    private fun parseCurrency(text: String): BigDecimal {
        val clean = text.filter { it.isDigit() || it == ',' || it == '.' }.trim()
        if (clean.isBlank()) return BigDecimal.ZERO
        val normalized = if (clean.contains(',')) {
            clean.replace(".", "").replace(',', '.')
        } else {
            clean
        }
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
