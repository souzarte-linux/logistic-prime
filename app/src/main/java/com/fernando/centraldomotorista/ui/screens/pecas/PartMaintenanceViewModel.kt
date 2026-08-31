package com.fernando.centraldomotorista.ui.screens.pecas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.*
import com.fernando.centraldomotorista.ui.screens.expenses.CardPaymentData
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

val SUGGESTED_PARTS = listOf(
    "Óleo do Motor",
    "Filtro de Óleo",
    "Filtro de Ar",
    "Pneu Traseiro",
    "Pneu Dianteiro",
    "Pastilhas de Freio",
    "Kit Relação (Corrente/Coroa/Pinhão)",
    "Vela de Ignição",
    "Fluido de Freio"
)

data class PartMaintenanceUiState(
    val parts: List<PartMaintenance> = emptyList(),
    val companies: List<Company> = emptyList(),
    val partTypes: List<PartType> = emptyList(),
    val partProducts: List<PartProduct> = emptyList(),
    val currentOdometerKm: BigDecimal = BigDecimal.ZERO,

    // Cards / Pagamento
    val availableCards: List<CreditCard> = emptyList(),
    val availableBrands: List<CardBrand> = emptyList(),
    val availableOperators: List<CardOperator> = emptyList(),

    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val isAddCompanyDialogOpen: Boolean = false,
    val isAddProductDialogOpen: Boolean = false,
    val isAddTypeDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields - Peça & Vida Útil
    val editingPartId: String? = null,
    val partName: String = "",
    val lifeKm: String = "",
    val lastChangeKm: String = "",
    val selectedCompanyId: String? = null,
    val selectedPartProductId: String? = null,
    val partBrand: String = "",
    val partModel: String = "",

    // Form fields - Impacto Financeiro (Expense)
    val editingExpenseId: String? = null,
    val totalAmountText: String = "",
    val lastChangeDateTime: LocalDateTime = LocalDateTime.now(),
    val receiptNumber: String = "",
    val notes: String = "",
    val paymentMethod: String = "dinheiro", // "pix", "cartao", "dinheiro"
    val cardPaymentData: CardPaymentData? = null,

    // Quick Add Product Form
    val quickProductTypeId: String? = null,
    val quickProductBrand: String = "",
    val quickProductModel: String = "",
    val quickProductLifeKm: String = ""
)

class PartMaintenanceViewModel(
    private val partRepository: PartMaintenanceRepository = PartMaintenanceRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val partTypeRepository: PartTypeRepository = PartTypeRepository(),
    private val partProductRepository: PartProductRepository = PartProductRepository(),
    private val routeRepository: RouteRepository = RouteRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val creditCardRepository: CreditCardRepository = CreditCardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartMaintenanceUiState())
    val uiState: StateFlow<PartMaintenanceUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val partsList = partRepository.getPartMaintenances(currentUserId)
            val companiesList = companyRepository.getCompanies(currentUserId)
            val typesList = partTypeRepository.getPartTypes(currentUserId)
            val productsList = partProductRepository.getPartProducts(currentUserId)
            val lastOdometer = routeRepository.getLastOdometerKm(currentUserId) ?: BigDecimal.ZERO
            val cards = creditCardRepository.getCreditCards(currentUserId)
            val brands = creditCardRepository.getCardBrands(currentUserId)
            val operators = creditCardRepository.getCardOperators(currentUserId)

            _uiState.update {
                it.copy(
                    parts = partsList,
                    companies = companiesList,
                    partTypes = typesList,
                    partProducts = productsList,
                    currentOdometerKm = lastOdometer,
                    availableCards = cards,
                    availableBrands = brands,
                    availableOperators = operators,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddDialog(prefillPartName: String? = null) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPartId = null,
                editingExpenseId = null,
                partName = prefillPartName ?: "",
                lifeKm = "",
                lastChangeKm = "",
                selectedCompanyId = null,
                selectedPartProductId = null,
                partBrand = "",
                partModel = "",
                totalAmountText = "",
                lastChangeDateTime = LocalDateTime.now(),
                receiptNumber = "",
                notes = "",
                paymentMethod = "dinheiro",
                cardPaymentData = null,
                error = null
            )
        }
    }

    fun startEditing(part: PartMaintenance) {
        viewModelScope.launch {
            val linkedProduct = _uiState.value.partProducts.firstOrNull { it.id == part.partProductId }
            val initialDateTime = part.lastChangeAt.toLocalDateTime()

            _uiState.update {
                it.copy(
                    isFormOpen = true,
                    editingPartId = part.id,
                    editingExpenseId = part.expenseId,
                    partName = part.partName,
                    lifeKm = part.lifeKm.toPlainString(),
                    lastChangeKm = part.lastChangeKm.toPlainString(),
                    selectedCompanyId = part.companyId,
                    selectedPartProductId = part.partProductId,
                    partBrand = linkedProduct?.brand ?: "",
                    partModel = linkedProduct?.model ?: "",
                    lastChangeDateTime = initialDateTime,
                    totalAmountText = "",
                    receiptNumber = "",
                    notes = "",
                    paymentMethod = "dinheiro",
                    cardPaymentData = null,
                    error = null
                )
            }

            // Se tiver expenseId vinculado, busca os dados financeiros para preenchimento
            if (!part.expenseId.isNullOrBlank()) {
                val expenses = expenseRepository.getExpenses(currentUserId)
                val linkedExpense = expenses.firstOrNull { it.id == part.expenseId }
                if (linkedExpense != null) {
                    val cardData = if (linkedExpense.paymentMethod == "cartao" && linkedExpense.cardId != null) {
                        CardPaymentData(
                            cardId = linkedExpense.cardId,
                            cardBrand = linkedExpense.cardBrand,
                            cardOperator = linkedExpense.cardOperator,
                            cardDueDay = linkedExpense.cardDueDay,
                            isInstallment = (linkedExpense.installmentTotal ?: 1) > 1,
                            installmentTotal = linkedExpense.installmentTotal,
                            firstInstallmentMonth = null,
                            installmentGroupId = linkedExpense.installmentGroupId
                        )
                    } else null

                    _uiState.update {
                        it.copy(
                            totalAmountText = linkedExpense.amount.toPlainString(),
                            lastChangeDateTime = linkedExpense.occurredAt.toLocalDateTime(),
                            receiptNumber = linkedExpense.receiptNumber ?: "",
                            notes = linkedExpense.description ?: "",
                            paymentMethod = linkedExpense.paymentMethod,
                            partBrand = linkedExpense.partBrand ?: it.partBrand,
                            partModel = linkedExpense.partModel ?: it.partModel,
                            cardPaymentData = cardData
                        )
                    }
                }
            }
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingPartId = null,
                editingExpenseId = null,
                partName = "",
                lifeKm = "",
                lastChangeKm = "",
                selectedCompanyId = null,
                selectedPartProductId = null,
                partBrand = "",
                partModel = "",
                totalAmountText = "",
                receiptNumber = "",
                notes = "",
                paymentMethod = "dinheiro",
                cardPaymentData = null,
                error = null
            )
        }
    }

    // Seleção de Produto
    fun onSelectProduct(product: PartProduct) {
        val type = _uiState.value.partTypes.firstOrNull { it.id == product.partTypeId }
        val typeName = type?.name ?: "Peça"
        val modelText = if (!product.model.isNullOrBlank()) " ${product.model}" else ""
        val autoName = "$typeName — ${product.brand}$modelText"

        _uiState.update {
            it.copy(
                selectedPartProductId = product.id,
                partName = autoName,
                lifeKm = product.defaultLifeKm.toPlainString(),
                partBrand = product.brand,
                partModel = product.model ?: ""
            )
        }
    }

    fun clearSelectedProduct() {
        _uiState.update { it.copy(selectedPartProductId = null) }
    }

    fun onPartNameChanged(name: String) = _uiState.update { it.copy(partName = name) }
    fun onLifeKmChanged(lifeKm: String) = _uiState.update { it.copy(lifeKm = lifeKm.filter { c -> c.isDigit() || c == '.' }) }
    fun onLastChangeKmChanged(lastChangeKm: String) = _uiState.update { it.copy(lastChangeKm = lastChangeKm.filter { c -> c.isDigit() || c == '.' }) }
    fun onCompanySelected(companyId: String?) = _uiState.update { it.copy(selectedCompanyId = companyId) }

    fun onPartBrandChanged(brand: String) = _uiState.update { it.copy(partBrand = brand) }
    fun onPartModelChanged(model: String) = _uiState.update { it.copy(partModel = model) }
    fun onTotalAmountChanged(text: String) = _uiState.update { it.copy(totalAmountText = text.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')) }
    fun onLastChangeDateTimeChanged(dt: LocalDateTime) = _uiState.update { it.copy(lastChangeDateTime = dt) }
    fun onReceiptNumberChanged(receipt: String) = _uiState.update { it.copy(receiptNumber = receipt) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun onPaymentMethodSelected(method: String) = _uiState.update { it.copy(paymentMethod = method) }
    fun onCardPaymentConfirmed(cardData: CardPaymentData) = _uiState.update { it.copy(cardPaymentData = cardData, paymentMethod = "cartao") }

    fun addCardBrand(name: String) {
        viewModelScope.launch {
            creditCardRepository.createCardBrand(currentUserId, name.trim())
            val updated = creditCardRepository.getCardBrands(currentUserId)
            _uiState.update { it.copy(availableBrands = updated) }
        }
    }

    fun addCardOperator(name: String) {
        viewModelScope.launch {
            creditCardRepository.createCardOperator(currentUserId, name.trim())
            val updated = creditCardRepository.getCardOperators(currentUserId)
            _uiState.update { it.copy(availableOperators = updated) }
        }
    }

    fun openAddProductDialog() {
        val defaultType = _uiState.value.partTypes.firstOrNull()?.id
        _uiState.update {
            it.copy(
                isAddProductDialogOpen = true,
                quickProductTypeId = defaultType,
                quickProductBrand = "",
                quickProductModel = "",
                quickProductLifeKm = "",
                error = null
            )
        }
    }

    fun closeAddProductDialog() = _uiState.update { it.copy(isAddProductDialogOpen = false) }

    fun onQuickProductTypeChanged(typeId: String?) = _uiState.update { it.copy(quickProductTypeId = typeId) }
    fun onQuickProductBrandChanged(brand: String) = _uiState.update { it.copy(quickProductBrand = brand) }
    fun onQuickProductModelChanged(model: String) = _uiState.update { it.copy(quickProductModel = model) }
    fun onQuickProductLifeKmChanged(lifeKm: String) = _uiState.update { it.copy(quickProductLifeKm = lifeKm.filter { c -> c.isDigit() || c == '.' }) }

    fun openAddTypeDialog() = _uiState.update { it.copy(isAddTypeDialogOpen = true) }
    fun closeAddTypeDialog() = _uiState.update { it.copy(isAddTypeDialogOpen = false) }

    fun createQuickPartType(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newType = PartType(id = "", userId = currentUserId, name = name.trim())
                val created = partTypeRepository.savePartType(newType)
                val updatedTypes = partTypeRepository.getPartTypes(currentUserId)
                _uiState.update {
                    it.copy(
                        partTypes = updatedTypes,
                        quickProductTypeId = created.id,
                        isAddTypeDialogOpen = false,
                        message = "Tipo de peça '${created.name}' criado!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar tipo: ${e.message}") }
            }
        }
    }

    fun saveQuickProduct() {
        val state = _uiState.value
        val typeId = state.quickProductTypeId
        if (typeId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Selecione o tipo de peça.") }
            return
        }
        if (state.quickProductBrand.isBlank()) {
            _uiState.update { it.copy(error = "Informe a marca da peça.") }
            return
        }
        val defaultLife = state.quickProductLifeKm.toBigDecimalOrNull()
        if (defaultLife == null || defaultLife <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Informe uma vida útil padrão em KM.") }
            return
        }

        viewModelScope.launch {
            try {
                val product = PartProduct(
                    id = "",
                    userId = currentUserId,
                    partTypeId = typeId,
                    brand = state.quickProductBrand.trim(),
                    model = state.quickProductModel.trim().takeIf { it.isNotBlank() },
                    defaultLifeKm = defaultLife
                )
                val created = partProductRepository.savePartProduct(product)
                val updatedProducts = partProductRepository.getPartProducts(currentUserId)
                _uiState.update {
                    it.copy(
                        partProducts = updatedProducts,
                        isAddProductDialogOpen = false
                    )
                }
                onSelectProduct(created)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao cadastrar produto: ${e.message}") }
            }
        }
    }

    fun openAddCompanyDialog() = _uiState.update { it.copy(isAddCompanyDialogOpen = true) }
    fun closeAddCompanyDialog() = _uiState.update { it.copy(isAddCompanyDialogOpen = false) }

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
                _uiState.update { state ->
                    state.copy(
                        companies = updatedCompanies,
                        selectedCompanyId = created.id,
                        isAddCompanyDialogOpen = false,
                        message = "Empresa '${created.name}' cadastrada e selecionada!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao cadastrar empresa: ${e.message}") }
            }
        }
    }

    fun savePartMaintenance(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.partName.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da peça.") }
            return
        }

        val lifeKmDecimal = state.lifeKm.toBigDecimalOrNull()
        if (lifeKmDecimal == null || lifeKmDecimal <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Informe uma vida útil válida em KM.") }
            return
        }

        val lastChangeKmDecimal = state.lastChangeKm.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val finalOffsetDateTime = state.lastChangeDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            var savedExpenseId: String? = state.editingExpenseId
            val amountDecimal = state.totalAmountText.toBigDecimalOrNull()

            // 1. Criar ou Atualizar Registro Financeiro (Expense) se houver valor informado
            if (amountDecimal != null && amountDecimal > BigDecimal.ZERO) {
                try {
                    val companyName = state.companies.firstOrNull { it.id == state.selectedCompanyId }?.name
                    val expense = Expense(
                        id = state.editingExpenseId ?: "",
                        userId = currentUserId,
                        category = "manutencao",
                        title = "Manutenção: ${state.partName.trim()}",
                        vendor = companyName,
                        amount = amountDecimal,
                        odometerKm = lastChangeKmDecimal.takeIf { it > BigDecimal.ZERO },
                        description = state.notes.trim().takeIf { it.isNotBlank() },
                        paymentMethod = state.paymentMethod,
                        receiptNumber = state.receiptNumber.trim().takeIf { it.isNotBlank() },
                        occurredAt = finalOffsetDateTime,
                        partBrand = state.partBrand.trim().takeIf { it.isNotBlank() },
                        partModel = state.partModel.trim().takeIf { it.isNotBlank() },
                        companyId = state.selectedCompanyId?.takeIf { it.isNotBlank() },
                        cardId = state.cardPaymentData?.cardId,
                        cardBrand = state.cardPaymentData?.cardBrand,
                        cardOperator = state.cardPaymentData?.cardOperator,
                        cardDueDay = state.cardPaymentData?.cardDueDay,
                        installmentGroupId = state.cardPaymentData?.installmentGroupId,
                        installmentNumber = if (state.cardPaymentData?.isInstallment == true) 1 else null,
                        installmentTotal = state.cardPaymentData?.installmentTotal
                    )

                    if (state.editingExpenseId.isNullOrBlank()) {
                        val createdExpense = expenseRepository.createExpense(expense)
                        savedExpenseId = createdExpense.id
                    } else {
                        val updatedExpense = expenseRepository.updateExpense(expense)
                        savedExpenseId = updatedExpense.id
                    }
                } catch (e: Exception) {
                    // Log or handle expense creation error
                }
            }

            // 2. Salvar Registro de Monitoramento de Peça (PartMaintenance)
            val part = PartMaintenance(
                id = state.editingPartId ?: "",
                userId = currentUserId,
                partName = state.partName.trim(),
                lifeKm = lifeKmDecimal,
                lastChangeKm = lastChangeKmDecimal,
                lastChangeAt = finalOffsetDateTime,
                companyId = state.selectedCompanyId?.takeIf { it.isNotBlank() },
                partProductId = state.selectedPartProductId?.takeIf { it.isNotBlank() },
                expenseId = savedExpenseId
            )

            try {
                partRepository.savePartMaintenance(part)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingPartId != null) "Manutenção atualizada com sucesso!" else "Manutenção lançada com sucesso!"
                    )
                }
                loadData()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar manutenção: ${e.message}") }
            }
        }
    }

    fun deletePartMaintenance(partId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val part = _uiState.value.parts.firstOrNull { it.id == partId }
            if (part?.expenseId != null) {
                try {
                    expenseRepository.deleteExpense(part.expenseId)
                } catch (e: Exception) {
                    // Ignore expense delete error
                }
            }

            val success = partRepository.deletePartMaintenance(partId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingPartId = null,
                        editingExpenseId = null,
                        message = "Peça excluída com sucesso!"
                    )
                }
                loadData()
                onSuccess()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir peça.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
