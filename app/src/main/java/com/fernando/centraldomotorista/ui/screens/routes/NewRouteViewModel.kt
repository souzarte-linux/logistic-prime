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
import java.util.Locale

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
    
    // Gorjeta / Bônus / Observações
    val tipText: String = "",
    val bonusText: String = "",
    val notesText: String = "",
    
    // Horários, Data e Odômetro
    val selectedDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val endTime: LocalTime = LocalTime.now().plusHours(2).withSecond(0).withNano(0),
    val breakMinutesText: String = "",
    val startKmText: String = "",
    val endKmText: String = "",
    
    val editingRouteId: String? = null,
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

    val workedMinutes: Int
        get() {
            val startMin = startTime.hour * 60 + startTime.minute
            var endMin = endTime.hour * 60 + endTime.minute
            if (endMin < startMin) {
                endMin += 24 * 60
            }
            val totalMin = endMin - startMin
            val pause = breakMinutesText.toIntOrNull() ?: 0
            return (totalMin - pause).coerceAtLeast(0)
        }

    val workedTimeFormatted: String
        get() {
            val total = workedMinutes
            val h = total / 60
            val m = total % 60
            return String.format(Locale.getDefault(), "%02dh %02dmin", h, m)
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

    fun initOrLoad(itemId: String?) {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuário não autenticado.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val platforms = platformRepository.getActivePlatforms(user.id)
                val lastOdometerKm = routeRepository.getLastOdometerKm(user.id)
                var existingRoute: Route? = null
                if (!itemId.isNullOrBlank()) {
                    existingRoute = routeRepository.getRouteById(itemId)
                }

                withContext(Dispatchers.Main) {
                    if (existingRoute != null) {
                        val zone = ZoneId.systemDefault()
                        val sDate = existingRoute.occurredAt.atZoneSameInstant(zone).toLocalDate()
                        val sTime = existingRoute.startedAt?.atZoneSameInstant(zone)?.toLocalTime() ?: LocalTime.now().withSecond(0).withNano(0)
                        val eTime = existingRoute.endedAt?.atZoneSameInstant(zone)?.toLocalTime() ?: LocalTime.now().plusHours(2).withSecond(0).withNano(0)

                        val smallCount = existingRoute.smallPackagesCount
                        val smallPrice = existingRoute.packageUnitPrice
                        val smallTotal = BigDecimal(smallCount).multiply(smallPrice).setScale(2, RoundingMode.HALF_UP)

                        val largeCount = existingRoute.largePackagesCount
                        val largePrices = existingRoute.largePackagesPrices
                        val isIndividual = largePrices.distinct().size > 1
                        val singleLargePrice = largePrices.firstOrNull() ?: BigDecimal.ZERO
                        val largeTotal = largePrices.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP)

                        _uiState.value = _uiState.value.copy(
                            editingRouteId = existingRoute.id,
                            platforms = platforms,
                            selectedPlatformId = existingRoute.platformId ?: platforms.firstOrNull()?.id,
                            selectedDate = sDate,
                            origin = existingRoute.origin ?: "",
                            destination = existingRoute.destination ?: "",
                            distanceKmText = if (existingRoute.distanceKm > BigDecimal.ZERO) existingRoute.distanceKm.toPlainString().replace('.', ',') else "",
                            selectedProductTypeCode = existingRoute.productType,
                            smallPackagesCountText = if (smallCount > 0) smallCount.toString() else "",
                            smallPackagesUnitPriceText = if (smallPrice > BigDecimal.ZERO) smallPrice.toPlainString().replace('.', ',') else "",
                            smallPackagesTotal = smallTotal,
                            largePackagesCountText = if (largeCount > 0) largeCount.toString() else "",
                            isLargePackageIndividualValue = isIndividual,
                            largePackageSingleUnitPriceText = if (singleLargePrice > BigDecimal.ZERO) singleLargePrice.toPlainString().replace('.', ',') else "",
                            largePackagesIndividualPrices = largePrices,
                            largePackagesTotal = largeTotal,
                            tipText = if (existingRoute.tip > BigDecimal.ZERO) existingRoute.tip.toPlainString().replace('.', ',') else "",
                            bonusText = if (existingRoute.bonus > BigDecimal.ZERO) existingRoute.bonus.toPlainString().replace('.', ',') else "",
                            notesText = existingRoute.notes ?: "",
                            startTime = sTime,
                            endTime = eTime,
                            breakMinutesText = if (existingRoute.breakMinutes > 0) existingRoute.breakMinutes.toString() else "",
                            startKmText = if (existingRoute.startKm > BigDecimal.ZERO) existingRoute.startKm.toPlainString().replace('.', ',') else "",
                            endKmText = if (existingRoute.endKm > BigDecimal.ZERO) existingRoute.endKm.toPlainString().replace('.', ',') else "",
                            isLoading = false
                        )
                    } else {
                        loadPlatforms()
                    }
                }
            } catch (e: Exception) {
                Log.e("NewRouteVM", "Erro ao carregar rota: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro ao carregar dados da rota.")
                }
            }
        }
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
                val lastOdometerKm = routeRepository.getLastOdometerKm(user.id)
                withContext(Dispatchers.Main) {
                    val initialStartKm = if (_uiState.value.startKmText.isEmpty() && lastOdometerKm != null && lastOdometerKm > BigDecimal.ZERO) {
                        if (lastOdometerKm.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                            lastOdometerKm.toBigInteger().toString()
                        } else {
                            lastOdometerKm.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
                        }
                    } else {
                        _uiState.value.startKmText
                    }

                    _uiState.value = _uiState.value.copy(
                        platforms = platforms,
                        selectedPlatformId = _uiState.value.selectedPlatformId ?: platforms.firstOrNull()?.id,
                        startKmText = initialStartKm,
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

    // --- Gorjeta, Bônus e Observações ---
    fun onTipChanged(tip: String) {
        _uiState.value = _uiState.value.copy(tipText = tip.filter { it.isDigit() || it == ',' || it == '.' })
    }

    fun onBonusChanged(bonus: String) {
        _uiState.value = _uiState.value.copy(bonusText = bonus.filter { it.isDigit() || it == ',' || it == '.' })
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notesText = notes)
    }

    // --- Horários e Odômetro ---
    fun onDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

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
        recalculateDistanceIfPossible(clean, _uiState.value.endKmText)
    }

    fun onEndKmChanged(kmText: String) {
        val clean = kmText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.value = _uiState.value.copy(endKmText = clean)
        recalculateDistanceIfPossible(_uiState.value.startKmText, clean)
    }

    fun onDistanceKmChanged(kmText: String) {
        val clean = kmText.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.value = _uiState.value.copy(distanceKmText = clean)
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
                val routeDate = state.selectedDate
                val zone = ZoneId.systemDefault()

                val startedAt = LocalDateTime.of(routeDate, state.startTime).atZone(zone).toOffsetDateTime()
                
                // Se horário final for anterior ao inicial, assumimos virada de dia
                val endLocalDate = if (state.endTime.isBefore(state.startTime)) routeDate.plusDays(1) else routeDate
                val endedAt = LocalDateTime.of(endLocalDate, state.endTime).atZone(zone).toOffsetDateTime()
                val occurredAt = LocalDateTime.of(routeDate, state.startTime).atZone(zone).toOffsetDateTime()

                val distanceKm = state.distanceKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val startKm = state.startKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val endKm = state.endKmText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val breakMinutes = state.breakMinutesText.toIntOrNull() ?: 0
                
                val smallCount = state.smallPackagesCountText.toIntOrNull() ?: 0
                val largeCount = state.largePackagesCountText.toIntOrNull() ?: 0
                val smallUnitPrice = state.smallPackagesUnitPriceText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

                val tip = state.tipText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val bonus = state.bonusText.replace(',', '.').trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val notes = state.notesText.trim().ifBlank { null }

                val route = Route(
                    id = state.editingRouteId ?: "",
                    userId = user.id,
                    platformId = state.selectedPlatformId,
                    origin = state.origin.trim().ifBlank { null },
                    destination = state.destination.trim().ifBlank { null },
                    distanceKm = distanceKm,
                    amount = totalAmount,
                    tip = tip,
                    bonus = bonus,
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
                    occurredAt = occurredAt
                )

                if (state.editingRouteId.isNullOrBlank()) {
                    Log.d("NewRouteVM", "Criando nova rota: $route")
                    routeRepository.createRoute(route)
                } else {
                    Log.d("NewRouteVM", "Atualizando rota existente: $route")
                    routeRepository.updateRoute(route)
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = if (state.editingRouteId.isNullOrBlank()) "Rota lançada com sucesso!" else "Rota atualizada com sucesso!"
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
