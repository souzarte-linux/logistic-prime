package com.fernando.centraldomotorista.ui.screens.emissores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CardOperatorRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val POPULAR_EMISSORES = listOf(
    "Nubank",
    "Itaú",
    "Bradesco",
    "Banco do Brasil",
    "Santander",
    "Caixa Econômica",
    "Banco Inter",
    "C6 Bank",
    "Mercado Pago",
    "PagBank",
    "PicPay",
    "BTG Pactual"
)

data class EmissoresUiState(
    val operators: List<CardOperator> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    
    // Form fields
    val editingOperatorId: String? = null,
    val name: String = ""
)

class EmissoresViewModel(
    private val repository: CardOperatorRepository = CardOperatorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmissoresUiState())
    val uiState: StateFlow<EmissoresUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadOperators()
    }

    fun loadOperators() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getCardOperators(currentUserId)
            _uiState.update {
                it.copy(
                    operators = list,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddDialog(prefillName: String? = null) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingOperatorId = null,
                name = prefillName ?: "",
                error = null
            )
        }
    }

    fun startEditing(operator: CardOperator) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingOperatorId = operator.id,
                name = operator.name,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingOperatorId = null,
                name = "",
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun saveOperator(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome do emissor.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val operator = CardOperator(
                id = state.editingOperatorId ?: "",
                userId = currentUserId,
                name = state.name.trim()
            )

            try {
                repository.saveCardOperator(operator)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingOperatorId != null) "Emissor atualizado com sucesso!" else "Emissor cadastrado com sucesso!"
                    )
                }
                loadOperators()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar emissor: ${e.message}") }
            }
        }
    }

    fun deleteOperator(operatorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deleteCardOperator(operatorId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingOperatorId = null,
                        message = "Emissor excluído com sucesso!"
                    )
                }
                loadOperators()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir emissor.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
