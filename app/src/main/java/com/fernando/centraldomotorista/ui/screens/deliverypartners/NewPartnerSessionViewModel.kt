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

data class NewPartnerSessionUiState(
    val partner: DeliveryPartner? = null,
    val routes: List<DeliveryRoute> = emptyList(),
    val selectedRouteId: String? = null,
    val expectedPackageCount: Int = 0,
    val expectedPackageCountText: String = "0",
    val packageRateText: String = "0,00",
    val defaultBonusText: String = "0,00",
    val selectedDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val scannedBarcodes: Set<String> = emptySet(),
    val isScannerOpen: Boolean = false,
    val showDivergenceDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val sessionCreated: Boolean = false
) {
    val scannedCount: Int
        get() = scannedBarcodes.size

    val hasDivergence: Boolean
        get() = scannedCount != expectedPackageCount
}

class NewPartnerSessionViewModel(
    private val partnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository(),
    private val routeRepository: DeliveryRouteRepository = DeliveryRouteRepository(),
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPartnerSessionUiState())
    val uiState: StateFlow<NewPartnerSessionUiState> = _uiState.asStateFlow()

    fun loadData(partnerId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val partners = partnerRepository.getDeliveryPartners(user.id)
                val targetPartner = partners.firstOrNull { it.id == partnerId }
                val routes = routeRepository.getDeliveryRoutes(user.id)

                _uiState.update {
                    it.copy(
                        partner = targetPartner,
                        routes = routes,
                        selectedRouteId = targetPartner?.preferredRouteId ?: routes.firstOrNull()?.id,
                        packageRateText = String.format(Locale("pt", "BR"), "%.2f", targetPartner?.packageRate ?: BigDecimal.ZERO),
                        defaultBonusText = String.format(Locale("pt", "BR"), "%.2f", targetPartner?.defaultBonus ?: BigDecimal.ZERO),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("NewSessionVM", "Erro ao carregar dados da sessão: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar entregador e rotas.") }
            }
        }
    }

    fun onRouteSelected(routeId: String?) {
        _uiState.update { it.copy(selectedRouteId = routeId) }
    }

    fun onPackageRateChanged(text: String) {
        val clean = text.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(packageRateText = clean) }
    }

    fun onDefaultBonusChanged(text: String) {
        val clean = text.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(defaultBonusText = clean) }
    }

    fun onExpectedPackageCountChanged(text: String) {
        val clean = text.filter { it.isDigit() }
        val count = clean.toIntOrNull() ?: 0
        _uiState.update {
            it.copy(
                expectedPackageCountText = clean,
                expectedPackageCount = count
            )
        }
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onStartTimeChanged(time: LocalTime) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun openScanner() {
        _uiState.update { it.copy(isScannerOpen = true) }
    }

    fun closeScanner() {
        _uiState.update { it.copy(isScannerOpen = false) }
    }

    fun onBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                it.copy(scannedBarcodes = it.scannedBarcodes + trimmed)
            }
        }
    }

    fun removeBarcode(barcode: String) {
        _uiState.update {
            it.copy(scannedBarcodes = it.scannedBarcodes - barcode)
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.partner == null) {
            _uiState.update { it.copy(error = "Entregador não encontrado.") }
            return
        }

        // Se X != Y, mostrar diálogo de divergência
        if (state.hasDivergence) {
            _uiState.update { it.copy(showDivergenceDialog = true) }
        } else {
            performSaveSession()
        }
    }

    fun confirmSaveAnyway() {
        _uiState.update { it.copy(showDivergenceDialog = false) }
        performSaveSession()
    }

    fun dismissDivergenceDialog() {
        _uiState.update { it.copy(showDivergenceDialog = false) }
    }

    private fun performSaveSession() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val state = _uiState.value
        val partner = state.partner ?: return

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val sessionDate = state.selectedDate
                val zone = ZoneId.systemDefault()
                val startedAt = LocalDateTime.of(sessionDate, state.startTime).atZone(zone).toOffsetDateTime()

                val rate = parseCurrency(state.packageRateText)
                val bonus = parseCurrency(state.defaultBonusText)

                val newSession = DeliveryPartnerSession(
                    id = "",
                    userId = user.id,
                    partnerId = partner.id,
                    routeId = state.selectedRouteId,
                    expectedPackageCount = state.expectedPackageCount,
                    scannedBarcodes = state.scannedBarcodes.toList(),
                    scannedCount = state.scannedCount,
                    deliveredCount = 0,
                    returnedCount = 0,
                    startTime = startedAt,
                    endTime = null, // Em andamento
                    amountPaid = BigDecimal.ZERO,
                    expenseId = null,
                    packageRate = rate,
                    defaultBonus = bonus
                )

                sessionRepository.saveSession(newSession)
                _uiState.update { it.copy(isSaving = false, sessionCreated = true) }
            } catch (e: Exception) {
                Log.e("NewSessionVM", "Erro ao salvar sessão: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Erro ao iniciar sessão: ${e.message}"
                    )
                }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
