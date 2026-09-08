package com.fernando.centraldomotorista.ui.screens.dailytotal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.DailyTotal
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DailyTotalRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class LancarTotalDiaUiState(
    val editingId: String? = null,
    val platforms: List<Platform> = emptyList(),
    val selectedPlatformId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val distanceKmText: String = "",
    val amountText: String = "",
    val productType: String = "alimento", // "alimento", "pacote", "documento"
    val subtractRoutes: Boolean = true,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class LancarTotalDiaViewModel(
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LancarTotalDiaUiState(isLoading = true))
    val uiState: StateFlow<LancarTotalDiaUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = supabase.auth.currentUserOrNull()?.id ?: "anonymous"

    fun initOrLoad(itemId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val platforms = platformRepository.getActivePlatforms(currentUserId)
                var editItem: DailyTotal? = null
                if (!itemId.isNullOrBlank()) {
                    editItem = dailyTotalRepository.getDailyTotalById(itemId)
                }

                withContext(Dispatchers.Main) {
                    if (editItem != null) {
                        val zone = ZoneId.systemDefault()
                        val localDateTime = editItem.occurredAt.atZoneSameInstant(zone).toLocalDateTime()
                        _uiState.value = LancarTotalDiaUiState(
                            editingId = editItem.id,
                            platforms = platforms,
                            selectedPlatformId = editItem.platformId ?: platforms.firstOrNull()?.id,
                            date = localDateTime.toLocalDate(),
                            time = localDateTime.toLocalTime(),
                            distanceKmText = if (editItem.distanceKm > BigDecimal.ZERO) editItem.distanceKm.toPlainString().replace('.', ',') else "",
                            amountText = editItem.amount.toPlainString().replace('.', ','),
                            productType = editItem.productType,
                            subtractRoutes = editItem.subtractRoutes,
                            notes = editItem.notes ?: "",
                            isLoading = false
                        )
                    } else {
                        _uiState.value = LancarTotalDiaUiState(
                            platforms = platforms,
                            selectedPlatformId = platforms.firstOrNull()?.id,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LancarTotalDiaVM", "Erro ao carregar dados: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar dados."
                    )
                }
            }
        }
    }

    fun onPlatformSelected(platformId: String) {
        _uiState.value = _uiState.value.copy(selectedPlatformId = platformId)
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onTimeChanged(time: LocalTime) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun onDistanceKmChanged(dist: String) {
        _uiState.value = _uiState.value.copy(distanceKmText = dist.filter { it.isDigit() || it == ',' || it == '.' })
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountText = amount.filter { it.isDigit() || it == ',' || it == '.' })
    }

    fun onProductTypeSelected(type: String) {
        _uiState.value = _uiState.value.copy(productType = type)
    }

    fun onSubtractRoutesChanged(subtract: Boolean) {
        _uiState.value = _uiState.value.copy(subtractRoutes = subtract)
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    fun save(onSuccess: () -> Unit) {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado.")
            return
        }

        val state = _uiState.value
        val amount = state.amountText.replace(',', '.').trim().toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.value = _uiState.value.copy(error = "Informe um valor válido.")
            return
        }

        val distance = state.distanceKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

        val zone = ZoneId.systemDefault()
        val localDateTime = LocalDateTime.of(state.date, state.time)
        val occurredAt = localDateTime.atZone(zone).toOffsetDateTime()

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dailyTotal = DailyTotal(
                    id = state.editingId ?: "",
                    userId = user.id,
                    platformId = state.selectedPlatformId,
                    amount = amount,
                    distanceKm = distance,
                    productType = state.productType,
                    subtractRoutes = state.subtractRoutes,
                    notes = state.notes.trim().ifBlank { null },
                    billingCycleId = null,
                    occurredAt = occurredAt
                )

                if (state.editingId.isNullOrBlank()) {
                    dailyTotalRepository.createDailyTotal(dailyTotal)
                } else {
                    dailyTotalRepository.updateDailyTotal(dailyTotal)
                }

                AppDataSync.notifyDataChanged()

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = if (state.editingId.isNullOrBlank()) "Total do dia registrado com sucesso!" else "Total do dia atualizado com sucesso!"
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("LancarTotalDiaVM", "Erro ao salvar total do dia: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Erro ao salvar total do dia: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }
}
