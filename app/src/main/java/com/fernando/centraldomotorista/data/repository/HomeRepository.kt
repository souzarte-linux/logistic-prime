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
import java.time.ZoneId

enum class TipoAlertaManutencao {
    NENHUM,
    PREVENTIVO,
    CRITICO
}

data class PartMaintenanceAlertItem(
    val part: PartMaintenance,
    val tipoAlerta: TipoAlertaManutencao,
    val percentage: Int,
    val kmRemaining: BigDecimal,
    val kmOverdue: BigDecimal,
    val isOverdue: Boolean
)

data class ReceivableItem(
    val id: String,
    val title: String,
    val platformName: String?,
    val date: OffsetDateTime,
    val amount: BigDecimal,
    val isDailyTotal: Boolean = false
)

data class HomeData(
    val profile: Profile,
    val lucroHoje: BigDecimal,
    val ganhosHoje: BigDecimal,
    val despesasHoje: BigDecimal,
    val metaDiaria: BigDecimal,
    val faltamParaMeta: BigDecimal,
    val sessaoAtiva: Boolean,
    val alertasManutencao: List<PartMaintenanceAlertItem> = emptyList(),
    val alertaManutencao: PartMaintenance? = null,
    val tipoAlertaManutencao: TipoAlertaManutencao = TipoAlertaManutencao.NENHUM,
    val kmManutencao: BigDecimal = BigDecimal.ZERO,
    val contasAReceber: BigDecimal,
    val itensAReceber: List<ReceivableItem> = emptyList(),
    val rotasRecentes: List<Route>,
    val plataformasMap: Map<String, String> = emptyMap(),
    val notificacoesNaoLidas: Int,
    val notificacoes: List<AppNotification> = emptyList()
)

class HomeRepository(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val routeApi: RouteApi = RetrofitClient.routeApi,
    private val expenseApi: ExpenseApi = RetrofitClient.expenseApi,
    private val dailyTotalApi: DailyTotalApi = RetrofitClient.dailyTotalApi,
    private val partMaintenanceApi: PartMaintenanceApi = RetrofitClient.partMaintenanceApi,
    private val billingCycleApi: BillingCycleApi = RetrofitClient.billingCycleApi,
    private val notificationApi: NotificationApi = RetrofitClient.notificationApi,
    private val platformApi: PlatformApi = RetrofitClient.platformApi
) {
    suspend fun loadHomeData(
        userId: String,
        email: String?,
        fullName: String?,
        avatarUrl: String?
    ): HomeData = withContext(Dispatchers.IO) {
        coroutineScope {
            val userFilter = "eq.$userId"
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

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
            val platformsDeferred = async {
                try {
                    platformApi.getPlatforms(
                        userIdFilter = userFilter,
                        partnerIdFilter = "is.null",
                        activeFilter = null,
                        order = "name.asc"
                    ).map { it.toDomain() }
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Erro ao buscar platforms: ${e.message}", e)
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
            val platforms = platformsDeferred.await()
            val platformsMap = platforms.associate { it.id to it.name }

            // 1. Filtros de Hoje (convertendo datas para o fuso local)
            val todayRoutes = allRoutes.filter { it.occurredAt.atZoneSameInstant(zone).toLocalDate() == today }
            val todayDailyTotals = allDailyTotals.filter { it.occurredAt.atZoneSameInstant(zone).toLocalDate() == today }
            val todayExpenses = allExpenses.filter { it.occurredAt.atZoneSameInstant(zone).toLocalDate() == today }

            val totalGanhosHoje = com.fernando.centraldomotorista.util.EarningsCalculator.calcularTotalGanhos(
                routes = todayRoutes,
                dailyTotals = todayDailyTotals,
                zone = zone
            )
            val totalGastosHoje = todayExpenses.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)

            val lucroHoje = totalGanhosHoje.subtract(totalGastosHoje)
            val metaDiaria = profile.dailyGoal
            val faltamParaMeta = maxOf(BigDecimal.ZERO, metaDiaria.subtract(lucroHoje))

            // 2. Sessão Ativa (rota iniciada hoje e não finalizada)
            val sessaoAtiva = todayRoutes.any { it.startedAt != null && it.endedAt == null }

            // 3. Alertas de Manutenção Proativo (75% para Amarelo, 95% para Vermelho)
            val currentOdometerKm = maxOf(
                allExpenses.firstOrNull { it.odometerKm != null }?.odometerKm ?: BigDecimal.ZERO,
                allRoutes.firstOrNull { it.endKm > BigDecimal.ZERO }?.endKm ?: BigDecimal.ZERO
            )

            val alertasManutencao = calcularAlertasManutencao(allPartMaintenances, currentOdometerKm)
            val alertaMaisCritico = alertasManutencao.firstOrNull()
            val alertaManutencao = alertaMaisCritico?.part
            val tipoAlertaManutencao = alertaMaisCritico?.tipoAlerta ?: TipoAlertaManutencao.NENHUM
            val kmManutencao = alertaMaisCritico?.let {
                if (it.isOverdue) it.kmOverdue else it.kmRemaining
            } ?: BigDecimal.ZERO

            // 4. Contas a Receber e Detalhamento de Itens
            val pendingCycleIds = pendingCycles.map { it.id }.toSet()
            val pendingRoutes = allRoutes.filter { it.billingCycleId != null && pendingCycleIds.contains(it.billingCycleId) }
            val pendingDailyTotals = allDailyTotals.filter { it.billingCycleId != null && pendingCycleIds.contains(it.billingCycleId) }

            val itensAReceber = mutableListOf<ReceivableItem>()
            pendingRoutes.forEach { r ->
                val origin = r.origin?.ifBlank { "Origem" } ?: "Rota"
                val dest = r.destination?.ifBlank { "Destino" } ?: "Entregas"
                itensAReceber.add(
                    ReceivableItem(
                        id = r.id,
                        title = "$origin ➔ $dest",
                        platformName = r.platformId?.let { platformsMap[it] },
                        date = r.occurredAt,
                        amount = r.amount,
                        isDailyTotal = false
                    )
                )
            }
            pendingDailyTotals.forEach { dt ->
                itensAReceber.add(
                    ReceivableItem(
                        id = dt.id,
                        title = "Lançamento Total do Dia",
                        platformName = dt.platformId?.let { platformsMap[it] },
                        date = dt.occurredAt,
                        amount = dt.amount,
                        isDailyTotal = true
                    )
                )
            }
            val itensAReceberOrdenados = itensAReceber.sortedByDescending { it.date }
            val contasAReceber = itensAReceberOrdenados.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)

            // 5. Rotas Recentes (top 5)
            val rotasRecentes = allRoutes.take(5)

            HomeData(
                profile = profile,
                lucroHoje = lucroHoje,
                ganhosHoje = totalGanhosHoje,
                despesasHoje = totalGastosHoje,
                metaDiaria = metaDiaria,
                faltamParaMeta = faltamParaMeta,
                sessaoAtiva = sessaoAtiva,
                alertasManutencao = alertasManutencao,
                alertaManutencao = alertaManutencao,
                tipoAlertaManutencao = tipoAlertaManutencao,
                kmManutencao = kmManutencao,
                contasAReceber = contasAReceber,
                itensAReceber = itensAReceberOrdenados,
                rotasRecentes = rotasRecentes,
                plataformasMap = platformsMap,
                notificacoesNaoLidas = unreadNotifications.size,
                notificacoes = unreadNotifications
            )
        }
    }

    suspend fun updateDailyGoal(userId: String, dailyGoal: BigDecimal): Boolean {
        return profileRepository.updateDailyGoal(userId, dailyGoal)
    }

    suspend fun markNotificationAsRead(notificationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            notificationApi.updateNotification("eq.$notificationId", mapOf("read" to true))
            true
        } catch (e: Exception) {
            Log.e("HomeRepository", "Erro ao marcar notificação como lida: ${e.message}", e)
            false
        }
    }

    companion object {
        /**
         * Calcula a lista de alertas de peças monitoradas:
         * - Abaixo de 75%: não exibe alerta (lista vazia para a peça).
         * - De 75% a 94%: Alerta PREVENTIVO (Amarelo).
         * - A partir de 95%: Alerta CRÍTICO (Vermelho).
         * As peças são ordenadas da maior porcentagem de uso para a menor.
         */
        fun calcularAlertasManutencao(
            parts: List<PartMaintenance>,
            currentOdometerKm: BigDecimal
        ): List<PartMaintenanceAlertItem> {
            return parts.mapNotNull { part ->
                if (part.lifeKm <= BigDecimal.ZERO) return@mapNotNull null
                val percentage = part.usagePercentage(currentOdometerKm)
                val remaining = part.kmRemaining(currentOdometerKm)
                val isOverdue = remaining <= BigDecimal.ZERO
                val overdueKm = if (isOverdue) remaining.abs() else BigDecimal.ZERO

                when {
                    percentage >= 95 -> {
                        PartMaintenanceAlertItem(
                            part = part,
                            tipoAlerta = TipoAlertaManutencao.CRITICO,
                            percentage = percentage,
                            kmRemaining = maxOf(BigDecimal.ZERO, remaining),
                            kmOverdue = overdueKm,
                            isOverdue = isOverdue
                        )
                    }
                    percentage >= 75 -> {
                        PartMaintenanceAlertItem(
                            part = part,
                            tipoAlerta = TipoAlertaManutencao.PREVENTIVO,
                            percentage = percentage,
                            kmRemaining = maxOf(BigDecimal.ZERO, remaining),
                            kmOverdue = overdueKm,
                            isOverdue = isOverdue
                        )
                    }
                    else -> null // Antes de 75% não deve ser mostrado o card de aviso na aba início
                }
            }.sortedWith(
                compareByDescending<PartMaintenanceAlertItem> { it.percentage }
                    .thenBy { it.kmRemaining }
            )
        }
    }
}
