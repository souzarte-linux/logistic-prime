package com.fernando.centraldomotorista.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CompanyRepository
import com.fernando.centraldomotorista.data.repository.CreditCardRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

data class MealExpenseUiState(
    val companies: List<Company> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val cardBrands: List<CardBrand> = emptyList(),
    val cardOperators: List<CardOperator> = emptyList(),
    val isLoadingData: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // Fields
    val selectedCompanyId: String? = null,
    val selectedMealType: String = "Almoço", // "Café Manhã", "Almoço", "Lanche", "Jantar"
    val title: String = "", // O que foi comprado (ex: Almoço, lanche...)
    val amountText: String = "", // Valor total pago
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val notes: String = "", // Observação (opcional)
    val paymentMethod: String = "pix", // "pix", "cartao", "dinheiro"
    val cardPaymentData: CardPaymentData? = null,
    val isAddCompanyDialogOpen: Boolean = false
)

class MealExpenseViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val creditCardRepository: CreditCardRepository = CreditCardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealExpenseUiState())
    val uiState: StateFlow<MealExpenseUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true, error = null) }
            try {
                val companies = companyRepository.getCompanies(currentUserId)
                val cards = creditCardRepository.getCreditCards(currentUserId)
                val brands = creditCardRepository.getCardBrands(currentUserId)
                val operators = creditCardRepository.getCardOperators(currentUserId)

                _uiState.update {
                    it.copy(
                        companies = companies,
                        creditCards = cards,
                        cardBrands = brands,
                        cardOperators = operators,
                        selectedCompanyId = it.selectedCompanyId ?: companies.firstOrNull()?.id,
                        isLoadingData = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingData = false,
                        error = "Erro ao carregar dados: ${e.message}"
                    )
                }
            }
        }
    }

    fun onCompanySelected(companyId: String?) {
        _uiState.update { it.copy(selectedCompanyId = companyId) }
    }

    fun onMealTypeSelected(mealType: String) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onAmountChanged(amount: String) {
        val digitsOnly = amount.filter { it.isDigit() }
        _uiState.update { it.copy(amountText = digitsOnly) }
    }

    fun onDateTimeChanged(dateTime: LocalDateTime) {
        _uiState.update { it.copy(dateTime = dateTime) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onPaymentMethodChanged(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onCardPaymentDataChanged(data: CardPaymentData?) {
        _uiState.update { it.copy(cardPaymentData = data) }
    }

    fun openAddCompanyDialog() {
        _uiState.update { it.copy(isAddCompanyDialogOpen = true) }
    }

    fun closeAddCompanyDialog() {
        _uiState.update { it.copy(isAddCompanyDialogOpen = false) }
    }

    fun addQuickCompany(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newCompany = Company(
                    id = "",
                    userId = currentUserId,
                    name = name.trim()
                )
                val created = companyRepository.saveCompany(newCompany)
                val updatedCompanies = companyRepository.getCompanies(currentUserId)
                _uiState.update {
                    it.copy(
                        companies = updatedCompanies,
                        selectedCompanyId = created.id,
                        isAddCompanyDialogOpen = false,
                        message = "Empresa '${created.name}' criada com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao cadastrar empresa: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun saveMealExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amountInCents = state.amountText.toLongOrNull() ?: 0L
        val amount = BigDecimal(amountInCents).divide(BigDecimal(100))

        if (amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Informe um valor válido maior que zero.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val selectedCompany = state.companies.firstOrNull { it.id == state.selectedCompanyId }
                val title = state.title.trim().ifBlank {
                    if (selectedCompany != null) "${state.selectedMealType} - ${selectedCompany.name}" else state.selectedMealType
                }

                val occurredAtOffset = state.dateTime
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()

                val cardData = if (state.paymentMethod == "cartao") state.cardPaymentData else null

                val expense = Expense(
                    id = "",
                    userId = currentUserId,
                    category = "alimentacao",
                    title = title,
                    vendor = selectedCompany?.name,
                    amount = amount,
                    description = state.notes.trim().takeIf { it.isNotBlank() },
                    paymentMethod = state.paymentMethod,
                    occurredAt = occurredAtOffset,
                    companyId = state.selectedCompanyId?.takeIf { it.isNotBlank() },
                    mealType = state.selectedMealType,
                    cardId = cardData?.cardId,
                    cardBrand = cardData?.cardBrand,
                    cardOperator = cardData?.cardOperator,
                    installmentGroupId = cardData?.installmentGroupId,
                    installmentNumber = if (cardData?.isInstallment == true) 1 else null,
                    installmentTotal = if (cardData?.isInstallment == true) cardData.installmentTotal else null,
                    cardDueDay = cardData?.cardDueDay
                )

                expenseRepository.createExpense(expense)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "Despesa de alimentação salva com sucesso!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Erro ao salvar despesa: ${e.message}"
                    )
                }
            }
        }
    }
}
