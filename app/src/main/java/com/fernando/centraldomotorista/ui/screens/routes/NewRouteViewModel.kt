package com.fernando.centraldomotorista.ui.screens.routes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.data.repository.RouteRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class ProductTypeOption(val code: String, val label: String)

val AVAILABLE_PRODUCT_TYPES = listOf(
    ProductTypeOption("logistico", "Logístico"),
    ProductTypeOption("alimento", "Alimento"),
    ProductTypeOption("documento", "Documento"),
    ProductTypeOption("farmacia", "Farmácia"),
    ProductTypeOption("mercado", "Mercado"),
    ProductTypeOption("outro", "Outro")
)

data class NewRouteUiState(
    val platforms: List<Platform> = emptyList(),
    val selectedPlatformId: String? = null,
    val origin: String = "",
    val destination: String = "",
    val distanceKmText: String = "",
    val selectedProductTypeCode: String = "logistico",
    
    // Seção Pacotinhos
    val smallPackagesCountText: String = "",
    val smallPackagesUnitPriceText: String = "",
    val smallPackagesTotal: BigDecimal = BigDecimal.ZERO,
    
    // Seção Volumosos
    val largePackagesCountText: String = "",
    val isLargePackageIndividualValue: Boolean = false, // false = Valor Único, true = Valor Individual
    val largePackageSingleUnitPriceText: String = "",
    val largePackagesIndividualPrices: List<BigDecimal> = emptyList(),
    val largePackagesTotal: BigDecimal = BigDecimal.ZERO,
    
    // Gorjeta / Bônus
    val tipText: String = "",
    val bonusText: String = "",
    
    // Horários e Odômetro
    val startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val endTime: LocalTime = LocalTime.now().plusHours(2).withSecond(0).withNano(0),
    val breakMinutesText: String = "0",
    val startKmText: String = "",
    val endKmText: String = "",
    
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val totalPackagesCount: Int
        get() {
            val small = smallPackagesCountText.toIntOrNull() ?: 0
            val large = largePackagesCountText.toIntOrNull() ?: 0
            return (small + large).coerceAtLeast(0)
        }

    val totalAmount: BigDecimal
        get() {
            val tip = tipText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val bonus = bonusText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            return smallPackagesTotal
                .add(largePackagesTotal)
                .add(tip)
                .add(bonus)
        }
}

class NewRouteViewModel(
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val routeRepository: RouteRepository = RouteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewRouteUiState(isLoading = true))
    val uiState: StateFlow<NewRouteUiState> = _uiState.asStateFlow()

    init {
        loadPlatforms()
    }

    fun loadPlatforms() {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuário não autenticado.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val platforms = platformRepository.getActivePlatforms(user.id)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        platforms = platforms,
                        selectedPlatformId = _uiState.value.selectedPlatformId ?: platforms.firstOrNull()?.id,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("NewRouteVM", "Erro ao carregar plataformas: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar plataformas ativas."
                    )
                }
            }
        }
    }

    fun onPlatformSelected(platformId: String) {
        _uiState.value = _uiState.value.copy(selectedPlatformId = platformId)
    }

    fun onOriginChanged(origin: String) {
        _uiState.value = _uiState.value.copy(origin = origin)
    }

    fun onDestinationChanged(destination: String) {
        _uiState.value = _uiState.value.copy(destination = destination)
    }

    fun onDistanceKmChanged(distance: String) {
        _uiState.value = _uiState.value.copy(distanceKmText = distance)
    }

    fun onProductTypeSelected(code: String) {
        _uiState.value = _uiState.value.copy(selectedProductTypeCode = code)
    }

    // --- Pacotinhos ---
    fun onSmallPackagesCountChanged(countText: String) {
        val clean = countText.filter { it.isDigit() }
        val count = clean.toIntOrNull() ?: 0
        val unitPrice = _uiState.value.smallPackagesUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val total = BigDecimal(count).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)

        _uiState.value = _uiState.value.copy(
            smallPackagesCountText = clean,
            smallPackagesTotal = total
        )
    }

    fun onSmallPackagesUnitPriceChanged(priceText: String) {
        val clean = priceText.filter { it.isDigit() || it == ',' || it == '.' }
        val unitPrice = clean.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val count = _uiState.value.smallPackagesCountText.toIntOrNull() ?: 0
        val total = BigDecimal(count).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)

        _uiState.value = _uiState.value.copy(
            smallPackagesUnitPriceText = clean,
            smallPackagesTotal = total
        )
    }

    fun onSmallPackagesTotalChanged(totalText: String) {
        val clean = totalText.filter { it.isDigit() || it == ',' || it == '.' }
        val total = clean.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        _uiState.value = _uiState.value.copy(smallPackagesTotal = total)
    }

    // --- Volumosos ---
    fun onLargePackagesCountChanged(countText: String) {
        val clean = countText.filter { it.isDigit() }
        val count = clean.toIntOrNull() ?: 0
        
        val currentState = _uiState.value
        val total: BigDecimal
        val updatedPrices: List<BigDecimal>

        if (!currentState.isLargePackageIndividualValue) {
            // Valor Único
            val unitPrice = currentState.largePackageSingleUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            total = BigDecimal(count).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
            updatedPrices = List(count) { unitPrice }
        } else {
            // Valor Individual: ajusta tamanho da lista preservando os valores existentes
            updatedPrices = List(count) { i ->
                currentState.largePackagesIndividualPrices.getOrNull(i) ?: BigDecimal.ZERO
            }
            total = updatedPrices.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP)
        }

        _uiState.value = _uiState.value.copy(
            largePackagesCountText = clean,
            largePackagesIndividualPrices = updatedPrices,
            largePackagesTotal = total
        )
    }

    fun onLargePackagePricingModeChanged(isIndividual: Boolean) {
        val count = _uiState.value.largePackagesCountText.toIntOrNull() ?: 0
        val total: BigDecimal
        val prices: List<BigDecimal>

        if (isIndividual) {
            prices = if (_uiState.value.largePackagesIndividualPrices.size == count && count > 0) {
                _uiState.value.largePackagesIndividualPrices
            } else {
                val defaultUnit = _uiState.value.largePackageSingleUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                List(count) { defaultUnit }
            }
            total = prices.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP)
        } else {
            val unitPrice = _uiState.value.largePackageSingleUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            total = BigDecimal(count).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
            prices = List(count) { unitPrice }
        }

        _uiState.value = _uiState.value.copy(
            isLargePackageIndividualValue = isIndividual,
            largePackagesIndividualPrices = prices,
            largePackagesTotal = total
        )
    }

    fun onLargePackageSingleUnitPriceChanged(priceText: String) {
        val clean = priceText.filter { it.isDigit() || it == ',' || it == '.' }
        val unitPrice = clean.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val count = _uiState.value.largePackagesCountText.toIntOrNull() ?: 0
        val total = BigDecimal(count).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)

        _uiState.value = _uiState.value.copy(
            largePackageSingleUnitPriceText = clean,
            largePackagesTotal = total,
            largePackagesIndividualPrices = List(count) { unitPrice }
        )
    }

    fun onLargePackagesIndividualPricesConfirmed(prices: List<BigDecimal>) {
        val total = prices.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP)
        _uiState.value = _uiState.value.copy(
            largePackagesCountText = prices.size.toString(),
            largePackagesIndividualPrices = prices,
            largePackagesTotal = total,
            isLargePackageIndividualValue = true
        )
    }

    fun onLargePackagesTotalChanged(totalText: String) {
        val clean = totalText.filter { it.isDigit() || it == ',' || it == '.' }
        val total = clean.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        _uiState.value = _uiState.value.copy(largePackagesTotal = total)
    }

    // --- Gorjeta e Bônus ---
    fun onTipChanged(tip: String) {
        _uiState.value = _uiState.value.copy(tipText = tip.filter { it.isDigit() || it == ',' || it == '.' })
    }

    fun onBonusChanged(bonus: String) {
        _uiState.value = _uiState.value.copy(bonusText = bonus.filter { it.isDigit() || it == ',' || it == '.' })
    }

    // --- Horários e Odômetro ---
    fun onStartTimeChanged(time: LocalTime) {
        _uiState.value = _uiState.value.copy(startTime = time)
    }

    fun onEndTimeChanged(time: LocalTime) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun onBreakMinutesChanged(minutesText: String) {
        _uiState.value = _uiState.value.copy(breakMinutesText = minutesText.filter { it.isDigit() })
    }

    fun onStartKmChanged(kmText: String) {
        val clean = kmText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.value = _uiState.value.copy(startKmText = clean)
        recalculateDistanceIfPossible(startKm = clean, endKm = _uiState.value.endKmText)
    }

    fun onEndKmChanged(kmText: String) {
        val clean = kmText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.value = _uiState.value.copy(endKmText = clean)
        recalculateDistanceIfPossible(startKm = _uiState.value.startKmText, endKm = clean)
    }

    private fun recalculateDistanceIfPossible(startKm: String, endKm: String) {
        val start = startKm.replace(',', '.').trim().toBigDecimalOrNull()
        val end = endKm.replace(',', '.').trim().toBigDecimalOrNull()
        if (start != null && end != null && end > start) {
            val dist = end.subtract(start).setScale(1, RoundingMode.HALF_UP)
            _uiState.value = _uiState.value.copy(distanceKmText = dist.toPlainString().replace('.', ','))
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    fun saveRoute(onSuccess: () -> Unit) {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado.")
            return
        }

        val state = _uiState.value
        val totalAmount = state.totalAmount

        if (totalAmount <= BigDecimal.ZERO && state.totalPackagesCount == 0) {
            _uiState.value = _uiState.value.copy(error = "Informe a quantidade de pacotes ou valores da rota.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = OffsetDateTime.now()
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()

                val startedAt = LocalDateTime.of(today, state.startTime).atZone(zone).toOffsetDateTime()
                
                // Se horário final for anterior ao inicial, assumimos virada de dia
                val endLocalDate = if (state.endTime.isBefore(state.startTime)) today.plusDays(1) else today
                val endedAt = LocalDateTime.of(endLocalDate, state.endTime).atZone(zone).toOffsetDateTime()

                val distanceKm = state.distanceKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val startKm = state.startKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val endKm = state.endKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val breakMinutes = state.breakMinutesText.toIntOrNull() ?: 0
                
                val smallCount = state.smallPackagesCountText.toIntOrNull() ?: 0
                val largeCount = state.largePackagesCountText.toIntOrNull() ?: 0
                val smallUnitPrice = state.smallPackagesUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

                val tip = state.tipText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val bonus = state.bonusText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

                val notes = if (bonus > BigDecimal.ZERO) {
                    "Bônus: R$ ${bonus.toPlainString()}"
                } else null

                val route = Route(
                    id = "",
                    userId = user.id,
                    platformId = state.selectedPlatformId,
                    origin = state.origin.trim().ifBlank { null },
                    destination = state.destination.trim().ifBlank { null },
                    distanceKm = distanceKm,
                    amount = totalAmount,
                    tip = tip.add(bonus),
                    productType = state.selectedProductTypeCode,
                    notes = notes,
                    packageCount = (smallCount + largeCount).coerceAtLeast(1),
                    packageUnitPrice = smallUnitPrice,
                    smallPackagesCount = smallCount,
                    largePackagesCount = largeCount,
                    largePackagesPrices = state.largePackagesIndividualPrices,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    breakMinutes = breakMinutes,
                    startKm = startKm,
                    endKm = endKm,
                    billingCycleId = null,
                    occurredAt = now
                )

                Log.d("NewRouteVM", "Salvando nova rota: $route")
                val created = routeRepository.createRoute(route)
                Log.d("NewRouteVM", "Rota criada com sucesso: $created")

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Rota lançada com sucesso!"
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("NewRouteVM", "Erro ao salvar rota: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Erro ao salvar rota: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }
}
