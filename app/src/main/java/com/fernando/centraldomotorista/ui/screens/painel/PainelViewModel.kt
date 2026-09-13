package com.fernando.centraldomotorista.ui.screens.painel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.*
import com.fernando.centraldomotorista.util.AppDataSync
import com.fernando.centraldomotorista.util.EarningsCalculator
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class PainelViewModel(
    private val routeRepository: RouteRepository = RouteRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val partMaintenanceRepository: PartMaintenanceRepository = PartMaintenanceRepository(),
    private val externalScope: CoroutineScope? = null,
    observeDataSync: Boolean = true,
    loadOnInit: Boolean = true,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val fixedToday: LocalDate? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(PainelUiState(isLoading = true))
    val uiState: StateFlow<PainelUiState> = _uiState.asStateFlow()

    // Cache interno de dados carregados para recálculos rápidos (ex: troca de range 7D/30D)
    private var cachedProfile: Profile? = null
    private var cachedRoutes: List<Route> = emptyList()
    private var cachedDailyTotals: List<DailyTotal> = emptyList()
    private var cachedExpenses: List<Expense> = emptyList()
    private var cachedPlatforms: List<Platform> = emptyList()
    private var cachedParts: List<PartMaintenance> = emptyList()

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
        loadData()
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun toggleQuickActions(open: Boolean? = null) {
        _uiState.update { current ->
            current.copy(isQuickActionsOpen = open ?: !current.isQuickActionsOpen)
        }
    }

    fun setTrendRange(range: TrendRange) {
        if (_uiState.value.trendRange == range) return
        _uiState.update { it.copy(trendRange = range) }
        recalculateTrend()
    }

    fun selectTrendDay(bucket: DailyTrendBucket?) {
        _uiState.update { current ->
            current.copy(selectedTrendDay = if (current.selectedTrendDay == bucket) null else bucket)
        }
    }

    fun openResetPartDialog(alertItem: PartMaintenanceAlertItem) {
        _uiState.update { it.copy(partToReset = alertItem) }
    }

    fun closeResetPartDialog() {
        _uiState.update { it.copy(partToReset = null) }
    }

    fun confirmResetPart(partId: String, newKm: BigDecimal) {
        val user = supabase.auth.currentUserOrNull()
        val currentPart = cachedParts.firstOrNull { it.id == partId }
        if (user == null || currentPart == null) {
            closeResetPartDialog()
            return
        }

        _uiState.update { it.copy(isUpdatingPart = true) }

        scope.launch(Dispatchers.IO) {
            try {
                val updated = currentPart.copy(
                    lastChangeKm = newKm,
                    lastChangeAt = OffsetDateTime.now()
                )
                partMaintenanceRepository.savePartMaintenance(updated)
                AppDataSync.notifyDataChanged()

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isUpdatingPart = false,
                            partToReset = null,
                            actionMessage = "Troca de \"${currentPart.partName}\" atualizada com sucesso!"
                        )
                    }
                    loadData()
                }
            } catch (e: Exception) {
                Log.e("PainelViewModel", "Erro ao atualizar troca de peça: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isUpdatingPart = false,
                            actionMessage = "Erro ao atualizar manutenção da peça: ${e.localizedMessage ?: e.message}"
                        )
                    }
                }
            }
        }
    }

    fun calculateEarliestSince(today: LocalDate = getEffectiveToday()): LocalDate {
        val startOfMonth = today.withDayOfMonth(1)
        val minus35 = today.minusDays(35)
        return if (minus35.isBefore(startOfMonth)) minus35 else startOfMonth
    }

    private fun getEffectiveToday(): LocalDate = fixedToday ?: LocalDate.now(zone)

    fun loadData() {
        val user = try {
            supabase.auth.currentUserOrNull()
        } catch (e: Throwable) {
            null
        }
        if (user == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Usuário não autenticado."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        scope.launch(Dispatchers.IO) {
            try {
                val userId = user.id
                val today = getEffectiveToday()
                val earliestSince = calculateEarliestSince(today)

                coroutineScope {
                    val profileDeferred = async {
                        profileRepository.createOrFetchProfile(userId, user.email, null, null)
                    }
                    val routesDeferred = async {
                        routeRepository.getRoutes(userId)
                    }
                    val dailyTotalsDeferred = async {
                        dailyTotalRepository.getDailyTotals(userId)
                    }
                    val expensesDeferred = async {
                        expenseRepository.getExpenses(userId)
                    }
                    val platformsDeferred = async {
                        platformRepository.getActivePlatforms(userId)
                    }
                    val partsDeferred = async {
                        partMaintenanceRepository.getPartMaintenances(userId)
                    }

                    cachedProfile = profileDeferred.await()
                    cachedRoutes = routesDeferred.await()
                    cachedDailyTotals = dailyTotalsDeferred.await()
                    cachedExpenses = expensesDeferred.await()
                    cachedPlatforms = platformsDeferred.await()
                    cachedParts = partsDeferred.await()
                }

                computeDashboardState(earliestSince, today)
            } catch (e: Exception) {
                Log.e("PainelViewModel", "Erro ao carregar dados da aba Painel: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Erro ao carregar dados do painel: ${e.localizedMessage ?: e.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Calcula todas as agregações financeiras e métricas com base no cache atual.
     */
    fun computeDashboardState(earliestSince: LocalDate, today: LocalDate = getEffectiveToday()) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        // Filtro prévio a partir de earliestSince
        val routesInEarliest = cachedRoutes.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(earliestSince)
        }
        val dailyTotalsInEarliest = cachedDailyTotals.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(earliestSince)
        }
        val expensesInEarliest = cachedExpenses.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(earliestSince)
        }

        // 1. DIÁRIO (Hoje)
        val todayRoutes = routesInEarliest.filter {
            it.occurredAt.atZoneSameInstant(zone).toLocalDate() == today
        }
        val todayDailyTotals = dailyTotalsInEarliest.filter {
            it.occurredAt.atZoneSameInstant(zone).toLocalDate() == today
        }
        val dailyEarnings = EarningsCalculator.calcularTotalGanhos(todayRoutes, todayDailyTotals, zone)
        val dailyPackages = todayRoutes.sumOf {
            (it.smallPackagesCount.takeIf { c -> c > 0 } ?: it.packageCount) + it.largePackagesCount
        }

        // 2. SEMANAL (Segunda a Domingo da semana corrente)
        val weekRoutes = routesInEarliest.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(weekStart) && !d.isAfter(weekEnd)
        }
        val weekDailyTotals = dailyTotalsInEarliest.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(weekStart) && !d.isAfter(weekEnd)
        }
        val weeklyEarnings = EarningsCalculator.calcularTotalGanhos(weekRoutes, weekDailyTotals, zone)
        val weeklyPackages = weekRoutes.sumOf {
            (it.smallPackagesCount.takeIf { c -> c > 0 } ?: it.packageCount) + it.largePackagesCount
        }

        // 3. MENSAL (1º dia até último dia do mês corrente)
        val monthRoutes = routesInEarliest.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(monthStart) && !d.isAfter(monthEnd)
        }
        val monthDailyTotals = dailyTotalsInEarliest.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(monthStart) && !d.isAfter(monthEnd)
        }
        val monthlyEarnings = EarningsCalculator.calcularTotalGanhos(monthRoutes, monthDailyTotals, zone)
        val monthlyPackages = monthRoutes.sumOf {
            (it.smallPackagesCount.takeIf { c -> c > 0 } ?: it.packageCount) + it.largePackagesCount
        }

        // 4. METAS DO PERFIL
        val dailyGoal = cachedProfile?.dailyGoal ?: BigDecimal.ZERO
        val weeklyGoal = cachedProfile?.weeklyGoal ?: BigDecimal.ZERO
        val monthlyGoal = cachedProfile?.monthlyGoal ?: BigDecimal.ZERO

        val dailyProgressPct = if (dailyGoal > BigDecimal.ZERO) {
            dailyEarnings.multiply(BigDecimal("100")).divide(dailyGoal, 0, RoundingMode.HALF_UP)
        } else null

        val weeklyProgressPct = if (weeklyGoal > BigDecimal.ZERO) {
            weeklyEarnings.multiply(BigDecimal("100")).divide(weeklyGoal, 0, RoundingMode.HALF_UP)
        } else null

        val monthlyProgressPct = if (monthlyGoal > BigDecimal.ZERO) {
            monthlyEarnings.multiply(BigDecimal("100")).divide(monthlyGoal, 0, RoundingMode.HALF_UP)
        } else null

        // 5. GANHOS POR PLATAFORMA (Top 5 plataformas ativas no mês)
        val platformEarningMap = mutableMapOf<String, BigDecimal>()
        monthRoutes.forEach { r ->
            if (!r.platformId.isNullOrBlank()) {
                val sumAmount = r.amount.add(r.tip).add(r.bonus)
                platformEarningMap[r.platformId] = (platformEarningMap[r.platformId] ?: BigDecimal.ZERO).add(sumAmount)
            }
        }
        monthDailyTotals.forEach { dt ->
            if (!dt.platformId.isNullOrBlank()) {
                val net = EarningsCalculator.calcularGanhoLiquidoDoDia(dt, monthRoutes, zone)
                platformEarningMap[dt.platformId] = (platformEarningMap[dt.platformId] ?: BigDecimal.ZERO).add(net)
            }
        }

        val topPlatformsRaw = cachedPlatforms
            .map { p ->
                PlatformEarningItem(
                    id = p.id,
                    name = p.name,
                    total = platformEarningMap[p.id] ?: BigDecimal.ZERO,
                    percentageOfTotal = 0f
                )
            }
            .sortedByDescending { it.total }
            .take(5)

        val totalPlatformSum = topPlatformsRaw.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.total) }
        val platformEarnings = if (totalPlatformSum > BigDecimal.ZERO) {
            topPlatformsRaw.map { p ->
                val pct = p.total.multiply(BigDecimal("100"))
                    .divide(totalPlatformSum, 1, RoundingMode.HALF_UP)
                    .toFloat()
                p.copy(percentageOfTotal = pct)
            }
        } else {
            topPlatformsRaw
        }

        // 6. DESPESAS POR CATEGORIA DO MÊS (Combustível, Manutenção, Alimentação)
        val monthExpenses = expensesInEarliest.filter {
            val d = it.occurredAt.atZoneSameInstant(zone).toLocalDate()
            !d.isBefore(monthStart) && !d.isAfter(monthEnd)
        }
        val expCategories = listOf(
            "combustivel" to "Combustível",
            "manutencao" to "Manutenção",
            "alimentacao" to "Alimentação",
            "equipe" to "Equipe"
        )
        val expensesByCategory = expCategories.map { (catKey, catLabel) ->
            val catTotal = monthExpenses
                .filter { it.category.equals(catKey, ignoreCase = true) }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
            CategoryExpenseItem(
                category = catKey,
                label = catLabel,
                total = catTotal
            )
        }

        // 6.1 GASTO POR ENTREGADOR PARCEIRO (a partir das mesmas despesas category == "equipe")
        val teamExpenses = monthExpenses.filter { it.category.equals("equipe", ignoreCase = true) }
        val teamByVendor = teamExpenses
            .groupBy { it.vendor?.trim().takeUnless { v -> v.isNullOrBlank() } ?: "Sem identificação" }
            .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) } }

        val totalTeamExpenses = teamByVendor.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

        val teamExpensesByPartner = teamByVendor.entries
            .sortedByDescending { it.value }
            .map { (vendorName, total) ->
                val pct = if (totalTeamExpenses > BigDecimal.ZERO) {
                    total.multiply(BigDecimal("100")).divide(totalTeamExpenses, 1, RoundingMode.HALF_UP).toFloat()
                } else 0f
                PartnerExpenseItem(vendorName = vendorName, total = total, percentageOfTotal = pct)
            }

        // 7. ALERTAS DE MANUTENÇÃO DE PEÇAS (Desgaste >= 90%)
        val currentOdometerKm = maxOf(
            cachedExpenses.firstOrNull { it.odometerKm != null }?.odometerKm ?: BigDecimal.ZERO,
            cachedRoutes.firstOrNull { it.endKm > BigDecimal.ZERO }?.endKm ?: BigDecimal.ZERO
        )

        val maintenanceAlerts = mutableListOf<PartMaintenanceAlertItem>()
        for (part in cachedParts) {
            val lifeKm = part.lifeKm
            if (lifeKm <= BigDecimal.ZERO) continue

            val lastKm = part.lastChangeKm
            var drivenKm = if (currentOdometerKm > lastKm) currentOdometerKm.subtract(lastKm) else BigDecimal.ZERO

            // Fallback: se odômetro não foi atualizado, estima pela soma de km das rotas desde lastChangeAt
            if (drivenKm == BigDecimal.ZERO) {
                val sumDist = cachedRoutes
                    .filter { it.occurredAt >= part.lastChangeAt }
                    .fold(BigDecimal.ZERO) { acc, r -> acc.add(r.distanceKm) }
                if (sumDist > BigDecimal.ZERO) {
                    drivenKm = sumDist
                }
            }

            val wearRatio = drivenKm.divide(lifeKm, 4, RoundingMode.HALF_UP)
            val pct = wearRatio.multiply(BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)

            // Alerta se desgaste >= 90%
            if (pct >= BigDecimal("90.0")) {
                val isOverdue = pct >= BigDecimal("100.0")
                val remainingKm = maxOf(BigDecimal.ZERO, lifeKm.subtract(drivenKm))
                val overdueKm = if (isOverdue) drivenKm.subtract(lifeKm) else BigDecimal.ZERO

                maintenanceAlerts.add(
                    PartMaintenanceAlertItem(
                        part = part,
                        lifeKm = lifeKm,
                        lastChangeKm = lastKm,
                        drivenKm = drivenKm,
                        wearPercentage = pct,
                        isOverdue = isOverdue,
                        remainingKm = remainingKm,
                        overdueKm = overdueKm
                    )
                )
            }
        }
        maintenanceAlerts.sortByDescending { it.wearPercentage }

        // 8. TENDÊNCIA DE DESEMPENHO (Buckets 7D / 30D)
        val (buckets, maxTrend) = buildTrendBuckets(_uiState.value.trendRange, today)

        _uiState.update {
            it.copy(
                profile = cachedProfile,
                dailyEarnings = dailyEarnings,
                weeklyEarnings = weeklyEarnings,
                monthlyEarnings = monthlyEarnings,
                dailyPackages = dailyPackages,
                weeklyPackages = weeklyPackages,
                monthlyPackages = monthlyPackages,
                dailyGoal = dailyGoal,
                weeklyGoal = weeklyGoal,
                monthlyGoal = monthlyGoal,
                dailyProgressPct = dailyProgressPct,
                weeklyProgressPct = weeklyProgressPct,
                monthlyProgressPct = monthlyProgressPct,
                platformEarnings = platformEarnings,
                totalPlatformEarnings = totalPlatformSum,
                expensesByCategory = expensesByCategory,
                teamExpensesByPartner = teamExpensesByPartner,
                totalTeamExpenses = totalTeamExpenses,
                trendBuckets = buckets,
                maxTrendAmount = maxTrend,
                maintenanceAlerts = maintenanceAlerts,
                isLoading = false,
                error = null
            )
        }
    }

    private fun recalculateTrend() {
        val today = getEffectiveToday()
        val (buckets, maxTrend) = buildTrendBuckets(_uiState.value.trendRange, today)
        _uiState.update {
            it.copy(
                trendBuckets = buckets,
                maxTrendAmount = maxTrend,
                selectedTrendDay = null
            )
        }
    }

    fun buildTrendBuckets(range: TrendRange, today: LocalDate = getEffectiveToday()): Pair<List<DailyTrendBucket>, BigDecimal> {
        val daysCount = if (range == TrendRange.SEVEN_DAYS) 7 else 30
        val startDate = if (range == TrendRange.SEVEN_DAYS) {
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        } else {
            today.minusDays((daysCount - 1).toLong())
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
        val buckets = mutableListOf<DailyTrendBucket>()

        for (i in 0 until daysCount) {
            val date = startDate.plusDays(i.toLong())
            val dayOfWeekShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                .replace(".", "")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
            val dayOfWeekFull = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

            val dayRoutes = cachedRoutes.filter {
                it.occurredAt.atZoneSameInstant(zone).toLocalDate() == date
            }
            val dayDailyTotals = cachedDailyTotals.filter {
                it.occurredAt.atZoneSameInstant(zone).toLocalDate() == date
            }

            val dayAmount = EarningsCalculator.calcularTotalGanhos(dayRoutes, dayDailyTotals, zone)
            val dayPackages = dayRoutes.sumOf {
                (it.smallPackagesCount.takeIf { c -> c > 0 } ?: it.packageCount) + it.largePackagesCount
            }

            buckets.add(
                DailyTrendBucket(
                    date = date,
                    dayOfWeekLabel = dayOfWeekShort,
                    fullDayOfWeekLabel = dayOfWeekFull,
                    dateLabel = date.format(dateFormatter),
                    totalAmount = dayAmount,
                    packageCount = dayPackages,
                    isToday = (date == today)
                )
            )
        }

        val maxAmount = buckets.maxOfOrNull { it.totalAmount } ?: BigDecimal.ONE
        val safeMax = if (maxAmount > BigDecimal.ZERO) maxAmount else BigDecimal.ONE

        return Pair(buckets, safeMax)
    }

    // Para injeção de dados diretos em testes unitários
    fun setTestData(
        profile: Profile? = null,
        routes: List<Route> = emptyList(),
        dailyTotals: List<DailyTotal> = emptyList(),
        expenses: List<Expense> = emptyList(),
        platforms: List<Platform> = emptyList(),
        parts: List<PartMaintenance> = emptyList()
    ) {
        cachedProfile = profile
        cachedRoutes = routes
        cachedDailyTotals = dailyTotals
        cachedExpenses = expenses
        cachedPlatforms = platforms
        cachedParts = parts
        computeDashboardState(calculateEarliestSince(getEffectiveToday()), getEffectiveToday())
    }
}
