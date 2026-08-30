package com.fernando.centraldomotorista.ui.screens.bandeiras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CardBrandRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val POPULAR_BRANDS = listOf(
    "Mastercard",
    "Visa",
    "Elo",
    "Hipercard",
    "American Express",
    "Alelo",
    "VR Benefícios",
    "Ticket",
    "Sodexo / Pluxee",
    "Flash",
    "Caju",
    "Swile"
)

data class BandeirasUiState(
    val brands: List<CardBrand> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    
    // Form fields
    val editingBrandId: String? = null,
    val name: String = ""
)

class BandeirasViewModel(
    private val repository: CardBrandRepository = CardBrandRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BandeirasUiState())
    val uiState: StateFlow<BandeirasUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadBrands()
    }

    fun loadBrands() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getCardBrands(currentUserId)
            _uiState.update {
                it.copy(
                    brands = list,
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
                editingBrandId = null,
                name = prefillName ?: "",
                error = null
            )
        }
    }

    fun startEditing(brand: CardBrand) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingBrandId = brand.id,
                name = brand.name,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingBrandId = null,
                name = "",
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun saveBrand(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da bandeira.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val brand = CardBrand(
                id = state.editingBrandId ?: "",
                userId = currentUserId,
                name = state.name.trim()
            )

            try {
                repository.saveCardBrand(brand)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingBrandId != null) "Bandeira atualizada com sucesso!" else "Bandeira cadastrada com sucesso!"
                    )
                }
                loadBrands()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar bandeira: ${e.message}") }
            }
        }
    }

    fun deleteBrand(brandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deleteCardBrand(brandId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingBrandId = null,
                        message = "Bandeira excluída com sucesso!"
                    )
                }
                loadBrands()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir bandeira.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
