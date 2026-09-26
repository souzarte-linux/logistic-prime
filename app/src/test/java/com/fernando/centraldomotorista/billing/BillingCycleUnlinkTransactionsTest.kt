package com.fernando.centraldomotorista.billing

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.remote.api.*
import com.fernando.centraldomotorista.data.remote.dto.*
import com.fernando.centraldomotorista.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BillingCycleUnlinkTransactionsTest {

    // --- Fakes das APIs envolvidas ---

    private class FakeBillingCycleApi : BillingCycleApi {
        val deletedCycles = mutableListOf<String>()

        override suspend fun getBillingCycles(
            userIdFilter: String,
            statusFilter: String?,
            platformFilter: String?,
            order: String
        ): List<BillingCycleDto> = emptyList()

        override suspend fun createBillingCycle(cycle: BillingCycleDto): List<BillingCycleDto> = listOf(cycle)

        override suspend fun updateBillingCycle(
            idFilter: String,
            cycle: Map<String, Any?>
        ): List<BillingCycleDto> = emptyList()

        override suspend fun deleteBillingCycle(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            deletedCycles.add(id)
        }
    }

    private class FakeDeliveryPartnerSessionApi : DeliveryPartnerSessionApi {
        val storage = mutableMapOf<String, DeliveryPartnerSessionDto>()

        override suspend fun getSessions(userIdFilter: String, cycleIdFilter: String?, order: String): List<DeliveryPartnerSessionDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == uid }
        }
        override suspend fun getSessionsForPartner(userIdFilter: String, partnerFilter: String, order: String): List<DeliveryPartnerSessionDto> = emptyList()
        override suspend fun getSessionById(idFilter: String): List<DeliveryPartnerSessionDto> = emptyList()
        override suspend fun createSession(session: DeliveryPartnerSessionDto): List<DeliveryPartnerSessionDto> = listOf(session)
        override suspend fun updateSession(idFilter: String, session: DeliveryPartnerSessionDto): List<DeliveryPartnerSessionDto> {
            val id = idFilter.removePrefix("eq.")
            storage[id] = session
            return listOf(session)
        }
        override suspend fun deleteSession(idFilter: String) {}
        override suspend fun getSessionByExpenseId(expenseIdFilter: String): List<DeliveryPartnerSessionDto> = emptyList()
    }

    private class FakeFinancialAdjustmentApi : FinancialAdjustmentApi {
        val storage = mutableMapOf<String, FinancialAdjustmentDto>()

        override suspend fun getFinancialAdjustments(
            userIdFilter: String,
            platformIdFilter: String?,
            cycleIdFilter: String?,
            sinceFilter: String?,
            untilFilter: String?,
            order: String
        ): List<FinancialAdjustmentDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == uid }
        }
        override suspend fun createFinancialAdjustment(adjustment: FinancialAdjustmentDto): List<FinancialAdjustmentDto> = listOf(adjustment)
        override suspend fun updateFinancialAdjustment(idFilter: String, adjustment: FinancialAdjustmentDto): List<FinancialAdjustmentDto> {
            val id = idFilter.removePrefix("eq.")
            storage[id] = adjustment
            return listOf(adjustment)
        }
        override suspend fun deleteFinancialAdjustment(idFilter: String) {}
    }

    private class FakeRouteApi : RouteApi {
        val storage = mutableMapOf<String, RouteDto>()

        override suspend fun getRoutes(userIdFilter: String, order: String): List<RouteDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == uid }
        }
        override suspend fun createRoute(route: RouteDto): List<RouteDto> = listOf(route)
        override suspend fun updateRoute(idFilter: String, route: RouteDto): List<RouteDto> {
            val id = idFilter.removePrefix("eq.")
            storage[id] = route
            return listOf(route)
        }
        override suspend fun deleteRoute(idFilter: String) {}
        override suspend fun getRouteById(idFilter: String): List<RouteDto> = emptyList()
    }

    private class FakeDailyTotalApi : DailyTotalApi {
        val storage = mutableMapOf<String, DailyTotalDto>()

        override suspend fun getDailyTotals(userIdFilter: String, order: String, filters: Map<String, String>): List<DailyTotalDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == uid }
        }
        override suspend fun createDailyTotal(dailyTotal: DailyTotalDto): List<DailyTotalDto> = listOf(dailyTotal)
        override suspend fun updateDailyTotal(idFilter: String, dailyTotal: DailyTotalDto): List<DailyTotalDto> {
            val id = idFilter.removePrefix("eq.")
            storage[id] = dailyTotal
            return listOf(dailyTotal)
        }
        override suspend fun deleteDailyTotal(idFilter: String) {}
        override suspend fun getDailyTotalById(idFilter: String): List<DailyTotalDto> = emptyList()
    }

    @Test
    fun testUnlinkCycleTransactionsUnlinksAllLinkedEntities() = runBlocking {
        val cycleTargetId = "cycle-100"
        val cycleOtherId = "cycle-200"
        val userId = "user-qa"

        // 1. Configuração dos Fakes
        val fakeBillingApi = FakeBillingCycleApi()
        val fakeSessionApi = FakeDeliveryPartnerSessionApi()
        val fakeAdjustmentApi = FakeFinancialAdjustmentApi()
        val fakeRouteApi = FakeRouteApi()
        val fakeDailyApi = FakeDailyTotalApi()

        // 2. Popular Sessões de Parceiros
        val sess1 = DeliveryPartnerSessionDto(id = "sess-1", userId = userId, partnerId = "p1", billingCycleId = cycleTargetId)
        val sess2 = DeliveryPartnerSessionDto(id = "sess-2", userId = userId, partnerId = "p2", billingCycleId = cycleOtherId)
        val sess3 = DeliveryPartnerSessionDto(id = "sess-3", userId = userId, partnerId = "p3", billingCycleId = null)
        fakeSessionApi.storage["sess-1"] = sess1
        fakeSessionApi.storage["sess-2"] = sess2
        fakeSessionApi.storage["sess-3"] = sess3

        // 3. Popular Ajustes Financeiros
        val adj1 = FinancialAdjustmentDto(id = "adj-1", userId = userId, platformId = "plat-1", billingCycleId = cycleTargetId, type = "credit", amount = BigDecimal("50.00"), occurredAt = "2026-09-20T10:00:00Z")
        val adj2 = FinancialAdjustmentDto(id = "adj-2", userId = userId, platformId = "plat-1", billingCycleId = cycleOtherId, type = "debit", amount = BigDecimal("20.00"), occurredAt = "2026-09-20T10:00:00Z")
        fakeAdjustmentApi.storage["adj-1"] = adj1
        fakeAdjustmentApi.storage["adj-2"] = adj2

        // 4. Popular Rotas
        val route1 = RouteDto(id = "route-1", userId = userId, platformId = "plat-1", origin = "A", destination = "B", amount = BigDecimal("100.00"), occurredAt = "2026-09-20T10:00:00Z", billingCycleId = cycleTargetId)
        val route2 = RouteDto(id = "route-2", userId = userId, platformId = "plat-1", origin = "C", destination = "D", amount = BigDecimal("120.00"), occurredAt = "2026-09-20T10:00:00Z", billingCycleId = cycleOtherId)
        fakeRouteApi.storage["route-1"] = route1
        fakeRouteApi.storage["route-2"] = route2

        // 5. Popular Diárias
        val daily1 = DailyTotalDto(id = "daily-1", userId = userId, platformId = "plat-1", amount = BigDecimal("300.00"), occurredAt = "2026-09-20T00:00:00Z", billingCycleId = cycleTargetId)
        val daily2 = DailyTotalDto(id = "daily-2", userId = userId, platformId = "plat-1", amount = BigDecimal("350.00"), occurredAt = "2026-09-21T00:00:00Z", billingCycleId = null)
        fakeDailyApi.storage["daily-1"] = daily1
        fakeDailyApi.storage["daily-2"] = daily2

        // 6. Montar repositório com dependências fakes
        val sessionRepo = DeliveryPartnerSessionRepository(sessionApi = fakeSessionApi)
        val routeRepo = RouteRepository(routeApi = fakeRouteApi)
        val dailyRepo = DailyTotalRepository(dailyTotalApi = fakeDailyApi)

        val billingRepo = BillingCycleRepository(
            api = fakeBillingApi,
            routeRepository = routeRepo,
            dailyTotalRepository = dailyRepo,
            sessionRepository = sessionRepo,
            adjustmentApi = fakeAdjustmentApi
        )

        // 7. Executar a desvinculação para o cycleTargetId
        billingRepo.unlinkCycleTransactions(cycleId = cycleTargetId, userId = userId)

        // --- Verificações Assertivas Rigorosas ---

        // Sessões: sess-1 deve estar desvinculada (null); sess-2 deve manter cycle-200; sess-3 permanece null
        assertNull("sess-1 deve ter billingCycleId nulo após unlink", fakeSessionApi.storage["sess-1"]?.billingCycleId)
        assertEquals("cycle-200", fakeSessionApi.storage["sess-2"]?.billingCycleId)
        assertNull(fakeSessionApi.storage["sess-3"]?.billingCycleId)

        // Ajustes: adj-1 deve estar desvinculado (null); adj-2 deve manter cycle-200
        assertNull("adj-1 deve ter billingCycleId nulo após unlink", fakeAdjustmentApi.storage["adj-1"]?.billingCycleId)
        assertEquals("cycle-200", fakeAdjustmentApi.storage["adj-2"]?.billingCycleId)

        // Rotas: route-1 desvinculada (null); route-2 intacta (cycle-200)
        assertNull("route-1 deve ter billingCycleId nulo após unlink", fakeRouteApi.storage["route-1"]?.billingCycleId)
        assertEquals("cycle-200", fakeRouteApi.storage["route-2"]?.billingCycleId)

        // Diárias: daily-1 desvinculada (null); daily-2 intacta (null)
        assertNull("daily-1 deve ter billingCycleId nulo após unlink", fakeDailyApi.storage["daily-1"]?.billingCycleId)
        assertNull(fakeDailyApi.storage["daily-2"]?.billingCycleId)
    }

    @Test
    fun testUnlinkCycleTransactionsWithNullOrBlankUserIdDoesNothing() = runBlocking {
        val fakeSessionApi = FakeDeliveryPartnerSessionApi()
        val sess = DeliveryPartnerSessionDto(id = "sess-1", userId = "u1", partnerId = "p1", billingCycleId = "cycle-1")
        fakeSessionApi.storage["sess-1"] = sess

        val billingRepo = BillingCycleRepository(
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = fakeSessionApi)
        )

        // Chamada com userId nulo
        billingRepo.unlinkCycleTransactions(cycleId = "cycle-1", userId = null)
        assertEquals("cycle-1", fakeSessionApi.storage["sess-1"]?.billingCycleId)

        // Chamada com userId em branco
        billingRepo.unlinkCycleTransactions(cycleId = "cycle-1", userId = "   ")
        assertEquals("cycle-1", fakeSessionApi.storage["sess-1"]?.billingCycleId)
    }

    @Test
    fun testDeleteBillingCycleTriggersUnlinkTransactionsAndDeletesCycle() = runBlocking {
        val cycleId = "cycle-to-delete"
        val userId = "user-123"

        val fakeBillingApi = FakeBillingCycleApi()
        val fakeSessionApi = FakeDeliveryPartnerSessionApi()
        val fakeAdjustmentApi = FakeFinancialAdjustmentApi()
        val fakeRouteApi = FakeRouteApi()
        val fakeDailyApi = FakeDailyTotalApi()

        val sess = DeliveryPartnerSessionDto(id = "sess-del", userId = userId, partnerId = "p1", billingCycleId = cycleId)
        fakeSessionApi.storage["sess-del"] = sess

        val billingRepo = BillingCycleRepository(
            api = fakeBillingApi,
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = fakeDailyApi),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = fakeSessionApi),
            adjustmentApi = fakeAdjustmentApi
        )

        val deleted = billingRepo.deleteBillingCycle(cycleId = cycleId, userId = userId)
        assertTrue("deleteBillingCycle deve retornar true em caso de sucesso", deleted)

        // Transação desvinculada
        assertNull("A sessão deve ter sido desvinculada antes da deleção", fakeSessionApi.storage["sess-del"]?.billingCycleId)

        // Ciclo deletado na API
        assertEquals(1, fakeBillingApi.deletedCycles.size)
        assertEquals(cycleId, fakeBillingApi.deletedCycles.first())
    }
}
