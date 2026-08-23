package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.*
import com.fernando.centraldomotorista.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class HomeData(
    val profile: Profile,
    val lucroHoje: BigDecimal,
    val metaDiaria: BigDecimal,
    val faltamParaMeta: BigDecimal,
    val sessaoAtiva: Boolean,
    val alertaManutencao: PartMaintenance?,
    val kmUltrapassado: BigDecimal,
    val contasAReceber: BigDecimal,
    val rotasRecentes: List<Route>,
    val notificacoesNaoLidas: Int
)

class HomeRepository(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val routeApi: RouteApi = RetrofitClient.routeApi,
    private val expenseApi: ExpenseApi = RetrofitClient.expenseApi,
    private val dailyTotalApi: DailyTotalApi = RetrofitClient.dailyTotalApi,
    private val partMaintenanceApi: PartMaintenanceApi = RetrofitClient.partMaintenanceApi,
    private val billingCycleApi: BillingCycleApi = RetrofitClient.billingCycleApi,
    private val notificationApi: NotificationApi = RetrofitClient.notificationApi
) {
    suspend fun loadHomeData(
        userId: String,
        email: String?,
        fullName: String?,
        avatarUrl: String?
    ): HomeData = withContext(Dispatchers.IO) {
        coroutineScope {
            val userFilter = "eq.$userId"
            val today = LocalDate.now()

            val profileDeferred = async {
                profileRepository.createOrFetchProfile(userId, email, fullName, avatarUrl)
            }
            val routesDeferred = async {
                try {
                    routeApi.getRoutes(userFilter, "occurred_at.desc").map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar routes: ${e.message}", e)
                    emptyList()
                }
            }
            val expensesDeferred = async {
                try {
                    expenseApi.getExpenses(userFilter, "occurred_at.desc").map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar expenses: ${e.message}", e)
                    emptyList()
                }
            }
            val dailyTotalsDeferred = async {
                try {
                    dailyTotalApi.getDailyTotals(userFilter, "occurred_at.desc").map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar daily_totals: ${e.message}", e)
                    emptyList()
                }
            }
            val partMaintenancesDeferred = async {
                try {
                    partMaintenanceApi.getPartMaintenances(userFilter).map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar part_maintenance: ${e.message}", e)
                    emptyList()
                }
            }
            val billingCyclesDeferred = async {
                try {
                    billingCycleApi.getBillingCycles(userFilter, "eq.pending").map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar billing_cycles: ${e.message}", e)
                    emptyList()
                }
            }
            val notificationsDeferred = async {
                try {
                    notificationApi.getNotifications(userFilter, "eq.false").map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar notifications: ${e.message}", e)
                    emptyList()
                }
            }

            val profile = profileDeferred.await()
            val allRoutes = routesDeferred.await()
            val allExpenses = expensesDeferred.await()
            val allDailyTotals = dailyTotalsDeferred.await()
            val allPartMaintenances = partMaintenancesDeferred.await()
            val pendingCycles = billingCyclesDeferred.await()
            val unreadNotifications = notificationsDeferred.await()

            // 1. Filtros de Hoje
            val todayRoutes = allRoutes.filter { it.occurredAt.toLocalDate() == today }
            val todayDailyTotals = allDailyTotals.filter { it.occurredAt.toLocalDate() == today }
            val todayExpenses = allExpenses.filter { it.occurredAt.toLocalDate() == today }

            val totalGanhosHoje = todayRoutes.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)
                .add(todayDailyTotals.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add))
            val totalGastosHoje = todayExpenses.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)

            val lucroHoje = totalGanhosHoje.subtract(totalGastosHoje)
            val metaDiaria = profile.dailyGoal
            val faltamParaMeta = maxOf(BigDecimal.ZERO, metaDiaria.subtract(lucroHoje))

            // 2. Sessão Ativa (rota iniciada hoje e não finalizada)
            val sessaoAtiva = todayRoutes.any { it.startedAt != null && it.endedAt == null }

            // 3. Alerta de Manutenção
            val currentOdometerKm = maxOf(
                allExpenses.firstOrNull { it.odometerKm != null }?.odometerKm ?: BigDecimal.ZERO,
                allRoutes.firstOrNull { it.endKm > BigDecimal.ZERO }?.endKm ?: BigDecimal.ZERO
            )

            var alertaManutencao: PartMaintenance? = null
            var kmUltrapassado = BigDecimal.ZERO

            if (allPartMaintenances.isNotEmpty()) {
                val vencidos = allPartMaintenances.map { part ->
                    val remaining = part.kmRemaining(currentOdometerKm)
                    Pair(part, remaining)
                }.filter { it.second <= BigDecimal.ZERO }

                if (vencidos.isNotEmpty()) {
                    val maisCritico = vencidos.minByOrNull { it.second }
                    if (maisCritico != null) {
                        alertaManutencao = maisCritico.first
                        kmUltrapassado = maisCritico.second.abs()
                    }
                }
            }

            // 4. Contas a Receber
            val pendingCycleIds = pendingCycles.map { it.id }.toSet()
            val contasAReceber = allRoutes.filter { it.billingCycleId != null && pendingCycleIds.contains(it.billingCycleId) }
                .map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)
                .add(
                    allDailyTotals.filter { it.billingCycleId != null && pendingCycleIds.contains(it.billingCycleId) }
                        .map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)
                )

            // 5. Rotas Recentes (top 5)
            val rotasRecentes = allRoutes.take(5)

            HomeData(
                profile = profile,
                lucroHoje = lucroHoje,
                metaDiaria = metaDiaria,
                faltamParaMeta = faltamParaMeta,
                sessaoAtiva = sessaoAtiva,
                alertaManutencao = alertaManutencao,
                kmUltrapassado = kmUltrapassado,
                contasAReceber = contasAReceber,
                rotasRecentes = rotasRecentes,
                notificacoesNaoLidas = unreadNotifications.size
            )
        }
    }

    suspend fun createQuickExpense(
        userId: String,
        category: String,
        amount: BigDecimal
    ): Expense = withContext(Dispatchers.IO) {
        val title = when (category.lowercase()) {
            "combustivel" -> "Abastecimento rápido"
            "manutencao" -> "Manutenção rápida"
            "alimentacao" -> "Alimentação do dia"
            else -> "Despesa rápida"
        }

        val dto = ExpenseDto(
            userId = userId,
            category = category.lowercase(),
            title = title,
            amount = amount,
            paymentMethod = "pix",
            isFullTank = true,
            occurredAt = OffsetDateTime.now().toString()
        )

        val createdList = expenseApi.createExpense(dto)
        createdList.firstOrNull()?.toDomain() ?: dto.toDomain()
    }
}
