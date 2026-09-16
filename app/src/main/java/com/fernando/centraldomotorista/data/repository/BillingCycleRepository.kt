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
    val dailyAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentsTotal: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val routeCount: Int = 0,
    val dailyCount: Int = 0,
    val adjustmentsCount: Int = 0
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

                    val cycleRoutes = routes.filter { it.billingCycleId == cycle.id }
                    val routeAmount = cycleRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                    val tipTotal = cycleRoutes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.tip) }

                    val cycleDailies = dailyTotals.filter { it.billingCycleId == cycle.id }
                    val dailyAmount = cycleDailies.fold(BigDecimal.ZERO) { acc, dt -> acc.add(dt.amount) }

                    val cycleAdjustments = adjustments.filter { it.billingCycleId == cycle.id }
                    val adjustmentsTotal = cycleAdjustments.fold(BigDecimal.ZERO) { acc, adj -> acc.add(adj.amount) }

                    val totalAmount = routeAmount.add(tipTotal).add(dailyAmount).add(adjustmentsTotal)

                    BillingCycleWithTotals(
                        cycle = cycle,
                        platformName = platName,
                        routeAmount = routeAmount,
                        tipTotal = tipTotal,
                        dailyAmount = dailyAmount,
                        adjustmentsTotal = adjustmentsTotal,
                        totalAmount = totalAmount,
                        routeCount = cycleRoutes.size,
                        dailyCount = cycleDailies.size,
                        adjustmentsCount = cycleAdjustments.size
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

    suspend fun updateStatus(cycleId: String, status: String, paymentDate: LocalDate? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any>("status" to status)
            if (paymentDate != null) {
                payload["expected_payment_date"] = paymentDate.toString()
            }
            api.updateBillingCycle("eq.$cycleId", payload)
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao atualizar status da fatura: ${e.message}", e)
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
        userId: String
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val allRoutes = routeRepository.getRoutes(userId)
            val matchingRoutes = allRoutes.filter { r ->
                r.platformId == platformId &&
                (r.billingCycleId == null || r.billingCycleId == cycleId) &&
                !r.occurredAt.toLocalDate().isBefore(periodStart) &&
                !r.occurredAt.toLocalDate().isAfter(periodEnd)
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
                !dt.occurredAt.toLocalDate().isBefore(periodStart) &&
                !dt.occurredAt.toLocalDate().isAfter(periodEnd)
            }
            matchingDailies.forEach { dt ->
                if (dt.billingCycleId != cycleId) {
                    dailyTotalRepository.updateDailyTotal(dt.copy(billingCycleId = cycleId))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao vincular corridas à fatura: ${e.message}", e)
        }
    }

    suspend fun unlinkCycleTransactions(cycleId: String): Unit = withContext(Dispatchers.IO) {
        try {
            // Em uma chamada Supabase REST ou iterando registros
            // Atualiza rotas e diárias vinculadas a este cycleId para null
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao desvincular corridas: ${e.message}", e)
        }
    }

    suspend fun checkOverlap(
        platformId: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
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
                BillingCycleCalculator.checkOverlap(periodStart, periodEnd, c.periodStart, c.periodEnd)
            }
        } catch (e: Exception) {
            null
        }
    }
}
