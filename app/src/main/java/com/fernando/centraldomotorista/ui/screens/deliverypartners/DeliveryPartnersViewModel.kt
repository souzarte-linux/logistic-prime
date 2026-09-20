package com.fernando.centraldomotorista.ui.screens.deliverypartners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CycleEntry
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

val WEEK_DAYS = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")

enum class PartnerStatusFilter {
    ALL, ACTIVE, INACTIVE
}

data class FormCycleEntry(
    val id: String = UUID.randomUUID().toString(),
    val cutText: String = "1",
    val payDelayText: String = "7"
) {
    val cut: Int get() = cutText.toIntOrNull() ?: 1
    val payDelay: Int get() = payDelayText.toIntOrNull() ?: 0
}

data class VariableCycleFormEntry(
    val id: String = UUID.randomUUID().toString(),
    val startDate: String = "2026-09-01",
    val endDate: String = "2026-09-07",
    val includeEndDate: Boolean = true,
    val paymentDelayDaysText: String = "7"
) {
    val paymentDelayDays: Int
        get() = paymentDelayDaysText.toIntOrNull() ?: 7

    val startDateParsed: LocalDate?
        get() = runCatching { LocalDate.parse(startDate) }.getOrNull()

    val endDateParsed: LocalDate?
        get() = runCatching { LocalDate.parse(endDate) }.getOrNull()

    val calculatedPaymentDate: LocalDate?
        get() {
            val end = endDateParsed ?: return null
            return end.plusDays(paymentDelayDays.toLong())
        }

    val formattedStartDate: String
        get() = startDateParsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: startDate

    val formattedEndDate: String
        get() = endDateParsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: endDate

    val formattedPaymentDateText: String
        get() {
            val date = calculatedPaymentDate ?: return "Data a definir"
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
            val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            return "Seu pagamento será realizado em: $dayOfWeek, $formattedDate."
        }
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
    val cycle: String = "semanal", // "semanal" | "quinzenal" | "mensal" | "misto"
    val paymentDay: String = "QUA",
    val fixedPayDelayText: String = "7",
    val cycleEntries: List<FormCycleEntry> = listOf(
        FormCycleEntry(cutText = "1", payDelayText = "7"),
        FormCycleEntry(cutText = "16", payDelayText = "7")
    ),
    val paymentCycleType: String = "fixed", // "fixed" | "variable"
    val paymentCycleFixed: String = "semanal", // "semanal" | "quinzenal" | "mensal"
    val paymentCycleVariableDays: List<Int> = listOf(1, 7, 16, 7),
    val cycleStartDate: String = "2026-09-01",
    val cycleEndDate: String = "2026-09-07",
    val includeEndDate: Boolean = true,
    val paymentDelayDaysText: String = "7",
    val variableCycles: List<VariableCycleFormEntry> = listOf(VariableCycleFormEntry()),
    val active: Boolean = true,
    val photoUrl: String = ""
) {
    val fixedPayDelay: Int
        get() = fixedPayDelayText.toIntOrNull() ?: 7

    val paymentDelayDays: Int
        get() = paymentDelayDaysText.toIntOrNull() ?: 7

    val cycleStartDateParsed: LocalDate?
        get() = runCatching { LocalDate.parse(cycleStartDate) }.getOrNull()

    val cycleEndDateParsed: LocalDate?
        get() = runCatching { LocalDate.parse(cycleEndDate) }.getOrNull()

    val calculatedPaymentDate: LocalDate?
        get() {
            val end = cycleEndDateParsed ?: return null
            return end.plusDays(paymentDelayDays.toLong())
        }

    val formattedCycleStartDate: String
        get() = cycleStartDateParsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: cycleStartDate

    val formattedCycleEndDate: String
        get() = cycleEndDateParsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: cycleEndDate

    val formattedPaymentDateText: String
        get() {
            val date = calculatedPaymentDate ?: return "Data a definir"
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
            val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            return "Seu pagamento será realizado em: $dayOfWeek, $formattedDate."
        }

    val isDirty: Boolean
        get() = this != DeliveryPartnerFormData()
}

data class DeliveryPartnersUiState(
    val partners: List<DeliveryPartner> = emptyList(),
    val routes: List<DeliveryRoute> = emptyList(),
    val partnerSessions: List<DeliveryPartnerSession> = emptyList(),
    val activeSessionsMap: Map<String, DeliveryPartnerSession> = emptyMap(),
    val sessionsByPartnerMap: Map<String, List<DeliveryPartnerSession>> = emptyMap(),
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
            val sessionsByPartner = sessions.groupBy { it.partnerId }

            _uiState.update {
                it.copy(
                    partners = partners,
                    routes = routes,
                    activeSessionsMap = activeSessions,
                    sessionsByPartnerMap = sessionsByPartner,
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
        val mappedCycle = if (partner.paymentCycleType == "variable") {
            "misto"
        } else {
            partner.paymentCycleFixed?.lowercase() ?: "semanal"
        }

        val mappedEntries: List<FormCycleEntry> = if (!partner.paymentCycleVariableDays.isNullOrEmpty()) {
            val list = partner.paymentCycleVariableDays
            if (list.size >= 2 && list.size % 2 == 0) {
                list.chunked(2).map {
                    FormCycleEntry(
                        cutText = it[0].coerceIn(1, 31).toString(),
                        payDelayText = it[1].coerceAtLeast(0).toString()
                    )
                }
            } else {
                list.map { FormCycleEntry(cutText = it.coerceIn(1, 31).toString(), payDelayText = "7") }
            }
        } else {
            listOf(FormCycleEntry(cutText = "1", payDelayText = "7"), FormCycleEntry(cutText = "16", payDelayText = "7"))
        }

        val mappedVariableCycles = if (!partner.variableCycles.isNullOrEmpty()) {
            partner.variableCycles.map {
                VariableCycleFormEntry(
                    id = it.id.ifBlank { UUID.randomUUID().toString() },
                    startDate = it.startDate.ifBlank { "2026-09-01" },
                    endDate = it.endDate.ifBlank { "2026-09-07" },
                    includeEndDate = it.includeEndDate,
                    paymentDelayDaysText = it.paymentDelayDays.toString()
                )
            }
        } else if (!partner.cycleStartDate.isNullOrBlank() && !partner.cycleEndDate.isNullOrBlank()) {
            listOf(
                VariableCycleFormEntry(
                    startDate = partner.cycleStartDate,
                    endDate = partner.cycleEndDate,
                    includeEndDate = partner.includeEndDate,
                    paymentDelayDaysText = partner.paymentDelayDays.toString()
                )
            )
        } else {
            listOf(VariableCycleFormEntry())
        }

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
            cycle = mappedCycle,
            paymentDay = "QUA",
            fixedPayDelayText = "7",
            cycleEntries = mappedEntries,
            paymentCycleType = if (mappedCycle == "misto" || mappedCycle == "variavel") "variable" else "fixed",
            paymentCycleFixed = if (mappedCycle != "misto" && mappedCycle != "variavel") mappedCycle else "semanal",
            paymentCycleVariableDays = mappedEntries.flatMap { listOf(it.cut, it.payDelay) },
            cycleStartDate = partner.cycleStartDate ?: "2026-09-01",
            cycleEndDate = partner.cycleEndDate ?: "2026-09-07",
            includeEndDate = partner.includeEndDate,
            paymentDelayDaysText = partner.paymentDelayDays.toString(),
            variableCycles = mappedVariableCycles,
            active = partner.active,
            photoUrl = partner.photoUrl ?: ""
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

    fun onPhotoUrlChanged(url: String) {
        _uiState.update { it.copy(formData = it.formData.copy(photoUrl = url)) }
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

    fun onCycleChanged(cycle: String) {
        val mappedType = if (cycle == "misto" || cycle == "variavel") "variable" else "fixed"
        val mappedFixed = if (mappedType == "fixed") cycle else null
        _uiState.update { state ->
            val curEntries = if (state.formData.cycleEntries.isEmpty()) {
                listOf(FormCycleEntry(cutText = "1", payDelayText = "7"), FormCycleEntry(cutText = "16", payDelayText = "7"))
            } else {
                state.formData.cycleEntries
            }
            state.copy(
                formData = state.formData.copy(
                    cycle = cycle,
                    paymentCycleType = mappedType,
                    paymentCycleFixed = mappedFixed ?: "semanal",
                    cycleEntries = curEntries,
                    paymentCycleVariableDays = curEntries.flatMap { listOf(it.cut, it.payDelay) }
                )
            )
        }
    }

    fun onCycleSelectionChanged(cycleKey: String) = onCycleChanged(cycleKey)

    fun onPaymentDayChanged(day: String) {
        _uiState.update { it.copy(formData = it.formData.copy(paymentDay = day)) }
    }

    fun onFixedPayDelayTextChanged(text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(formData = it.formData.copy(fixedPayDelayText = filtered)) }
    }

    fun onFixedPayDelayChanged(delay: Int) {
        _uiState.update { it.copy(formData = it.formData.copy(fixedPayDelayText = delay.toString())) }
    }

    fun onCycleStartDateSelected(date: LocalDate) {
        _uiState.update { it.copy(formData = it.formData.copy(cycleStartDate = date.toString())) }
    }

    fun onCycleEndDateSelected(date: LocalDate) {
        _uiState.update { it.copy(formData = it.formData.copy(cycleEndDate = date.toString())) }
    }

    fun onIncludeEndDateChanged(include: Boolean) {
        _uiState.update { it.copy(formData = it.formData.copy(includeEndDate = include)) }
    }

    fun onPaymentDelayDaysChanged(text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(formData = it.formData.copy(paymentDelayDaysText = filtered)) }
    }

    fun clearPaymentDelayDays() {
        _uiState.update { it.copy(formData = it.formData.copy(paymentDelayDaysText = "")) }
    }

    fun addVariableCycle() {
        _uiState.update { state ->
            val lastCycle = state.formData.variableCycles.lastOrNull()
            val newStartDate = lastCycle?.endDateParsed?.plusDays(1)?.toString() ?: "2026-09-08"
            val newEndDate = lastCycle?.endDateParsed?.plusDays(7)?.toString() ?: "2026-09-14"
            val newCycle = VariableCycleFormEntry(
                startDate = newStartDate,
                endDate = newEndDate,
                includeEndDate = true,
                paymentDelayDaysText = "7"
            )
            state.copy(formData = state.formData.copy(variableCycles = state.formData.variableCycles + newCycle))
        }
    }

    fun removeVariableCycle(index: Int) {
        _uiState.update { state ->
            if (state.formData.variableCycles.size <= 1) return@update state
            val updated = state.formData.variableCycles.filterIndexed { i, _ -> i != index }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun updateVariableCycleStartDate(index: Int, date: LocalDate) {
        _uiState.update { state ->
            val updated = state.formData.variableCycles.mapIndexed { i, cycle ->
                if (i == index) cycle.copy(startDate = date.toString()) else cycle
            }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun updateVariableCycleEndDate(index: Int, date: LocalDate) {
        _uiState.update { state ->
            val updated = state.formData.variableCycles.mapIndexed { i, cycle ->
                if (i == index) cycle.copy(endDate = date.toString()) else cycle
            }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun updateVariableCycleIncludeEndDate(index: Int, include: Boolean) {
        _uiState.update { state ->
            val updated = state.formData.variableCycles.mapIndexed { i, cycle ->
                if (i == index) cycle.copy(includeEndDate = include) else cycle
            }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun updateVariableCyclePaymentDelayDays(index: Int, text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        _uiState.update { state ->
            val updated = state.formData.variableCycles.mapIndexed { i, cycle ->
                if (i == index) cycle.copy(paymentDelayDaysText = filtered) else cycle
            }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun clearVariableCyclePaymentDelayDays(index: Int) {
        _uiState.update { state ->
            val updated = state.formData.variableCycles.mapIndexed { i, cycle ->
                if (i == index) cycle.copy(paymentDelayDaysText = "") else cycle
            }
            state.copy(formData = state.formData.copy(variableCycles = updated))
        }
    }

    fun addCycleEntry() {
        _uiState.update { state ->
            val updated = state.formData.cycleEntries + FormCycleEntry(cutText = "1", payDelayText = "7")
            val flatDays = updated.flatMap { listOf(it.cut, it.payDelay) }
            state.copy(
                formData = state.formData.copy(
                    cycleEntries = updated,
                    paymentCycleVariableDays = flatDays
                )
            )
        }
    }

    fun removeCycleEntry(index: Int) {
        _uiState.update { state ->
            if (state.formData.cycleEntries.size <= 1) return@update state
            val updated = state.formData.cycleEntries.filterIndexed { i, _ -> i != index }
            val flatDays = updated.flatMap { listOf(it.cut, it.payDelay) }
            state.copy(
                formData = state.formData.copy(
                    cycleEntries = updated,
                    paymentCycleVariableDays = flatDays
                )
            )
        }
    }

    fun updateCycleCutText(index: Int, text: String) {
        val filtered = text.filter { it.isDigit() }.take(2)
        _uiState.update { state ->
            val updated = state.formData.cycleEntries.mapIndexed { i, entry ->
                if (i == index) entry.copy(cutText = filtered) else entry
            }
            val flatDays = updated.flatMap { listOf(it.cut, it.payDelay) }
            state.copy(
                formData = state.formData.copy(
                    cycleEntries = updated,
                    paymentCycleVariableDays = flatDays
                )
            )
        }
    }

    fun updateCyclePayDelayText(index: Int, text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        _uiState.update { state ->
            val updated = state.formData.cycleEntries.mapIndexed { i, entry ->
                if (i == index) entry.copy(payDelayText = filtered) else entry
            }
            val flatDays = updated.flatMap { listOf(it.cut, it.payDelay) }
            state.copy(
                formData = state.formData.copy(
                    cycleEntries = updated,
                    paymentCycleVariableDays = flatDays
                )
            )
        }
    }

    fun updateCycleEntry(index: Int, cut: Int? = null, payDelay: Int? = null) {
        _uiState.update { state ->
            val updated = state.formData.cycleEntries.mapIndexed { i, entry ->
                if (i == index) {
                    entry.copy(
                        cutText = cut?.toString() ?: entry.cutText,
                        payDelayText = payDelay?.toString() ?: entry.payDelayText
                    )
                } else entry
            }
            val flatDays = updated.flatMap { listOf(it.cut, it.payDelay) }
            state.copy(
                formData = state.formData.copy(
                    cycleEntries = updated,
                    paymentCycleVariableDays = flatDays
                )
            )
        }
    }

    fun updateCycleEntry(index: Int, cut: Int, payDelay: Int) {
        updateCycleEntry(index, cut as Int?, payDelay as Int?)
    }

    fun addVariableCycleDay(days: Int) = addCycleEntry()
    fun updateVariableCycleDay(index: Int, days: Int) = updateCycleEntry(index, payDelay = days)
    fun removeVariableCycleDay(index: Int) = removeCycleEntry(index)

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
                paymentCycleType = if (form.cycle == "misto" || form.cycle == "variavel") "variable" else "fixed",
                paymentCycleFixed = if (form.cycle != "misto" && form.cycle != "variavel") form.cycle else null,
                paymentCycleVariableDays = if (form.cycle == "misto" || form.cycle == "variavel") {
                    form.cycleEntries
                        .map { CycleEntry(cut = if (it.cut in 1..31) it.cut else 1, payDelay = if (it.payDelay >= 0) it.payDelay else 7) }
                        .sortedBy { it.cut }
                        .flatMap { listOf(it.cut, it.payDelay) }
                } else null,
                cycleStartDate = if (form.cycle == "misto" || form.cycle == "variavel") {
                    form.variableCycles.firstOrNull()?.startDate ?: form.cycleStartDate
                } else null,
                cycleEndDate = if (form.cycle == "misto" || form.cycle == "variavel") {
                    form.variableCycles.firstOrNull()?.endDate ?: form.cycleEndDate
                } else null,
                includeEndDate = form.variableCycles.firstOrNull()?.includeEndDate ?: form.includeEndDate,
                paymentDelayDays = form.variableCycles.firstOrNull()?.paymentDelayDays ?: form.paymentDelayDays,
                paymentDate = if (form.cycle == "misto" || form.cycle == "variavel") {
                    form.variableCycles.firstOrNull()?.calculatedPaymentDate?.toString() ?: form.calculatedPaymentDate?.toString()
                } else null,
                variableCycles = if (form.cycle == "misto" || form.cycle == "variavel") {
                    form.variableCycles.map {
                        com.fernando.centraldomotorista.data.model.VariableCycleItem(
                            id = it.id,
                            startDate = it.startDate,
                            endDate = it.endDate,
                            includeEndDate = it.includeEndDate,
                            paymentDelayDays = it.paymentDelayDays,
                            paymentDate = it.calculatedPaymentDate?.toString()
                        )
                    }
                } else null,
                active = form.active,
                photoUrl = form.photoUrl.trim().ifBlank { null }
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
