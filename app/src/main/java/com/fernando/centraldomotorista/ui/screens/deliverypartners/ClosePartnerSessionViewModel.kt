package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

data class ClosePartnerSessionUiState(
    val session: DeliveryPartnerSession? = null,
    val partner: DeliveryPartner? = null,
    val route: DeliveryRoute? = null,
    val endTime: OffsetDateTime = OffsetDateTime.now(),
    val deliveredCount: Int = 0,
    val deliveredCountText: String = "0",
    val returnedCount: Int = 0,
    val returnedCountText: String = "0",
    val suggestedAmount: BigDecimal = BigDecimal.ZERO,
    val amountPaidText: String = "0,00",
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    val returnedBarcodes: Set<String> = emptySet(),
    val isScannerOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isFinalizing: Boolean = false,
    val error: String? = null,
    val sessionFinalized: Boolean = false
) {
    val basePackageCount: Int
        get() = if (session != null && session.expectedPackageCount > 0) session.expectedPackageCount else (session?.scannedCount ?: 0)

    val totalAccounted: Int
        get() = deliveredCount + returnedCount

    val hasDivergence: Boolean
        get() = session != null && totalAccounted != basePackageCount
}

class ClosePartnerSessionViewModel(
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository(),
    private val partnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: DeliveryRouteRepository = DeliveryRouteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClosePartnerSessionUiState())
    val uiState: StateFlow<ClosePartnerSessionUiState> = _uiState.asStateFlow()

    fun loadData(sessionId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val session = sessionRepository.getSessionById(sessionId)
                if (session == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Sessão não encontrada.") }
                    return@launch
                }

                val partners = partnerRepository.getDeliveryPartners(user.id)
                val partner = partners.firstOrNull { it.id == session.partnerId }

                val routes = routeRepository.getDeliveryRoutes(user.id)
                val route = routes.firstOrNull { it.id == session.routeId }

                // Pre-fill deliveredCount with expectedPackageCount (base) initially
                val baseCount = if (session.expectedPackageCount > 0) session.expectedPackageCount else session.scannedCount
                val initialDelivered = baseCount
                val packageRate = if (session.packageRate > BigDecimal.ZERO) session.packageRate else (partner?.packageRate ?: BigDecimal.ZERO)
                val defaultBonus = if (session.defaultBonus > BigDecimal.ZERO) session.defaultBonus else (partner?.defaultBonus ?: BigDecimal.ZERO)
                val calculatedSuggested = BigDecimal(initialDelivered).multiply(packageRate).add(defaultBonus)
                val formattedAmount = String.format(Locale("pt", "BR"), "%.2f", calculatedSuggested)

                val sessionDate = session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
                val nowTime = LocalTime.now().withSecond(0).withNano(0)
                val initialEndTime = LocalDateTime.of(sessionDate, nowTime).atZone(ZoneId.systemDefault()).toOffsetDateTime()

                _uiState.update {
                    it.copy(
                        session = session,
                        partner = partner,
                        route = route,
                        endTime = initialEndTime,
                        deliveredCount = initialDelivered,
                        deliveredCountText = initialDelivered.toString(),
                        returnedCount = 0,
                        returnedCountText = "0",
                        returnedBarcodes = emptySet(),
                        isScannerOpen = false,
                        suggestedAmount = calculatedSuggested,
                        amountPaid = calculatedSuggested,
                        amountPaidText = formattedAmount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CloseSessionVM", "Erro ao carregar sessão: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar sessão.") }
            }
        }
    }

    fun openScanner() {
        _uiState.update { it.copy(isScannerOpen = true) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onReturnedBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return
        _uiState.update { current ->
            val updatedBarcodes = current.returnedBarcodes + trimmed
            val newReturnedCount = updatedBarcodes.size
            val session = current.session
            val partner = current.partner
            val base = current.basePackageCount

            val delivered = (base - newReturnedCount).coerceAtLeast(0)

            val packageRate = if (session != null && session.packageRate > BigDecimal.ZERO) session.packageRate else (partner?.packageRate ?: BigDecimal.ZERO)
            val defaultBonus = if (session != null && session.defaultBonus > BigDecimal.ZERO) session.defaultBonus else (partner?.defaultBonus ?: BigDecimal.ZERO)
            val suggested = BigDecimal(delivered).multiply(packageRate).add(defaultBonus)
            val formatted = String.format(Locale("pt", "BR"), "%.2f", suggested)

            current.copy(
                returnedBarcodes = updatedBarcodes,
                returnedCount = newReturnedCount,
                returnedCountText = newReturnedCount.toString(),
                deliveredCount = delivered,
                deliveredCountText = delivered.toString(),
                suggestedAmount = suggested,
                amountPaid = suggested,
                amountPaidText = formatted
            )
        }
    }

    fun removeReturnedBarcode(barcode: String) {
        _uiState.update { current ->
            val updatedBarcodes = current.returnedBarcodes - barcode
            val newReturnedCount = updatedBarcodes.size
            val session = current.session
            val partner = current.partner
            val base = current.basePackageCount

            val delivered = (base - newReturnedCount).coerceAtLeast(0)

            val packageRate = if (session != null && session.packageRate > BigDecimal.ZERO) session.packageRate else (partner?.packageRate ?: BigDecimal.ZERO)
            val defaultBonus = if (session != null && session.defaultBonus > BigDecimal.ZERO) session.defaultBonus else (partner?.defaultBonus ?: BigDecimal.ZERO)
            val suggested = BigDecimal(delivered).multiply(packageRate).add(defaultBonus)
            val formatted = String.format(Locale("pt", "BR"), "%.2f", suggested)

            current.copy(
                returnedBarcodes = updatedBarcodes,
                returnedCount = newReturnedCount,
                returnedCountText = newReturnedCount.toString(),
                deliveredCount = delivered,
                deliveredCountText = delivered.toString(),
                suggestedAmount = suggested,
                amountPaid = suggested,
                amountPaidText = formatted
            )
        }
    }

    fun onEndTimeChanged(hour: Int, minute: Int) {
        val current = _uiState.value.endTime.atZoneSameInstant(ZoneId.systemDefault())
        val updated = current.withHour(hour).withMinute(minute).withSecond(0).toOffsetDateTime()
        _uiState.update { it.copy(endTime = updated) }
    }

    fun onDeliveredCountChanged(text: String) {
        val clean = text.filter { it.isDigit() }
        _uiState.update { current ->
            val session = current.session
            val partner = current.partner
            val base = current.basePackageCount

            val delivered = if (clean.isEmpty()) 0 else (clean.toIntOrNull() ?: 0)
            val returned = if (clean.isEmpty()) base else (base - delivered).coerceAtLeast(0)

            val packageRate = if (session != null && session.packageRate > BigDecimal.ZERO) session.packageRate else (partner?.packageRate ?: BigDecimal.ZERO)
            val defaultBonus = if (session != null && session.defaultBonus > BigDecimal.ZERO) session.defaultBonus else (partner?.defaultBonus ?: BigDecimal.ZERO)
            val suggested = BigDecimal(delivered).multiply(packageRate).add(defaultBonus)
            val formatted = String.format(Locale("pt", "BR"), "%.2f", suggested)

            current.copy(
                deliveredCountText = clean,
                deliveredCount = delivered,
                returnedCountText = if (clean.isEmpty()) base.toString() else returned.toString(),
                returnedCount = returned,
                suggestedAmount = suggested,
                amountPaid = suggested,
                amountPaidText = formatted
            )
        }
    }

    fun onReturnedCountChanged(text: String) {
        val clean = text.filter { it.isDigit() }
        _uiState.update { current ->
            val session = current.session
            val partner = current.partner
            val base = current.basePackageCount

            val returned = if (clean.isEmpty()) 0 else (clean.toIntOrNull() ?: 0)
            val delivered = if (clean.isEmpty()) base else (base - returned).coerceAtLeast(0)

            val packageRate = if (session != null && session.packageRate > BigDecimal.ZERO) session.packageRate else (partner?.packageRate ?: BigDecimal.ZERO)
            val defaultBonus = if (session != null && session.defaultBonus > BigDecimal.ZERO) session.defaultBonus else (partner?.defaultBonus ?: BigDecimal.ZERO)
            val suggested = BigDecimal(delivered).multiply(packageRate).add(defaultBonus)
            val formatted = String.format(Locale("pt", "BR"), "%.2f", suggested)

            current.copy(
                returnedCountText = clean,
                returnedCount = returned,
                deliveredCountText = if (clean.isEmpty()) base.toString() else delivered.toString(),
                deliveredCount = delivered,
                suggestedAmount = suggested,
                amountPaid = suggested,
                amountPaidText = formatted
            )
        }
    }

    fun onAmountPaidChanged(formattedText: String, rawValue: BigDecimal) {
        _uiState.update {
            it.copy(
                amountPaidText = formattedText,
                amountPaid = rawValue
            )
        }
    }

    fun finalizeAndPay() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val state = _uiState.value
        val session = state.session ?: return
        val partner = state.partner ?: return
        val routeName = state.route?.name ?: "Sem Rota"

        _uiState.update { it.copy(isFinalizing = true, error = null) }

        viewModelScope.launch {
            try {
                // 1. Criar objeto Expense
                val expenseDescription = "Pagamento a ${partner.fullName} — rota $routeName"
                val newExpense = Expense(
                    id = "",
                    userId = user.id,
                    category = "equipe",
                    title = "Pagamento a ${partner.fullName}",
                    vendor = partner.fullName,
                    amount = state.amountPaid,
                    description = expenseDescription,
                    paymentMethod = partner.pixKey?.let { "pix" } ?: "pix",
                    occurredAt = state.endTime
                )

                // 2. Atualizar dados da sessão
                val sessionToFinalize = session.copy(
                    deliveredCount = state.deliveredCount,
                    returnedCount = state.returnedCount,
                    endTime = state.endTime,
                    amountPaid = state.amountPaid
                )

                // 3. Finalizar sessão no repositório (cria despesa e vincula expense_id)
                sessionRepository.finalizeSession(sessionToFinalize, newExpense)

                _uiState.update { it.copy(isFinalizing = false, sessionFinalized = true) }
            } catch (e: Exception) {
                Log.e("CloseSessionVM", "Erro ao finalizar sessão: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isFinalizing = false,
                        error = "Erro ao finalizar sessão: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
