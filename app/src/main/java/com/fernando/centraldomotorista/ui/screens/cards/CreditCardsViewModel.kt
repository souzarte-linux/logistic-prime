package com.fernando.centraldomotorista.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.data.repository.CreditCardRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreditCardsUiState(
    val cards: List<CreditCard> = emptyList(),
    val brands: List<CardBrand> = emptyList(),
    val operators: List<CardOperator> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // Add Dialog
    val isAddDialogOpen: Boolean = false,
    val holderName: String = "",
    val nickname: String = "",
    val firstFour: String = "",
    val lastFour: String = "",
    val selectedBrandId: String? = null,
    val selectedIssuerId: String? = null,
    val dueDay: String = "10",
    val closingDay: String = "3",
    val cardType: String = "credito"
)

class CreditCardsViewModel(
    private val repository: CreditCardRepository = CreditCardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardsUiState())
    val uiState: StateFlow<CreditCardsUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val cards = repository.getCreditCards(currentUserId)
            val brands = repository.getCardBrands(currentUserId)
            val operators = repository.getCardOperators(currentUserId)
            _uiState.update {
                it.copy(
                    cards = cards,
                    brands = brands,
                    operators = operators,
                    isLoading = false
                )
            }
        }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                isAddDialogOpen = true,
                holderName = "",
                nickname = "",
                firstFour = "",
                lastFour = "",
                selectedBrandId = it.brands.firstOrNull()?.id,
                selectedIssuerId = it.operators.firstOrNull()?.id,
                dueDay = "10",
                closingDay = "3",
                cardType = "credito"
            )
        }
    }

    fun closeAddDialog() = _uiState.update { it.copy(isAddDialogOpen = false) }

    fun onHolderNameChanged(v: String) = _uiState.update { it.copy(holderName = v) }
    fun onNicknameChanged(v: String) = _uiState.update { it.copy(nickname = v) }
    fun onFirstFourChanged(v: String) = _uiState.update { it.copy(firstFour = v.take(4).filter { c -> c.isDigit() }) }
    fun onLastFourChanged(v: String) = _uiState.update { it.copy(lastFour = v.take(4).filter { c -> c.isDigit() }) }
    fun onBrandSelected(brandId: String?) = _uiState.update { it.copy(selectedBrandId = brandId) }
    fun onIssuerSelected(issuerId: String?) = _uiState.update { it.copy(selectedIssuerId = issuerId) }
    fun onDueDayChanged(v: String) = _uiState.update { it.copy(dueDay = v.filter { c -> c.isDigit() }.take(2)) }
    fun onClosingDayChanged(v: String) = _uiState.update { it.copy(closingDay = v.filter { c -> c.isDigit() }.take(2)) }
    fun onCardTypeChanged(v: String) = _uiState.update { it.copy(cardType = v) }

    fun saveCard() {
        val state = _uiState.value
        if (state.holderName.isBlank() || state.nickname.isBlank() || state.lastFour.length < 4) {
            _uiState.update { it.copy(error = "Preencha o titular, apelido e os 4 últimos dígitos.") }
            return
        }

        val due = state.dueDay.toIntOrNull() ?: 10
        val closing = state.closingDay.toIntOrNull() ?: 3

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val card = CreditCard(
                id = "",
                userId = currentUserId,
                holderName = state.holderName.trim(),
                nickname = state.nickname.trim(),
                firstFour = state.firstFour.trim().ifBlank { null },
                lastFour = state.lastFour.trim(),
                brandId = state.selectedBrandId,
                issuerId = state.selectedIssuerId,
                dueDay = due.coerceIn(1, 31),
                closingDay = closing.coerceIn(1, 31),
                cardType = state.cardType,
                active = true
            )

            try {
                repository.saveCreditCard(card)
                _uiState.update { it.copy(isSaving = false, isAddDialogOpen = false, message = "Cartão cadastrado com sucesso!") }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao cadastrar cartão: ${e.message}") }
            }
        }
    }

    fun toggleCardActive(card: CreditCard, newActive: Boolean) {
        viewModelScope.launch {
            val success = repository.toggleCardActive(card.id, newActive, card)
            if (success) {
                _uiState.update { state ->
                    val updated = state.cards.map {
                        if (it.id == card.id) it.copy(active = newActive) else it
                    }
                    state.copy(cards = updated)
                }
            } else {
                _uiState.update { it.copy(error = "Erro ao atualizar status do cartão.") }
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            val success = repository.deleteCreditCard(cardId)
            if (success) {
                _uiState.update { it.copy(message = "Cartão excluído!") }
                loadData()
            } else {
                _uiState.update { it.copy(error = "Erro ao excluir cartão.") }
            }
        }
    }

    fun addBrand(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = repository.createCardBrand(currentUserId, name.trim())
                _uiState.update {
                    it.copy(
                        brands = it.brands + created,
                        selectedBrandId = created.id,
                        message = "Bandeira adicionada!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar bandeira: ${e.message}") }
            }
        }
    }

    fun addOperator(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val created = repository.createCardOperator(currentUserId, name.trim())
                _uiState.update {
                    it.copy(
                        operators = it.operators + created,
                        selectedIssuerId = created.id,
                        message = "Emissor adicionado!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar emissor: ${e.message}") }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(message = null, error = null) }
}
