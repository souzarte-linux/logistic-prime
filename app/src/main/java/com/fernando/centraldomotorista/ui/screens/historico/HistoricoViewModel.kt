package com.fernando.centraldomotorista.ui.screens.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.HistoricoRepository
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoricoTab {
    TODOS, GANHOS, DESPESAS
}

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

    // Resumo de hoje
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
    private val historicoRepository: HistoricoRepository = HistoricoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricoUiState())
    val uiState: StateFlow<HistoricoUiState> = _uiState.asStateFlow()

    private val ptLocale = Locale("pt", "BR")
    private val dayShortFormatter = DateTimeFormatter.ofPattern("dd 'DE' MMM", ptLocale)
    private val ddMMFormatter = DateTimeFormatter.ofPattern("dd/MM", ptLocale)
    private val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ptLocale)

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadData()
        viewModelScope.launch {
            AppDataSync.dataChangedEvents.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (profile, items) = historicoRepository.loadHistoricoData(currentUserId)
                val today = LocalDate.now()

                var entradasHoje = BigDecimal.ZERO
                var saidasHoje = BigDecimal.ZERO

                items.forEach { tx ->
                    if (tx.occurredAt.toLocalDate() == today) {
                        if (tx.type == TransactionType.GANHO) {
                            entradasHoje = entradasHoje.add(tx.netAmount)
                        } else {
                            saidasHoje = saidasHoje.add(tx.amount)
                        }
                    }
                }

                val saldoHoje = entradasHoje.subtract(saidasHoje)
                val metaDiaria = profile.dailyGoal ?: BigDecimal("320.00")
                val metaPercent = if (metaDiaria > BigDecimal.ZERO) {
                    val ratio = maxOf(BigDecimal.ZERO, saldoHoje).multiply(BigDecimal(100)).divide(metaDiaria, 0, RoundingMode.HALF_UP)
                    ratio.toInt()
                } else 0
                val faltamParaMeta = maxOf(BigDecimal.ZERO, metaDiaria.subtract(saldoHoje))

                val todayMonthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val todayWeekKey = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

                val currentExpandedMonths = if (_uiState.value.expandedMonths.isEmpty()) {
                    setOf(todayMonthKey)
                } else {
                    _uiState.value.expandedMonths
                }

                val currentExpandedWeeks = if (_uiState.value.expandedWeeks.isEmpty()) {
                    setOf(todayWeekKey)
                } else {
                    _uiState.value.expandedWeeks
                }

                val (monthGroups, filteredList) = withContext(Dispatchers.Default) {
                    buildGroups(
                        items = items,
                        tab = _uiState.value.selectedTab,
                        query = _uiState.value.searchQuery,
                        today = today
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        allTransactions = items,
                        filteredTransactions = filteredList,
                        saldoHoje = saldoHoje,
                        entradasHoje = entradasHoje,
                        saidasHoje = saidasHoje,
                        metaDiaria = metaDiaria,
                        metaPercent = metaPercent,
                        faltamParaMeta = faltamParaMeta,
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

    fun setTab(tab: HistoricoTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        rebuildGroups()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildGroups()
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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

    private fun rebuildGroups() {
        viewModelScope.launch {
            val state = _uiState.value
            val today = LocalDate.now()
            val (monthGroups, filteredList) = withContext(Dispatchers.Default) {
                buildGroups(
                    items = state.allTransactions,
                    tab = state.selectedTab,
                    query = state.searchQuery,
                    today = today
                )
            }
            _uiState.update {
                it.copy(
                    monthGroups = monthGroups,
                    filteredTransactions = filteredList
                )
            }
        }
    }

    private fun buildGroups(
        items: List<TransactionItem>,
        tab: HistoricoTab,
        query: String,
        today: LocalDate
    ): Pair<List<MonthGroup>, List<TransactionItem>> {
        val filtered = items.filter { item ->
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
            val mKey = item.occurredAt.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
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
                val d = item.occurredAt.toLocalDate()
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
                    val dKey = item.occurredAt.toLocalDate().toString()
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
