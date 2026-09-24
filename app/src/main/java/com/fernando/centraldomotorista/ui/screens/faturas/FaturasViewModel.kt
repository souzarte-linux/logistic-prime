package com.fernando.centraldomotorista.ui.screens.faturas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.BillingCycleRepository
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class FaturasTab(val label: String) {
    EM_ABERTO("Ciclo em Aberto"),
    A_VENCER("Ciclo A Vencer"),
    PAGO("Ciclo Pago")
}

data class FaturasUiState(
    val cycles: List<BillingCycleWithTotals> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val selectedPlatformFilter: String = "all",
    val activeTab: FaturasTab = FaturasTab.EM_ABERTO,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null,

    // Modal Nova Fatura
    val isNewCycleModalOpen: Boolean = false,
    val newCyclePlatformId: String = "",
    val newCyclePeriodStart: LocalDate = LocalDate.now().minusDays(7),
    val newCyclePeriodEnd: LocalDate = LocalDate.now(),
    val newCycleExpectedDate: LocalDate = LocalDate.now().plusDays(3),
    val newCycleIncludeEndDate: Boolean = true,

    // Modal Baixar / Liquidar Pagamento
    val payingCycle: BillingCycleWithTotals? = null,
    val paymentReceivedDate: LocalDate = LocalDate.now(),

    // Modal Detalhes
    val viewingCycle: BillingCycleWithTotals? = null,

    // Modal Edição de Itens (Valores / Pacotes)
    val editingCycle: BillingCycleWithTotals? = null,

    // Modal Ajustes Financeiros (9 subtipos)
    val adjustingCycle: BillingCycleWithTotals? = null,
    val newAdjustmentSubtype: FinancialAdjustmentSubtype = FinancialAdjustmentSubtype.OUTROS_DESCONTOS,
    val newAdjustmentAmount: String = "",
    val newAdjustmentDescription: String = "",
    val newAdjustmentNotes: String = "",
    val newAdjustmentDate: LocalDate = LocalDate.now(),

    // Estado da Cascata / Accordion (Ciclo Pago)
    val expandedMonths: Set<String> = emptySet(),
    val expandedWeeks: Set<String> = emptySet()
) {
    val emAbertoCycles: List<BillingCycleWithTotals>
        get() = cycles.filter {
            it.cycle.status == "em_aberto" &&
            (selectedPlatformFilter == "all" || it.cycle.platformId == selectedPlatformFilter)
        }

    val aVencerCycles: List<BillingCycleWithTotals>
        get() = cycles.filter {
            it.cycle.status == "a_vencer" &&
            (selectedPlatformFilter == "all" || it.cycle.platformId == selectedPlatformFilter)
        }

    val pagoCycles: List<BillingCycleWithTotals>
        get() = cycles.filter {
            it.cycle.status == "pago" &&
            (selectedPlatformFilter == "all" || it.cycle.platformId == selectedPlatformFilter)
        }

    val totalEmAberto: BigDecimal
        get() = emAbertoCycles.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

    val totalAVencer: BigDecimal
        get() = aVencerCycles.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

    val totalPago: BigDecimal
        get() = pagoCycles.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

    val pagoMonthGroups: List<FaturasPaidMonthGroup>
        get() {
            if (pagoCycles.isEmpty()) return emptyList()

            val ptLocale = java.util.Locale("pt", "BR")
            val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptLocale)

            val byMonth = pagoCycles.groupBy { c ->
                val pDate = c.cycle.paymentReceivedDate ?: c.cycle.expectedPaymentDate
                YearMonth.from(pDate)
            }

            return byMonth.entries.sortedByDescending { it.key }.map { (ym, monthItems) ->
                val monthLabel = ym.format(monthFormatter).replaceFirstChar { it.uppercase() }
                val monthTotal = monthItems.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

                val byWeek = monthItems.groupBy { c ->
                    val pDate = c.cycle.paymentReceivedDate ?: c.cycle.expectedPaymentDate
                    getWeekOfMonthInfo(pDate)
                }

                val weekGroups = byWeek.entries.sortedByDescending { it.key.first }.map { (weekInfo, weekItems) ->
                    val (weekNum, weekLabel) = weekInfo
                    val sortedItems = weekItems.sortedWith(
                        compareBy<BillingCycleWithTotals> { it.platformName.lowercase().trim() }
                            .thenBy { it.cycle.paymentReceivedDate ?: it.cycle.expectedPaymentDate }
                    )
                    val weekTotal = sortedItems.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

                    FaturasPaidWeekGroup(
                        weekNumber = weekNum,
                        weekLabel = weekLabel,
                        totalAmount = weekTotal,
                        totalItems = sortedItems.size,
                        items = sortedItems
                    )
                }

                FaturasPaidMonthGroup(
                    yearMonth = ym,
                    monthLabel = monthLabel,
                    totalAmount = monthTotal,
                    totalInvoices = monthItems.size,
                    weeks = weekGroups
                )
            }
        }
}

data class FaturasPaidWeekGroup(
    val weekNumber: Int,
    val weekLabel: String,
    val totalAmount: BigDecimal,
    val totalItems: Int,
    val items: List<BillingCycleWithTotals>
)

data class FaturasPaidMonthGroup(
    val yearMonth: YearMonth,
    val monthLabel: String,
    val totalAmount: BigDecimal,
    val totalInvoices: Int,
    val weeks: List<FaturasPaidWeekGroup>
)

fun getWeekOfMonthInfo(date: LocalDate): Pair<Int, String> {
    val day = date.dayOfMonth
    val lastDay = date.lengthOfMonth()
    return when {
        day <= 7 -> 1 to "Semana 1 (01 a 07)"
        day <= 14 -> 2 to "Semana 2 (08 a 14)"
        day <= 21 -> 3 to "Semana 3 (15 a 21)"
        day <= 28 -> 4 to "Semana 4 (22 a 28)"
        else -> 5 to "Semana 5 (29 a %02d)".format(lastDay)
    }
}

class FaturasViewModel(
    private val billingCycleRepository: BillingCycleRepository = BillingCycleRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaturasUiState())
    val uiState: StateFlow<FaturasUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val platforms = platformRepository.getActivePlatforms(currentUserId)
                val cyclesWithTotals = billingCycleRepository.getBillingCyclesWithTotals(currentUserId)

                val currentYm = YearMonth.now()
                val currentYmStr = currentYm.toString()

                val availablePaidMonths = cyclesWithTotals.filter { it.cycle.status == "pago" }
                    .map { YearMonth.from(it.cycle.paymentReceivedDate ?: it.cycle.expectedPaymentDate) }
                    .distinct()

                val targetMonthYm = if (availablePaidMonths.contains(currentYm)) {
                    currentYm
                } else {
                    availablePaidMonths.maxOrNull()
                }

                _uiState.update { current ->
                    val newExpandedMonths = if (current.expandedMonths.isEmpty() && targetMonthYm != null) {
                        setOf(targetMonthYm.toString())
                    } else {
                        current.expandedMonths
                    }

                    val newExpandedWeeks = if (current.expandedWeeks.isEmpty() && targetMonthYm != null) {
                        val matchingPaid = cyclesWithTotals.filter { c ->
                            c.cycle.status == "pago" &&
                            YearMonth.from(c.cycle.paymentReceivedDate ?: c.cycle.expectedPaymentDate) == targetMonthYm
                        }
                        val latestWeekNum = matchingPaid.map { c ->
                            getWeekOfMonthInfo(c.cycle.paymentReceivedDate ?: c.cycle.expectedPaymentDate).first
                        }.maxOrNull()

                        if (latestWeekNum != null) {
                            setOf("${targetMonthYm}_$latestWeekNum")
                        } else {
                            emptySet()
                        }
                    } else {
                        current.expandedWeeks
                    }

                    current.copy(
                        platforms = platforms,
                        cycles = cyclesWithTotals,
                        isLoading = false,
                        expandedMonths = newExpandedMonths,
                        expandedWeeks = newExpandedWeeks,
                        newCyclePlatformId = current.newCyclePlatformId.ifBlank { platforms.firstOrNull()?.id ?: "" },
                        editingCycle = current.editingCycle?.let { ec -> cyclesWithTotals.find { c -> c.cycle.id == ec.cycle.id } },
                        adjustingCycle = current.adjustingCycle?.let { ac -> cyclesWithTotals.find { c -> c.cycle.id == ac.cycle.id } },
                        viewingCycle = current.viewingCycle?.let { vc -> cyclesWithTotals.find { c -> c.cycle.id == vc.cycle.id } }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao carregar faturas: ${e.message}")
                }
            }
        }
    }

    fun toggleMonthExpanded(yearMonthKey: String) {
        _uiState.update {
            val next = if (it.expandedMonths.contains(yearMonthKey)) {
                it.expandedMonths - yearMonthKey
            } else {
                it.expandedMonths + yearMonthKey
            }
            it.copy(expandedMonths = next)
        }
    }

    fun toggleWeekExpanded(weekKey: String) {
        _uiState.update {
            val next = if (it.expandedWeeks.contains(weekKey)) {
                it.expandedWeeks - weekKey
            } else {
                it.expandedWeeks + weekKey
            }
            it.copy(expandedWeeks = next)
        }
    }

    fun onPlatformFilterChanged(platformId: String) {
        _uiState.update { it.copy(selectedPlatformFilter = platformId) }
    }

    fun onTabChanged(tab: FaturasTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // Ações de Criação de Fatura
    fun openNewCycleModal() {
        val platforms = _uiState.value.platforms
        val defaultStart = LocalDate.now().minusDays(7)
        val defaultEnd = LocalDate.now()
        val defaultPay = LocalDate.now().plusDays(3)

        _uiState.update {
            it.copy(
                isNewCycleModalOpen = true,
                newCyclePlatformId = it.newCyclePlatformId.ifBlank { platforms.firstOrNull()?.id ?: "" },
                newCyclePeriodStart = defaultStart,
                newCyclePeriodEnd = defaultEnd,
                newCycleExpectedDate = defaultPay,
                newCycleIncludeEndDate = true,
                errorMessage = null
            )
        }
    }

    fun closeNewCycleModal() {
        _uiState.update { it.copy(isNewCycleModalOpen = false, errorMessage = null) }
    }

    fun onNewCyclePlatformChanged(id: String) = _uiState.update { it.copy(newCyclePlatformId = id) }
    fun onNewCyclePeriodStartChanged(date: LocalDate) = _uiState.update { it.copy(newCyclePeriodStart = date) }
    fun onNewCyclePeriodEndChanged(date: LocalDate) = _uiState.update { it.copy(newCyclePeriodEnd = date) }
    fun onNewCycleExpectedDateChanged(date: LocalDate) = _uiState.update { it.copy(newCycleExpectedDate = date) }
    fun onNewCycleIncludeEndDateChanged(include: Boolean) = _uiState.update { it.copy(newCycleIncludeEndDate = include) }

    fun createBillingCycle() {
        val state = _uiState.value
        if (state.newCyclePlatformId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecione uma plataforma.") }
            return
        }
        if (state.newCyclePeriodEnd.isBefore(state.newCyclePeriodStart)) {
            _uiState.update { it.copy(errorMessage = "A data final do período não pode ser anterior à data inicial.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val conflict = billingCycleRepository.checkOverlap(
                platformId = state.newCyclePlatformId,
                periodStart = state.newCyclePeriodStart,
                periodEnd = state.newCyclePeriodEnd,
                includeEndDate = state.newCycleIncludeEndDate,
                userId = currentUserId
            )

            if (conflict != null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Conflito de período! Já existe uma fatura ativa nesta plataforma entre ${conflict.periodStart} e ${conflict.periodEnd}."
                    )
                }
                return@launch
            }

            val newCycle = BillingCycle(
                id = "",
                userId = currentUserId,
                platformId = state.newCyclePlatformId,
                periodStart = state.newCyclePeriodStart,
                periodEnd = state.newCyclePeriodEnd,
                expectedPaymentDate = state.newCycleExpectedDate,
                status = normalizeBillingCycleStatus("em_aberto", state.newCyclePeriodEnd),
                includeEndDate = state.newCycleIncludeEndDate
            )

            val created = billingCycleRepository.createBillingCycle(newCycle)
            if (created != null) {
                billingCycleRepository.linkCycleTransactions(
                    cycleId = created.id,
                    platformId = state.newCyclePlatformId,
                    periodStart = state.newCyclePeriodStart,
                    periodEnd = state.newCyclePeriodEnd,
                    includeEndDate = state.newCycleIncludeEndDate,
                    userId = currentUserId
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isNewCycleModalOpen = false,
                        actionMessage = "Fatura gerada e transações vinculadas com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao criar fatura. Tente novamente.")
                }
            }
        }
    }

    // Modal de Baixa / Pagamento
    fun openPayModal(cycle: BillingCycleWithTotals) {
        _uiState.update {
            it.copy(
                payingCycle = cycle,
                paymentReceivedDate = LocalDate.now()
            )
        }
    }

    fun closePayModal() {
        _uiState.update { it.copy(payingCycle = null) }
    }

    fun onPaymentReceivedDateChanged(date: LocalDate) {
        _uiState.update { it.copy(paymentReceivedDate = date) }
    }

    fun confirmPayment() {
        val cycle = _uiState.value.payingCycle ?: return
        val date = _uiState.value.paymentReceivedDate

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = billingCycleRepository.updateStatus(cycle.cycle.id, "pago", date)
            if (success) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        payingCycle = null,
                        actionMessage = "Fatura liquidada! Marcada como recebida."
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao liquidar fatura.")
                }
            }
        }
    }

    // Modal de Detalhes
    fun openDetailsModal(cycle: BillingCycleWithTotals) {
        _uiState.update { it.copy(viewingCycle = cycle) }
    }

    fun closeDetailsModal() {
        _uiState.update { it.copy(viewingCycle = null) }
    }

    // Modal de Edição de Itens (Valores / Pacotes)
    fun openEditCycleModal(cycle: BillingCycleWithTotals) {
        _uiState.update { it.copy(editingCycle = cycle) }
    }

    fun closeEditCycleModal() {
        _uiState.update { it.copy(editingCycle = null) }
    }

    fun updateRouteItem(route: Route, newPackageCount: Int, newAmount: BigDecimal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = route.copy(
                packageCount = newPackageCount.coerceAtLeast(0),
                amount = newAmount.coerceAtLeast(BigDecimal.ZERO)
            )
            val ok = billingCycleRepository.updateRoute(updated)
            if (ok) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Corrida e totais recalculados com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao atualizar rota.")
                }
            }
        }
    }

    fun updateDailyTotalItem(dailyTotal: DailyTotal, newAmount: BigDecimal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = dailyTotal.copy(
                amount = newAmount.coerceAtLeast(BigDecimal.ZERO)
            )
            val ok = billingCycleRepository.updateDailyTotal(updated)
            if (ok) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Diária recalculada com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao atualizar diária.")
                }
            }
        }
    }

    // Modal de Ajustes Financeiros (9 subtipos)
    fun openAdjustmentModal(cycle: BillingCycleWithTotals) {
        _uiState.update {
            it.copy(
                adjustingCycle = cycle,
                newAdjustmentSubtype = FinancialAdjustmentSubtype.OUTROS_DESCONTOS,
                newAdjustmentAmount = "",
                newAdjustmentDescription = "",
                newAdjustmentNotes = "",
                newAdjustmentDate = LocalDate.now(),
                errorMessage = null
            )
        }
    }

    fun closeAdjustmentModal() {
        _uiState.update { it.copy(adjustingCycle = null, errorMessage = null) }
    }

    fun onAdjustmentSubtypeChanged(subtype: FinancialAdjustmentSubtype) {
        _uiState.update { it.copy(newAdjustmentSubtype = subtype) }
    }

    fun onAdjustmentAmountChanged(amount: String) {
        _uiState.update { it.copy(newAdjustmentAmount = amount) }
    }

    fun onAdjustmentDescriptionChanged(description: String) {
        _uiState.update { it.copy(newAdjustmentDescription = description) }
    }

    fun onAdjustmentNotesChanged(notes: String) {
        _uiState.update { it.copy(newAdjustmentNotes = notes) }
    }

    fun onAdjustmentDateChanged(date: LocalDate) {
        _uiState.update { it.copy(newAdjustmentDate = date) }
    }

    fun saveAdjustment() {
        val state = _uiState.value
        val cycle = state.adjustingCycle ?: return

        val cleanAmount = state.newAdjustmentAmount
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        val parsedAmount = cleanAmount.toBigDecimalOrNull()
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Informe um valor válido maior que zero.") }
            return
        }

        val subtype = state.newAdjustmentSubtype
        if (subtype.requiresTrackingCode && state.newAdjustmentNotes.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o código de rastreio ou detalhes do produto extraviado.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val adj = FinancialAdjustment(
                id = "",
                userId = currentUserId,
                platformId = cycle.cycle.platformId,
                billingCycleId = cycle.cycle.id,
                type = subtype.defaultType,
                subtype = subtype.key,
                amount = parsedAmount,
                description = state.newAdjustmentDescription.trim().ifBlank { subtype.label },
                notes = state.newAdjustmentNotes.trim().ifBlank { null },
                occurredAt = state.newAdjustmentDate
            )

            val created = billingCycleRepository.addFinancialAdjustment(adj)
            if (created != null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        newAdjustmentAmount = "",
                        newAdjustmentDescription = "",
                        newAdjustmentNotes = "",
                        actionMessage = "Ajuste financeiro adicionado e fatura recalculada!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao adicionar ajuste financeiro.")
                }
            }
        }
    }

    fun deleteAdjustment(adjustmentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val ok = billingCycleRepository.deleteFinancialAdjustment(adjustmentId)
            if (ok) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Ajuste removido com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Erro ao excluir ajuste.")
                }
            }
        }
    }

    // Exclusão de Fatura
    fun deleteCycle(cycle: BillingCycleWithTotals) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = billingCycleRepository.deleteBillingCycle(cycle.cycle.id)
            if (success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        viewingCycle = null,
                        actionMessage = "Fatura excluída e transações desvinculadas!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao excluir fatura.")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(actionMessage = null, errorMessage = null) }
    }
}
