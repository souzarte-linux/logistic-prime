package com.fernando.centraldomotorista.ui.screens.pecas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CompanyRepository
import com.fernando.centraldomotorista.data.repository.PartMaintenanceRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.OffsetDateTime

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
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val isAddCompanyDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields
    val editingPartId: String? = null,
    val partName: String = "",
    val lifeKm: String = "",
    val lastChangeKm: String = "",
    val selectedCompanyId: String? = null
)

class PartMaintenanceViewModel(
    private val partRepository: PartMaintenanceRepository = PartMaintenanceRepository(),
    private val companyRepository: CompanyRepository = CompanyRepository()
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
            _uiState.update {
                it.copy(
                    parts = partsList,
                    companies = companiesList,
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
                partName = prefillPartName ?: "",
                lifeKm = "",
                lastChangeKm = "",
                selectedCompanyId = null,
                error = null
            )
        }
    }

    fun startEditing(part: PartMaintenance) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPartId = part.id,
                partName = part.partName,
                lifeKm = part.lifeKm.toPlainString(),
                lastChangeKm = part.lastChangeKm.toPlainString(),
                selectedCompanyId = part.companyId,
                error = null
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                editingPartId = null,
                partName = "",
                lifeKm = "",
                lastChangeKm = "",
                selectedCompanyId = null,
                error = null
            )
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

    fun onPartNameChanged(name: String) = _uiState.update { it.copy(partName = name) }
    fun onLifeKmChanged(lifeKm: String) = _uiState.update { it.copy(lifeKm = lifeKm.filter { c -> c.isDigit() || c == '.' }) }
    fun onLastChangeKmChanged(lastChangeKm: String) = _uiState.update { it.copy(lastChangeKm = lastChangeKm.filter { c -> c.isDigit() || c == '.' }) }
    fun onCompanySelected(companyId: String?) = _uiState.update { it.copy(selectedCompanyId = companyId) }

    fun savePartMaintenance() {
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

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val part = PartMaintenance(
                id = state.editingPartId ?: "",
                userId = currentUserId,
                partName = state.partName.trim(),
                lifeKm = lifeKmDecimal,
                lastChangeKm = lastChangeKmDecimal,
                lastChangeAt = OffsetDateTime.now(),
                companyId = state.selectedCompanyId?.takeIf { it.isNotBlank() }
            )

            try {
                partRepository.savePartMaintenance(part)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isFormOpen = false,
                        message = if (state.editingPartId != null) "Peça atualizada com sucesso!" else "Peça cadastrada com sucesso!"
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar peça: ${e.message}") }
            }
        }
    }

    fun deletePartMaintenance(partId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = partRepository.deletePartMaintenance(partId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        editingPartId = null,
                        message = "Peça excluída com sucesso!"
                    )
                }
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao excluir peça.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
