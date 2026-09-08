package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.BillingCycleApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.util.AppDataSync
import com.fernando.centraldomotorista.util.EarningsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoricoRepository(
    private val routeRepository: RouteRepository = RouteRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val billingCycleApi: BillingCycleApi = RetrofitClient.billingCycleApi
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    suspend fun loadHistoricoData(userId: String): Pair<Profile, List<TransactionItem>> = withContext(Dispatchers.IO) {
        coroutineScope {
            val userFilter = "eq.$userId"

            val profileDeferred = async {
                profileRepository.createOrFetchProfile(userId, null, null, null)
            }
            val routesDeferred = async {
                routeRepository.getRoutes(userId)
            }
            val expensesDeferred = async {
                expenseRepository.getExpenses(userId)
            }
            val dailyTotalsDeferred = async {
                dailyTotalRepository.getDailyTotals(userId)
            }
            val platformsDeferred = async {
                platformRepository.getPlatforms(userId)
            }
            val billingCyclesDeferred = async {
                try {
                    billingCycleApi.getBillingCycles(userFilter, null).map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HistoricoRepo", "Erro ao buscar billing_cycles: ${e.message}", e)
                    emptyList()
                }
            }

            val profile = profileDeferred.await()
            val allRoutes = routesDeferred.await()
            val allExpenses = expensesDeferred.await()
            val allDailyTotals = dailyTotalsDeferred.await()
            val platforms = platformsDeferred.await()
            val billingCycles = billingCyclesDeferred.await()

            val platformsMap = platforms.associate { it.id to it.name }
            val cycleMap = billingCycles.associate { it.id to it.status }

            val transactions = mutableListOf<TransactionItem>()

            // 1. Mapear Routes (Ganhos)
            allRoutes.forEach { r ->
                val platName = platformsMap[r.platformId] ?: "AVULSO"
                val pkgs = (r.smallPackagesCount + r.largePackagesCount).let { if (it > 0) it else r.packageCount }
                val km = r.distanceKm

                val originAbbr = r.origin?.trim()?.take(3)?.uppercase() ?: "---"
                val destAbbr = r.destination?.trim()?.take(3)?.uppercase() ?: "---"
                val timeStr = r.occurredAt.format(timeFormatter)
                val subtitle = "$originAbbr - $destAbbr • $timeStr"

                val meta1 = "$pkgs Pac${if (pkgs == 1) "" else "s"} • $km KM"
                var meta2: String? = null
                if (r.startedAt != null && r.endedAt != null) {
                    val ms = r.endedAt.toInstant().toEpochMilli() - r.startedAt.toInstant().toEpochMilli()
                    val totalMins = maxOf(0L, (ms / 60000) - r.breakMinutes)
                    val h = totalMins / 60
                    val m = totalMins % 60
                    meta2 = String.format(Locale.getDefault(), "%02dH %02dMIN TRABALHADOS", h, m)
                }

                val grossAmount = r.amount.add(r.tip).add(r.bonus)
                val isPaid = r.billingCycleId != null && cycleMap[r.billingCycleId] == "pago"
                val tagText = if (isPaid) "PAGO" else "A RECEBER"

                transactions.add(
                    TransactionItem(
                        id = r.id,
                        type = TransactionType.GANHO,
                        sourceType = TransactionSourceType.ROUTE,
                        title = platName.uppercase(),
                        subtitle = subtitle,
                        amount = grossAmount,
                        netAmount = grossAmount,
                        category = "ROTA",
                        occurredAt = r.occurredAt,
                        establishment = platName,
                        meta1 = meta1,
                        meta2 = meta2,
                        tag = tagText,
                        subtractRoutes = false,
                        rawRoute = r
                    )
                )
            }

            // 2. Mapear DailyTotals (Ganhos Consolidados)
            allDailyTotals.forEach { d ->
                val platName = platformsMap[d.platformId] ?: "AVULSO"
                val timeStr = d.occurredAt.format(timeFormatter)
                val subtitle = "$platName • $timeStr"

                val netAmount = EarningsCalculator.calcularGanhoLiquidoDoDia(
                    dailyTotal = d,
                    routes = allRoutes
                )

                transactions.add(
                    TransactionItem(
                        id = d.id,
                        type = TransactionType.GANHO,
                        sourceType = TransactionSourceType.DAILY_TOTAL,
                        title = "TOTAL DO DIA",
                        subtitle = subtitle,
                        amount = d.amount,
                        netAmount = netAmount,
                        category = "TOTAL DO DIA",
                        occurredAt = d.occurredAt,
                        establishment = platName,
                        meta1 = if (d.distanceKm > BigDecimal.ZERO) "${d.distanceKm} KM" else null,
                        meta2 = null,
                        tag = "TOTAL",
                        subtractRoutes = d.subtractRoutes,
                        rawDailyTotal = d
                    )
                )
            }

            // 3. Mapear Expenses (Despesas)
            allExpenses.forEach { e ->
                val catFormatted = when (e.category.lowercase()) {
                    "combustivel", "combustível" -> "COMBUSTÍVEL"
                    "manutencao", "manutenção" -> "MANUTENÇÃO"
                    "alimentacao", "alimentação" -> "ALIMENTAÇÃO"
                    else -> e.category.uppercase()
                }

                val title = e.title.trim().ifBlank { catFormatted }
                val timeStr = e.occurredAt.format(timeFormatter)
                val subtitle = "${e.vendor ?: "—"} • $timeStr"

                var meta1: String? = null
                if (e.category.lowercase().contains("manuten")) {
                    val brandModel = listOfNotNull(e.partBrand, e.partModel).filter { it.isNotBlank() }.joinToString(" ")
                    if (brandModel.isNotBlank() && e.odometerKm != null && e.odometerKm > BigDecimal.ZERO) {
                        meta1 = "${brandModel.uppercase()} • ${e.odometerKm} KM"
                    } else if (brandModel.isNotBlank()) {
                        meta1 = brandModel.uppercase()
                    } else if (e.odometerKm != null && e.odometerKm > BigDecimal.ZERO) {
                        meta1 = "${e.odometerKm} KM"
                    }
                } else if (e.category.lowercase() == "combustivel") {
                    val parts = mutableListOf<String>()
                    if (!e.fuelType.isNullOrBlank()) parts.add(e.fuelType)
                    if (e.liters != null && e.liters > BigDecimal.ZERO) parts.add("${e.liters} L")
                    if (e.odometerKm != null && e.odometerKm > BigDecimal.ZERO) parts.add("${e.odometerKm} KM")
                    if (parts.isNotEmpty()) meta1 = parts.joinToString(" • ")
                }

                transactions.add(
                    TransactionItem(
                        id = e.id,
                        type = TransactionType.DESPESA,
                        sourceType = TransactionSourceType.EXPENSE,
                        title = title.uppercase(),
                        subtitle = subtitle,
                        amount = e.amount,
                        netAmount = e.amount,
                        category = catFormatted,
                        occurredAt = e.occurredAt,
                        establishment = e.vendor,
                        meta1 = meta1,
                        meta2 = null,
                        tag = catFormatted,
                        subtractRoutes = false,
                        rawExpense = e
                    )
                )
            }

            // Ordenação cronológica decrescente (mais recente primeiro)
            transactions.sortByDescending { it.occurredAt }

            Pair(profile, transactions)
        }
    }

    suspend fun deleteTransaction(item: TransactionItem): Boolean = withContext(Dispatchers.IO) {
        val success = when (item.sourceType) {
            TransactionSourceType.ROUTE -> {
                val routeId = item.rawRoute?.id ?: item.id
                routeRepository.deleteRoute(routeId)
            }
            TransactionSourceType.EXPENSE -> {
                val expenseId = item.rawExpense?.id ?: item.id
                expenseRepository.deleteExpense(expenseId)
            }
            TransactionSourceType.DAILY_TOTAL -> {
                val dailyId = item.rawDailyTotal?.id ?: item.id
                dailyTotalRepository.deleteDailyTotal(dailyId)
            }
        }
        if (success) {
            AppDataSync.notifyDataChanged()
        }
        success
    }

    suspend fun updateDailyGoal(userId: String, dailyGoal: BigDecimal): Boolean {
        val success = profileRepository.updateDailyGoal(userId, dailyGoal)
        if (success) {
            AppDataSync.notifyDataChanged()
        }
        return success
    }
}
