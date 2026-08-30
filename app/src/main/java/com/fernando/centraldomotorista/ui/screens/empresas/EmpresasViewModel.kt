package com.fernando.centraldomotorista.ui.screens.empresas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.remote.api.ViaCepApi
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
    val isSearchingCep: Boolean = false,
    val isFormOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields
    val editingCompanyId: String? = null,
    val name: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val cnpj: String = "",
    val phone: String = "",
    val isWhatsapp: Boolean = false,
    val socialMedia: String = "",
    val website: String = ""
)

class EmpresasViewModel(
    private val repository: CompanyRepository = CompanyRepository(),
    private val viaCepApi: ViaCepApi = ViaCepApi.instance
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
                cep = "",
                street = "",
                number = "",
                complement = "",
                cnpj = "",
                phone = "",
                isWhatsapp = false,
                socialMedia = "",
                website = "",
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
                cep = formatCep(company.cep ?: ""),
                street = company.street ?: "",
                number = company.number ?: "",
                complement = company.complement ?: "",
                cnpj = formatCnpj(company.cnpj ?: ""),
                phone = formatPhone(company.phone ?: ""),
                isWhatsapp = company.isWhatsapp,
                socialMedia = company.socialMedia ?: "",
                website = company.website ?: "",
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
                cep = "",
                street = "",
                number = "",
                complement = "",
                cnpj = "",
                phone = "",
                isWhatsapp = false,
                socialMedia = "",
                website = "",
                error = null
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun onCepChanged(cep: String) {
        val formatted = formatCep(cep)
        _uiState.update { it.copy(cep = formatted) }

        val digits = cep.filter { it.isDigit() }
        if (digits.length == 8) {
            searchCep(digits)
        }
    }

    private fun searchCep(cepDigits: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCep = true) }
            try {
                val result = viaCepApi.getAddressByCep(cepDigits)
                if (result.erro != true) {
                    _uiState.update { state ->
                        state.copy(
                            isSearchingCep = false,
                            street = result.logradouro ?: state.street,
                            message = "Endereço preenchido pelo CEP!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSearchingCep = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingCep = false) }
            }
        }
    }

    fun onStreetChanged(street: String) = _uiState.update { it.copy(street = street) }
    fun onNumberChanged(number: String) = _uiState.update { it.copy(number = number) }
    fun onComplementChanged(complement: String) {
        if (complement.length <= 500) {
            _uiState.update { it.copy(complement = complement) }
        }
    }

    fun onCnpjChanged(cnpj: String) {
        val formatted = formatCnpj(cnpj)
        _uiState.update { it.copy(cnpj = formatted) }
    }

    fun onPhoneChanged(phone: String) {
        val formatted = formatPhone(phone)
        _uiState.update { it.copy(phone = formatted) }
    }

    fun onIsWhatsappChanged(isWhatsapp: Boolean) = _uiState.update { it.copy(isWhatsapp = isWhatsapp) }
    fun onSocialMediaChanged(socialMedia: String) = _uiState.update { it.copy(socialMedia = socialMedia) }
    fun onWebsiteChanged(website: String) = _uiState.update { it.copy(website = website) }

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
                name = state.name.trim(),
                cep = state.cep.trim().takeIf { it.isNotBlank() },
                street = state.street.trim().takeIf { it.isNotBlank() },
                number = state.number.trim().takeIf { it.isNotBlank() },
                complement = state.complement.trim().takeIf { it.isNotBlank() },
                cnpj = state.cnpj.filter { it.isDigit() }.takeIf { it.isNotBlank() },
                phone = state.phone.trim().takeIf { it.isNotBlank() },
                isWhatsapp = state.isWhatsapp,
                socialMedia = state.socialMedia.trim().takeIf { it.isNotBlank() },
                website = state.website.trim().takeIf { it.isNotBlank() }
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

    companion object {
        fun formatCep(raw: String): String {
            val digits = raw.filter { it.isDigit() }.take(8)
            return if (digits.length > 5) "${digits.take(5)}-${digits.substring(5)}" else digits
        }

        fun formatCnpj(raw: String): String {
            val digits = raw.filter { it.isDigit() }.take(14)
            val sb = StringBuilder()
            for (i in digits.indices) {
                if (i == 2 || i == 5) sb.append('.')
                else if (i == 8) sb.append('/')
                else if (i == 12) sb.append('-')
                sb.append(digits[i])
            }
            return sb.toString()
        }

        fun formatPhone(raw: String): String {
            val digits = raw.filter { it.isDigit() }.take(11)
            if (digits.isEmpty()) return ""
            val sb = StringBuilder("(")
            if (digits.length <= 2) {
                sb.append(digits)
            } else {
                sb.append(digits.substring(0, 2)).append(") ")
                val remaining = digits.substring(2)
                if (remaining.length <= 4) {
                    sb.append(remaining)
                } else if (remaining.length <= 8) {
                    sb.append(remaining.substring(0, 4)).append("-").append(remaining.substring(4))
                } else {
                    sb.append(remaining.substring(0, 5)).append("-").append(remaining.substring(5))
                }
            }
            return sb.toString()
        }
    }
}
