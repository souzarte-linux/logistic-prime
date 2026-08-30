package com.fernando.centraldomotorista.ui.screens.empresas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CompanyRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val POPULAR_COMPANIES = listOf(
    "Oficina Mecânica",
    "Auto Peças & Acessórios",
    "Lava Rápido / Estética",
    "Borracharia & Pneus",
    "Troca de Óleo Rápida",
    "Concessionária",
    "Seguradora / Proteção Veicular",
    "Cooperativa / Transportadora"
)

data class EmpresasUiState(
    val companies: List<Company> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    
    // Form fields
    val editingCompanyId: String? = null,
    val name: String = ""
)

class EmpresasViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmpresasUiState())
    val uiState: StateFlow<EmpresasUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadCompanies()
    }

    fun loadCompanies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val list = repository.getCompanies(currentUserId)
            _uiState.update {
                it.copy(
                    companies = list,
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
                editingCompanyId = null,
                name = prefillName ?: "",
                error = null
            )
        }
    }

    fun startEditing(company: Company) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingCompanyId = company.id,
                name = company.name,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingCompanyId = null,
                name = "",
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun saveCompany(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da empresa.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val company = Company(
                id = state.editingCompanyId ?: "",
                userId = currentUserId,
                name = state.name.trim()
            )

            try {
                repository.saveCompany(company)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingCompanyId != null) "Empresa atualizada com sucesso!" else "Empresa cadastrada com sucesso!"
                    )
                }
                loadCompanies()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar empresa: ${e.message}") }
            }
        }
    }

    fun deleteCompany(companyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = repository.deleteCompany(companyId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingCompanyId = null,
                        message = "Empresa excluída com sucesso!"
                    )
                }
                loadCompanies()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir empresa.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
