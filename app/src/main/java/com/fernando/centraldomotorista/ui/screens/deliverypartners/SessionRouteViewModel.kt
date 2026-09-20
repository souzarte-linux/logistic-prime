package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.util.AppDataSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SessionRouteUiState(
    val session: DeliveryPartnerSession? = null,
    val partner: DeliveryPartner? = null,
    val routes: List<DeliveryRoute> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val isReadOnly: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class SessionRouteViewModel(
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository(),
    private val partnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: DeliveryRouteRepository = DeliveryRouteRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionRouteUiState())
    val uiState: StateFlow<SessionRouteUiState> = _uiState.asStateFlow()

    private var originalSession: DeliveryPartnerSession? = null

    fun loadSession(sessionId: String?, expenseId: String?, readOnly: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isReadOnly = readOnly) }
            try {
                val session = withContext(Dispatchers.IO) {
                    var s: DeliveryPartnerSession? = null
                    if (!sessionId.isNullOrBlank()) {
                        s = sessionRepository.getSessionById(sessionId)
                    }
                    if (s == null && !expenseId.isNullOrBlank()) {
                        s = sessionRepository.getSessionByExpenseId(expenseId)
                    }
                    s
                }

                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Sessão de rota não encontrada."
                        )
                    }
                    return@launch
                }

                originalSession = session

                val partners = withContext(Dispatchers.IO) {
                    partnerRepository.getDeliveryPartners(session.userId)
                }
                val partner = partners.firstOrNull { it.id == session.partnerId }
                val routes = withContext(Dispatchers.IO) {
                    routeRepository.getDeliveryRoutes(session.userId)
                }
                val platforms = withContext(Dispatchers.IO) {
                    platformRepository.getActivePlatforms(session.userId, session.partnerId)
                }

                _uiState.update {
                    it.copy(
                        session = session,
                        partner = partner,
                        routes = routes,
                        platforms = platforms,
                        isReadOnly = readOnly,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("SessionRouteVM", "Erro ao carregar sessão da rota: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao carregar sessão: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isReadOnly = !it.isReadOnly) }
    }

    fun setReadOnly(readOnly: Boolean) {
        _uiState.update { it.copy(isReadOnly = readOnly) }
    }

    fun saveSession(updatedSession: DeliveryPartnerSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val saved = withContext(Dispatchers.IO) {
                    val result = sessionRepository.saveSession(updatedSession)
                    val expId = updatedSession.expenseId
                    val origAmount = originalSession?.amountPaid ?: updatedSession.amountPaid
                    if (!expId.isNullOrBlank() && updatedSession.amountPaid.compareTo(origAmount) != 0) {
                        val linkedExpense = expenseRepository.getExpenseById(expId)
                        if (linkedExpense != null) {
                            expenseRepository.updateExpense(linkedExpense.copy(amount = updatedSession.amountPaid))
                        }
                    }
                    result
                }

                originalSession = saved
                AppDataSync.notifyDataChanged()

                _uiState.update {
                    it.copy(
                        session = saved,
                        isReadOnly = true,
                        isLoading = false,
                        message = "Sessão atualizada com sucesso!"
                    )
                }
            } catch (e: Exception) {
                Log.e("SessionRouteVM", "Erro ao salvar sessão da rota: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao salvar alterações: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
