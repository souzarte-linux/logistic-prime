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
import java.time.ZoneId

data class BillingCycleWithTotals(
    val cycle: BillingCycle,
    val platformName: String,
    val routeAmount: BigDecimal = BigDecimal.ZERO,
    val tipTotal: BigDecimal = BigDecimal.ZERO,
    val bonusTotal: BigDecimal = BigDecimal.ZERO,
    val dailyAmount: BigDecimal = BigDecimal.ZERO,
    val sessionAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentsCredit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsDebit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsTotal: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val routeCount: Int = 0,
    val packageCount: Int = 0,
    val dailyCount: Int = 0,
    val sessionsCount: Int = 0,
    val adjustmentsCount: Int = 0,
    val routes: List<com.fernando.centraldomotorista.data.model.Route> = emptyList(),
    val dailyTotals: List<com.fernando.centraldomotorista.data.model.DailyTotal> = emptyList(),
    val adjustments: List<com.fernando.centraldomotorista.data.model.FinancialAdjustment> = emptyList(),
    val sessions: List<com.fernando.centraldomotorista.data.model.DeliveryPartnerSession> = emptyList()
) {
    val isOverdue: Boolean
        get() = cycle.status != "pago" && cycle.status != "cancelado" && cycle.expectedPaymentDate.isBefore(LocalDate.now())
}

class BillingCycleRepository(
    private val api: BillingCycleApi = RetrofitClient.billingCycleApi,
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val routeRepository: RouteRepository = RouteRepository(),
    private val dailyTotalRepository: DailyTotalRepository = DailyTotalRepository(),
    private val sessionRepository: DeliveryPartnerSessionRepository = DeliveryPartnerSessionRepository(),
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
                val sessionsDeferred = async { sessionRepository.getSessions(userId) }
                val adjustmentsDeferred = async {
                    try {
                        adjustmentApi.getFinancialAdjustments("eq.$userId").map { it.toDomain() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val cycles = cyclesDeferred.await()
                val platforms = platformsDeferred.await()
                val platformsMap = platforms.associateBy { it.id }
                val routes = routesDeferred.await()
                val dailyTotals = dailyTotalsDeferred.await()
                val sessions = sessionsDeferred.await()
                val adjustments = adjustmentsDeferred.await()

                val activeCycleIds = cycles.filter { it.status != "cancelado" }.map { it.id }.toSet()
                val systemZone = ZoneId.systemDefault()

                fun isEffectivelyUnlinked(bcId: String?): Boolean {
                    if (bcId.isNullOrBlank()) return true
                    return bcId !in activeCycleIds
                }

                cycles.map { cycle ->
                    val platform = platformsMap[cycle.platformId]
                    val platName = platform?.name ?: "Plataforma"
                    val platPartnerId = platform?.partnerId

                    val cycleRoutes = routes.filter { r ->
                        val rDate = r.occurredAt.atZoneSameInstant(systemZone).toLocalDate()
                        val isMatchingPlatform = (r.platformId == cycle.platformId) ||
                            (platPartnerId != null && platPartnerId == r.platformId)
                        val isInDate = BillingCycleCalculator.isDateInCycle(
                            rDate,
                            cycle.periodStart,
                            cycle.periodEnd,
                            cycle.includeEndDate
                        )

                        r.billingCycleId == cycle.id || (
                            isEffectivelyUnlinked(r.billingCycleId) && isMatchingPlatform && isInDate
                        )
                    }

                    val cycleDailies = dailyTotals.filter { dt ->
                        val dtDate = dt.occurredAt.atZoneSameInstant(systemZone).toLocalDate()
                        val isMatchingPlatform = (dt.platformId == cycle.platformId) ||
                            (platPartnerId != null && platPartnerId == dt.platformId)
                        val isInDate = BillingCycleCalculator.isDateInCycle(
                            dtDate,
                            cycle.periodStart,
                            cycle.periodEnd,
                            cycle.includeEndDate
                        )

                        dt.billingCycleId == cycle.id || (
                            isEffectivelyUnlinked(dt.billingCycleId) && isMatchingPlatform && isInDate
                        )
                    }

                    val cycleSessions = sessions.filter { s ->
                        val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(systemZone)?.toLocalDate()
                        val isMatchingPlatform = (s.platformId == cycle.platformId) ||
                            (platPartnerId != null && platPartnerId == s.partnerId)
                        val isInDate = sDate != null && BillingCycleCalculator.isDateInCycle(
                            sDate,
                            cycle.periodStart,
                            cycle.periodEnd,
                            cycle.includeEndDate
                        )

                        s.billingCycleId == cycle.id || (
                            isEffectivelyUnlinked(s.billingCycleId) && isMatchingPlatform && isInDate
                        )
                    }

                    val cycleAdjustments = adjustments.filter { adj ->
                        val isMatchingPlatform = (adj.platformId == cycle.platformId) ||
                            (platPartnerId != null && platPartnerId == adj.platformId)
                        val isInDate = BillingCycleCalculator.isDateInCycle(
                            adj.occurredAt,
                            cycle.periodStart,
                            cycle.periodEnd,
                            cycle.includeEndDate
                        )

                        adj.billingCycleId == cycle.id || (
                            isEffectivelyUnlinked(adj.billingCycleId) && isMatchingPlatform && isInDate
                        )
                    }

                    val totals = BillingCycleCalculator.calculateCycleTotals(
                        routes = cycleRoutes,
                        dailyTotals = cycleDailies,
                        adjustments = cycleAdjustments,
                        sessions = cycleSessions
                    )

                    BillingCycleWithTotals(
                        cycle = cycle,
                        platformName = platName,
                        routeAmount = totals.grossRoutesAmount,
                        tipTotal = totals.totalTipsAmount,
                        bonusTotal = totals.totalBonusAmount,
                        dailyAmount = totals.grossDailyAmount,
                        sessionAmount = totals.sessionAmount,
                        adjustmentsCredit = totals.adjustmentsCredit,
                        adjustmentsDebit = totals.adjustmentsDebit,
                        adjustmentsTotal = totals.adjustmentsTotal,
                        totalAmount = totals.netTotalAmount,
                        routeCount = totals.routesCount,
                        packageCount = totals.packagesCount,
                        dailyCount = totals.dailyTotalsCount,
                        sessionsCount = totals.sessionsCount,
                        adjustmentsCount = totals.adjustmentsCount,
                        routes = cycleRoutes,
                        dailyTotals = cycleDailies,
                        adjustments = cycleAdjustments,
                        sessions = cycleSessions
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

    suspend fun deleteBillingCycle(cycleId: String, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            unlinkCycleTransactions(cycleId, userId)
            api.deleteBillingCycle("eq.$cycleId")
            true
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao excluir fatura: ${e.message}", e)
            false
        }
    }

    suspend fun unlinkCycleTransactions(cycleId: String, userId: String? = null): Unit = withContext(Dispatchers.IO) {
        try {
            if (!userId.isNullOrBlank()) {
                val sessions = sessionRepository.getSessions(userId).filter { it.billingCycleId == cycleId }
                sessions.forEach { s ->
                    sessionRepository.saveSession(s.copy(billingCycleId = null))
                }

                val adjustments = adjustmentApi.getFinancialAdjustments("eq.$userId").map { it.toDomain() }.filter { it.billingCycleId == cycleId }
                adjustments.forEach { adj ->
                    adjustmentApi.updateFinancialAdjustment("eq.${adj.id}", adj.copy(billingCycleId = null).toDto())
                }

                val routes = routeRepository.getRoutes(userId).filter { it.billingCycleId == cycleId }
                routes.forEach { r ->
                    routeRepository.updateRoute(r.copy(billingCycleId = null))
                }

                val dailies = dailyTotalRepository.getDailyTotals(userId).filter { it.billingCycleId == cycleId }
                dailies.forEach { dt ->
                    dailyTotalRepository.updateDailyTotal(dt.copy(billingCycleId = null))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao desvincular transações da fatura: ${e.message}", e)
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
            val systemZone = ZoneId.systemDefault()
            val platforms = platformRepository.getPlatforms(userId)
            val currentPlatform = platforms.firstOrNull { it.id == platformId }
            val platPartnerId = currentPlatform?.partnerId

            val allRoutes = routeRepository.getRoutes(userId)
            val matchingRoutes = allRoutes.filter { r ->
                val rDate = r.occurredAt.atZoneSameInstant(systemZone).toLocalDate()
                val isMatchingPlatform = (r.platformId == platformId) || (platPartnerId != null && platPartnerId == r.platformId)
                isMatchingPlatform &&
                (r.billingCycleId == null || r.billingCycleId == cycleId) &&
                BillingCycleCalculator.isDateInCycle(rDate, periodStart, periodEnd, includeEndDate)
            }
            matchingRoutes.forEach { r ->
                if (r.billingCycleId != cycleId) {
                    routeRepository.updateRoute(r.copy(billingCycleId = cycleId))
                }
            }

            val allDailies = dailyTotalRepository.getDailyTotals(userId)
            val matchingDailies = allDailies.filter { dt ->
                val dtDate = dt.occurredAt.atZoneSameInstant(systemZone).toLocalDate()
                val isMatchingPlatform = (dt.platformId == platformId) || (platPartnerId != null && platPartnerId == dt.platformId)
                isMatchingPlatform &&
                (dt.billingCycleId == null || dt.billingCycleId == cycleId) &&
                BillingCycleCalculator.isDateInCycle(dtDate, periodStart, periodEnd, includeEndDate)
            }
            matchingDailies.forEach { dt ->
                if (dt.billingCycleId != cycleId) {
                    dailyTotalRepository.updateDailyTotal(dt.copy(billingCycleId = cycleId))
                }
            }

            val allSessions = sessionRepository.getSessions(userId)
            val matchingSessions = allSessions.filter { s ->
                val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(systemZone)?.toLocalDate()
                val isMatchingPlatform = (s.platformId == platformId) || (platPartnerId != null && platPartnerId == s.partnerId)
                isMatchingPlatform &&
                (s.billingCycleId == null || s.billingCycleId == cycleId) &&
                sDate != null && BillingCycleCalculator.isDateInCycle(sDate, periodStart, periodEnd, includeEndDate)
            }
            matchingSessions.forEach { s ->
                if (s.billingCycleId != cycleId) {
                    sessionRepository.saveSession(s.copy(billingCycleId = cycleId))
                }
            }

            val allAdjustments = adjustmentApi.getFinancialAdjustments("eq.$userId").map { it.toDomain() }
            val matchingAdjustments = allAdjustments.filter { adj ->
                val isMatchingPlatform = (adj.platformId == platformId) || (platPartnerId != null && platPartnerId == adj.platformId)
                isMatchingPlatform &&
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
