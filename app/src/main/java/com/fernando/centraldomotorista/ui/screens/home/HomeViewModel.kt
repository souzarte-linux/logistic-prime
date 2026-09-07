package com.fernando.centraldomotorista.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.AppNotification
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.HomeRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

import com.fernando.centraldomotorista.data.repository.ReceivableItem
import com.fernando.centraldomotorista.data.repository.TipoAlertaManutencao

data class HomeUiState(
    val profile: Profile? = null,
    val lucroHoje: BigDecimal = BigDecimal.ZERO,
    val ganhosHoje: BigDecimal = BigDecimal.ZERO,
    val despesasHoje: BigDecimal = BigDecimal.ZERO,
    val metaDiaria: BigDecimal = BigDecimal("200"),
    val faltamParaMeta: BigDecimal = BigDecimal("200"),
    val sessaoAtiva: Boolean = false,
    val alertaManutencao: PartMaintenance? = null,
    val tipoAlertaManutencao: TipoAlertaManutencao = TipoAlertaManutencao.NENHUM,
    val kmManutencao: BigDecimal = BigDecimal.ZERO,
    val contasAReceber: BigDecimal = BigDecimal.ZERO,
    val itensAReceber: List<ReceivableItem> = emptyList(),
    val rotasRecentes: List<Route> = emptyList(),
    val plataformasMap: Map<String, String> = emptyMap(),
    val notificacoesNaoLidas: Int = 0,
    val notificacoes: List<AppNotification> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

class HomeViewModel(
    private val homeRepository: HomeRepository = HomeRepository()
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

    fun updateDailyGoal(newGoal: BigDecimal) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val currentLucro = _uiState.value.lucroHoje
        val newFaltam = maxOf(BigDecimal.ZERO, newGoal.subtract(currentLucro))

        _uiState.value = _uiState.value.copy(
            metaDiaria = newGoal,
            faltamParaMeta = newFaltam,
            actionMessage = "Meta diária atualizada com sucesso!"
        )

        viewModelScope.launch(Dispatchers.IO) {
            val success = homeRepository.updateDailyGoal(user.id, newGoal)
            if (!success) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "Erro ao salvar meta no servidor. Verifique a conexão."
                    )
                }
            }
        }
    }

    private fun loadData() {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = "Usuário não autenticado."
            )
            return
        }

        val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
            ?: user.email
        val avatarUrl = user.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
            ?: user.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull

        _uiState.value = _uiState.value.copy(loading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("HomeViewModel", "Carregando dados da tela Início para Supabase UID: ${user.id}")
                val data = homeRepository.loadHomeData(
                    userId = user.id,
                    email = user.email,
                    fullName = fullName,
                    avatarUrl = avatarUrl
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = HomeUiState(
                        profile = data.profile,
                        lucroHoje = data.lucroHoje,
                        ganhosHoje = data.ganhosHoje,
                        despesasHoje = data.despesasHoje,
                        metaDiaria = data.metaDiaria,
                        faltamParaMeta = data.faltamParaMeta,
                        sessaoAtiva = data.sessaoAtiva,
                        alertaManutencao = data.alertaManutencao,
                        tipoAlertaManutencao = data.tipoAlertaManutencao,
                        kmManutencao = data.kmManutencao,
                        contasAReceber = data.contasAReceber,
                        itensAReceber = data.itensAReceber,
                        rotasRecentes = data.rotasRecentes,
                        plataformasMap = data.plataformasMap,
                        notificacoesNaoLidas = data.notificacoesNaoLidas,
                        notificacoes = data.notificacoes,
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

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            val currentList = _uiState.value.notificacoes
            val updatedList = currentList.filter { it.id != notificationId }
            _uiState.value = _uiState.value.copy(
                notificacoes = updatedList,
                notificacoesNaoLidas = updatedList.size,
                actionMessage = "Notificação marcada como lida!"
            )
            val success = homeRepository.markNotificationAsRead(notificationId)
            if (!success) {
                refresh()
            }
        }
    }
}
