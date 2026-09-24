package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.BillingCycleApi
import com.fernando.centraldomotorista.data.remote.api.FinancialAdjustmentApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate

data class BillingCycleWithTotals(
    val cycle: BillingCycle,
    val platformName: String,
    val routeAmount: BigDecimal = BigDecimal.ZERO,
    val tipTotal: BigDecimal = BigDecimal.ZERO,
    val bonusTotal: BigDecimal = BigDecimal.ZERO,
    val dailyAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentsCredit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsDebit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsTotal: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val routeCount: Int = 0,
    val packageCount: Int = 0,
    val dailyCount: Int = 0,
    val adjustmentsCount: Int = 0,
    val routes: List<com.fernando.centraldomotorista.data.model.Route> = emptyList(),
    val dailyTotals: List<com.fernando.centraldomotorista.data.model.DailyTotal> = emptyList(),
    val adjustments: List<com.fernando.centraldomotorista.data.model.FinancialAdjustment> = emptyList()
) {
    val isOverdue: Boolean
        get() = cycle.status != "pago" && cycle.status != "cancelado" && cycle.expectedPaymentDate.isBefore(LocalDate.now())
}

class BillingCycleRepository(
    private val api: BillingCycleApi = RetrofitClient.billingCycleApi,
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val routeRepository: RouteRepository = RouteRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val adjustmentApi: FinancialAdjustmentApi = RetrofitClient.financialAdjustmentApi
) {
    suspend fun getBillingCycles(userId: String, status: String? = null): List<BillingCycle> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val statusFilter = if (status != null) "eq.$status" else null
            api.getBillingCycles(userFilter, statusFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao buscar ciclos de faturamento: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBillingCyclesWithTotals(userId: String): List<BillingCycleWithTotals> = withContext(Dispatchers.IO) {
        coroutineScope {
            try {
                val cyclesDeferred = async { getBillingCycles(userId, null) }
                val platformsDeferred = async { platformRepository.getPlatforms(userId) }
                val routesDeferred = async { routeRepository.getRoutes(userId) }
                val dailyTotalsDeferred = async { dailyTotalRepository.getDailyTotals(userId) }
                val adjustmentsDeferred = async {
                    try {
                        adjustmentApi.getFinancialAdjustments("eq.$userId").map { it.toDomain() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val cycles = cyclesDeferred.await()
                val platformsMap = platformsDeferred.await().associate { it.id to it.name }
                val routes = routesDeferred.await()
                val dailyTotals = dailyTotalsDeferred.await()
                val adjustments = adjustmentsDeferred.await()

                cycles.map { cycle ->
                    val platName = platformsMap[cycle.platformId] ?: "Plataforma"

                    val cycleRoutes = routes.filter { r ->
                        r.billingCycleId == cycle.id || (
                            r.billingCycleId == null &&
                            r.platformId == cycle.platformId &&
                            BillingCycleCalculator.isDateInCycle(
                                r.occurredAt.toLocalDate(),
                                cycle.periodStart,
                                cycle.periodEnd,
                                cycle.includeEndDate
                            )
                        )
                    }
                    val routeAmount = cycleRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                    val tipTotal = cycleRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.tip) }
                    val bonusTotal = cycleRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.bonus) }
                    val packageCount = cycleRoutes.sumOf { it.packageCount }

                    val cycleDailies = dailyTotals.filter { dt ->
                        dt.billingCycleId == cycle.id || (
                            dt.billingCycleId == null &&
                            dt.platformId == cycle.platformId &&
                            BillingCycleCalculator.isDateInCycle(
                                dt.occurredAt.toLocalDate(),
                                cycle.periodStart,
                                cycle.periodEnd,
                                cycle.includeEndDate
                            )
                        )
                    }
                    val dailyAmount = cycleDailies.fold(BigDecimal.ZERO) { acc, dt -> acc.add(dt.amount) }

                    val cycleAdjustments = adjustments.filter { adj ->
                        adj.billingCycleId == cycle.id || (
                            adj.billingCycleId == null &&
                            adj.platformId == cycle.platformId &&
                            BillingCycleCalculator.isDateInCycle(
                                adj.occurredAt,
                                cycle.periodStart,
                                cycle.periodEnd,
                                cycle.includeEndDate
                            )
                        )
                    }

                    val totals = BillingCycleCalculator.calculateCycleTotals(cycleRoutes, cycleDailies, cycleAdjustments)

                    BillingCycleWithTotals(
                        cycle = cycle,
                        platformName = platName,
                        routeAmount = totals.grossRoutesAmount,
                        tipTotal = totals.totalTipsAmount,
                        bonusTotal = totals.totalBonusAmount,
                        dailyAmount = totals.grossDailyAmount,
                        adjustmentsCredit = totals.adjustmentsCredit,
                        adjustmentsDebit = totals.adjustmentsDebit,
                        adjustmentsTotal = totals.adjustmentsTotal,
                        totalAmount = totals.netTotalAmount,
                        routeCount = totals.routesCount,
                        packageCount = totals.packagesCount,
                        dailyCount = totals.dailyTotalsCount,
                        adjustmentsCount = totals.adjustmentsCount,
                        routes = cycleRoutes,
                        dailyTotals = cycleDailies,
                        adjustments = cycleAdjustments
                    )
                }
            } catch (e: Exception) {
                Log.e("BillingCycleRepository", "Erro ao carregar faturas com totais: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun createBillingCycle(cycle: BillingCycle): BillingCycle? = withContext(Dispatchers.IO) {
        try {
            val dto = cycle.toDto()
            val result = api.createBillingCycle(dto)
            result.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao criar fatura: ${e.message}", e)
            null
        }
    }

    suspend fun updateStatus(cycleId: String, status: String, paymentReceivedDate: LocalDate? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any>("status" to status)
            if (paymentReceivedDate != null) {
                payload["payment_received_date"] = paymentReceivedDate.toString()
            }
            api.updateBillingCycle("eq.$cycleId", payload)
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao atualizar status da fatura: ${e.message}", e)
            false
        }
    }

    suspend fun updateRoute(route: com.fernando.centraldomotorista.data.model.Route): Boolean = withContext(Dispatchers.IO) {
        try {
            routeRepository.updateRoute(route)
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao atualizar corrida da fatura: ${e.message}", e)
            false
        }
    }

    suspend fun updateDailyTotal(dailyTotal: com.fernando.centraldomotorista.data.model.DailyTotal): Boolean = withContext(Dispatchers.IO) {
        try {
            dailyTotalRepository.updateDailyTotal(dailyTotal)
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao atualizar diária da fatura: ${e.message}", e)
            false
        }
    }

    suspend fun addFinancialAdjustment(adjustment: com.fernando.centraldomotorista.data.model.FinancialAdjustment): com.fernando.centraldomotorista.data.model.FinancialAdjustment? = withContext(Dispatchers.IO) {
        try {
            val dto = adjustment.toDto()
            val res = adjustmentApi.createFinancialAdjustment(dto)
            res.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao criar ajuste na fatura: ${e.message}", e)
            null
        }
    }

    suspend fun deleteFinancialAdjustment(adjustmentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            adjustmentApi.deleteFinancialAdjustment("eq.$adjustmentId")
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao excluir ajuste da fatura: ${e.message}", e)
            false
        }
    }

    suspend fun deleteBillingCycle(cycleId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            unlinkCycleTransactions(cycleId)
            api.deleteBillingCycle("eq.$cycleId")
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao excluir fatura: ${e.message}", e)
            false
        }
    }

    suspend fun linkCycleTransactions(
        cycleId: String,
        platformId: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        includeEndDate: Boolean = true,
        userId: String
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val allRoutes = routeRepository.getRoutes(userId)
            val matchingRoutes = allRoutes.filter { r ->
                r.platformId == platformId &&
                (r.billingCycleId == null || r.billingCycleId == cycleId) &&
                BillingCycleCalculator.isDateInCycle(r.occurredAt.toLocalDate(), periodStart, periodEnd, includeEndDate)
            }
            matchingRoutes.forEach { r ->
                if (r.billingCycleId != cycleId) {
                    routeRepository.updateRoute(r.copy(billingCycleId = cycleId))
                }
            }

            val allDailies = dailyTotalRepository.getDailyTotals(userId)
            val matchingDailies = allDailies.filter { dt ->
                dt.platformId == platformId &&
                (dt.billingCycleId == null || dt.billingCycleId == cycleId) &&
                BillingCycleCalculator.isDateInCycle(dt.occurredAt.toLocalDate(), periodStart, periodEnd, includeEndDate)
            }
            matchingDailies.forEach { dt ->
                if (dt.billingCycleId != cycleId) {
                    dailyTotalRepository.updateDailyTotal(dt.copy(billingCycleId = cycleId))
                }
            }

            val allAdjustments = adjustmentApi.getFinancialAdjustments("eq.$userId").map { it.toDomain() }
            val matchingAdjustments = allAdjustments.filter { adj ->
                adj.platformId == platformId &&
                (adj.billingCycleId == null || adj.billingCycleId == cycleId) &&
                BillingCycleCalculator.isDateInCycle(adj.occurredAt, periodStart, periodEnd, includeEndDate)
            }
            matchingAdjustments.forEach { adj ->
                if (adj.billingCycleId != cycleId) {
                    adjustmentApi.updateFinancialAdjustment("eq.${adj.id}", adj.copy(billingCycleId = cycleId).toDto())
                }
            }
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao vincular transações à fatura: ${e.message}", e)
        }
    }

    suspend fun unlinkCycleTransactions(cycleId: String): Unit = withContext(Dispatchers.IO) {
        try {
            // Em rotas e diárias vinculadas
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao desvincular corridas: ${e.message}", e)
        }
    }

    suspend fun checkOverlap(
        platformId: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        includeEndDate: Boolean = true,
        excludeCycleId: String? = null,
        userId: String
    ): BillingCycle? = withContext(Dispatchers.IO) {
        try {
            val cycles = getBillingCycles(userId, null).filter {
                it.platformId == platformId &&
                it.status != "cancelado" &&
                (excludeCycleId == null || it.id != excludeCycleId)
            }
            cycles.firstOrNull { c ->
                BillingCycleCalculator.checkOverlap(
                    periodStart, periodEnd, includeEndDate,
                    c.periodStart, c.periodEnd, c.includeEndDate
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
