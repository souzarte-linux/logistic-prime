package com.fernando.centraldomotorista.ui.screens.pecas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.data.model.PartType
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.CompanyRepository
import com.fernando.centraldomotorista.data.repository.PartMaintenanceRepository
import com.fernando.centraldomotorista.data.repository.PartProductRepository
import com.fernando.centraldomotorista.data.repository.PartTypeRepository
import com.fernando.centraldomotorista.data.repository.RouteRepository
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
    val partTypes: List<PartType> = emptyList(),
    val partProducts: List<PartProduct> = emptyList(),
    val currentOdometerKm: BigDecimal = BigDecimal.ZERO,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormOpen: Boolean = false,
    val isAddCompanyDialogOpen: Boolean = false,
    val isAddProductDialogOpen: Boolean = false,
    val isAddTypeDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,

    // Form fields
    val editingPartId: String? = null,
    val partName: String = "",
    val lifeKm: String = "",
    val lastChangeKm: String = "",
    val selectedCompanyId: String? = null,
    val selectedPartProductId: String? = null,

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
    private val routeRepository: RouteRepository = RouteRepository()
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
            _uiState.update {
                it.copy(
                    parts = partsList,
                    companies = companiesList,
                    partTypes = typesList,
                    partProducts = productsList,
                    currentOdometerKm = lastOdometer,
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
                selectedPartProductId = null,
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
                selectedPartProductId = part.partProductId,
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
                selectedPartProductId = null,
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
                lifeKm = product.defaultLifeKm.toPlainString()
            )
        }
    }

    fun clearSelectedProduct() {
        _uiState.update { it.copy(selectedPartProductId = null) }
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
                companyId = state.selectedCompanyId?.takeIf { it.isNotBlank() },
                partProductId = state.selectedPartProductId?.takeIf { it.isNotBlank() }
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
