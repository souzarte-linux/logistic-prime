package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class PartnerRoutesUiState(
    val partner: DeliveryPartner? = null,
    val routes: List<DeliveryRoute> = emptyList(),
    val sessions: List<DeliveryPartnerSession> = emptyList(),
    val monthGroups: List<PartnerSessionMonthGroup> = emptyList(),
    val expandedMonths: Set<String> = emptySet(),
    val expandedWeeks: Set<String> = emptySet(),
    val periodFilter: PartnerPeriodFilter = PartnerPeriodFilter(),
    val isPeriodDropdownExpanded: Boolean = false,
    val monthDeliveredCount: Int = 0,
    val monthTotalAmountPaid: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val editingSession: DeliveryPartnerSession? = null,
    val deletingSession: DeliveryPartnerSession? = null,
    val viewDetailSession: DeliveryPartnerSession? = null
)

class PartnerRoutesViewModel(
    private val partnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: DeliveryRouteRepository = DeliveryRouteRepository(),
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val externalScope: CoroutineScope? = null,
    observeDataSync: Boolean = true
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(PartnerRoutesUiState())
    val uiState: StateFlow<PartnerRoutesUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    private var currentPartnerId: String = ""

    init {
        if (observeDataSync) {
            scope.launch {
                AppDataSync.dataChangedEvents.collect {
                    if (currentPartnerId.isNotBlank()) {
                        loadData(currentPartnerId)
                    }
                }
            }
        }
    }

    fun loadData(partnerId: String) {
        currentPartnerId = partnerId
        _uiState.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val partners = partnerRepository.getDeliveryPartners(currentUserId)
                val targetPartner = partners.firstOrNull { it.id == partnerId }
                val routes = routeRepository.getDeliveryRoutes(currentUserId)
                val allSessions = sessionRepository.getSessionsForPartner(currentUserId, partnerId)
                    .sortedWith(
                        compareByDescending<DeliveryPartnerSession> { it.startTime ?: it.createdAt }
                            .thenByDescending { it.id }
                    )

                val today = LocalDate.now()
                val filter = _uiState.value.periodFilter
                val (monthGroups, deliveredTotal, amountPaidTotal) = withContext(Dispatchers.Default) {
                    buildHierarchy(allSessions, filter, today)
                }

                val initialExpandedMonths = resolveInitialExpandedMonths(monthGroups, _uiState.value.expandedMonths)
                val initialExpandedWeeks = resolveInitialExpandedWeeks(monthGroups, _uiState.value.expandedWeeks)

                _uiState.update {
                    it.copy(
                        partner = targetPartner,
                        routes = routes,
                        sessions = allSessions,
                        monthGroups = monthGroups,
                        expandedMonths = initialExpandedMonths,
                        expandedWeeks = initialExpandedWeeks,
                        monthDeliveredCount = deliveredTotal,
                        monthTotalAmountPaid = amountPaidTotal,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PartnerRoutesVM", "Erro ao carregar dados do parceiro: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados do parceiro.") }
            }
        }
    }

    fun applyPeriodPreset(preset: PartnerPeriodPreset) {
        val currentFilter = _uiState.value.periodFilter.copy(preset = preset)
        _uiState.update { it.copy(periodFilter = currentFilter, isPeriodDropdownExpanded = false) }
        recalculateFilteredData()
    }

    fun applyCustomPeriod(start: LocalDate, end: LocalDate) {
        val today = LocalDate.now()
        if (end.isBefore(start)) {
            _uiState.update {
                it.copy(
                    error = "Data final não pode ser anterior à data inicial.",
                    isPeriodDropdownExpanded = false
                )
            }
            return
        }
        if (end.isAfter(today)) {
            _uiState.update {
                it.copy(
                    error = "Data final não pode ser posterior a hoje.",
                    isPeriodDropdownExpanded = false
                )
            }
            return
        }

        val filter = PartnerPeriodFilter(
            preset = PartnerPeriodPreset.PERSONALIZADO,
            customStart = start,
            customEnd = end
        )
        _uiState.update { it.copy(periodFilter = filter, isPeriodDropdownExpanded = false) }
        recalculateFilteredData()
    }

    fun resetPeriodFilter() {
        applyPeriodPreset(PartnerPeriodPreset.SEMANA)
    }

    fun togglePeriodDropdown() {
        _uiState.update { it.copy(isPeriodDropdownExpanded = !it.isPeriodDropdownExpanded) }
    }

    fun closePeriodDropdown() {
        _uiState.update { it.copy(isPeriodDropdownExpanded = false) }
    }

    fun toggleMonth(monthKey: String) {
        _uiState.update { state ->
            val next = if (state.expandedMonths.contains(monthKey)) {
                state.expandedMonths - monthKey
            } else {
                state.expandedMonths + monthKey
            }
            state.copy(expandedMonths = next)
        }
    }

    fun toggleWeek(weekKey: String) {
        _uiState.update { state ->
            val next = if (state.expandedWeeks.contains(weekKey)) {
                state.expandedWeeks - weekKey
            } else {
                state.expandedWeeks + weekKey
            }
            state.copy(expandedWeeks = next)
        }
    }

    fun openEditSession(session: DeliveryPartnerSession) {
        _uiState.update { it.copy(editingSession = session) }
    }

    fun closeEditSession() {
        _uiState.update { it.copy(editingSession = null) }
    }

    fun openViewDetailSession(session: DeliveryPartnerSession) {
        _uiState.update { it.copy(viewDetailSession = session) }
    }

    fun closeViewDetailSession() {
        _uiState.update { it.copy(viewDetailSession = null) }
    }

    fun promptDeleteSession(session: DeliveryPartnerSession) {
        _uiState.update { it.copy(deletingSession = session) }
    }

    fun dismissDeleteSession() {
        _uiState.update { it.copy(deletingSession = null) }
    }

    fun saveEditedSession(updatedSession: DeliveryPartnerSession) {
        scope.launch {
            try {
                sessionRepository.saveSession(updatedSession)
                _uiState.update {
                    it.copy(
                        editingSession = null,
                        message = "Sessão atualizada com sucesso!"
                    )
                }
                loadData(currentPartnerId)
            } catch (e: Exception) {
                Log.e("PartnerRoutesVM", "Erro ao atualizar sessão: ${e.message}", e)
                _uiState.update { it.copy(error = "Erro ao atualizar sessão.") }
            }
        }
    }

    fun confirmDeleteSession(session: DeliveryPartnerSession, deleteExpenseToo: Boolean) {
        scope.launch {
            try {
                if (deleteExpenseToo && !session.expenseId.isNullOrBlank()) {
                    expenseRepository.deleteExpense(session.expenseId)
                }
                val success = sessionRepository.deleteSession(session.id)
                if (success) {
                    _uiState.update {
                        it.copy(
                            deletingSession = null,
                            message = if (deleteExpenseToo) "Sessão e despesa excluídas com sucesso!" else "Sessão excluída com sucesso!"
                        )
                    }
                    loadData(currentPartnerId)
                } else {
                    _uiState.update { it.copy(error = "Falha ao excluir sessão.") }
                }
            } catch (e: Exception) {
                Log.e("PartnerRoutesVM", "Erro ao excluir sessão: ${e.message}", e)
                _uiState.update { it.copy(error = "Erro ao excluir sessão: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun recalculateFilteredData() {
        scope.launch {
            val state = _uiState.value
            val today = LocalDate.now()
            val (monthGroups, deliveredTotal, amountPaidTotal) = withContext(Dispatchers.Default) {
                buildHierarchy(state.sessions, state.periodFilter, today)
            }

            val nextExpandedMonths = resolveInitialExpandedMonths(monthGroups, state.expandedMonths)
            val nextExpandedWeeks = resolveInitialExpandedWeeks(monthGroups, state.expandedWeeks)

            _uiState.update {
                it.copy(
                    monthGroups = monthGroups,
                    expandedMonths = nextExpandedMonths,
                    expandedWeeks = nextExpandedWeeks,
                    monthDeliveredCount = deliveredTotal,
                    monthTotalAmountPaid = amountPaidTotal
                )
            }
        }
    }

    private fun resolveInitialExpandedMonths(
        monthGroups: List<PartnerSessionMonthGroup>,
        currentExpanded: Set<String>
    ): Set<String> {
        if (monthGroups.isEmpty()) return emptySet()
        if (currentExpanded.isEmpty()) {
            val currentM = monthGroups.firstOrNull { it.isCurrentMonth } ?: monthGroups.firstOrNull()
            return if (currentM != null) setOf(currentM.monthKey) else emptySet()
        }
        val valid = currentExpanded.intersect(monthGroups.map { it.monthKey }.toSet())
        return if (valid.isNotEmpty()) valid else monthGroups.take(1).map { it.monthKey }.toSet()
    }

    private fun resolveInitialExpandedWeeks(
        monthGroups: List<PartnerSessionMonthGroup>,
        currentExpanded: Set<String>
    ): Set<String> {
        if (monthGroups.isEmpty()) return emptySet()
        val allWeekKeys = monthGroups.flatMap { it.weeks }.map { it.weekKey }.toSet()
        if (currentExpanded.isEmpty()) {
            val currentW = monthGroups.flatMap { it.weeks }.firstOrNull { it.isCurrentWeek }
                ?: monthGroups.firstOrNull()?.weeks?.firstOrNull()
            return if (currentW != null) setOf(currentW.weekKey) else emptySet()
        }
        val valid = currentExpanded.intersect(allWeekKeys)
        return if (valid.isNotEmpty()) valid else monthGroups.firstOrNull()?.weeks?.take(1)?.map { it.weekKey }?.toSet().orEmpty()
    }

    companion object {
        private val ptLocale = Locale("pt", "BR")
        private val dayShortFormatter = DateTimeFormatter.ofPattern("dd 'DE' MMM", ptLocale)
        private val ddMMFormatter = DateTimeFormatter.ofPattern("dd/MM", ptLocale)
        private val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ptLocale)

        fun buildHierarchy(
            sessions: List<DeliveryPartnerSession>,
            filter: PartnerPeriodFilter,
            today: LocalDate,
            zone: ZoneId = ZoneId.systemDefault()
        ): Triple<List<PartnerSessionMonthGroup>, Int, BigDecimal> {
            val (rangeStart, rangeEnd) = filter.resolveRange(today)

            val filteredSessions = sessions.filter { s ->
                val d = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: today
                !d.isBefore(rangeStart) && !d.isAfter(rangeEnd)
            }.sortedWith(
                compareByDescending<DeliveryPartnerSession> { (it.startTime ?: it.createdAt) }
                    .thenByDescending { it.id }
            )

            val monthsMap = mutableMapOf<String, MutableList<DeliveryPartnerSession>>()
            filteredSessions.forEach { s ->
                val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: today
                val mKey = sDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                monthsMap.getOrPut(mKey) { mutableListOf() }.add(s)
            }

            val outMonths = mutableListOf<PartnerSessionMonthGroup>()
            val sortedMonthKeys = monthsMap.keys.sortedDescending()

            sortedMonthKeys.forEach { mKey ->
                val monthSessions = monthsMap[mKey] ?: return@forEach
                val monthDate = LocalDate.parse("$mKey-01")
                val yearMonth = YearMonth.from(monthDate)
                val isCurrentMonth = monthDate.year == today.year && monthDate.monthValue == today.monthValue
                val monthLabel = monthDate.format(monthNameFormatter).uppercase()

                val weeksMap = mutableMapOf<String, MutableList<DeliveryPartnerSession>>()
                monthSessions.forEach { s ->
                    val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: today
                    val wStart = sDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val wKey = "$mKey-$wStart"
                    weeksMap.getOrPut(wKey) { mutableListOf() }.add(s)
                }

                val outWeeks = mutableListOf<PartnerSessionWeekGroup>()
                val sortedWeekKeys = weeksMap.keys.sortedDescending()

                sortedWeekKeys.forEach { wKey ->
                    val weekSessions = weeksMap[wKey] ?: return@forEach
                    val wStartDate = LocalDate.parse(wKey.substringAfter("$mKey-"))
                    val wEndDate = wStartDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    val isCurrentWeek = !today.isBefore(wStartDate) && !today.isAfter(wEndDate)

                    val monthStart = monthDate.withDayOfMonth(1)
                    val monthEnd = monthDate.with(TemporalAdjusters.lastDayOfMonth())
                    val displayStart = if (wStartDate.isBefore(monthStart)) monthStart else wStartDate
                    val displayEnd = if (wEndDate.isAfter(monthEnd)) monthEnd else wEndDate

                    val weekLabel = if (isCurrentWeek) {
                        "Esta Semana"
                    } else {
                        "Semana de ${displayStart.format(ddMMFormatter)} a ${displayEnd.format(ddMMFormatter)}"
                    }

                    val daysMap = mutableMapOf<LocalDate, MutableList<DeliveryPartnerSession>>()
                    weekSessions.forEach { s ->
                        val d = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: today
                        daysMap.getOrPut(d) { mutableListOf() }.add(s)
                    }

                    val outDays = mutableListOf<PartnerSessionDayGroup>()
                    val sortedDates = daysMap.keys.sortedDescending()

                    sortedDates.forEach { dayDate ->
                        val daySessions = daysMap[dayDate] ?: return@forEach
                        val dayLabel = when (dayDate) {
                            today -> "HOJE, ${dayDate.format(dayShortFormatter).uppercase()}"
                            today.minusDays(1) -> "ONTEM, ${dayDate.format(dayShortFormatter).uppercase()}"
                            else -> {
                                val weekday = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, ptLocale).uppercase()
                                "$weekday, ${dayDate.format(dayShortFormatter).uppercase()}"
                            }
                        }

                        val dayDelivered = daySessions.sumOf { it.deliveredCount }
                        val dayAmountPaid = daySessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }

                        outDays.add(
                            PartnerSessionDayGroup(
                                date = dayDate,
                                label = dayLabel,
                                sessions = daySessions,
                                totalDelivered = dayDelivered,
                                totalAmountPaid = dayAmountPaid
                            )
                        )
                    }

                    val weekDelivered = outDays.sumOf { it.totalDelivered }
                    val weekAmountPaid = outDays.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.totalAmountPaid) }

                    outWeeks.add(
                        PartnerSessionWeekGroup(
                            weekKey = wKey,
                            label = weekLabel,
                            startDate = displayStart,
                            endDate = displayEnd,
                            days = outDays,
                            totalDelivered = weekDelivered,
                            totalAmountPaid = weekAmountPaid,
                            isCurrentWeek = isCurrentWeek
                        )
                    )
                }

                val monthDelivered = outWeeks.sumOf { it.totalDelivered }
                val monthAmountPaid = outWeeks.fold(BigDecimal.ZERO) { acc, w -> acc.add(w.totalAmountPaid) }

                outMonths.add(
                    PartnerSessionMonthGroup(
                        monthKey = mKey,
                        label = monthLabel,
                        yearMonth = yearMonth,
                        weeks = outWeeks,
                        totalDelivered = monthDelivered,
                        totalAmountPaid = monthAmountPaid,
                        isCurrentMonth = isCurrentMonth
                    )
                )
            }

            val totalDelivered = outMonths.sumOf { it.totalDelivered }
            val totalAmountPaid = outMonths.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.totalAmountPaid) }

            return Triple(outMonths, totalDelivered, totalAmountPaid)
        }
    }
}
