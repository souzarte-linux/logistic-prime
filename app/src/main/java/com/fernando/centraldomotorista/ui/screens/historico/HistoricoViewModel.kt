package com.fernando.centraldomotorista.ui.screens.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.HistoricoRepository
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
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
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoricoTab {
    TODOS, GANHOS, DESPESAS
}

fun PeriodPreset.toSaldoCardTitle(): String = when (this) {
    PeriodPreset.DIA -> "SALDO DE HOJE"
    PeriodPreset.SEMANA -> "SALDO DA SEMANA"
    PeriodPreset.QUINZENA -> "SALDO DA QUINZENA"
    PeriodPreset.MES -> "SALDO MENSAL"
    PeriodPreset.ANO -> "SALDO ANUAL"
    PeriodPreset.PERSONALIZADO -> "SALDO DO PERÍODO"
}

data class PeriodMetrics(
    val cardTitle: String,
    val saldoPeriodo: BigDecimal,
    val entradasPeriodo: BigDecimal,
    val saidasPeriodo: BigDecimal,
    val metaPeriodo: BigDecimal,
    val metaPercent: Int,
    val faltamParaMeta: BigDecimal
)

data class DayGroup(
    val dateKey: String,
    val date: LocalDate,
    val label: String,
    val items: List<TransactionItem>,
    val balance: BigDecimal
)

data class WeekGroup(
    val weekKey: String,
    val start: LocalDate,
    val end: LocalDate,
    val label: String,
    val days: List<DayGroup>,
    val balance: BigDecimal,
    val isCurrentWeek: Boolean
)

data class MonthGroup(
    val monthKey: String,
    val label: String,
    val weeks: List<WeekGroup>,
    val balance: BigDecimal,
    val isCurrentMonth: Boolean
)

data class HistoricoUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val allTransactions: List<TransactionItem> = emptyList(),
    val filteredTransactions: List<TransactionItem> = emptyList(),
    val selectedTab: HistoricoTab = HistoricoTab.TODOS,
    val searchQuery: String = "",
    val profile: Profile? = null,

    // Filtro de período
    val periodFilter: PeriodFilter = PeriodFilter(),
    val isPeriodDropdownExpanded: Boolean = false,

    // Card dinâmico de saldo
    val saldoCardTitle: String = "SALDO DA SEMANA",
    val saldoPeriodo: BigDecimal = BigDecimal.ZERO,
    val entradasPeriodo: BigDecimal = BigDecimal.ZERO,
    val saidasPeriodo: BigDecimal = BigDecimal.ZERO,
    val metaPeriodo: BigDecimal = BigDecimal("320.00"),

    // Resumo de hoje / compatibilidade
    val saldoHoje: BigDecimal = BigDecimal.ZERO,
    val entradasHoje: BigDecimal = BigDecimal.ZERO,
    val saidasHoje: BigDecimal = BigDecimal.ZERO,
    val metaDiaria: BigDecimal = BigDecimal("320.00"),
    val metaPercent: Int = 0,
    val faltamParaMeta: BigDecimal = BigDecimal.ZERO,

    // Grupos hierárquicos
    val monthGroups: List<MonthGroup> = emptyList(),

    // Expansões
    val expandedMonths: Set<String> = emptySet(),
    val expandedWeeks: Set<String> = emptySet(),

    // Diálogos
    val itemToDelete: TransactionItem? = null,
    val isEditGoalDialogOpen: Boolean = false
)

class HistoricoViewModel(
    private val historicoRepository: HistoricoRepository = HistoricoRepository(),
    private val externalScope: CoroutineScope? = null,
    observeDataSync: Boolean = true,
    autoLoad: Boolean = true
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(HistoricoUiState())
    val uiState: StateFlow<HistoricoUiState> = _uiState.asStateFlow()

    private val ptLocale = Locale("pt", "BR")
    private val dayShortFormatter = DateTimeFormatter.ofPattern("dd 'DE' MMM", ptLocale)
    private val ddMMFormatter = DateTimeFormatter.ofPattern("dd/MM", ptLocale)
    private val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ptLocale)

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        if (autoLoad) {
            loadData()
        }
        if (observeDataSync) {
            scope.launch {
                AppDataSync.dataChangedEvents.collect {
                    loadData()
                }
            }
        }
    }

    fun loadData() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (profile, items) = historicoRepository.loadHistoricoData(currentUserId)
                val today = LocalDate.now()
                val metaDiaria = profile.dailyGoal ?: BigDecimal("320.00")
                val currentFilter = _uiState.value.periodFilter

                val periodMetrics = calculatePeriodMetrics(
                    items = items,
                    periodFilter = currentFilter,
                    dailyGoal = metaDiaria,
                    today = today
                )

                val (monthGroups, filteredList) = withContext(Dispatchers.Default) {
                    buildGroups(
                        items = items,
                        tab = _uiState.value.selectedTab,
                        query = _uiState.value.searchQuery,
                        periodFilter = currentFilter,
                        today = today
                    )
                }

                val periodMonthKeys = monthGroups.map { it.monthKey }.toSet()
                val periodWeekKeys = monthGroups.flatMap { it.weeks.map { w -> w.weekKey } }.toSet()

                val currentExpandedMonths = if (_uiState.value.expandedMonths.isEmpty()) {
                    periodMonthKeys
                } else {
                    _uiState.value.expandedMonths.intersect(periodMonthKeys).ifEmpty { periodMonthKeys }
                }

                val currentExpandedWeeks = if (_uiState.value.expandedWeeks.isEmpty()) {
                    periodWeekKeys
                } else {
                    _uiState.value.expandedWeeks.intersect(periodWeekKeys).ifEmpty { periodWeekKeys }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        allTransactions = items,
                        filteredTransactions = filteredList,
                        saldoCardTitle = periodMetrics.cardTitle,
                        saldoPeriodo = periodMetrics.saldoPeriodo,
                        entradasPeriodo = periodMetrics.entradasPeriodo,
                        saidasPeriodo = periodMetrics.saidasPeriodo,
                        metaPeriodo = periodMetrics.metaPeriodo,
                        metaPercent = periodMetrics.metaPercent,
                        faltamParaMeta = periodMetrics.faltamParaMeta,
                        saldoHoje = periodMetrics.saldoPeriodo,
                        entradasHoje = periodMetrics.entradasPeriodo,
                        saidasHoje = periodMetrics.saidasPeriodo,
                        metaDiaria = metaDiaria,
                        monthGroups = monthGroups,
                        expandedMonths = currentExpandedMonths,
                        expandedWeeks = currentExpandedWeeks
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar histórico: ${e.message}") }
            }
        }
    }

    fun applyPeriodPreset(preset: PeriodPreset) {
        val currentFilter = _uiState.value.periodFilter.copy(preset = preset)
        _uiState.update { it.copy(periodFilter = currentFilter, isPeriodDropdownExpanded = false) }
        rebuildGroupsAndMetrics(expandAllInPeriod = true)
    }

    fun applyCustomPeriod(start: LocalDate, end: LocalDate) {
        val (finalStart, finalEnd) = if (start.isAfter(end)) end to start else start to end
        val filter = _uiState.value.periodFilter.copy(
            preset = PeriodPreset.PERSONALIZADO,
            customStart = finalStart,
            customEnd = finalEnd
        )
        _uiState.update { it.copy(periodFilter = filter, isPeriodDropdownExpanded = false) }
        rebuildGroupsAndMetrics(expandAllInPeriod = true)
    }

    fun resetPeriodFilter() {
        applyPeriodPreset(PeriodPreset.SEMANA)
    }

    fun togglePeriodDropdown() {
        _uiState.update { it.copy(isPeriodDropdownExpanded = !it.isPeriodDropdownExpanded) }
    }

    fun closePeriodDropdown() {
        _uiState.update { it.copy(isPeriodDropdownExpanded = false) }
    }

    fun setTab(tab: HistoricoTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        rebuildGroupsAndMetrics(expandAllInPeriod = false)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildGroupsAndMetrics(expandAllInPeriod = false)
    }

    fun toggleMonth(monthKey: String) {
        _uiState.update { state ->
            val set = state.expandedMonths.toMutableSet()
            if (set.contains(monthKey)) {
                set.remove(monthKey)
            } else {
                set.add(monthKey)
            }
            state.copy(expandedMonths = set)
        }
    }

    fun toggleWeek(weekKey: String) {
        _uiState.update { state ->
            val set = state.expandedWeeks.toMutableSet()
            if (set.contains(weekKey)) {
                set.remove(weekKey)
            } else {
                set.add(weekKey)
            }
            state.copy(expandedWeeks = set)
        }
    }

    fun promptDelete(item: TransactionItem) {
        _uiState.update { it.copy(itemToDelete = item) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(itemToDelete = null) }
    }

    fun confirmDelete() {
        val item = _uiState.value.itemToDelete ?: return
        scope.launch {
            _uiState.update { it.copy(itemToDelete = null, isLoading = true) }
            val ok = historicoRepository.deleteTransaction(item)
            if (ok) {
                _uiState.update { it.copy(message = "Transação excluída com sucesso!") }
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Não foi possível excluir a transação.") }
            }
        }
    }

    fun openEditGoalDialog() {
        _uiState.update { it.copy(isEditGoalDialogOpen = true) }
    }

    fun closeEditGoalDialog() {
        _uiState.update { it.copy(isEditGoalDialogOpen = false) }
    }

    fun saveDailyGoal(newGoal: BigDecimal) {
        scope.launch {
            _uiState.update { it.copy(isEditGoalDialogOpen = false, isLoading = true) }
            val ok = historicoRepository.updateDailyGoal(currentUserId, newGoal)
            if (ok) {
                _uiState.update { it.copy(message = "Meta diária atualizada com sucesso!") }
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Não foi possível atualizar a meta.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun getEditRoute(item: TransactionItem): String {
        return when (item.sourceType) {
            TransactionSourceType.ROUTE -> "lancar_rota?itemId=${item.id}"
            TransactionSourceType.DAILY_TOTAL -> "lancar_total_dia?itemId=${item.id}"
            TransactionSourceType.EXPENSE -> {
                val cat = item.rawExpense?.category?.lowercase() ?: item.category.lowercase()
                when {
                    cat.contains("combustivel") || cat.contains("abastecimento") -> "fuel_expense?itemId=${item.id}"
                    cat.contains("alimentacao") -> "meal_expense?itemId=${item.id}"
                    cat.contains("manutencao") || cat.contains("peca") -> "lancar-manutencao?itemId=${item.id}"
                    else -> "fuel_expense?itemId=${item.id}"
                }
            }
        }
    }

    private fun rebuildGroupsAndMetrics(expandAllInPeriod: Boolean = false) {
        scope.launch {
            val state = _uiState.value
            val today = LocalDate.now()
            val metaDiaria = state.profile?.dailyGoal ?: state.metaDiaria

            val periodMetrics = calculatePeriodMetrics(
                items = state.allTransactions,
                periodFilter = state.periodFilter,
                dailyGoal = metaDiaria,
                today = today
            )

            val (monthGroups, filteredList) = withContext(Dispatchers.Default) {
                buildGroups(
                    items = state.allTransactions,
                    tab = state.selectedTab,
                    query = state.searchQuery,
                    periodFilter = state.periodFilter,
                    today = today
                )
            }

            val periodMonthKeys = monthGroups.map { it.monthKey }.toSet()
            val periodWeekKeys = monthGroups.flatMap { it.weeks.map { w -> w.weekKey } }.toSet()

            val currentExpandedMonths = if (expandAllInPeriod || state.expandedMonths.isEmpty()) {
                periodMonthKeys
            } else {
                state.expandedMonths.intersect(periodMonthKeys).ifEmpty { periodMonthKeys }
            }

            val currentExpandedWeeks = if (expandAllInPeriod || state.expandedWeeks.isEmpty()) {
                periodWeekKeys
            } else {
                state.expandedWeeks.intersect(periodWeekKeys).ifEmpty { periodWeekKeys }
            }

            _uiState.update {
                it.copy(
                    saldoCardTitle = periodMetrics.cardTitle,
                    saldoPeriodo = periodMetrics.saldoPeriodo,
                    entradasPeriodo = periodMetrics.entradasPeriodo,
                    saidasPeriodo = periodMetrics.saidasPeriodo,
                    metaPeriodo = periodMetrics.metaPeriodo,
                    metaPercent = periodMetrics.metaPercent,
                    faltamParaMeta = periodMetrics.faltamParaMeta,
                    saldoHoje = periodMetrics.saldoPeriodo,
                    entradasHoje = periodMetrics.entradasPeriodo,
                    saidasHoje = periodMetrics.saidasPeriodo,
                    metaDiaria = metaDiaria,
                    monthGroups = monthGroups,
                    filteredTransactions = filteredList,
                    expandedMonths = currentExpandedMonths,
                    expandedWeeks = currentExpandedWeeks
                )
            }
        }
    }

    fun calculatePeriodMetrics(
        items: List<TransactionItem>,
        periodFilter: PeriodFilter,
        dailyGoal: BigDecimal,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): PeriodMetrics {
        val (rangeStart, rangeEnd) = periodFilter.resolveRange(today)
        var entradas = BigDecimal.ZERO
        var saidas = BigDecimal.ZERO

        items.forEach { tx ->
            val txLocalDate = tx.occurredAt.atZoneSameInstant(zone).toLocalDate()
            if (!txLocalDate.isBefore(rangeStart) && !txLocalDate.isAfter(rangeEnd)) {
                if (tx.type == TransactionType.GANHO) {
                    entradas = entradas.add(tx.netAmount)
                } else {
                    saidas = saidas.add(tx.amount)
                }
            }
        }

        val saldo = entradas.subtract(saidas)
        val diasNoIntervalo = ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1
        val metaPeriodo = dailyGoal.multiply(BigDecimal(diasNoIntervalo))
        val metaPercent = if (metaPeriodo > BigDecimal.ZERO) {
            val ratio = maxOf(BigDecimal.ZERO, saldo)
                .multiply(BigDecimal(100))
                .divide(metaPeriodo, 0, RoundingMode.HALF_UP)
            ratio.toInt()
        } else 0
        val faltamParaMeta = maxOf(BigDecimal.ZERO, metaPeriodo.subtract(saldo))

        return PeriodMetrics(
            cardTitle = periodFilter.preset.toSaldoCardTitle(),
            saldoPeriodo = saldo,
            entradasPeriodo = entradas,
            saidasPeriodo = saidas,
            metaPeriodo = metaPeriodo,
            metaPercent = metaPercent,
            faltamParaMeta = faltamParaMeta
        )
    }

    fun buildGroups(
        items: List<TransactionItem>,
        tab: HistoricoTab,
        query: String,
        periodFilter: PeriodFilter,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<List<MonthGroup>, List<TransactionItem>> {
        val (rangeStart, rangeEnd) = periodFilter.resolveRange(today)

        val filtered = items.filter { item ->
            val localDate = item.occurredAt.atZoneSameInstant(zone).toLocalDate()
            if (localDate.isBefore(rangeStart) || localDate.isAfter(rangeEnd)) return@filter false

            val matchTab = when (tab) {
                HistoricoTab.TODOS -> true
                HistoricoTab.GANHOS -> item.type == TransactionType.GANHO
                HistoricoTab.DESPESAS -> item.type == TransactionType.DESPESA
            }
            if (!matchTab) return@filter false

            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val match = item.title.lowercase().contains(q) ||
                        item.subtitle.lowercase().contains(q) ||
                        item.category.lowercase().contains(q) ||
                        (item.tag?.lowercase()?.contains(q) == true) ||
                        (item.meta1?.lowercase()?.contains(q) == true) ||
                        item.amount.toPlainString().contains(q)
                if (!match) return@filter false
            }

            true
        }

        val monthsMap = mutableMapOf<String, MutableList<TransactionItem>>()
        filtered.forEach { item ->
            val localDate = item.occurredAt.atZoneSameInstant(zone).toLocalDate()
            val mKey = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            monthsMap.getOrPut(mKey) { mutableListOf() }.add(item)
        }

        val outMonths = mutableListOf<MonthGroup>()
        val sortedMonthKeys = monthsMap.keys.sortedDescending()

        sortedMonthKeys.forEach { mKey ->
            val monthItems = monthsMap[mKey] ?: return@forEach
            val monthDate = LocalDate.parse("$mKey-01")
            val isCurrentMonth = monthDate.year == today.year && monthDate.monthValue == today.monthValue

            val weeksMap = mutableMapOf<String, MutableList<TransactionItem>>()
            monthItems.forEach { item ->
                val d = item.occurredAt.atZoneSameInstant(zone).toLocalDate()
                val wStart = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val wKey = wStart.toString()
                weeksMap.getOrPut(wKey) { mutableListOf() }.add(item)
            }

            val outWeeks = mutableListOf<WeekGroup>()
            val sortedWeekKeys = weeksMap.keys.sortedDescending()

            sortedWeekKeys.forEach { wKey ->
                val weekItems = weeksMap[wKey] ?: return@forEach
                val wStartDate = LocalDate.parse(wKey)
                val wEndDate = wStartDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val isCurrentWeek = !today.isBefore(wStartDate) && !today.isAfter(wEndDate)

                val daysMap = mutableMapOf<String, MutableList<TransactionItem>>()
                weekItems.forEach { item ->
                    val dKey = item.occurredAt.atZoneSameInstant(zone).toLocalDate().toString()
                    daysMap.getOrPut(dKey) { mutableListOf() }.add(item)
                }

                val outDays = mutableListOf<DayGroup>()
                val sortedDayKeys = daysMap.keys.sortedDescending()

                sortedDayKeys.forEach { dKey ->
                    val dayItems = daysMap[dKey] ?: return@forEach
                    val dayDate = LocalDate.parse(dKey)

                    val dayLabel = when (dayDate) {
                        today -> "HOJE, ${dayDate.format(dayShortFormatter).uppercase()}"
                        today.minusDays(1) -> "ONTEM, ${dayDate.format(dayShortFormatter).uppercase()}"
                        else -> {
                            val weekday = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, ptLocale).uppercase()
                            "$weekday, ${dayDate.format(dayShortFormatter).uppercase()}"
                        }
                    }

                    val dayBalance = dayItems.fold(BigDecimal.ZERO) { acc, item ->
                        if (item.type == TransactionType.GANHO) acc.add(item.netAmount) else acc.subtract(item.amount)
                    }

                    outDays.add(
                        DayGroup(
                            dateKey = dKey,
                            date = dayDate,
                            label = dayLabel,
                            items = dayItems.sortedByDescending { it.occurredAt },
                            balance = dayBalance
                        )
                    )
                }

                val weekBalance = outDays.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.balance) }

                val monthStart = monthDate.withDayOfMonth(1)
                val monthEnd = monthDate.with(TemporalAdjusters.lastDayOfMonth())
                val displayStart = if (wStartDate.isBefore(monthStart)) monthStart else wStartDate
                val displayEnd = if (wEndDate.isAfter(monthEnd)) monthEnd else wEndDate

                val weekLabel = "SEMANA DE ${displayStart.format(ddMMFormatter)} A ${displayEnd.format(ddMMFormatter)}"

                outWeeks.add(
                    WeekGroup(
                        weekKey = wKey,
                        start = wStartDate,
                        end = wEndDate,
                        label = weekLabel,
                        days = outDays,
                        balance = weekBalance,
                        isCurrentWeek = isCurrentWeek
                    )
                )
            }

            val monthBalance = outWeeks.fold(BigDecimal.ZERO) { acc, week -> acc.add(week.balance) }
            val monthLabel = monthDate.format(monthNameFormatter).uppercase()

            outMonths.add(
                MonthGroup(
                    monthKey = mKey,
                    label = monthLabel,
                    weeks = outWeeks,
                    balance = monthBalance,
                    isCurrentMonth = isCurrentMonth
                )
            )
        }

        return Pair(outMonths, filtered)
    }
}
