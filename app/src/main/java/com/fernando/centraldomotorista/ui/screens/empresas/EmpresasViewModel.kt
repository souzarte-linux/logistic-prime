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

enum class EmpresaScreenMode {
    LIST,
    VIEW,
    FORM
}

data class CompanyFormData(
    val id: String? = null,
    val name: String = "",
    val cep: String = "",          // dígitos apenas (ex: "01001000")
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val cnpj: String = "",         // dígitos apenas (ex: "12345678000199")
    val phone: String = "",        // dígitos apenas (ex: "11987654321")
    val isWhatsapp: Boolean = false,
    val socialMedia: String = "",
    val website: String = ""
)

data class EmpresasUiState(
    val companies: List<Company> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSearchingCep: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    val mode: EmpresaScreenMode = EmpresaScreenMode.LIST,
    val selectedCompany: Company? = null,
    val formData: CompanyFormData = CompanyFormData(),
    val initialFormData: CompanyFormData = CompanyFormData(),
    val showDiscardAlert: Boolean = false
) {
    val isDirty: Boolean
        get() = formData != initialFormData

    val isEditing: Boolean
        get() = formData.id != null
}

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
            _uiState.update { state ->
                val updatedSelected = state.selectedCompany?.let { sel ->
                    list.firstOrNull { it.id == sel.id } ?: sel
                }
                state.copy(
                    companies = list,
                    selectedCompany = updatedSelected,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openCreateForm(prefillName: String? = null) {
        val initial = CompanyFormData(
            name = prefillName ?: ""
        )
        _uiState.update {
            it.copy(
                mode = EmpresaScreenMode.FORM,
                formData = initial,
                initialFormData = initial,
                showDiscardAlert = false,
                error = null
            )
        }
    }

    fun openViewDetails(company: Company) {
        _uiState.update {
            it.copy(
                mode = EmpresaScreenMode.VIEW,
                selectedCompany = company,
                showDiscardAlert = false,
                error = null
            )
        }
    }

    fun openEditFormFromDetails() {
        val company = _uiState.value.selectedCompany ?: return
        val initial = CompanyFormData(
            id = company.id,
            name = company.name,
            cep = company.cep?.filter { it.isDigit() } ?: "",
            street = company.street ?: "",
            number = company.number ?: "",
            complement = company.complement ?: "",
            neighborhood = company.neighborhood ?: "",
            city = company.city ?: "",
            state = company.state ?: "",
            cnpj = company.cnpj?.filter { it.isDigit() } ?: "",
            phone = company.phone?.filter { it.isDigit() } ?: "",
            isWhatsapp = company.isWhatsapp,
            socialMedia = company.socialMedia ?: "",
            website = company.website ?: ""
        )
        _uiState.update {
            it.copy(
                mode = EmpresaScreenMode.FORM,
                formData = initial,
                initialFormData = initial,
                showDiscardAlert = false,
                error = null
            )
        }
    }

    fun navigateBackFromForm(force: Boolean = false) {
        val state = _uiState.value
        if (!force && state.isDirty) {
            _uiState.update { it.copy(showDiscardAlert = true) }
            return
        }

        val previousMode = if (state.selectedCompany != null && state.formData.id != null) {
            EmpresaScreenMode.VIEW
        } else {
            EmpresaScreenMode.LIST
        }

        _uiState.update {
            it.copy(
                mode = previousMode,
                showDiscardAlert = false,
                error = null
            )
        }
    }

    fun dismissDiscardAlert() {
        _uiState.update { it.copy(showDiscardAlert = false) }
    }

    fun navigateBackFromView() {
        _uiState.update {
            it.copy(
                mode = EmpresaScreenMode.LIST,
                selectedCompany = null
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(formData = it.formData.copy(name = name)) }
    }

    fun onCepChanged(cepInput: String) {
        val digits = cepInput.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(formData = it.formData.copy(cep = digits)) }

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
                            formData = state.formData.copy(
                                street = result.logradouro ?: state.formData.street,
                                neighborhood = result.bairro ?: state.formData.neighborhood,
                                city = result.localidade ?: state.formData.city,
                                state = result.uf ?: state.formData.state
                            ),
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

    fun onStreetChanged(street: String) {
        _uiState.update { it.copy(formData = it.formData.copy(street = street)) }
    }

    fun onNumberChanged(number: String) {
        _uiState.update { it.copy(formData = it.formData.copy(number = number)) }
    }

    fun onComplementChanged(complement: String) {
        if (complement.length <= 500) {
            _uiState.update { it.copy(formData = it.formData.copy(complement = complement)) }
        }
    }

    fun onNeighborhoodChanged(neighborhood: String) {
        _uiState.update { it.copy(formData = it.formData.copy(neighborhood = neighborhood)) }
    }

    fun onCityChanged(city: String) {
        _uiState.update { it.copy(formData = it.formData.copy(city = city)) }
    }

    fun onStateChanged(state: String) {
        _uiState.update { it.copy(formData = it.formData.copy(state = state.take(2).uppercase())) }
    }

    fun onCnpjChanged(cnpjInput: String) {
        val digits = cnpjInput.filter { it.isDigit() }.take(14)
        _uiState.update { it.copy(formData = it.formData.copy(cnpj = digits)) }
    }

    fun onPhoneChanged(phoneInput: String) {
        val digits = phoneInput.filter { it.isDigit() }.take(11)
        _uiState.update { it.copy(formData = it.formData.copy(phone = digits)) }
    }

    fun onIsWhatsappChanged(isWhatsapp: Boolean) {
        _uiState.update { it.copy(formData = it.formData.copy(isWhatsapp = isWhatsapp)) }
    }

    fun onSocialMediaChanged(socialMedia: String) {
        _uiState.update { it.copy(formData = it.formData.copy(socialMedia = socialMedia)) }
    }

    fun onWebsiteChanged(website: String) {
        _uiState.update { it.copy(formData = it.formData.copy(website = website)) }
    }

    fun saveCompany() {
        val state = _uiState.value
        val form = state.formData
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da empresa.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val company = Company(
                id = form.id ?: "",
                userId = currentUserId,
                name = form.name.trim(),
                cep = form.cep.takeIf { it.isNotBlank() },
                street = form.street.trim().takeIf { it.isNotBlank() },
                number = form.number.trim().takeIf { it.isNotBlank() },
                complement = form.complement.trim().takeIf { it.isNotBlank() },
                neighborhood = form.neighborhood.trim().takeIf { it.isNotBlank() },
                city = form.city.trim().takeIf { it.isNotBlank() },
                state = form.state.trim().takeIf { it.isNotBlank() },
                cnpj = form.cnpj.takeIf { it.isNotBlank() },
                phone = form.phone.takeIf { it.isNotBlank() },
                isWhatsapp = form.isWhatsapp,
                socialMedia = form.socialMedia.trim().takeIf { it.isNotBlank() },
                website = form.website.trim().takeIf { it.isNotBlank() }
            )

            try {
                val saved = repository.saveCompany(company)
                val isEditing = form.id != null
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        mode = if (isEditing) EmpresaScreenMode.VIEW else EmpresaScreenMode.LIST,
                        selectedCompany = if (isEditing) saved else null,
                        message = if (isEditing) "Empresa atualizada com sucesso!" else "Empresa cadastrada com sucesso!"
                    )
                }
                loadCompanies()
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
                        mode = EmpresaScreenMode.LIST,
                        selectedCompany = null,
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
        fun formatCep(digits: String): String {
            val clean = digits.filter { it.isDigit() }.take(8)
            return if (clean.length > 5) "${clean.take(5)}-${clean.substring(5)}" else clean
        }

        fun formatCnpj(digits: String): String {
            val clean = digits.filter { it.isDigit() }.take(14)
            val sb = StringBuilder()
            for (i in clean.indices) {
                if (i == 2 || i == 5) sb.append('.')
                else if (i == 8) sb.append('/')
                else if (i == 12) sb.append('-')
                sb.append(clean[i])
            }
            return sb.toString()
        }

        fun formatPhone(digits: String): String {
            val clean = digits.filter { it.isDigit() }.take(11)
            if (clean.isEmpty()) return ""
            val sb = StringBuilder("(")
            if (clean.length <= 2) {
                sb.append(clean)
            } else {
                sb.append(clean.substring(0, 2)).append(") ")
                val remaining = clean.substring(2)
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
