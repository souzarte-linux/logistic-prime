package com.fernando.centraldomotorista.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.PlatformRules
import com.fernando.centraldomotorista.data.remote.supabase
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
import java.time.temporal.TemporalAdjusters

fun defaultVariableCycleEntries(delay: Int = 7): List<CycleEntry> {
    val now = LocalDate.now()
    val start1 = now.withDayOfMonth(1)
    val end1 = now.withDayOfMonth(15)
    val safeDelay = delay.coerceAtLeast(0)
    val pay1 = end1.plusDays(safeDelay.toLong())

    val start2 = now.withDayOfMonth(16)
    val end2 = now.with(TemporalAdjusters.lastDayOfMonth())
    val pay2 = end2.plusDays(safeDelay.toLong())

    return listOf(
        CycleEntry(
            cut = 1,
            payDelay = safeDelay,
            startDate = start1,
            endDate = end1,
            includeEndDate = true,
            payDelayDays = safeDelay,
            paymentDate = pay1
        ),
        CycleEntry(
            cut = 16,
            payDelay = safeDelay,
            startDate = start2,
            endDate = end2,
            includeEndDate = true,
            payDelayDays = safeDelay,
            paymentDate = pay2
        )
    )
}

enum class PlatformStatusFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Ativas"),
    INACTIVE("Inativas")
}

enum class PlatformSegmentFilter(val label: String, val rawValue: String?) {
    ALL("Todas", null),
    DELIVERY("Delivery", "delivery"),
    LOGISTICA("Logística", "logistica")
}

enum class PlatformSortBy(val label: String) {
    ALPHABETICAL("Ordem Alfabética (A-Z)"),
    HIGHEST_EARNING("Maior Ganho"),
    LOWEST_EARNING("Menor Ganho")
}

val WEEK_DAYS = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")
val PIX_KEY_TYPES = listOf("CPF", "CNPJ", "E-mail", "Celular", "Aleatória")

val POPULAR_PLATFORMS = listOf(
    Triple("Mercado Envios", "logistica", "semanal"),
    Triple("iFood", "delivery", "semanal"),
    Triple("Loggi", "logistica", "semanal"),
    Triple("Lalamove", "logistica", "semanal"),
    Triple("Rappi", "delivery", "semanal"),
    Triple("Shopee Entregas", "logistica", "quinzenal"),
    Triple("Uber Direct", "delivery", "semanal"),
    Triple("Zé Delivery", "delivery", "semanal")
)

data class PlatformsUiState(
    val platforms: List<Platform> = emptyList(),
    val earningsMap: Map<String, BigDecimal> = emptyMap(),
    val searchQuery: String = "",
    val statusFilter: PlatformStatusFilter = PlatformStatusFilter.ALL,
    val segmentFilter: PlatformSegmentFilter = PlatformSegmentFilter.ALL,
    val sortBy: PlatformSortBy = PlatformSortBy.ALPHABETICAL,
    val isFilterModalOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields
    val editingPlatformId: String? = null,
    val name: String = "",
    val segment: String = "logistica",
    val paymentModel: String = "producao",
    val cycle: String = "semanal",
    val paymentDay: String = "QUA",
    val fixedPayDelay: Int = 7,
    val cycleEntries: List<CycleEntry> = defaultVariableCycleEntries(),
    val bankName: String = "",
    val bankAgency: String = "",
    val bankAccount: String = "",
    val pixKeyType: String = "CPF",
    val pixKey: String = "",
    val active: Boolean = true
) {
    val activeFilterCount: Int
        get() = (if (statusFilter != PlatformStatusFilter.ALL) 1 else 0) +
                (if (segmentFilter != PlatformSegmentFilter.ALL) 1 else 0) +
                (if (sortBy != PlatformSortBy.ALPHABETICAL) 1 else 0)

    val filteredAndSortedPlatforms: List<Platform>
        get() {
            var list = platforms

            // Filtro de status
            list = when (statusFilter) {
                PlatformStatusFilter.ALL -> list
                PlatformStatusFilter.ACTIVE -> list.filter { it.active }
                PlatformStatusFilter.INACTIVE -> list.filter { !it.active }
            }

            // Filtro de segmento
            if (segmentFilter != PlatformSegmentFilter.ALL && segmentFilter.rawValue != null) {
                list = list.filter { it.segment.equals(segmentFilter.rawValue, ignoreCase = true) }
            }

            // Busca textual
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                    it.segment.lowercase().contains(q) ||
                    it.cycle.lowercase().contains(q) ||
                    (it.paymentDay?.lowercase()?.contains(q) == true)
                }
            }

            // Ordenação
            return when (sortBy) {
                PlatformSortBy.ALPHABETICAL -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                PlatformSortBy.HIGHEST_EARNING -> list.sortedByDescending { earningsMap[it.id] ?: BigDecimal.ZERO }
                PlatformSortBy.LOWEST_EARNING -> list.sortedBy { earningsMap[it.id] ?: BigDecimal.ZERO }
            }
        }
}

class PlatformsViewModel(
    private val repository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatformsUiState())
    val uiState: StateFlow<PlatformsUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadPlatforms()
        viewModelScope.launch {
            AppDataSync.dataChangedEvents.collect {
                loadPlatforms()
            }
        }
    }

    fun loadPlatforms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val list = repository.getPlatforms(currentUserId)
                val earnings = repository.getMonthEarningsByPlatform(currentUserId)
                _uiState.update {
                    it.copy(
                        platforms = list,
                        earningsMap = earnings,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao carregar plataformas: ${e.message}"
                    )
                }
            }
        }
    }

    // Filtros e ordenação
    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun onStatusFilterChanged(status: PlatformStatusFilter) = _uiState.update { it.copy(statusFilter = status) }
    fun onSegmentFilterChanged(segment: PlatformSegmentFilter) = _uiState.update { it.copy(segmentFilter = segment) }
    fun onSortByChanged(sortBy: PlatformSortBy) = _uiState.update { it.copy(sortBy = sortBy) }
    fun openFilterModal() = _uiState.update { it.copy(isFilterModalOpen = true) }
    fun closeFilterModal() = _uiState.update { it.copy(isFilterModalOpen = false) }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                statusFilter = PlatformStatusFilter.ALL,
                segmentFilter = PlatformSegmentFilter.ALL,
                sortBy = PlatformSortBy.ALPHABETICAL
            )
        }
    }

    // Toggle rápido de ativação no card
    fun togglePlatformActive(platform: Platform) {
        viewModelScope.launch {
            val nextActive = !platform.active
            val updatedPlatform = platform.copy(active = nextActive)
            try {
                // Atualização otimista
                _uiState.update { state ->
                    val updatedList = state.platforms.map { if (it.id == platform.id) updatedPlatform else it }
                    state.copy(platforms = updatedList)
                }
                repository.savePlatform(updatedPlatform)
                val statusText = if (nextActive) "ativada" else "desativada"
                _uiState.update { it.copy(message = "Plataforma '${platform.name}' $statusText!") }
            } catch (e: Exception) {
                // Rollback em caso de erro
                _uiState.update { state ->
                    val reverted = state.platforms.map { if (it.id == platform.id) platform else it }
                    state.copy(platforms = reverted, error = "Erro ao alterar status da plataforma: ${e.message}")
                }
            }
        }
    }

    // Abertura e fechamento de formulário
    fun openAddDialog(prefillName: String? = null, prefillSegment: String? = null, prefillCycle: String? = null) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPlatformId = null,
                name = prefillName ?: "",
                segment = prefillSegment ?: "logistica",
                paymentModel = "producao",
                cycle = prefillCycle ?: "semanal",
                paymentDay = "QUA",
                fixedPayDelay = 7,
                cycleEntries = defaultVariableCycleEntries(7),
                bankName = "",
                bankAgency = "",
                bankAccount = "",
                pixKeyType = "CPF",
                pixKey = "",
                active = true,
                error = null
            )
        }
    }

    fun startEditing(platform: Platform) {
        val rules = platform.rules
        val entries = if (rules.cycleEntries.isNotEmpty()) {
            rules.cycleEntries.map { entry ->
                val now = LocalDate.now()
                val sDate = entry.startDate ?: now.withDayOfMonth(entry.cut.coerceIn(1, now.lengthOfMonth()))
                val eDate = entry.endDate ?: sDate.plusDays(14)
                val delay = entry.payDelayDays
                val pDate = entry.paymentDate ?: eDate.plusDays(delay.toLong())
                entry.copy(
                    startDate = sDate,
                    endDate = eDate,
                    includeEndDate = entry.includeEndDate,
                    payDelayDays = delay,
                    paymentDate = pDate
                )
            }
        } else {
            defaultVariableCycleEntries(rules.fixedPayDelay)
        }

        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPlatformId = platform.id,
                name = platform.name,
                segment = platform.segment,
                paymentModel = platform.paymentModel,
                cycle = platform.cycle,
                paymentDay = platform.paymentDay ?: "QUA",
                fixedPayDelay = rules.fixedPayDelay,
                cycleEntries = entries,
                bankName = platform.bankName ?: "",
                bankAgency = platform.bankAgency ?: "",
                bankAccount = platform.bankAccount ?: "",
                pixKeyType = platform.pixKeyType ?: "CPF",
                pixKey = platform.pixKey ?: "",
                active = platform.active,
                error = null
            )
        }
    }

    fun loadPlatformForEditing(platformId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val existing = _uiState.value.platforms.firstOrNull { it.id == platformId }
                if (existing != null) {
                    startEditing(existing)
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val platform = repository.getPlatformById(platformId)
                        ?: repository.getPlatforms(currentUserId).firstOrNull { it.id == platformId }
                    if (platform != null) {
                        startEditing(platform)
                    } else {
                        _uiState.update { it.copy(error = "Plataforma não encontrada.") }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar plataforma: ${e.message}") }
            }
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingPlatformId = null,
                name = "",
                error = null
            )
        }
    }

    // Atualizadores dos campos de formulário
    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onSegmentChanged(segment: String) = _uiState.update { it.copy(segment = segment) }
    fun onPaymentModelChanged(model: String) = _uiState.update { it.copy(paymentModel = model) }
    fun onCycleChanged(cycle: String) = _uiState.update { it.copy(cycle = cycle) }
    fun onPaymentDayChanged(day: String) = _uiState.update { it.copy(paymentDay = day) }
    fun onFixedPayDelayChanged(delay: Int) = _uiState.update { it.copy(fixedPayDelay = delay.coerceAtLeast(1)) }
    fun onActiveChanged(active: Boolean) = _uiState.update { it.copy(active = active) }
    fun onBankNameChanged(bank: String) = _uiState.update { it.copy(bankName = bank) }
    fun onBankAgencyChanged(agency: String) = _uiState.update { it.copy(bankAgency = agency) }
    fun onBankAccountChanged(account: String) = _uiState.update { it.copy(bankAccount = account) }
    fun onPixKeyTypeChanged(type: String) = _uiState.update { it.copy(pixKeyType = type) }
    fun onPixKeyChanged(key: String) = _uiState.update { it.copy(pixKey = key) }

    // Manipulação dos ciclos dinâmicos (ciclo misto / variável)
    fun addCycleEntry() {
        _uiState.update { state ->
            val last = state.cycleEntries.lastOrNull()
            val newStart = last?.endDate?.plusDays(1) ?: LocalDate.now()
            val newEnd = newStart.plusDays(14)
            val delay = last?.payDelayDays ?: 7
            val pay = newEnd.plusDays(delay.toLong())

            val entry = CycleEntry(
                cut = newStart.dayOfMonth,
                payDelay = delay,
                startDate = newStart,
                endDate = newEnd,
                includeEndDate = true,
                payDelayDays = delay,
                paymentDate = pay
            )
            state.copy(cycleEntries = state.cycleEntries + entry)
        }
    }

    fun removeCycleEntry(index: Int) {
        _uiState.update { state ->
            if (state.cycleEntries.size <= 1) return@update state
            val updated = state.cycleEntries.filterIndexed { i, _ -> i != index }
            state.copy(cycleEntries = updated)
        }
    }

    fun updateCycleEntry(index: Int, cut: Int? = null, payDelay: Int? = null) {
        _uiState.update { state ->
            val updated = state.cycleEntries.mapIndexed { i, entry ->
                if (i == index) {
                    val safeCut = cut?.coerceIn(1, 28) ?: entry.cut
                    val safeDelay = payDelay?.coerceAtLeast(0) ?: entry.payDelay
                    val sDate = entry.startDate ?: LocalDate.now().withDayOfMonth(safeCut)
                    val eDate = entry.endDate ?: sDate.plusDays(14)
                    entry.copy(
                        cut = safeCut,
                        payDelay = safeDelay,
                        payDelayDays = safeDelay,
                        paymentDate = eDate.plusDays(safeDelay.toLong())
                    )
                } else entry
            }
            state.copy(cycleEntries = updated)
        }
    }

    fun updateCycleEntryVariable(
        index: Int,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        includeEndDate: Boolean? = null,
        payDelayDays: Int? = null
    ) {
        _uiState.update { state ->
            val updated = state.cycleEntries.mapIndexed { i, entry ->
                if (i == index) {
                    val sDate = startDate ?: entry.startDate ?: LocalDate.now()
                    val eDate = endDate ?: entry.endDate ?: sDate.plusDays(14)
                    val inc = includeEndDate ?: entry.includeEndDate
                    val delay = (payDelayDays ?: entry.payDelayDays).coerceAtLeast(0)
                    val payDate = eDate.plusDays(delay.toLong())

                    entry.copy(
                        cut = sDate.dayOfMonth,
                        payDelay = delay,
                        startDate = sDate,
                        endDate = eDate,
                        includeEndDate = inc,
                        payDelayDays = delay,
                        paymentDate = payDate
                    )
                } else entry
            }
            state.copy(cycleEntries = updated)
        }
    }

    fun savePlatform(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da plataforma.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val rules = if (state.cycle == "misto" || state.cycle == "variavel") {
                PlatformRules(
                    fixedPayDelay = state.fixedPayDelay,
                    cycleEntries = state.cycleEntries
                )
            } else {
                PlatformRules(
                    fixedPayDelay = state.fixedPayDelay,
                    cycleEntries = emptyList()
                )
            }

            val platform = Platform(
                id = state.editingPlatformId ?: "",
                userId = currentUserId,
                name = state.name.trim(),
                cycle = state.cycle,
                paymentDay = if (state.cycle == "semanal") state.paymentDay else null,
                active = state.active,
                segment = state.segment,
                paymentModel = state.paymentModel,
                rules = rules,
                bankName = state.bankName.trim().ifBlank { null },
                bankAgency = state.bankAgency.trim().ifBlank { null },
                bankAccount = state.bankAccount.trim().ifBlank { null },
                pixKeyType = state.pixKeyType.ifBlank { null },
                pixKey = state.pixKey.trim().ifBlank { null },
                pixBank = null
            )

            try {
                repository.savePlatform(platform)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingPlatformId != null) "Plataforma atualizada com sucesso!" else "Plataforma vinculada com sucesso!"
                    )
                }
                loadPlatforms()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar plataforma: ${e.message}") }
            }
        }
    }

    fun deletePlatform(platformId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deletePlatform(platformId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormOpen = false,
                        editingPlatformId = null,
                        message = "Plataforma excluída com sucesso!"
                    )
                }
                loadPlatforms()
                onSuccess?.invoke()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir plataforma.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
