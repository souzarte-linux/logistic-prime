package com.fernando.centraldomotorista.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.repository.HomeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

data class HomeUiState(
    val profile: Profile? = null,
    val lucroHoje: BigDecimal = BigDecimal.ZERO,
    val metaDiaria: BigDecimal = BigDecimal("200"),
    val faltamParaMeta: BigDecimal = BigDecimal("200"),
    val sessaoAtiva: Boolean = false,
    val alertaManutencao: PartMaintenance? = null,
    val kmUltrapassado: BigDecimal = BigDecimal.ZERO,
    val contasAReceber: BigDecimal = BigDecimal.ZERO,
    val rotasRecentes: List<Route> = emptyList(),
    val notificacoesNaoLidas: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

class HomeViewModel(
    private val homeRepository: HomeRepository = HomeRepository(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(loading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    private fun loadData() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = "Usuário não autenticado."
            )
            return
        }

        _uiState.value = _uiState.value.copy(loading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("HomeViewModel", "Carregando dados da tela Início para UID: ${user.uid}")
                val data = homeRepository.loadHomeData(
                    userId = user.uid,
                    email = user.email,
                    fullName = user.displayName,
                    avatarUrl = user.photoUrl?.toString()
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = HomeUiState(
                        profile = data.profile,
                        lucroHoje = data.lucroHoje,
                        metaDiaria = data.metaDiaria,
                        faltamParaMeta = data.faltamParaMeta,
                        sessaoAtiva = data.sessaoAtiva,
                        alertaManutencao = data.alertaManutencao,
                        kmUltrapassado = data.kmUltrapassado,
                        contasAReceber = data.contasAReceber,
                        rotasRecentes = data.rotasRecentes,
                        notificacoesNaoLidas = data.notificacoesNaoLidas,
                        loading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao carregar dados: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Erro ao carregar dados: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun createQuickExpense(
        category: String,
        amount: BigDecimal,
        onSuccess: () -> Unit
    ) {
        val user = firebaseAuth.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(loading = true)
                val expense = homeRepository.createQuickExpense(
                    userId = user.uid,
                    category = category,
                    amount = amount
                )
                Log.d("HomeViewModel", "Despesa rápida criada com sucesso: $expense")
                
                // Recarregar dados após inclusão
                val updatedData = homeRepository.loadHomeData(
                    userId = user.uid,
                    email = user.email,
                    fullName = user.displayName,
                    avatarUrl = user.photoUrl?.toString()
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = HomeUiState(
                        profile = updatedData.profile,
                        lucroHoje = updatedData.lucroHoje,
                        metaDiaria = updatedData.metaDiaria,
                        faltamParaMeta = updatedData.faltamParaMeta,
                        sessaoAtiva = updatedData.sessaoAtiva,
                        alertaManutencao = updatedData.alertaManutencao,
                        kmUltrapassado = updatedData.kmUltrapassado,
                        contasAReceber = updatedData.contasAReceber,
                        rotasRecentes = updatedData.rotasRecentes,
                        notificacoesNaoLidas = updatedData.notificacoesNaoLidas,
                        loading = false,
                        actionMessage = "Despesa de R$ $amount registrada!"
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao lançar despesa: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Erro ao salvar despesa: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }
}
