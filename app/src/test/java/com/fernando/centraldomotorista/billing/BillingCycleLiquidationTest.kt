package com.fernando.centraldomotorista.billing

import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.remote.api.*
import com.fernando.centraldomotorista.data.remote.dto.*
import com.fernando.centraldomotorista.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.time.LocalDate

/**
 * T-002: Teste de reprodução do bug de liquidação de fatura com ID unbilled (Ciclo Vermelho).
 *
 * Contexto:
 * Na tela de faturas, faturas projetadas na aba "Ciclo A Vencer" que possuem valores legítimos
 * são persistidas em background por [BillingCycleRepository.getBillingCyclesWithTotals].
 * Porém, a lista retornada [validCycles] mantém o id original "unbilled_..." em vez do id persistido no banco.
 * Ao tentar liquidar a fatura via confirmPayment (FaturasViewModel.kt:434), a checagem
 * cycle.id.startsWith("unbilled_") aciona [createBillingCycle] em vez de [updateStatus],
 * gerando erro 409 de conflito / colisão de período no banco de dados ou criando duplicatas indesejadas.
 */
class BillingCycleLiquidationTest {

    // =========================================================================
    // FAKES DA CAMADA DE REDE (APIs)
    // =========================================================================

    private class FakeBillingCycleApi(private val enforceUniquePeriod: Boolean = true) : BillingCycleApi {
        val persistedCycles = mutableMapOf<String, BillingCycleDto>()
        private var idCounter = 1

        override suspend fun getBillingCycles(
            userIdFilter: String,
            statusFilter: String?,
            platformFilter: String?,
            order: String
        ): List<BillingCycleDto> {
            val uid = userIdFilter.removePrefix("eq.")
            val status = statusFilter?.removePrefix("eq.")
            val plat = platformFilter?.removePrefix("eq.")
            return persistedCycles.values.filter { dto ->
                dto.userId == uid &&
                (status == null || dto.status == status) &&
                (plat == null || dto.platformId == plat)
            }
        }

        override suspend fun createBillingCycle(cycle: BillingCycleDto): List<BillingCycleDto> {
            if (enforceUniquePeriod) {
                val duplicate = persistedCycles.values.any { existing ->
                    existing.userId == cycle.userId &&
                    existing.platformId == cycle.platformId &&
                    existing.periodStart == cycle.periodStart &&
                    existing.periodEnd == cycle.periodEnd
                }
                if (duplicate) {
                    throw IllegalStateException("409 Conflict: Fatura já cadastrada para o período ${cycle.periodStart}..${cycle.periodEnd}")
                }
            }
            val id = if (cycle.id.isNullOrBlank()) "cycle-persisted-${String.format("%03d", idCounter++)}" else cycle.id!!
            val created = cycle.copy(id = id)
            persistedCycles[id] = created
            return listOf(created)
        }

        override suspend fun updateBillingCycle(
            idFilter: String,
            cycle: Map<String, Any?>
        ): List<BillingCycleDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = persistedCycles[id] ?: return emptyList()
            val updated = existing.copy(
                status = cycle["status"] as? String ?: existing.status,
                paymentReceivedDate = cycle["payment_received_date"] as? String ?: existing.paymentReceivedDate
            )
            persistedCycles[id] = updated
            return listOf(updated)
        }

        override suspend fun deleteBillingCycle(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            persistedCycles.remove(id)
        }
    }

    private class FakePlatformApi : PlatformApi {
        val platforms = mutableListOf<PlatformDto>()

        override suspend fun getPlatforms(
            userIdFilter: String,
            partnerIdFilter: String?,
            activeFilter: String?,
            order: String
        ): List<PlatformDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return platforms.filter { it.userId == uid }
        }

        override suspend fun getPlatformById(idFilter: String): List<PlatformDto> {
            val id = idFilter.removePrefix("eq.")
            return platforms.filter { it.id == id }
        }

        override suspend fun createPlatform(platform: PlatformDto): List<PlatformDto> = listOf(platform)
        override suspend fun updatePlatform(idFilter: String, platform: PlatformDto): List<PlatformDto> = listOf(platform)
        override suspend fun deletePlatform(idFilter: String): Response<Unit> = Response.success(Unit)
    }

    private class FakeRouteApi : RouteApi {
        val storage = mutableMapOf<String, RouteDto>()

        override suspend fun getRoutes(userIdFilter: String, order: String): List<RouteDto> {
            val uid = userIdFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == uid }
        }

        override suspend fun createRoute(route: RouteDto): List<RouteDto> {
            val id = route.id ?: "route-${storage.size + 1}"
            val created = route.copy(id = id)
            storage[id] = created
            return listOf(created)
        }

        override suspend fun updateRoute(idFilter: String, route: RouteDto): List<RouteDto> {
            val id = idFilter.removePrefix("eq.")
            storage[id] = route
            return listOf(route)
        }

        override suspend fun deleteRoute(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            storage.remove(id)
        }

        override suspend fun getRouteById(idFilter: String): List<RouteDto> = emptyList()
    }

    private class FakeDailyTotalApi : DailyTotalApi {
        override suspend fun getDailyTotals(userIdFilter: String, order: String, filters: Map<String, String>): List<DailyTotalDto> = emptyList()
        override suspend fun createDailyTotal(dailyTotal: DailyTotalDto): List<DailyTotalDto> = listOf(dailyTotal)
        override suspend fun updateDailyTotal(idFilter: String, dailyTotal: DailyTotalDto): List<DailyTotalDto> = listOf(dailyTotal)
        override suspend fun deleteDailyTotal(idFilter: String) {}
        override suspend fun getDailyTotalById(idFilter: String): List<DailyTotalDto> = emptyList()
    }

    private class FakeDeliveryPartnerSessionApi : DeliveryPartnerSessionApi {
        override suspend fun getSessions(userIdFilter: String, cycleIdFilter: String?, order: String): List<DeliveryPartnerSessionDto> = emptyList()
        override suspend fun getSessionsForPartner(userIdFilter: String, partnerFilter: String, order: String): List<DeliveryPartnerSessionDto> = emptyList()
        override suspend fun getSessionById(idFilter: String): List<DeliveryPartnerSessionDto> = emptyList()
        override suspend fun createSession(session: DeliveryPartnerSessionDto): List<DeliveryPartnerSessionDto> = listOf(session)
        override suspend fun updateSession(idFilter: String, session: DeliveryPartnerSessionDto): List<DeliveryPartnerSessionDto> = listOf(session)
        override suspend fun deleteSession(idFilter: String) {}
        override suspend fun getSessionByExpenseId(expenseIdFilter: String): List<DeliveryPartnerSessionDto> = emptyList()
    }

    private class FakeFinancialAdjustmentApi : FinancialAdjustmentApi {
        override suspend fun getFinancialAdjustments(
            userIdFilter: String,
            platformIdFilter: String?,
            cycleIdFilter: String?,
            sinceFilter: String?,
            untilFilter: String?,
            order: String
        ): List<FinancialAdjustmentDto> = emptyList()
        override suspend fun createFinancialAdjustment(adjustment: FinancialAdjustmentDto): List<FinancialAdjustmentDto> = listOf(adjustment)
        override suspend fun updateFinancialAdjustment(idFilter: String, adjustment: FinancialAdjustmentDto): List<FinancialAdjustmentDto> = listOf(adjustment)
        override suspend fun deleteFinancialAdjustment(idFilter: String) {}
    }

    // Helper que delega diretamente para a rotina segura BillingCycleRepository.liquidateCycle
    private suspend fun executeCurrentLiquidationLogic(
        repository: BillingCycleRepository,
        cycle: BillingCycle,
        paymentDate: LocalDate,
        userId: String
    ): Boolean {
        return repository.liquidateCycle(cycle, paymentDate, userId)
    }

    // =========================================================================
    // CENÁRIO 1: O repositório não substitui o ID 'unbilled_' pelo ID persistido no banco
    // =========================================================================

    @Test
    fun `test getBillingCyclesWithTotals must return persisted cycle ID instead of retaining unbilled prefix`() = runBlocking {
        val userId = "user-qa-1"
        val platformId = "plat-shopee"

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)
        val fakePlatformApi = FakePlatformApi()
        val fakeRouteApi = FakeRouteApi()

        fakePlatformApi.platforms.add(
            PlatformDto(
                id = platformId,
                userId = userId,
                name = "Shopee",
                cycle = "semanal",
                paymentDay = "DOM",
                active = true
            )
        )

        // Corrida em período encerrado (ex: semana anterior)
        val pastDate = LocalDate.now().minusWeeks(2).with(java.time.DayOfWeek.WEDNESDAY)
        fakeRouteApi.storage["route-1"] = RouteDto(
            id = "route-1",
            userId = userId,
            platformId = platformId,
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("180.00"),
            occurredAt = "${pastDate}T14:00:00Z",
            billingCycleId = null
        )

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = fakePlatformApi),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        val cyclesWithTotals = repository.getBillingCyclesWithTotals(userId)
        val validCycle = cyclesWithTotals.firstOrNull { it.cycle.platformId == platformId }
        assertNotNull("Deve encontrar um ciclo válido para a plataforma", validCycle)

        // A API persistiu a fatura em background
        assertEquals("A fatura deve ter sido persistida no banco pela rotina em background", 1, fakeBillingApi.persistedCycles.size)
        val persistedId = fakeBillingApi.persistedCycles.keys.first()

        // FALHA DO CICLO VERMELHO: O repositório retorna o validCycles original com ID unbilled_ em vez do ID persistido
        assertFalse(
            "BUG REPRODUZIDO: O ciclo retornado não deve manter o prefixo unbilled_ após ser persistido no banco",
            validCycle!!.cycle.id.startsWith("unbilled_")
        )
        assertEquals("O ciclo retornado deve conter o ID real gerado pelo banco", persistedId, validCycle.cycle.id)
    }

    // =========================================================================
    // CENÁRIO 2: Liquidação de fatura com ID unbilled colide com registro já persistido no banco (409 Conflict)
    // =========================================================================

    @Test
    fun `test liquidation of unbilled cycle fails due to conflict when cycle already exists in database`() = runBlocking {
        val userId = "user-qa-2"
        val platformId = "plat-shopee"
        val today = LocalDate.now()

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)
        val fakePlatformApi = FakePlatformApi()
        val fakeRouteApi = FakeRouteApi()

        fakePlatformApi.platforms.add(
            PlatformDto(
                id = platformId,
                userId = userId,
                name = "Shopee",
                cycle = "semanal",
                paymentDay = "DOM",
                active = true
            )
        )

        // Corrida em período encerrado
        val pastDate = today.minusWeeks(2).with(java.time.DayOfWeek.WEDNESDAY)
        fakeRouteApi.storage["route-1"] = RouteDto(
            id = "route-1",
            userId = userId,
            platformId = platformId,
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("220.00"),
            occurredAt = "${pastDate}T14:00:00Z",
            billingCycleId = null
        )

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = fakePlatformApi),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        // 1. Carrega faturas: o repositório persiste a fatura no banco com status 'a_vencer'
        val cyclesWithTotals = repository.getBillingCyclesWithTotals(userId)
        val cycleToPay = cyclesWithTotals.first { it.cycle.platformId == platformId }
        val persistedCycleId = fakeBillingApi.persistedCycles.keys.first()

        // 2. Tenta liquidar a fatura na tela (FaturasViewModel.confirmPayment)
        val liquidationSuccess = executeCurrentLiquidationLogic(
            repository = repository,
            cycle = cycleToPay.cycle,
            paymentDate = today,
            userId = userId
        )

        // FALHA DO CICLO VERMELHO:
        // Como cycleToPay.cycle.id ainda é 'unbilled_...', a lógica tenta executar createBillingCycle.
        // O banco/API rejeita com 409 Conflict (período já existe), retornando null e resultando em failure (false).
        assertTrue(
            "BUG REPRODUZIDO: Liquidação da fatura a vencer deve ter sucesso sem falhar por conflito de período existente",
            liquidationSuccess
        )

        // O registro no banco deve ter sido atualizado para 'pago'
        val cycleInDb = fakeBillingApi.persistedCycles[persistedCycleId]
        assertNotNull(cycleInDb)
        assertEquals("BUG REPRODUZIDO: O registro existente no banco deve ser atualizado para 'pago'", "pago", cycleInDb!!.status)
        assertEquals(today.toString(), cycleInDb.paymentReceivedDate)
    }

    // =========================================================================
    // CENÁRIO 3: Liquidação sem restrição única gera fatura duplicada em vez de atualizar a existente
    // =========================================================================

    @Test
    fun `test liquidation of unbilled cycle creates duplicate database records instead of updating existing`() = runBlocking {
        val userId = "user-qa-3"
        val platformId = "plat-shopee"
        val today = LocalDate.now()

        // enforceUniquePeriod = false para demonstrar o efeito de duplicatas no banco de dados
        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = false)
        val fakePlatformApi = FakePlatformApi()
        val fakeRouteApi = FakeRouteApi()

        fakePlatformApi.platforms.add(
            PlatformDto(
                id = platformId,
                userId = userId,
                name = "Shopee",
                cycle = "semanal",
                paymentDay = "DOM",
                active = true
            )
        )

        val pastDate = today.minusWeeks(2).with(java.time.DayOfWeek.WEDNESDAY)
        fakeRouteApi.storage["route-1"] = RouteDto(
            id = "route-1",
            userId = userId,
            platformId = platformId,
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("300.00"),
            occurredAt = "${pastDate}T10:00:00Z",
            billingCycleId = null
        )

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = fakePlatformApi),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        // 1. Carrega faturas (persiste 1 fatura 'a_vencer' no banco)
        val cyclesWithTotals = repository.getBillingCyclesWithTotals(userId)
        val cycleToPay = cyclesWithTotals.first { it.cycle.platformId == platformId }
        val initialPersistedId = fakeBillingApi.persistedCycles.keys.first()

        assertEquals("Deve existir exatamente 1 fatura persistida no banco inicialmente", 1, fakeBillingApi.persistedCycles.size)

        // 2. Executa a liquidação
        executeCurrentLiquidationLogic(
            repository = repository,
            cycle = cycleToPay.cycle,
            paymentDate = today,
            userId = userId
        )

        // FALHA DO CICLO VERMELHO:
        // Como o ID era unbilled_..., foi criado um SEGUNDO registro no banco (status pago) enquanto o original permaneceu (status a_vencer).
        assertEquals(
            "BUG REPRODUZIDO: Não deve criar faturas duplicadas no banco para o mesmo ciclo/período",
            1,
            fakeBillingApi.persistedCycles.size
        )

        val cycleInDb = fakeBillingApi.persistedCycles[initialPersistedId]
        assertEquals("A fatura existente deve ter seu status atualizado para 'pago'", "pago", cycleInDb?.status)
    }

    // =========================================================================
    // CENÁRIO 4: Fatura projetada na UI quando o banco já possui registro persistido prévio
    // =========================================================================

    @Test
    fun `test liquidation of projected unbilled cycle when record already exists in db must update existing`() = runBlocking {
        val userId = "user-qa-4"
        val platformId = "plat-shopee"
        val today = LocalDate.now()
        val periodStart = today.minusDays(14)
        val periodEnd = today.minusDays(8)

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)

        // Fatura já existe no banco de dados previamente com status 'a_vencer'
        val existingDbCycle = BillingCycleDto(
            id = "cycle-already-saved-999",
            userId = userId,
            platformId = platformId,
            periodStart = periodStart.toString(),
            periodEnd = periodEnd.toString(),
            expectedPaymentDate = today.minusDays(5).toString(),
            status = "a_vencer"
        )
        fakeBillingApi.persistedCycles["cycle-already-saved-999"] = existingDbCycle

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = FakePlatformApi()),
            routeRepository = RouteRepository(routeApi = FakeRouteApi()),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        // Fatura com ID projetado 'unbilled_...' enviada pela UI
        val unbilledProjectedCycle = BillingCycle(
            id = "unbilled_${platformId}_${periodStart}_${periodEnd}",
            userId = userId,
            platformId = platformId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            expectedPaymentDate = today.minusDays(5),
            status = "a_vencer"
        )

        // Tenta liquidar
        val success = executeCurrentLiquidationLogic(
            repository = repository,
            cycle = unbilledProjectedCycle,
            paymentDate = today,
            userId = userId
        )

        // FALHA DO CICLO VERMELHO:
        // A lógica atual tenta createBillingCycle cegamente porque começa com 'unbilled_',
        // gerando conflito 409 com a fatura 'cycle-already-saved-999' já existente.
        assertTrue("BUG REPRODUZIDO: Liquidação de fatura projetada deve atualizar o registro existente no banco com sucesso", success)
        assertEquals("pago", fakeBillingApi.persistedCycles["cycle-already-saved-999"]?.status)
    }

    // =========================================================================
    // CENÁRIO 5: Testes diretos dos métodos liquidateCycle e closeCycleToPending
    // =========================================================================

    @Test
    fun `test liquidateCycle directly with real persisted UUID updates status to pago and links transactions`() = runBlocking {
        val userId = "user-qa-5"
        val platformId = "plat-shopee"
        val today = LocalDate.now()
        val periodStart = today.minusDays(7)
        val periodEnd = today.minusDays(1)

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)
        val fakeRouteApi = FakeRouteApi()

        val cycleId = "cycle-uuid-1234"
        fakeBillingApi.persistedCycles[cycleId] = BillingCycleDto(
            id = cycleId,
            userId = userId,
            platformId = platformId,
            periodStart = periodStart.toString(),
            periodEnd = periodEnd.toString(),
            expectedPaymentDate = today.toString(),
            status = "a_vencer"
        )

        // Adiciona uma corrida desvinculada no período
        fakeRouteApi.storage["route-5"] = RouteDto(
            id = "route-5",
            userId = userId,
            platformId = platformId,
            origin = "Origem",
            destination = "Destino",
            amount = BigDecimal("150.00"),
            occurredAt = "${periodStart}T10:00:00Z",
            billingCycleId = null
        )

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = FakePlatformApi()),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        val cycle = BillingCycle(
            id = cycleId,
            userId = userId,
            platformId = platformId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            expectedPaymentDate = today,
            status = "a_vencer"
        )

        val success = repository.liquidateCycle(cycle, today, userId)
        assertTrue("liquidateCycle direto deve retornar true", success)

        val updatedCycle = fakeBillingApi.persistedCycles[cycleId]
        assertNotNull(updatedCycle)
        assertEquals("pago", updatedCycle?.status)
        assertEquals(today.toString(), updatedCycle?.paymentReceivedDate)

        // Corrida deve ter sido vinculada ao ciclo via linkCycleTransactions
        val routeInDb = fakeRouteApi.storage["route-5"]
        assertEquals(cycleId, routeInDb?.billingCycleId)
    }

    @Test
    fun `test liquidateCycle directly with dynamic cycle creates new record and marks pago`() = runBlocking {
        val userId = "user-qa-6"
        val platformId = "plat-shopee"
        val today = LocalDate.now()
        val periodStart = today.minusDays(3)
        val periodEnd = today.plusDays(3)

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)
        val fakeRouteApi = FakeRouteApi()

        fakeRouteApi.storage["route-6"] = RouteDto(
            id = "route-6",
            userId = userId,
            platformId = platformId,
            origin = "A",
            destination = "B",
            amount = BigDecimal("85.00"),
            occurredAt = "${today}T12:00:00Z",
            billingCycleId = null
        )

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = FakePlatformApi()),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        val dynamicCycle = BillingCycle(
            id = "dynamic_${platformId}_${periodStart}_${periodEnd}",
            userId = userId,
            platformId = platformId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            expectedPaymentDate = today.plusDays(7),
            status = "em_aberto"
        )

        val success = repository.liquidateCycle(dynamicCycle, today, userId)
        assertTrue("Deve liquidar ciclo dinâmico criando registro no banco", success)
        assertEquals(1, fakeBillingApi.persistedCycles.size)

        val created = fakeBillingApi.persistedCycles.values.first()
        assertEquals("pago", created.status)
        assertEquals(today.toString(), created.paymentReceivedDate)
        assertEquals(created.id, fakeRouteApi.storage["route-6"]?.billingCycleId)
    }

    @Test
    fun `test closeCycleToPending directly moves dynamic cycle to a_vencer`() = runBlocking {
        val userId = "user-qa-7"
        val platformId = "plat-shopee"
        val today = LocalDate.now()
        val periodStart = today.minusDays(5)
        val periodEnd = today.minusDays(1)

        val fakeBillingApi = FakeBillingCycleApi(enforceUniquePeriod = true)
        val fakeRouteApi = FakeRouteApi()

        val repository = BillingCycleRepository(
            api = fakeBillingApi,
            platformRepository = PlatformRepository(platformApi = FakePlatformApi()),
            routeRepository = RouteRepository(routeApi = fakeRouteApi),
            dailyTotalRepository = DailyTotalRepository(dailyTotalApi = FakeDailyTotalApi()),
            sessionRepository = DeliveryPartnerSessionRepository(sessionApi = FakeDeliveryPartnerSessionApi()),
            adjustmentApi = FakeFinancialAdjustmentApi()
        )

        val dynamicCycle = BillingCycle(
            id = "dynamic_${platformId}_${periodStart}_${periodEnd}",
            userId = userId,
            platformId = platformId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            expectedPaymentDate = today.plusDays(2),
            status = "em_aberto"
        )

        val success = repository.closeCycleToPending(dynamicCycle, userId)
        assertTrue("Deve fechar ciclo para pendente criando registro a_vencer", success)
        assertEquals(1, fakeBillingApi.persistedCycles.size)

        val created = fakeBillingApi.persistedCycles.values.first()
        assertEquals("a_vencer", created.status)
    }
}
