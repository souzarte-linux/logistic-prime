package com.fernando.centraldomotorista.ui.screens.partproducts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.data.model.PartType
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.PartProductRepository
import com.fernando.centraldomotorista.data.repository.PartTypeRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PartProductsUiState(
    val partTypes: List<PartType> = emptyList(),
    val partProducts: List<PartProduct> = emptyList(),
    val selectedFilterTypeId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isProductFormOpen: Boolean = false,
    val isAddTypeDialogOpen: Boolean = false,
    val editingTypeForRename: PartType? = null,
    val typeToDelete: PartType? = null,
    val message: String? = null,
    val error: String? = null,

    // Product Form
    val editingProductId: String? = null,
    val selectedFormTypeId: String? = null,
    val brand: String = "",
    val model: String = "",
    val defaultLifeKm: String = ""
)

class PartProductsViewModel(
    private val partTypeRepository: PartTypeRepository = PartTypeRepository(),
    private val partProductRepository: PartProductRepository = PartProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartProductsUiState())
    val uiState: StateFlow<PartProductsUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val types = partTypeRepository.getPartTypes(currentUserId)
            val products = partProductRepository.getPartProducts(currentUserId)
            _uiState.update {
                it.copy(
                    partTypes = types,
                    partProducts = products,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectFilterType(partTypeId: String?) {
        _uiState.update { it.copy(selectedFilterTypeId = partTypeId) }
    }

    fun openCreateProductDialog(defaultTypeId: String? = null) {
        val initialTypeId = defaultTypeId ?: _uiState.value.selectedFilterTypeId ?: _uiState.value.partTypes.firstOrNull()?.id
        _uiState.update {
            it.copy(
                isProductFormOpen = true,
                editingProductId = null,
                selectedFormTypeId = initialTypeId,
                brand = "",
                model = "",
                defaultLifeKm = "",
                error = null
            )
        }
    }

    fun openEditProductDialog(product: PartProduct) {
        _uiState.update {
            it.copy(
                isProductFormOpen = true,
                editingProductId = product.id,
                selectedFormTypeId = product.partTypeId,
                brand = product.brand,
                model = product.model ?: "",
                defaultLifeKm = product.defaultLifeKm.toPlainString(),
                error = null
            )
        }
    }

    fun closeProductDialog() {
        _uiState.update {
            it.copy(
                isProductFormOpen = false,
                editingProductId = null,
                selectedFormTypeId = null,
                brand = "",
                model = "",
                defaultLifeKm = "",
                error = null
            )
        }
    }

    fun onBrandChanged(brand: String) = _uiState.update { it.copy(brand = brand) }
    fun onModelChanged(model: String) = _uiState.update { it.copy(model = model) }
    fun onDefaultLifeKmChanged(lifeKm: String) = _uiState.update { it.copy(defaultLifeKm = lifeKm.filter { c -> c.isDigit() || c == '.' }) }
    fun onFormTypeSelected(typeId: String?) = _uiState.update { it.copy(selectedFormTypeId = typeId) }

    fun openAddTypeDialog() = _uiState.update { it.copy(isAddTypeDialogOpen = true) }
    fun closeAddTypeDialog() = _uiState.update { it.copy(isAddTypeDialogOpen = false) }

    fun createQuickPartType(name: String, onCreated: ((PartType) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newType = PartType(
                    id = "",
                    userId = currentUserId,
                    name = name.trim()
                )
                val created = partTypeRepository.savePartType(newType)
                val updatedTypes = partTypeRepository.getPartTypes(currentUserId)
                _uiState.update { state ->
                    state.copy(
                        partTypes = updatedTypes,
                        selectedFormTypeId = created.id,
                        isAddTypeDialogOpen = false,
                        message = "Tipo de peça '${created.name}' criado!"
                    )
                }
                onCreated?.invoke(created)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar tipo de peça: ${e.message}") }
            }
        }
    }

    fun openRenameTypeDialog(partType: PartType) = _uiState.update { it.copy(editingTypeForRename = partType) }
    fun closeRenameTypeDialog() = _uiState.update { it.copy(editingTypeForRename = null) }

    fun renamePartType(partType: PartType, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                val updated = partType.copy(name = newName.trim())
                partTypeRepository.savePartType(updated)
                val updatedTypes = partTypeRepository.getPartTypes(currentUserId)
                _uiState.update {
                    it.copy(
                        partTypes = updatedTypes,
                        editingTypeForRename = null,
                        message = "Tipo de peça atualizado!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao renomear tipo: ${e.message}") }
            }
        }
    }

    fun openDeleteTypeDialog(partType: PartType) = _uiState.update { it.copy(typeToDelete = partType) }
    fun closeDeleteTypeDialog() = _uiState.update { it.copy(typeToDelete = null) }

    fun deletePartType(partTypeId: String) {
        viewModelScope.launch {
            try {
                val success = partTypeRepository.deletePartType(partTypeId)
                if (success) {
                    val updatedTypes = partTypeRepository.getPartTypes(currentUserId)
                    val updatedProducts = partProductRepository.getPartProducts(currentUserId)
                    _uiState.update { state ->
                        state.copy(
                            partTypes = updatedTypes,
                            partProducts = updatedProducts,
                            typeToDelete = null,
                            selectedFilterTypeId = if (state.selectedFilterTypeId == partTypeId) null else state.selectedFilterTypeId,
                            message = "Tipo de peça excluído!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Não foi possível excluir o tipo de peça.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao excluir: ${e.message}") }
            }
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        if (state.selectedFormTypeId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Selecione o Tipo de Peça.") }
            return
        }
        if (state.brand.isBlank()) {
            _uiState.update { it.copy(error = "Informe a Marca do produto.") }
            return
        }

        val lifeKm = state.defaultLifeKm.toBigDecimalOrNull()
        if (lifeKm == null || lifeKm <= BigDecimal.ZERO) {
            _uiState.update { it.copy(error = "Informe uma vida útil padrão válida em KM.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val product = PartProduct(
                id = state.editingProductId ?: "",
                userId = currentUserId,
                partTypeId = state.selectedFormTypeId,
                brand = state.brand.trim(),
                model = state.model.trim().takeIf { it.isNotBlank() },
                defaultLifeKm = lifeKm
            )

            try {
                partProductRepository.savePartProduct(product)
                val updatedProducts = partProductRepository.getPartProducts(currentUserId)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isProductFormOpen = false,
                        partProducts = updatedProducts,
                        message = if (state.editingProductId != null) "Produto atualizado!" else "Produto cadastrado com sucesso!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Erro ao salvar produto: ${e.message}") }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                val success = partProductRepository.deletePartProduct(productId)
                if (success) {
                    val updatedProducts = partProductRepository.getPartProducts(currentUserId)
                    _uiState.update {
                        it.copy(
                            partProducts = updatedProducts,
                            message = "Produto excluído!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao excluir produto: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
