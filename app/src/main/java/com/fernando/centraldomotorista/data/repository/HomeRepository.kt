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
    val alertaManutencao: PartMaintenance?,
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
                    platformApi.getPlatforms(userFilter, null, "name.asc").map { it.toDomain() }
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

            // 3. Alerta de Manutenção Proativo (Crítico se vencido, Preventivo se próximo do vencimento)
            val currentOdometerKm = maxOf(
                allExpenses.firstOrNull { it.odometerKm != null }?.odometerKm ?: BigDecimal.ZERO,
                allRoutes.firstOrNull { it.endKm > BigDecimal.ZERO }?.endKm ?: BigDecimal.ZERO
            )

            var alertaManutencao: PartMaintenance? = null
            var tipoAlertaManutencao = TipoAlertaManutencao.NENHUM
            var kmManutencao = BigDecimal.ZERO

            if (allPartMaintenances.isNotEmpty()) {
                val partsWithRemaining = allPartMaintenances.map { part ->
                    val remaining = part.kmRemaining(currentOdometerKm)
                    Pair(part, remaining)
                }

                // 1º Critérios: Peças vencidas (km restante <= 0) -> Alerta Vermelho
                val vencidos = partsWithRemaining.filter { it.second <= BigDecimal.ZERO }
                if (vencidos.isNotEmpty()) {
                    val maisCritico = vencidos.minByOrNull { it.second }
                    if (maisCritico != null) {
                        alertaManutencao = maisCritico.first
                        tipoAlertaManutencao = TipoAlertaManutencao.CRITICO
                        kmManutencao = maisCritico.second.abs()
                    }
                } else {
                    // 2º Critérios: Peças próximas do vencimento (restam <= 500 km ou restam <= 20% da vida útil) -> Alerta Amarelo
                    val preventivos = partsWithRemaining.filter { (part, remaining) ->
                        remaining > BigDecimal.ZERO && (
                            remaining <= BigDecimal("500") ||
                            (part.lifeKm > BigDecimal.ZERO && remaining <= part.lifeKm.multiply(BigDecimal("0.20")))
                        )
                    }
                    if (preventivos.isNotEmpty()) {
                        val maisProximo = preventivos.minByOrNull { it.second }
                        if (maisProximo != null) {
                            alertaManutencao = maisProximo.first
                            tipoAlertaManutencao = TipoAlertaManutencao.PREVENTIVO
                            kmManutencao = maisProximo.second
                        }
                    }
                }
            }

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
}
