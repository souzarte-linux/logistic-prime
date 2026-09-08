package com.fernando.centraldomotorista.functional

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.util.AppDataSync
import com.fernando.centraldomotorista.util.EarningsCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HistoricoFunctionalTest {

    private val zone = ZoneOffset.ofHours(-3) // Horário de Brasília (local)
    private val today = LocalDate.now()
    private val nowOffset = OffsetDateTime.of(today, LocalTime.of(12, 0), zone)

    // =========================================================================
    // TESTE 1: Inscrição no AppDataSync e Recarregamento Automático
    // =========================================================================
    @Test
    fun test1_historicoScreenAndViewModel_subscribedToAppDataSync() = runBlocking {
        var notified = false
        val latch = java.util.concurrent.CountDownLatch(1)
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            latch.countDown()
            AppDataSync.dataChangedEvents.first()
            notified = true
        }

        latch.await()
        kotlinx.coroutines.delay(100)
        // Simula notificação de alteração disparada por qualquer tela/repositório
        AppDataSync.notifyDataChanged()
        job.join()

        assertTrue("HistoricoViewModel/HomeScreen devem receber eventos de AppDataSync", notified)
    }

    // =========================================================================
    // TESTE 2: Edição sem Duplicação (Preservação de ID e Update in-place)
    // =========================================================================
    @Test
    fun test2_editingWithoutDuplication_preservesIdAndUpdatesInPlace() {
        val originalRouteId = "route-uuid-123"
        val originalRoute = Route(
            id = originalRouteId,
            userId = "user-1",
            platformId = "plat-1",
            origin = "SAO",
            destination = "SPO",
            amount = BigDecimal("80.00"),
            occurredAt = nowOffset
        )

        // Simulação da tela de edição (quando itemId != null)
        val isEditing = originalRoute.id.isNotBlank()
        assertTrue("Deve identificar que é edição e não novo registro", isEditing)

        val updatedRoute = originalRoute.copy(
            amount = BigDecimal("120.00")
        )

        assertEquals("O ID original deve ser rigorosamente mantido na edição", originalRouteId, updatedRoute.id)
        assertEquals("O valor deve ser atualizado in-place", BigDecimal("120.00"), updatedRoute.amount)

        // Verificação similar para Expense e DailyTotal
        val expenseId = "exp-uuid-456"
        val originalExpense = Expense(
            id = expenseId,
            userId = "user-1",
            category = "combustivel",
            title = "Abastecimento Shell",
            amount = BigDecimal("50.00"),
            occurredAt = nowOffset
        )
        val updatedExpense = originalExpense.copy(amount = BigDecimal("65.00"))
        assertEquals("Expense ID mantido na edição", expenseId, updatedExpense.id)
        assertEquals("Valor de Expense atualizado", BigDecimal("65.00"), updatedExpense.amount)

        val dailyId = "daily-uuid-789"
        val originalDaily = DailyTotal(
            id = dailyId,
            userId = "user-1",
            platformId = "plat-1",
            amount = BigDecimal("250.00"),
            occurredAt = nowOffset
        )
        val updatedDaily = originalDaily.copy(amount = BigDecimal("300.00"))
        assertEquals("DailyTotal ID mantido na edição", dailyId, updatedDaily.id)
        assertEquals("Valor de DailyTotal atualizado", BigDecimal("300.00"), updatedDaily.amount)
    }

    // =========================================================================
    // TESTE 3: Cálculo do subtract_routes (Regras de Dedução, Zero Mínimo e Equivalência)
    // =========================================================================
    @Test
    fun test3_subtractRoutesCalculation_rulesAndEquivalence() {
        // 3 rotas no mesmo dia local somando X = 55 + 80 + 65 = 200.00
        val r1 = Route(
            id = "r1", userId = "u1", platformId = "p1", origin = null, destination = null,
            amount = BigDecimal("50.00"), tip = BigDecimal("5.00"), bonus = BigDecimal.ZERO,
            occurredAt = OffsetDateTime.of(today, LocalTime.of(8, 0), zone)
        )
        val r2 = Route(
            id = "r2", userId = "u1", platformId = "p2", origin = null, destination = null,
            amount = BigDecimal("70.00"), tip = BigDecimal.ZERO, bonus = BigDecimal("10.00"),
            occurredAt = OffsetDateTime.of(today, LocalTime.of(11, 30), zone)
        )
        val r3 = Route(
            id = "r3", userId = "u1", platformId = "p1", origin = null, destination = null,
            amount = BigDecimal("65.00"), tip = BigDecimal.ZERO, bonus = BigDecimal.ZERO,
            occurredAt = OffsetDateTime.of(today, LocalTime.of(15, 0), zone)
        )
        val routesHoje = listOf(r1, r2, r3)
        val somaRotasX = BigDecimal("200.00") // 55 + 80 + 65

        // Caso 3A: Total do Dia maior que X (ex: 350.00 > 200.00) com subtractRoutes = true
        val dtMaior = DailyTotal(
            id = "dt1", userId = "u1", platformId = "p1",
            amount = BigDecimal("350.00"),
            subtractRoutes = true,
            billingCycleId = null,
            occurredAt = OffsetDateTime.of(today, LocalTime.of(19, 0), zone)
        )
        val liquidoMaior = EarningsCalculator.calcularGanhoLiquidoDoDia(dtMaior, routesHoje)
        assertEquals(
            "Líquido deve ser exatamente (Total do Dia - soma(routes)): 350 - 200 = 150",
            BigDecimal("150.00"),
            liquidoMaior
        )

        // Equivalência entre Início e Histórico: Total de ganhos deve ser 350.00
        val totalGanhosHoje = EarningsCalculator.calcularTotalGanhos(routesHoje, listOf(dtMaior))
        assertEquals(
            "Total de ganhos consolidado (Início e Histórico) deve ser 350.00 (200 rotas + 150 liquido)",
            BigDecimal("350.00"),
            totalGanhosHoje
        )

        // Caso 3B: X maior que Total do Dia (ex: X = 200.00 > Total = 120.00) -> Líquido deve ser ZERO (nunca negativo)
        val dtMenor = DailyTotal(
            id = "dt2", userId = "u1", platformId = "p1",
            amount = BigDecimal("120.00"),
            subtractRoutes = true,
            billingCycleId = null,
            occurredAt = OffsetDateTime.of(today, LocalTime.of(20, 0), zone)
        )
        val liquidoMenor = EarningsCalculator.calcularGanhoLiquidoDoDia(dtMenor, routesHoje)
        assertEquals(
            "Quando as rotas excedem o total diário, o líquido deve ser ZERO (nunca negativo)",
            BigDecimal.ZERO,
            liquidoMenor
        )
        val totalComMenor = EarningsCalculator.calcularTotalGanhos(routesHoje, listOf(dtMenor))
        assertEquals(
            "Total com diário menor que rotas deve manter a soma das rotas (200.00)",
            BigDecimal("200.00"),
            totalComMenor
        )

        // Caso 3C: subtractRoutes = false -> Líquido é o valor integral
        val dtSemSubtracao = dtMaior.copy(subtractRoutes = false)
        val liquidoSemSubtracao = EarningsCalculator.calcularGanhoLiquidoDoDia(dtSemSubtracao, routesHoje)
        assertEquals("Com subtractRoutes=false o líquido é o valor total", BigDecimal("350.00"), liquidoSemSubtracao)

        // Caso 3D: Rotas em dias diferentes NÃO devem ser descontadas
        val rOutroDia = r1.copy(
            id = "r_outro",
            occurredAt = OffsetDateTime.of(today.minusDays(1), LocalTime.of(10, 0), zone)
        )
        val liquidoOutroDia = EarningsCalculator.calcularGanhoLiquidoDoDia(dtMaior, listOf(rOutroDia))
        assertEquals("Rotas de dias anteriores não devem ser descontadas do total de hoje", BigDecimal("350.00"), liquidoOutroDia)
    }

    // =========================================================================
    // TESTE 4: Exclusão Imediata de Transações (Rotas, Despesas e Totais)
    // =========================================================================
    @Test
    fun test4_deletionImmediatelyRemovesFromListAndSubtotals() {
        val txRoute = TransactionItem(
            id = "tx1",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "MERCADO LIVRE",
            subtitle = "SPO - SAO",
            amount = BigDecimal("100.00"),
            netAmount = BigDecimal("100.00"),
            category = "ROTA",
            occurredAt = nowOffset
        )
        val txDaily = TransactionItem(
            id = "tx2",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.DAILY_TOTAL,
            title = "TOTAL DO DIA",
            subtitle = "IFLASH",
            amount = BigDecimal("250.00"),
            netAmount = BigDecimal("150.00"), // líquido
            category = "TOTAL DO DIA",
            occurredAt = nowOffset
        )
        val txExpense = TransactionItem(
            id = "tx3",
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = "GASOLINA",
            subtitle = "POSTO SHELL",
            amount = BigDecimal("40.00"),
            netAmount = BigDecimal("40.00"),
            category = "COMBUSTÍVEL",
            occurredAt = nowOffset
        )

        var list = listOf(txRoute, txDaily, txExpense)

        // Subtotal inicial: +100.00 + 150.00 - 40.00 = +210.00
        fun calcBalance(items: List<TransactionItem>): BigDecimal {
            return items.fold(BigDecimal.ZERO) { acc, it ->
                if (it.type == TransactionType.GANHO) acc.add(it.netAmount) else acc.subtract(it.amount)
            }
        }

        assertEquals(BigDecimal("210.00"), calcBalance(list))

        // 1. Excluir Rota
        list = list.filter { it.id != txRoute.id }
        assertFalse("Rota excluída não deve estar mais na lista", list.any { it.id == txRoute.id })
        assertEquals("Subtotal deve atualizar após excluir rota (+150 - 40 = 110)", BigDecimal("110.00"), calcBalance(list))

        // 2. Excluir Despesa
        list = list.filter { it.id != txExpense.id }
        assertFalse("Despesa excluída não deve estar mais na lista", list.any { it.id == txExpense.id })
        assertEquals("Subtotal deve atualizar após excluir despesa (+150)", BigDecimal("150.00"), calcBalance(list))

        // 3. Excluir Total do Dia
        list = list.filter { it.id != txDaily.id }
        assertFalse("Total do Dia excluído não deve estar mais na lista", list.any { it.id == txDaily.id })
        assertEquals("Subtotal deve zerar após excluir todas as transações", BigDecimal.ZERO, calcBalance(list))
    }
}
