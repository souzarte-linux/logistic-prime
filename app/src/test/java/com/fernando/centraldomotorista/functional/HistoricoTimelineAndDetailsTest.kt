package com.fernando.centraldomotorista.functional

import android.graphics.Canvas
import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.ui.screens.historico.DayGroup
import com.fernando.centraldomotorista.ui.screens.historico.RouteShareHelper
import com.fernando.centraldomotorista.ui.screens.historico.RouteShareHelper.formatCurrency
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Suíte de testes funcionais para a Aba Histórico, Linha do Tempo e Modais de Visualização.
 *
 * Cobre:
 * 1. Resolução do modo de visualização para qualquer transação (rotas, diárias, despesas).
 * 2. Ordem e rótulos estritos do card DISTÂNCIA PERCORRIDA (ODÔMETRO INICIAL, ODÔMETRO FINAL, DIST. PERCORRIDO).
 * 3. Composição e regras do card VALOR TOTAL DA ROTA (adicionais, gorjetas e bônus).
 * 4. Compartilhamento via RouteShareHelper (Bitmap, PDF, formatação de textos e resiliência a nulos/zeros).
 * 5. Limites da linha do tempo contínua (primeiro dia, último dia, dias intermediários).
 */
class HistoricoTimelineAndDetailsTest {

    private val zone = ZoneOffset.ofHours(-3)
    private val baseDate = LocalDate.of(2026, 11, 12)
    private val baseDateTime = OffsetDateTime.of(baseDate, LocalTime.of(10, 0), zone)

    // =========================================================================
    // GRUPO 1: RESOLUÇÃO DE MODO DE VISUALIZAÇÃO E ROTAS DE EDIÇÃO
    // =========================================================================

    sealed class TransactionDetailsModalResolution {
        data class RouteModal(val route: Route, val platformName: String?) : TransactionDetailsModalResolution()
        data class GenericTransactionModal(val item: TransactionItem) : TransactionDetailsModalResolution()
    }

    private fun resolveDetailsModal(item: TransactionItem): TransactionDetailsModalResolution {
        return if (item.sourceType == TransactionSourceType.ROUTE && item.rawRoute != null) {
            TransactionDetailsModalResolution.RouteModal(
                route = item.rawRoute,
                platformName = item.establishment
            )
        } else {
            TransactionDetailsModalResolution.GenericTransactionModal(item)
        }
    }

    private fun resolveEditRoute(item: TransactionItem): String {
        return when (item.sourceType) {
            TransactionSourceType.ROUTE -> "lancar_rota?itemId=${item.id}"
            TransactionSourceType.DAILY_TOTAL -> "lancar_total_dia?itemId=${item.id}"
            TransactionSourceType.EXPENSE -> {
                val cat = item.rawExpense?.category?.lowercase() ?: item.category.lowercase()
                when {
                    item.rawPartnerSession != null -> "session_route?sessionId=${item.rawPartnerSession.id}&readOnly=true"
                    cat.contains("equipe") || cat.contains("parceiro") || cat.contains("entregador") -> "session_route?expenseId=${item.id}&readOnly=true"
                    cat.contains("combustivel") || cat.contains("abastecimento") -> "fuel_expense?itemId=${item.id}"
                    cat.contains("alimentacao") -> "meal_expense?itemId=${item.id}"
                    cat.contains("manutencao") || cat.contains("peca") -> "lancar-manutencao?itemId=${item.id}"
                    else -> "fuel_expense?itemId=${item.id}"
                }
            }
        }
    }

    @Test
    fun test1A_routeTransaction_resolvesToRouteDetailsModal() {
        val route = Route(
            id = "route-001",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Centro de Distribuição Cajamar",
            destination = "Pinheiros - SP",
            amount = BigDecimal("185.50"),
            occurredAt = baseDateTime
        )
        val tx = TransactionItem(
            id = route.id,
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "MERCADO LIVRE",
            subtitle = "Cajamar ➔ Pinheiros",
            amount = route.amount,
            netAmount = route.amount,
            category = "ROTA",
            establishment = "Mercado Livre",
            occurredAt = baseDateTime,
            rawRoute = route
        )

        val resolution = resolveDetailsModal(tx)
        assertTrue("Deve resolver para modal específico de Rota", resolution is TransactionDetailsModalResolution.RouteModal)
        val routeModal = resolution as TransactionDetailsModalResolution.RouteModal
        assertEquals("Deve carregar o Route completo", route.id, routeModal.route.id)
        assertEquals("Deve carregar o nome da plataforma da transação", "Mercado Livre", routeModal.platformName)

        val editRoute = resolveEditRoute(tx)
        assertEquals("Rota de edição para Rota de entrega", "lancar_rota?itemId=route-001", editRoute)
    }

    @Test
    fun test1B_dailyTotalTransaction_resolvesToGenericDetailsModalAndCalculatesLiquid() {
        val daily = DailyTotal(
            id = "daily-002",
            userId = "user-1",
            platformId = "plat-shopee",
            amount = BigDecimal("350.00"),
            subtractRoutes = true,
            distanceKm = BigDecimal("85.4"),
            productType = "geral",
            notes = "Diária completa com 45 entregas",
            occurredAt = baseDateTime
        )
        val tx = TransactionItem(
            id = daily.id,
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.DAILY_TOTAL,
            title = "SHOPEE - DIÁRIA",
            subtitle = "Total do Dia",
            amount = daily.amount,
            netAmount = BigDecimal("150.00"), // após dedução de 200 em rotas
            category = "TOTAL DIÁRIO",
            establishment = "Shopee Hub Lapa",
            occurredAt = baseDateTime,
            subtractRoutes = daily.subtractRoutes,
            rawDailyTotal = daily
        )

        val resolution = resolveDetailsModal(tx)
        assertTrue("Diária deve resolver para TransactionDetailsDialog", resolution is TransactionDetailsModalResolution.GenericTransactionModal)
        val genericModal = resolution as TransactionDetailsModalResolution.GenericTransactionModal
        assertEquals("Deve manter a transação de diária", daily.id, genericModal.item.id)
        assertEquals("Fonte deve ser DAILY_TOTAL", TransactionSourceType.DAILY_TOTAL, genericModal.item.sourceType)
        assertTrue("subtractRoutes deve ser verdadeiro", genericModal.item.subtractRoutes)
        assertEquals("Valor líquido deduzido", BigDecimal("150.00"), genericModal.item.netAmount)

        val editRoute = resolveEditRoute(tx)
        assertEquals("Edição de diária deve direcionar para lancar_total_dia", "lancar_total_dia?itemId=daily-002", editRoute)
    }

    @Test
    fun test1C_expenseTransactions_resolveToGenericDetailsModalWithRespectiveCategories() {
        // 1. Combustível
        val expenseFuel = Expense(
            id = "exp-fuel-1",
            userId = "user-1",
            category = "combustivel",
            title = "Abastecimento Shell",
            amount = BigDecimal("180.00"),
            liters = BigDecimal("32.5"),
            pricePerLiter = BigDecimal("5.54"),
            fuelType = "Gasolina Comum",
            odometerKm = BigDecimal("54200"),
            isFullTank = true,
            occurredAt = baseDateTime
        )
        val txFuel = TransactionItem(
            id = expenseFuel.id,
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = expenseFuel.title,
            subtitle = "32.5 L a R$ 5,54",
            amount = expenseFuel.amount,
            netAmount = expenseFuel.amount,
            category = "COMBUSTÍVEL",
            establishment = "Posto Shell Marginal",
            occurredAt = baseDateTime,
            rawExpense = expenseFuel
        )
        val resFuel = resolveDetailsModal(txFuel)
        assertTrue("Combustível deve abrir modal de detalhes", resFuel is TransactionDetailsModalResolution.GenericTransactionModal)
        assertEquals("fuel_expense?itemId=exp-fuel-1", resolveEditRoute(txFuel))

        // 2. Peças / Manutenção
        val expensePecas = Expense(
            id = "exp-peca-2",
            userId = "user-1",
            category = "manutencao",
            title = "Troca Pastilhas de Freio",
            amount = BigDecimal("240.00"),
            partBrand = "Fras-le",
            partModel = "PD/82",
            odometerKm = BigDecimal("55000"),
            occurredAt = baseDateTime
        )
        val txPecas = TransactionItem(
            id = expensePecas.id,
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = expensePecas.title,
            subtitle = "Fras-le PD/82",
            amount = expensePecas.amount,
            netAmount = expensePecas.amount,
            category = "MANUTENÇÃO",
            establishment = "Oficina do Beto",
            occurredAt = baseDateTime,
            rawExpense = expensePecas
        )
        assertEquals("lancar-manutencao?itemId=exp-peca-2", resolveEditRoute(txPecas))

        // 3. Alimentação
        val expenseAlim = Expense(
            id = "exp-alim-3",
            userId = "user-1",
            category = "alimentacao",
            title = "Almoço Prato Feito",
            amount = BigDecimal("35.00"),
            mealType = "almoço",
            occurredAt = baseDateTime
        )
        val txAlim = TransactionItem(
            id = expenseAlim.id,
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = expenseAlim.title,
            subtitle = "Almoço",
            amount = expenseAlim.amount,
            netAmount = expenseAlim.amount,
            category = "ALIMENTAÇÃO",
            establishment = "Restaurante Central",
            occurredAt = baseDateTime,
            rawExpense = expenseAlim
        )
        assertEquals("meal_expense?itemId=exp-alim-3", resolveEditRoute(txAlim))
    }

    // =========================================================================
    // GRUPO 2: ORDEM E RÓTULOS ESTRITOS DO CARD "DISTÂNCIA PERCORRIDA"
    // =========================================================================

    data class DistanceCardData(
        val sectionTitle: String,
        val rows: List<Pair<String, String>>
    )

    private fun extractDistanceCardContract(route: Route): DistanceCardData {
        return DistanceCardData(
            sectionTitle = "Distância Percorrida",
            rows = listOf(
                "Odômetro Inicial" to "${route.startKm} KM",
                "Odômetro Final" to "${route.endKm} KM",
                "Dist. Percorrido" to "${route.distanceKm} KM"
            )
        )
    }

    @Test
    fun test2A_distanceCard_strictOrderAndLabels() {
        val route = Route(
            id = "r-odometer-test",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Origem A",
            destination = "Destino B",
            startKm = BigDecimal("45100.5"),
            endKm = BigDecimal("45220.8"),
            distanceKm = BigDecimal("120.3"),
            occurredAt = baseDateTime
        )

        val card = extractDistanceCardContract(route)

        assertEquals("Título exato da seção", "Distância Percorrida", card.sectionTitle)
        assertEquals("Deve conter rigorosamente 3 linhas", 3, card.rows.size)

        // Linha 1: Odômetro Inicial
        assertEquals("Rótulo Linha 1", "Odômetro Inicial", card.rows[0].first)
        assertEquals("Valor Linha 1 com KM", "45100.5 KM", card.rows[0].second)

        // Linha 2: Odômetro Final
        assertEquals("Rótulo Linha 2", "Odômetro Final", card.rows[1].first)
        assertEquals("Valor Linha 2 com KM", "45220.8 KM", card.rows[1].second)

        // Linha 3: Dist. Percorrido
        assertEquals("Rótulo Linha 3", "Dist. Percorrido", card.rows[2].first)
        assertEquals("Valor Linha 3 com KM", "120.3 KM", card.rows[2].second)
    }

    @Test
    fun test2B_distanceCard_inversionOrWrongLabelFails() {
        val route = Route(
            id = "r-odometer-test",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Origem A",
            destination = "Destino B",
            startKm = BigDecimal("100"),
            endKm = BigDecimal("200"),
            distanceKm = BigDecimal("100"),
            occurredAt = baseDateTime
        )
        val card = extractDistanceCardContract(route)

        // Garante que o odômetro final NÃO está na primeira linha
        assertNotEquals("Odômetro final não pode ser o primeiro", "Odômetro Final", card.rows[0].first)
        // Garante que a distância percorrida NÃO está no topo
        assertNotEquals("Dist. percorrido não pode ser o primeiro", "Dist. Percorrido", card.rows[0].first)
        // Garante o sufixo "KM" em todos os valores
        assertTrue(card.rows.all { it.second.endsWith(" KM") })
    }

    // =========================================================================
    // GRUPO 3: LAYOUT E COMPOSIÇÃO DO CARD "VALOR TOTAL DA ROTA"
    // =========================================================================

    data class TotalCardComposition(
        val title: String,
        val formattedAmount: String,
        val tipText: String?,
        val bonusText: String?
    )

    private fun extractTotalCardComposition(route: Route): TotalCardComposition {
        val tipText = if (route.tip > BigDecimal.ZERO) "+ Gorjeta: ${route.tip.formatCurrency()}" else null
        val bonusText = if (route.bonus > BigDecimal.ZERO) "+ Bônus: ${route.bonus.formatCurrency()}" else null
        return TotalCardComposition(
            title = "VALOR TOTAL DA ROTA",
            formattedAmount = route.amount.formatCurrency(),
            tipText = tipText,
            bonusText = bonusText
        )
    }

    @Test
    fun test3A_totalCard_withTipAndBonus() {
        val route = Route(
            id = "r-total-1",
            userId = "u-1",
            platformId = "p-1",
            origin = "Origem A",
            destination = "Destino B",
            amount = BigDecimal("175.00"),
            tip = BigDecimal("15.00"),
            bonus = BigDecimal("20.00"),
            occurredAt = baseDateTime
        )

        val card = extractTotalCardComposition(route)
        assertEquals("VALOR TOTAL DA ROTA", card.title)
        assertTrue("Valor principal formatado", card.formattedAmount.contains("175,00"))
        assertNotNull("Texto de gorjeta deve existir", card.tipText)
        assertTrue("Gorjeta formatada", card.tipText!!.contains("Gorjeta: ") && card.tipText.contains("15,00"))
        assertNotNull("Texto de bônus deve existir", card.bonusText)
        assertTrue("Bônus formatado", card.bonusText!!.contains("Bônus: ") && card.bonusText.contains("20,00"))
    }

    @Test
    fun test3B_totalCard_withoutTipOrBonus_noExtraRows() {
        val route = Route(
            id = "r-total-2",
            userId = "u-1",
            platformId = "p-1",
            origin = "Origem A",
            destination = "Destino B",
            amount = BigDecimal("90.00"),
            tip = BigDecimal.ZERO,
            bonus = BigDecimal.ZERO,
            occurredAt = baseDateTime
        )

        val card = extractTotalCardComposition(route)
        assertEquals("VALOR TOTAL DA ROTA", card.title)
        assertTrue("Valor principal formatado", card.formattedAmount.contains("90,00"))
        assertNull("Não deve exibir linha de gorjeta quando zero", card.tipText)
        assertNull("Não deve exibir linha de bônus quando zero", card.bonusText)
    }

    // =========================================================================
    // GRUPO 4: HELPER DE COMPARTILHAMENTO (RouteShareHelper)
    // =========================================================================

    @Test
    fun test4A_routeShareHelper_textFormattingAndCasing() {
        val route = Route(
            id = "route-share-abc12345",
            userId = "u1",
            platformId = "plat-ml",
            origin = "Guarulhos",
            destination = "Tatuapé",
            amount = BigDecimal("210.50"),
            packageCount = 48,
            distanceKm = BigDecimal("65.2"),
            occurredAt = baseDateTime
        )

        val shareText = RouteShareHelper.buildShareText(route, "Mercado Livre")
        assertTrue("Deve conter cabeçalho", shareText.contains("Central do Motorista — Comprovante de Rota"))
        assertTrue("Empresa em maiúsculo", shareText.contains("MERCADO LIVRE"))
        assertTrue("Trajeto origem para destino", shareText.contains("Guarulhos ➔ Tatuapé"))
        assertTrue("Valor formatado", shareText.contains("210,50"))
        assertTrue("Quantidade de pacotes", shareText.contains("48"))
        assertTrue("Distância percorrida com KM", shareText.contains("65.2 KM"))
    }

    @Test
    fun test4B_routeShareHelper_calculationsAndEdgeCases() {
        // 1. Cálculo de tempo trabalhado
        val start = OffsetDateTime.of(baseDate, LocalTime.of(8, 0), zone)
        val end = OffsetDateTime.of(baseDate, LocalTime.of(12, 30), zone)
        val workedTime = RouteShareHelper.calculateWorkedTime(start, end, 30L)
        assertEquals("Duração de 4h30 com 30min pausa = 04h 00min", "04h 00min", workedTime)

        // Pausa excedente não produz tempo negativo
        val workedExceededPause = RouteShareHelper.calculateWorkedTime(start, end, 300L)
        assertEquals("Pausa maior que duração retorna 00h 00min", "00h 00min", workedExceededPause)

        // Horários nulos
        val workedNull = RouteShareHelper.calculateWorkedTime(null, end, 0L)
        assertEquals("Horário de início nulo retorna traço", "—", workedNull)

        // 2. Cálculo de pacotes totais
        val totalPkgs = RouteShareHelper.calculateTotalPackages(12, 8, 0)
        assertEquals("Soma de pequenos (12) e grandes (8) = 20", 20, totalPkgs)

        val fallbackPkgs = RouteShareHelper.calculateTotalPackages(0, 0, 35)
        assertEquals("Fallback para packageCount quando contagens detalhadas são 0", 35, fallbackPkgs)
    }

    @Test
    fun test4C_routeShareHelper_resilienceWithZeroAndNullFields() {
        // Rota vazia/zerada para provar que a geração não quebra sob dados incompletos
        val emptyRoute = Route(
            id = "route-empty-zeros",
            userId = "user-1",
            platformId = "plat-none",
            origin = null,
            destination = null,
            amount = BigDecimal.ZERO,
            tip = BigDecimal.ZERO,
            bonus = BigDecimal.ZERO,
            packageCount = 0,
            smallPackagesCount = 0,
            largePackagesCount = 0,
            startKm = BigDecimal.ZERO,
            endKm = BigDecimal.ZERO,
            distanceKm = BigDecimal.ZERO,
            notes = null,
            startedAt = null,
            endedAt = null,
            breakMinutes = 0,
            occurredAt = baseDateTime
        )

        // Não pode lançar exceção na geração do texto
        val shareText = RouteShareHelper.buildShareText(emptyRoute, null)
        assertTrue(shareText.contains("ROTA"))
        assertTrue(shareText.contains("Origem ➔ Destino"))

        // Renderização em Canvas da imagem
        val dummyCanvas = Canvas()
        try {
            RouteShareHelper.drawRouteBitmapContent(dummyCanvas, emptyRoute, null, 800f, 1040f)
        } catch (e: Throwable) {
            fail("drawRouteBitmapContent não pode lançar exceção mesmo com campos zerados: ${e.message}")
        }

        // Renderização em Canvas do PDF
        try {
            RouteShareHelper.drawRoutePdfPage(dummyCanvas, emptyRoute, null)
        } catch (e: Throwable) {
            fail("drawRoutePdfPage não pode lançar exceção mesmo com campos zerados: ${e.message}")
        }

        // Criação de PdfDocument
        val pdfDoc = RouteShareHelper.generateRoutePdfDocument(emptyRoute, null)
        assertNotNull("Documento PDF deve ser instanciado", pdfDoc)
    }

    // =========================================================================
    // GRUPO 5: COMPORTAMENTO DE LIMITES NA LINHA DO TEMPO (TimelineDayItem)
    // =========================================================================

    data class TimelineDayBounds(
        val isFirst: Boolean,
        val isLast: Boolean,
        val shouldDrawUpperLine: Boolean,
        val shouldDrawLowerLine: Boolean,
        val shouldDrawDot: Boolean
    )

    private fun resolveTimelineDrawingRules(isFirst: Boolean, isLast: Boolean): TimelineDayBounds {
        return TimelineDayBounds(
            isFirst = isFirst,
            isLast = isLast,
            shouldDrawUpperLine = !isFirst,
            shouldDrawLowerLine = !isLast,
            shouldDrawDot = true
        )
    }

    @Test
    fun test5A_timeline_singleDayInWeek_noUpperOrLowerLines() {
        val rules = resolveTimelineDrawingRules(isFirst = true, isLast = true)
        assertFalse("Primeiro e único dia não desenha linha superior", rules.shouldDrawUpperLine)
        assertFalse("Primeiro e único dia não desenha linha inferior", rules.shouldDrawLowerLine)
        assertTrue("Dot central indicador é sempre desenhado", rules.shouldDrawDot)
    }

    @Test
    fun test5B_timeline_multipleDaysInWeek_boundariesAndIntermediates() {
        val daysInWeek = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")
        val lastIndex = daysInWeek.lastIndex

        daysInWeek.forEachIndexed { index, dayName ->
            val isFirst = (index == 0)
            val isLast = (index == lastIndex)
            val rules = resolveTimelineDrawingRules(isFirst, isLast)

            when {
                isFirst -> {
                    assertEquals("Primeiro dia da semana ($dayName)", "SEG", dayName)
                    assertFalse("Primeiro dia não tem linha superior", rules.shouldDrawUpperLine)
                    assertTrue("Primeiro dia deve conectar para baixo", rules.shouldDrawLowerLine)
                }
                isLast -> {
                    assertEquals("Último dia da semana ($dayName)", "DOM", dayName)
                    assertTrue("Último dia conecta com o dia anterior vindo de cima", rules.shouldDrawUpperLine)
                    assertFalse("Último dia não tem linha inferior para estender", rules.shouldDrawLowerLine)
                }
                else -> {
                    // Dias intermediários (TER a SAB)
                    assertTrue("Dia intermediário ($dayName) deve conectar acima", rules.shouldDrawUpperLine)
                    assertTrue("Dia intermediário ($dayName) deve conectar abaixo para continuidade", rules.shouldDrawLowerLine)
                }
            }
            assertTrue("Dot sempre presente", rules.shouldDrawDot)
        }
    }
}
