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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

data class PartnerRoutesUiState(
    val partner: DeliveryPartner? = null,
    val routes: List<DeliveryRoute> = emptyList(),
    val sessions: List<DeliveryPartnerSession> = emptyList(),
    val todayDeliveredCount: Int = 0,
    val todayTotalAmountPaid: BigDecimal = BigDecimal.ZERO,
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
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartnerRoutesUiState())
    val uiState: StateFlow<PartnerRoutesUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    private var currentPartnerId: String = ""

    init {
        viewModelScope.launch {
            AppDataSync.dataChangedEvents.collect {
                if (currentPartnerId.isNotBlank()) {
                    loadData(currentPartnerId)
                }
            }
        }
    }

    fun loadData(partnerId: String) {
        currentPartnerId = partnerId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val partners = partnerRepository.getDeliveryPartners(currentUserId)
                val targetPartner = partners.firstOrNull { it.id == partnerId }
                val routes = routeRepository.getDeliveryRoutes(currentUserId)
                val sessions = sessionRepository.getSessionsForPartner(currentUserId, partnerId)
                    .sortedWith(
                        compareByDescending<DeliveryPartnerSession> { it.startTime }
                            .thenByDescending { it.createdAt }
                    )

                val today = LocalDate.now()
                val todaySessions = sessions.filter { session ->
                    session.endTime != null &&
                            session.endTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == today
                }

                val deliveredToday = todaySessions.sumOf { it.deliveredCount }
                val amountPaidToday = todaySessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }

                _uiState.update {
                    it.copy(
                        partner = targetPartner,
                        routes = routes,
                        sessions = sessions,
                        todayDeliveredCount = deliveredToday,
                        todayTotalAmountPaid = amountPaidToday,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PartnerRoutesVM", "Erro ao carregar dados do parceiro: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados do parceiro.") }
            }
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
}
