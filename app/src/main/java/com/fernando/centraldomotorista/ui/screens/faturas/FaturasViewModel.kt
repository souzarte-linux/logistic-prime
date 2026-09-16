package com.fernando.centraldomotorista.ui.screens.faturas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.BillingCycleRepository
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

enum class FaturasTab(val label: String) {
    ABERTO("Em Aberto"),
    PAGO("Recebidas")
}

data class FaturasUiState(
    val cycles: List<BillingCycleWithTotals> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val selectedPlatformFilter: String = "all",
    val activeTab: FaturasTab = FaturasTab.ABERTO,
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

    // Modal Baixar / Liquidar Pagamento
    val payingCycle: BillingCycleWithTotals? = null,
    val paymentReceivedDate: LocalDate = LocalDate.now(),

    // Modal Detalhes
    val viewingCycle: BillingCycleWithTotals? = null
) {
    val openCycles: List<BillingCycleWithTotals>
        get() = cycles.filter {
            it.cycle.status != "pago" && it.cycle.status != "cancelado" &&
            (selectedPlatformFilter == "all" || it.cycle.platformId == selectedPlatformFilter)
        }

    val paidCycles: List<BillingCycleWithTotals>
        get() = cycles.filter {
            it.cycle.status == "pago" &&
            (selectedPlatformFilter == "all" || it.cycle.platformId == selectedPlatformFilter)
        }

    val totalAReceber: BigDecimal
        get() = cycles.filter { it.cycle.status != "pago" && it.cycle.status != "cancelado" }
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

    val totalRecebido: BigDecimal
        get() = cycles.filter { it.cycle.status == "pago" }
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }
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
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val platforms = platformRepository.getActivePlatforms(currentUserId)
                val cyclesWithTotals = billingCycleRepository.getBillingCyclesWithTotals(currentUserId)

                _uiState.update {
                    it.copy(
                        platforms = platforms,
                        cycles = cyclesWithTotals,
                        isLoading = false,
                        newCyclePlatformId = it.newCyclePlatformId.ifBlank { platforms.firstOrNull()?.id ?: "" }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao carregar faturas: ${e.message}")
                }
            }
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

            // Checagem de sobreposição de ciclos
            val conflict = billingCycleRepository.checkOverlap(
                platformId = state.newCyclePlatformId,
                periodStart = state.newCyclePeriodStart,
                periodEnd = state.newCyclePeriodEnd,
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
                status = "open"
            )

            val created = billingCycleRepository.createBillingCycle(newCycle)
            if (created != null) {
                billingCycleRepository.linkCycleTransactions(
                    cycleId = created.id,
                    platformId = state.newCyclePlatformId,
                    periodStart = state.newCyclePeriodStart,
                    periodEnd = state.newCyclePeriodEnd,
                    userId = currentUserId
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isNewCycleModalOpen = false,
                        actionMessage = "Fatura gerada e corridas vinculadas com sucesso!"
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

    // Confirmar Fatura Automática (status pendente_confirmacao -> open)
    fun confirmCycle(cycle: BillingCycleWithTotals) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = billingCycleRepository.updateStatus(cycle.cycle.id, "open")
            if (success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        actionMessage = "Fatura confirmada e movida para A Receber!"
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao confirmar fatura.")
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
                        actionMessage = "Fatura excluída e corridas desvinculadas!"
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
