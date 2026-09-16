package com.fernando.centraldomotorista.ui.screens.relatorios

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.*
import com.fernando.centraldomotorista.ui.common.charts.FutureCashFlowItem
import com.fernando.centraldomotorista.ui.common.charts.PerformanceTimelineBucket
import com.fernando.centraldomotorista.ui.common.charts.PlatformProfitabilityBarItem
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import com.fernando.centraldomotorista.util.AppDataSync
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class RelatoriosViewModel(
    private val routeRepository: RouteRepository = RouteRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val partMaintenanceRepository: PartMaintenanceRepository = PartMaintenanceRepository(),
    private val billingCycleRepository: BillingCycleRepository = BillingCycleRepository(),
    private val financialAdjustmentRepository: FinancialAdjustmentRepository = FinancialAdjustmentRepository(),
    private val oilChangeRepository: OilChangeRepository = OilChangeRepository(),
    private val externalScope: CoroutineScope? = null,
    observeDataSync: Boolean = true,
    loadOnInit: Boolean = true,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val fixedToday: LocalDate? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(RelatoriosUiState(isLoading = true))
    val uiState: StateFlow<RelatoriosUiState> = _uiState.asStateFlow()

    // Caches internos em memória para recálculos síncronos imediatos
    private var cachedRoutes: List<Route> = emptyList()
    private var cachedDailyTotals: List<DailyTotal> = emptyList()
    private var cachedExpenses: List<Expense> = emptyList()
    private var cachedPlatforms: List<Platform> = emptyList()
    private var cachedParts: List<PartMaintenance> = emptyList()
    private var cachedBillingCycles: List<BillingCycle> = emptyList()
    private var cachedAdjustments: List<FinancialAdjustment> = emptyList()
    private var cachedOilChanges: List<OilChange> = emptyList()

    private val today: LocalDate get() = fixedToday ?: LocalDate.now(zone)

    init {
        if (loadOnInit) {
            loadData()
        }
        if (observeDataSync) {
            scope.launch {
                AppDataSync.dataChangedEvents.collect {
                    loadData()
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData()
    }

    fun loadData() {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val userId = user.id

                val routes = routeRepository.getRoutes(userId)
                val dailyTotals = dailyTotalRepository.getDailyTotals(userId)
                val expenses = expenseRepository.getExpenses(userId)
                val platforms = platformRepository.getPlatforms(userId)
                val parts = partMaintenanceRepository.getPartMaintenances(userId)
                val cycles = billingCycleRepository.getBillingCycles(userId)
                val adjustments = financialAdjustmentRepository.getFinancialAdjustments(userId)
                val oilChanges = oilChangeRepository.getOilChanges(userId)

                cachedRoutes = routes
                cachedDailyTotals = dailyTotals
                cachedExpenses = expenses
                cachedPlatforms = platforms
                cachedParts = parts
                cachedBillingCycles = cycles
                cachedAdjustments = adjustments
                cachedOilChanges = oilChanges

                withContext(Dispatchers.Default) {
                    recalculateState()
                }
            } catch (e: Exception) {
                Log.e("RelatoriosVM", "Erro ao carregar dados de relatórios: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Falha ao carregar relatórios: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setTestData(
        routes: List<Route> = emptyList(),
        dailyTotals: List<DailyTotal> = emptyList(),
        expenses: List<Expense> = emptyList(),
        platforms: List<Platform> = emptyList(),
        parts: List<PartMaintenance> = emptyList(),
        cycles: List<BillingCycle> = emptyList(),
        adjustments: List<FinancialAdjustment> = emptyList(),
        oilChanges: List<OilChange> = emptyList(),
        preset: PeriodPreset = PeriodPreset.SEMANA
    ) {
        cachedRoutes = routes
        cachedDailyTotals = dailyTotals
        cachedExpenses = expenses
        cachedPlatforms = platforms
        cachedParts = parts
        cachedBillingCycles = cycles
        cachedAdjustments = adjustments
        cachedOilChanges = oilChanges
        _uiState.update { it.copy(periodFilter = it.periodFilter.copy(preset = preset)) }
        recalculateState()
    }

    // --- Ações de Filtros e Seleções ---

    fun togglePeriodDropdown() {
        _uiState.update { it.copy(isPeriodDropdownExpanded = !it.isPeriodDropdownExpanded, isPlatformDropdownExpanded = false) }
    }

    fun applyPeriodPreset(preset: PeriodPreset) {
        _uiState.update {
            it.copy(
                periodFilter = it.periodFilter.copy(preset = preset),
                isPeriodDropdownExpanded = false
            )
        }
        recalculateState()
    }

    fun applyCustomPeriod(start: LocalDate, end: LocalDate) {
        if (end.isBefore(start) || end.isAfter(today)) {
            // Fallback seguro espelhado de Relatorios.tsx
            applyPeriodPreset(PeriodPreset.SEMANA)
            return
        }
        _uiState.update {
            it.copy(
                periodFilter = it.periodFilter.copy(
                    preset = PeriodPreset.PERSONALIZADO,
                    customStart = start,
                    customEnd = end
                ),
                isPeriodDropdownExpanded = false
            )
        }
        recalculateState()
    }

    fun togglePlatformDropdown() {
        _uiState.update { it.copy(isPlatformDropdownExpanded = !it.isPlatformDropdownExpanded, isPeriodDropdownExpanded = false) }
    }

    fun selectPlatform(platformId: String) {
        _uiState.update {
            it.copy(
                selectedPlatformId = platformId,
                isPlatformDropdownExpanded = false
            )
        }
        recalculateState()
    }

    fun setCategoryMetric(metric: CategoryMetric) {
        _uiState.update { it.copy(categoryMetric = metric) }
        recalculateState()
    }

    fun selectTimelineBucket(bucket: PerformanceTimelineBucket?) {
        _uiState.update { it.copy(selectedTimelineBucket = bucket) }
    }

    // --- Recálculo Completo de Estados e Métricas (Pure BigDecimal) ---

    private fun recalculateState() {
        val currentState = _uiState.value
        val (sinceDate, untilDate) = currentState.periodFilter.resolveRange(today)
        val selectedPlat = currentState.selectedPlatformId

        val activePlatformIds = cachedPlatforms.filter { it.active }.map { it.id }.toSet()

        // 1. Filtragem temporal
        val filteredRoutes = cachedRoutes.filter { r ->
            val date = r.occurredAt.toLocalDate()
            val dateMatch = !date.isBefore(sinceDate) && !date.isAfter(untilDate)
            val platMatch = selectedPlat == "all" || r.platformId == selectedPlat
            dateMatch && platMatch
        }

        val filteredDailies = cachedDailyTotals.filter { d ->
            val date = d.occurredAt.toLocalDate()
            val dateMatch = !date.isBefore(sinceDate) && !date.isAfter(untilDate)
            val platMatch = selectedPlat == "all" || d.platformId == selectedPlat
            dateMatch && platMatch
        }

        val filteredExpenses = cachedExpenses.filter { e ->
            val date = e.occurredAt.toLocalDate()
            !date.isBefore(sinceDate) && !date.isAfter(untilDate)
        }

        val filteredAdjustments = cachedAdjustments.filter { a ->
            val date = a.occurredAt
            val dateMatch = !date.isBefore(sinceDate) && !date.isAfter(untilDate)
            val platActiveMatch = activePlatformIds.contains(a.platformId)
            val platSelectedMatch = selectedPlat == "all" || a.platformId == selectedPlat
            dateMatch && platActiveMatch && platSelectedMatch
        }

        // 2. Cálculos dos 17 KPIs
        val routeRevenue = filteredRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount).add(r.tip) }
        val dailyRevenue = filteredDailies.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.amount) }
        val totalRevenue = routeRevenue.add(dailyRevenue)

        val routeKm = filteredRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.distanceKm) }
        val dailyKm = filteredDailies.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.distanceKm) }
        val totalKm = routeKm.add(dailyKm)

        val totalExpense = filteredExpenses.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
        val profit = totalRevenue.subtract(totalExpense)

        // Horas trabalhadas efetivas
        val rangeStartOffset = sinceDate.atStartOfDay(zone).toOffsetDateTime()
        val rangeEndOffset = untilDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toOffsetDateTime()

        var totalWorkedMs = 0L
        filteredRoutes.forEach { r ->
            if (r.startedAt != null) {
                val start = r.startedAt
                val end = r.endedAt ?: start
                val effectiveStart = if (start.isAfter(rangeStartOffset)) start else rangeStartOffset
                val effectiveEnd = if (end.isBefore(rangeEndOffset)) end else rangeEndOffset
                val durationMs = maxOf(0L, Duration.between(effectiveStart, effectiveEnd).toMillis())
                val breakMs = r.breakMinutes * 60_000L
                totalWorkedMs += maxOf(0L, durationMs - breakMs)
            }
        }

        val hoursWorked = BigDecimal(totalWorkedMs).divide(BigDecimal("3600000"), 2, RoundingMode.HALF_UP)
        val daysInRange = maxOf(1L, ChronoUnit.DAYS.between(sinceDate, untilDate) + 1L)
        val averageDailyHours = hoursWorked.divide(BigDecimal(daysInRange), 2, RoundingMode.HALF_UP)

        val revPerKm = if (totalKm > BigDecimal.ZERO) totalRevenue.divide(totalKm, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val costPerKm = if (totalKm > BigDecimal.ZERO) totalExpense.divide(totalKm, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val profitPerKm = if (totalKm > BigDecimal.ZERO) profit.divide(totalKm, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val revPerHour = if (hoursWorked > BigDecimal.ZERO) totalRevenue.divide(hoursWorked, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val profitPerHour = if (hoursWorked > BigDecimal.ZERO) profit.divide(hoursWorked, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val routeCount = filteredRoutes.size
        val totalSmallPackages = filteredRoutes.sumOf { if (it.smallPackagesCount > 0) it.smallPackagesCount else it.packageCount }
        val totalLargePackages = filteredRoutes.sumOf { it.largePackagesCount }
        val totalPackages = totalSmallPackages + totalLargePackages

        val smallPackagesValue = filteredRoutes
            .filter { it.productType == "pacote" }
            .fold(BigDecimal.ZERO) { acc, r ->
                val count = if (r.smallPackagesCount > 0) r.smallPackagesCount else r.packageCount
                acc.add(BigDecimal(count).multiply(r.packageUnitPrice))
            }

        val largePackagesValue = filteredRoutes
            .filter { it.productType == "pacote" }
            .fold(BigDecimal.ZERO) { acc, r ->
                acc.add(r.largePackagesPrices.fold(BigDecimal.ZERO) { pAcc, p -> pAcc.add(p) })
            }

        val avgTicket = if (routeCount > 0) totalRevenue.divide(BigDecimal(routeCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val avgPackagePrice = if (totalPackages > 0) {
            filteredRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                .divide(BigDecimal(totalPackages), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Odômetro estimado (máximo entre rotas, combustível, manutenção e trocas de óleo)
        val maxRouteKm = cachedRoutes.fold(BigDecimal.ZERO) { max, r -> maxOf(max, r.endKm, r.startKm) }
        val maxFuelKm = cachedExpenses.filter { it.category == "combustivel" }
            .fold(BigDecimal.ZERO) { max, e -> maxOf(max, e.odometerKm ?: BigDecimal.ZERO) }
        val maxMaintKm = cachedExpenses.filter { it.category == "manutencao" }
            .fold(BigDecimal.ZERO) { max, e -> maxOf(max, e.odometerKm ?: BigDecimal.ZERO) }
        val maxOilKm = cachedOilChanges.fold(BigDecimal.ZERO) { max, o -> maxOf(max, o.kmAtChange) }
        val currentOdometer = maxOf(maxRouteKm, maxFuelKm, maxMaintKm, maxOilKm)

        // Consumo Real (km/L) tanque-a-tanque
        val allFuelFills = cachedExpenses
            .filter { it.category == "combustivel" && (it.liters ?: BigDecimal.ZERO) > BigDecimal.ZERO && (it.odometerKm ?: BigDecimal.ZERO) > BigDecimal.ZERO }
            .sortedBy { it.odometerKm }

        var periodKm = BigDecimal.ZERO
        var periodLiters = BigDecimal.ZERO
        var prevFullIdx = -1
        var segmentLiters = BigDecimal.ZERO

        var lastKmFallback = BigDecimal.ZERO
        var lastLitFallback = BigDecimal.ZERO

        for (i in allFuelFills.indices) {
            val f = allFuelFills[i]
            val isFull = f.isFullTank
            if (prevFullIdx >= 0) {
                segmentLiters = segmentLiters.add(f.liters ?: BigDecimal.ZERO)
            }
            if (isFull) {
                if (prevFullIdx >= 0) {
                    val prev = allFuelFills[prevFullIdx]
                    val km = (f.odometerKm ?: BigDecimal.ZERO).subtract(prev.odometerKm ?: BigDecimal.ZERO)
                    if (km > BigDecimal.ZERO && segmentLiters > BigDecimal.ZERO) {
                        lastKmFallback = km
                        lastLitFallback = segmentLiters

                        val fDate = f.occurredAt.toLocalDate()
                        if (!fDate.isBefore(sinceDate) && !fDate.isAfter(untilDate)) {
                            periodKm = periodKm.add(km)
                            periodLiters = periodLiters.add(segmentLiters)
                        }
                    }
                }
                prevFullIdx = i
                segmentLiters = BigDecimal.ZERO
            }
        }

        val realConsumptionKml = if (periodLiters > BigDecimal.ZERO) {
            periodKm.divide(periodLiters, 2, RoundingMode.HALF_UP)
        } else if (lastLitFallback > BigDecimal.ZERO) {
            lastKmFallback.divide(lastLitFallback, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val stats = RelatoriosStats(
            totalRevenue = totalRevenue,
            totalKm = totalKm,
            totalExpense = totalExpense,
            profit = profit,
            hours = hoursWorked,
            averageDailyHours = averageDailyHours,
            revPerKm = revPerKm,
            costPerKm = costPerKm,
            profitPerKm = profitPerKm,
            revPerHour = revPerHour,
            profitPerHour = profitPerHour,
            routeCount = routeCount,
            totalSmallPackages = totalSmallPackages,
            totalLargePackages = totalLargePackages,
            totalPackages = totalPackages,
            smallPackagesValue = smallPackagesValue,
            largePackagesValue = largePackagesValue,
            avgTicket = avgTicket,
            avgPackagePrice = avgPackagePrice,
            realConsumptionKml = realConsumptionKml,
            currentOdometer = currentOdometer
        )

        // 3. Custos de Manutenção no Período
        val oilKeywords = Regex("\\b(oleo|óleo|filtro)\\b", RegexOption.IGNORE_CASE)
        var fuelCost = BigDecimal.ZERO
        var oilCost = BigDecimal.ZERO
        var partsCost = BigDecimal.ZERO

        filteredExpenses.forEach { e ->
            when (e.category) {
                "combustivel" -> fuelCost = fuelCost.add(e.amount)
                "manutencao" -> {
                    if (oilKeywords.containsMatchIn(e.title)) {
                        oilCost = oilCost.add(e.amount)
                    } else {
                        partsCost = partsCost.add(e.amount)
                    }
                }
            }
        }

        val maintBreakdown = MaintCostBreakdown(
            fuel = fuelCost,
            oil = oilCost,
            parts = partsCost,
            total = fuelCost.add(oilCost).add(partsCost)
        )

        // 4. Cronograma de Manutenção Preventiva
        val sortedMaintExpenses = cachedExpenses
            .filter { it.category == "manutencao" && it.title.isNotBlank() }
            .sortedByDescending { it.occurredAt }

        val maintSchedule = cachedParts.map { p ->
            val nextKm = p.lastChangeKm.add(p.lifeKm)
            val remainingKm = nextKm.subtract(currentOdometer)

            val status = when {
                remainingKm <= BigDecimal.ZERO -> MaintScheduleStatus.CRITICAL
                remainingKm <= BigDecimal("500") -> MaintScheduleStatus.WARN
                else -> MaintScheduleStatus.OK
            }

            val pNameNorm = p.partName.trim().lowercase()
            val matchedExpense = sortedMaintExpenses.firstOrNull { exp ->
                val titleNorm = exp.title.trim().lowercase()
                titleNorm == pNameNorm || titleNorm.contains(pNameNorm) || pNameNorm.contains(titleNorm)
            }

            MaintScheduleItem(
                name = p.partName,
                lifeKm = p.lifeKm,
                lastKm = p.lastChangeKm,
                nextKm = nextKm,
                remainingKm = remainingKm,
                status = status,
                expenseId = matchedExpense?.id
            )
        }

        // 5. Linha do Tempo (Receita x Despesa x Lucro)
        val spanDays = maxOf(1L, ChronoUnit.DAYS.between(sinceDate, untilDate) + 1L)
        val isMonthly = currentState.periodFilter.preset == PeriodPreset.ANO || spanDays > 90L

        val bucketsMap = linkedMapOf<String, PerformanceTimelineBucket>()
        val dayFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
        val monthLabels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

        if (!isMonthly) {
            var curDate = sinceDate
            while (!curDate.isAfter(untilDate)) {
                val key = curDate.toString()
                val label = curDate.format(dayFormatter)
                bucketsMap[key] = PerformanceTimelineBucket(key = key, label = label)
                curDate = curDate.plusDays(1)
            }
        } else {
            var curMonth = sinceDate.withDayOfMonth(1)
            while (!curMonth.isAfter(untilDate)) {
                val key = "${curMonth.year}-${curMonth.monthValue.toString().padStart(2, '0')}"
                val label = monthLabels.getOrElse(curMonth.monthValue - 1) { "M${curMonth.monthValue}" }
                bucketsMap[key] = PerformanceTimelineBucket(key = key, label = label)
                curMonth = curMonth.plusMonths(1)
            }
        }

        fun bucketKeyFor(dateTime: OffsetDateTime): String {
            val d = dateTime.toLocalDate()
            return if (isMonthly) {
                "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
            } else {
                d.toString()
            }
        }

        filteredRoutes.forEach { r ->
            val k = bucketKeyFor(r.occurredAt)
            bucketsMap[k]?.let { b ->
                val addAmount = r.amount.add(r.tip)
                bucketsMap[k] = b.copy(receita = b.receita.add(addAmount))
            }
        }

        filteredDailies.forEach { d ->
            val k = bucketKeyFor(d.occurredAt)
            bucketsMap[k]?.let { b ->
                bucketsMap[k] = b.copy(receita = b.receita.add(d.amount))
            }
        }

        filteredExpenses.forEach { e ->
            val k = bucketKeyFor(e.occurredAt)
            bucketsMap[k]?.let { b ->
                bucketsMap[k] = b.copy(despesa = b.despesa.add(e.amount))
            }
        }

        val timelineBuckets = bucketsMap.values.map { b ->
            b.copy(lucro = b.receita.subtract(b.despesa))
        }

        // 6. Fluxo de Caixa Futuro (Faturas a Receber)
        val futureCycles = cachedBillingCycles.filter {
            !it.expectedPaymentDate.isBefore(today) && it.status != "paid"
        }

        val cycleTotalsMap = mutableMapOf<String, BigDecimal>()
        cachedRoutes.forEach { r ->
            if (!r.billingCycleId.isNullOrBlank()) {
                val sum = cycleTotalsMap.getOrDefault(r.billingCycleId, BigDecimal.ZERO)
                cycleTotalsMap[r.billingCycleId] = sum.add(r.amount).add(r.tip)
            }
        }

        val futureCashFlow = futureCycles
            .groupBy { it.expectedPaymentDate }
            .toSortedMap()
            .map { (date, cyclesForDate) ->
                val amount = cyclesForDate.fold(BigDecimal.ZERO) { acc, c ->
                    acc.add(cycleTotalsMap.getOrDefault(c.id, BigDecimal.ZERO))
                }
                val label = "${date.dayOfMonth.toString().padStart(2, '0')} ${monthLabels.getOrElse(date.monthValue - 1) { "" }.uppercase()}"
                FutureCashFlowItem(date = date.toString(), label = label, amount = amount)
            }
            .filter { it.amount > BigDecimal.ZERO }

        // 7. Rentabilidade por Plataforma
        val platformMap = mutableMapOf<String, Triple<BigDecimal, BigDecimal, Long>>() // revenue, km, ms

        filteredRoutes.forEach { r ->
            val pId = r.platformId
            if (!pId.isNullOrBlank() && activePlatformIds.contains(pId)) {
                val current = platformMap.getOrDefault(pId, Triple(BigDecimal.ZERO, BigDecimal.ZERO, 0L))
                val rev = current.first.add(r.amount).add(r.tip)
                val km = current.second.add(r.distanceKm)
                var ms = current.third
                if (r.startedAt != null) {
                    val start = r.startedAt
                    val end = r.endedAt ?: start
                    val effStart = if (start.isAfter(rangeStartOffset)) start else rangeStartOffset
                    val effEnd = if (end.isBefore(rangeEndOffset)) end else rangeEndOffset
                    val dur = maxOf(0L, Duration.between(effStart, effEnd).toMillis())
                    ms += maxOf(0L, dur - (r.breakMinutes * 60_000L))
                }
                platformMap[pId] = Triple(rev, km, ms)
            }
        }

        filteredDailies.forEach { d ->
            val pId = d.platformId
            if (!pId.isNullOrBlank() && activePlatformIds.contains(pId)) {
                val current = platformMap.getOrDefault(pId, Triple(BigDecimal.ZERO, BigDecimal.ZERO, 0L))
                platformMap[pId] = Triple(current.first.add(d.amount), current.second.add(d.distanceKm), current.third)
            }
        }

        val platformProfitability = platformMap.entries
            .mapIndexed { idx, (pId, statsTriple) ->
                val pName = cachedPlatforms.firstOrNull { it.id == pId }?.name ?: "Plataforma"
                val rev = statsTriple.first
                val km = statsTriple.second
                val ms = statsTriple.third

                val h = BigDecimal(ms).divide(BigDecimal("3600000"), 4, RoundingMode.HALF_UP)
                val perH = if (h > BigDecimal.ZERO) rev.divide(h, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
                val perKm = if (km > BigDecimal.ZERO) rev.divide(km, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
                val col = RelatoriosChartColors[idx % RelatoriosChartColors.size]

                PlatformProfitabilityBarItem(
                    platformId = pId,
                    name = pName,
                    revenue = rev,
                    revPerHour = perH,
                    revPerKm = perKm,
                    color = col
                )
            }
            .sortedByDescending { it.revPerHour }

        // 8. Bonificações e Descontos por Plataforma
        val discountTypes = setOf("previdenciario", "extravio", "multa", "pnr")
        val adjustmentLabels = mapOf(
            "previdenciario" to "Previdenciário",
            "extravio" to "Extravios",
            "multa" to "Multas",
            "pnr" to "Outros descontos (legado)",
            "bonus_fatura" to "Bônus",
            "gratificacao" to "Gratificação",
            "incentivo" to "Incentivo",
            "premiacao" to "Premiação",
            "bonus" to "Outros acréscimos (legado)"
        )

        val bonificacoesByPlatform = filteredAdjustments
            .groupBy { it.platformId }
            .mapNotNull { (platId, adjList) ->
                val plat = cachedPlatforms.firstOrNull { it.id == platId } ?: return@mapNotNull null
                var descontosTotal = BigDecimal.ZERO
                var acrescimosTotal = BigDecimal.ZERO
                val typeSums = mutableMapOf<String, BigDecimal>()

                adjList.forEach { a ->
                    val amtAbs = a.amount.abs()
                    val isDiscount = discountTypes.contains(a.type) || a.amount < BigDecimal.ZERO
                    if (isDiscount) {
                        descontosTotal = descontosTotal.add(amtAbs)
                    } else {
                        acrescimosTotal = acrescimosTotal.add(amtAbs)
                    }
                    typeSums[a.type] = typeSums.getOrDefault(a.type, BigDecimal.ZERO).add(amtAbs)
                }

                if (descontosTotal <= BigDecimal.ZERO && acrescimosTotal <= BigDecimal.ZERO) return@mapNotNull null

                val details = typeSums.entries
                    .filter { it.value > BigDecimal.ZERO }
                    .map { (typeKey, sum) ->
                        AdjustmentDetail(
                            type = typeKey,
                            label = adjustmentLabels[typeKey] ?: typeKey,
                            amount = sum,
                            isDiscount = discountTypes.contains(typeKey)
                        )
                    }

                PlatformAdjustmentGroup(
                    platformId = platId,
                    name = plat.name,
                    descontosTotal = descontosTotal,
                    acrescimosTotal = acrescimosTotal,
                    details = details
                )
            }
            .sortedByDescending { it.acrescimosTotal.add(it.descontosTotal) }

        // 9. Categorias Entregues (Alimento, Pacotes, Documentos, Outro)
        val categoryLabels = mapOf(
            "alimento" to "Alimento",
            "pacote" to "Pacotes",
            "documento" to "Documentos",
            "outro" to "Outro"
        )
        val catMap = mutableMapOf<String, Pair<Int, BigDecimal>>()

        filteredRoutes.forEach { r ->
            val pType = r.productType.ifBlank { "alimento" }
            val cur = catMap.getOrDefault(pType, Pair(0, BigDecimal.ZERO))
            val pkg = if (r.packageCount > 0) r.packageCount else maxOf(1, r.smallPackagesCount + r.largePackagesCount)
            val rev = r.amount.add(r.tip)
            catMap[pType] = Pair(cur.first + pkg, cur.second.add(rev))
        }

        filteredDailies.forEach { d ->
            val pType = d.productType.ifBlank { "alimento" }
            val cur = catMap.getOrDefault(pType, Pair(0, BigDecimal.ZERO))
            catMap[pType] = Pair(cur.first + 1, cur.second.add(d.amount))
        }

        val deliveredCategories = catMap.entries.mapIndexed { idx, (typeKey, pair) ->
            DeliveredCategoryItem(
                name = categoryLabels[typeKey] ?: typeKey.replaceFirstChar { it.uppercase() },
                productType = typeKey,
                count = pair.first,
                amount = pair.second,
                color = RelatoriosChartColors[idx % RelatoriosChartColors.size]
            )
        }

        // 10. Top Origens e Top Destinos
        val originsMap = mutableMapOf<String, Int>()
        val destMap = mutableMapOf<String, Int>()

        filteredRoutes.forEach { r ->
            r.origin?.trim()?.takeIf { it.isNotBlank() }?.let {
                originsMap[it] = (originsMap[it] ?: 0) + 1
            }
            r.destination?.trim()?.takeIf { it.isNotBlank() }?.let {
                destMap[it] = (destMap[it] ?: 0) + 1
            }
        }

        val topOrigins = originsMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { RankedPlaceItem(it.key, it.value) }

        val topDestinations = destMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { RankedPlaceItem(it.key, it.value) }

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                platforms = cachedPlatforms,
                stats = stats,
                maintCostBreakdown = maintBreakdown,
                maintSchedule = maintSchedule,
                timelineBuckets = timelineBuckets,
                futureCashFlow = futureCashFlow,
                platformProfitability = platformProfitability,
                bonificacoes = bonificacoesByPlatform,
                deliveredCategories = deliveredCategories,
                topOrigins = topOrigins,
                topDestinations = topDestinations,
                errorMessage = null
            )
        }
    }
}
