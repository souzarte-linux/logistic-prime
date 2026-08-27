package com.fernando.centraldomotorista.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CreditCardRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.data.repository.GasStationRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class FuelExpenseUiState(
    val gasStations: List<GasStation> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val cardBrands: List<CardBrand> = emptyList(),
    val cardOperators: List<CardOperator> = emptyList(),
    val isLoadingData: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // Fields
    val selectedStationId: String? = null,
    val selectedFuelType: String = "Gasolina Comum",
    val pricePerLiterText: String = "",
    val litersText: String = "",
    val odometerKmText: String = "",
    val isFullTank: Boolean = true,
    val totalAmountText: String = "",
    val isTotalManuallyEdited: Boolean = false,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val receiptNumber: String = "",
    val notes: String = "",
    val paymentMethod: String = "pix", // "pix", "card", "dinheiro"
    val cardPaymentData: CardPaymentData? = null
)

class FuelExpenseViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val gasStationRepository: GasStationRepository = GasStationRepository(),
    private val creditCardRepository: CreditCardRepository = CreditCardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FuelExpenseUiState())
    val uiState: StateFlow<FuelExpenseUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true, error = null) }
            val stations = gasStationRepository.getGasStations(currentUserId)
            val cards = creditCardRepository.getCreditCards(currentUserId)
            val brands = creditCardRepository.getCardBrands(currentUserId)
            val operators = creditCardRepository.getCardOperators(currentUserId)

            val initialStation = stations.firstOrNull()
            val initialFuel = initialStation?.fuelTypes?.firstOrNull() ?: "Gasolina Comum"

            _uiState.update {
                it.copy(
                    gasStations = stations,
                    creditCards = cards,
                    cardBrands = brands,
                    cardOperators = operators,
                    selectedStationId = it.selectedStationId ?: initialStation?.id,
                    selectedFuelType = if (it.selectedStationId == null) initialFuel else it.selectedFuelType,
                    isLoadingData = false
                )
            }
        }
    }

    fun onStationSelected(stationId: String) {
        val station = _uiState.value.gasStations.firstOrNull { it.id == stationId }
        val defaultFuel = station?.fuelTypes?.firstOrNull() ?: _uiState.value.selectedFuelType
        _uiState.update {
            it.copy(
                selectedStationId = stationId,
                selectedFuelType = defaultFuel
            )
        }
    }

    fun onFuelTypeSelected(fuel: String) {
        _uiState.update { it.copy(selectedFuelType = fuel) }
    }

    fun onPricePerLiterChanged(priceText: String) {
        val sanitized = priceText.replace(',', '.')
        _uiState.update { state ->
            val newState = state.copy(pricePerLiterText = sanitized)
            if (!state.isTotalManuallyEdited) {
                val price = sanitized.toBigDecimalOrNull()
                val liters = state.litersText.toBigDecimalOrNull()
                if (price != null && liters != null) {
                    val calc = price.multiply(liters).setScale(2, RoundingMode.HALF_UP)
                    newState.copy(totalAmountText = calc.toString())
                } else {
                    newState
                }
            } else {
                newState
            }
        }
    }

    fun onLitersChanged(litersText: String) {
        val sanitized = litersText.replace(',', '.')
        _uiState.update { state ->
            val newState = state.copy(litersText = sanitized)
            if (!state.isTotalManuallyEdited) {
                val price = state.pricePerLiterText.toBigDecimalOrNull()
                val liters = sanitized.toBigDecimalOrNull()
                if (price != null && liters != null) {
                    val calc = price.multiply(liters).setScale(2, RoundingMode.HALF_UP)
                    newState.copy(totalAmountText = calc.toString())
                } else {
                    newState
                }
            } else {
                newState
            }
        }
    }

    fun onOdometerChanged(odoText: String) {
        _uiState.update { it.copy(odometerKmText = odoText.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')) }
    }

    fun onFullTankChanged(isFull: Boolean) {
        _uiState.update { it.copy(isFullTank = isFull) }
    }

    fun onTotalAmountChanged(totalText: String) {
        val sanitized = totalText.replace(',', '.')
        _uiState.update {
            it.copy(
                totalAmountText = sanitized,
                isTotalManuallyEdited = true
            )
        }
    }

    fun onDateTimeChanged(dateTime: LocalDateTime) {
        _uiState.update { it.copy(dateTime = dateTime) }
    }

    fun onReceiptNumberChanged(receipt: String) {
        _uiState.update { it.copy(receiptNumber = receipt) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onPaymentMethodSelected(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onCardPaymentDataConfirmed(cardData: CardPaymentData) {
        _uiState.update {
            it.copy(
                paymentMethod = "cartao",
                cardPaymentData = cardData
            )
        }
    }

    fun addBrand(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = creditCardRepository.createCardBrand(currentUserId, name.trim())
                _uiState.update { it.copy(cardBrands = it.cardBrands + created) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar bandeira: ${e.message}") }
            }
        }
    }

    fun addOperator(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = creditCardRepository.createCardOperator(currentUserId, name.trim())
                _uiState.update { it.copy(cardOperators = it.cardOperators + created) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar emissor: ${e.message}") }
            }
        }
    }

    fun saveFuelExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val totalAmount = state.totalAmountText.toBigDecimalOrNull()

        if (totalAmount == null || totalAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Informe o valor total do abastecimento.") }
            return
        }

        val price = state.pricePerLiterText.toBigDecimalOrNull()
        val liters = state.litersText.toBigDecimalOrNull()
        val odometer = state.odometerKmText.toBigDecimalOrNull()

        val selectedStation = state.gasStations.firstOrNull { it.id == state.selectedStationId }
        val title = "Abastecimento - ${selectedStation?.name ?: state.selectedFuelType}"

        val zoneOffset = ZoneId.systemDefault().rules.getOffset(state.dateTime)
        val occurredAt = state.dateTime.atOffset(zoneOffset)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val cardData = if (state.paymentMethod == "cartao") state.cardPaymentData else null

                val expense = Expense(
                    id = "",
                    userId = currentUserId,
                    category = "combustivel",
                    title = title,
                    vendor = selectedStation?.name,
                    amount = totalAmount,
                    liters = liters,
                    fuelType = state.selectedFuelType,
                    pricePerLiter = price,
                    odometerKm = odometer,
                    description = state.notes.trim().ifBlank { null },
                    paymentMethod = if (state.paymentMethod == "cartao") "cartao" else state.paymentMethod,
                    isFullTank = state.isFullTank,
                    receiptNumber = state.receiptNumber.trim().ifBlank { null },
                    occurredAt = occurredAt,
                    cardBrand = cardData?.cardBrand,
                    cardOperator = cardData?.cardOperator,
                    installmentGroupId = cardData?.installmentGroupId,
                    installmentNumber = if (cardData?.isInstallment == true) 1 else null,
                    installmentTotal = cardData?.installmentTotal,
                    cardDueDay = cardData?.cardDueDay,
                    gasStationId = state.selectedStationId,
                    cardId = cardData?.cardId?.ifBlank { null }
                )

                expenseRepository.createExpense(expense)
                _uiState.update { it.copy(isSaving = false, message = "Abastecimento salvo com sucesso!") }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar abastecimento: ${e.message}") }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(message = null, error = null) }
}
