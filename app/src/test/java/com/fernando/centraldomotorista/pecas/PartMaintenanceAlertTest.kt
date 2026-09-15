package com.fernando.centraldomotorista.pecas

import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.repository.HomeRepository
import com.fernando.centraldomotorista.data.repository.TipoAlertaManutencao
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class PartMaintenanceAlertTest {

    private fun createPart(
        id: String = "part-1",
        name: String = "Óleo do Motor",
        lifeKm: BigDecimal = BigDecimal("5000"),
        lastChangeKm: BigDecimal = BigDecimal("10000")
    ): PartMaintenance {
        return PartMaintenance(
            id = id,
            userId = "user-1",
            partName = name,
            lifeKm = lifeKm,
            lastChangeKm = lastChangeKm,
            lastChangeAt = OffsetDateTime.now()
        )
    }

    @Test
    fun testUsagePercentageCalculation() {
        val part = createPart(lifeKm = BigDecimal("10000"), lastChangeKm = BigDecimal("50000"))

        // Odometer at 50,000 -> 0% used
        assertEquals(0, part.usagePercentage(BigDecimal("50000")))
        assertEquals(BigDecimal.ZERO, part.usedKm(BigDecimal("50000")))

        // Odometer at 57,400 -> 74% used
        assertEquals(74, part.usagePercentage(BigDecimal("57400")))

        // Odometer at 57,500 -> 75% used
        assertEquals(75, part.usagePercentage(BigDecimal("57500")))

        // Odometer at 59,500 -> 95% used
        assertEquals(95, part.usagePercentage(BigDecimal("59500")))

        // Odometer at 60,000 -> 100% used
        assertEquals(100, part.usagePercentage(BigDecimal("60000")))

        // Odometer at 61,000 -> 110% used
        assertEquals(110, part.usagePercentage(BigDecimal("61000")))
    }

    @Test
    fun testNoAlertBefore75Percent() {
        // Life 10,000 km, last change at 10,000 km -> next due at 20,000 km
        val part = createPart(lifeKm = BigDecimal("10000"), lastChangeKm = BigDecimal("10000"))

        // Odometer at 17,400 (74% used)
        val alerts74 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("17400")
        )
        assertTrue("Abaixo de 75% não deve gerar nenhum card de alerta", alerts74.isEmpty())

        // Odometer at 15,000 (50% used)
        val alerts50 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("15000")
        )
        assertTrue("Com 50% de uso não deve gerar nenhum card de alerta", alerts50.isEmpty())
    }

    @Test
    fun testYellowPreventiveAlertBetween75And94Percent() {
        // Life 10,000 km, last change at 10,000 km
        val part = createPart(lifeKm = BigDecimal("10000"), lastChangeKm = BigDecimal("10000"))

        // Exact 75% (Odometer 17,500)
        val alerts75 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("17500")
        )
        assertEquals(1, alerts75.size)
        val alert75 = alerts75.first()
        assertEquals(TipoAlertaManutencao.PREVENTIVO, alert75.tipoAlerta)
        assertEquals(75, alert75.percentage)
        assertEquals(BigDecimal("2500"), alert75.kmRemaining)
        assertFalse(alert75.isOverdue)

        // 94% (Odometer 19,400)
        val alerts94 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("19400")
        )
        assertEquals(1, alerts94.size)
        val alert94 = alerts94.first()
        assertEquals(TipoAlertaManutencao.PREVENTIVO, alert94.tipoAlerta)
        assertEquals(94, alert94.percentage)
        assertEquals(BigDecimal("600"), alert94.kmRemaining)
        assertFalse(alert94.isOverdue)
    }

    @Test
    fun testRedCriticalAlertAt95PercentAndAbove() {
        val part = createPart(lifeKm = BigDecimal("10000"), lastChangeKm = BigDecimal("10000"))

        // Exactly 95% (Odometer 19,500)
        val alerts95 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("19500")
        )
        assertEquals(1, alerts95.size)
        val alert95 = alerts95.first()
        assertEquals(TipoAlertaManutencao.CRITICO, alert95.tipoAlerta)
        assertEquals(95, alert95.percentage)
        assertEquals(BigDecimal("500"), alert95.kmRemaining)
        assertFalse(alert95.isOverdue)

        // 100% (Odometer 20,000)
        val alerts100 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("20000")
        )
        assertEquals(1, alerts100.size)
        val alert100 = alerts100.first()
        assertEquals(TipoAlertaManutencao.CRITICO, alert100.tipoAlerta)
        assertEquals(100, alert100.percentage)
        assertTrue(alert100.isOverdue)
        assertEquals(BigDecimal.ZERO, alert100.kmRemaining)

        // 105% (Odometer 20,500 - Overdue by 500 km)
        val alerts105 = HomeRepository.calcularAlertasManutencao(
            parts = listOf(part),
            currentOdometerKm = BigDecimal("20500")
        )
        assertEquals(1, alerts105.size)
        val alert105 = alerts105.first()
        assertEquals(TipoAlertaManutencao.CRITICO, alert105.tipoAlerta)
        assertEquals(105, alert105.percentage)
        assertTrue(alert105.isOverdue)
        assertEquals(BigDecimal("500"), alert105.kmOverdue)
    }

    @Test
    fun testMultiplePartsOrderingAndFiltering() {
        val currentOdo = BigDecimal("20000")

        val partAbaixo75 = createPart(
            id = "1",
            name = "Pneu Dianteiro",
            lifeKm = BigDecimal("10000"),
            lastChangeKm = BigDecimal("13000") // 7000 km used -> 70% (Deve ser ignorado)
        )
        val partAmarelo80 = createPart(
            id = "2",
            name = "Pastilha de Freio",
            lifeKm = BigDecimal("10000"),
            lastChangeKm = BigDecimal("12000") // 8000 km used -> 80% (Amarelo)
        )
        val partVermelho96 = createPart(
            id = "3",
            name = "Óleo do Motor",
            lifeKm = BigDecimal("5000"),
            lastChangeKm = BigDecimal("15200") // 4800 km used -> 96% (Vermelho)
        )
        val partVermelhoVencida = createPart(
            id = "4",
            name = "Kit Relação",
            lifeKm = BigDecimal("10000"),
            lastChangeKm = BigDecimal("9000") // 11000 km used -> 110% (Vermelho Vencido)
        )

        val results = HomeRepository.calcularAlertasManutencao(
            parts = listOf(partAbaixo75, partAmarelo80, partVermelho96, partVermelhoVencida),
            currentOdometerKm = currentOdo
        )

        // partAbaixo75 deve ser excluído da lista de alertas
        assertEquals(3, results.size)

        // Ordenação por criticidade (% decrescente):
        // 1º: Kit Relação (110%)
        assertEquals("Kit Relação", results[0].part.partName)
        assertEquals(110, results[0].percentage)
        assertEquals(TipoAlertaManutencao.CRITICO, results[0].tipoAlerta)

        // 2º: Óleo do Motor (96%)
        assertEquals("Óleo do Motor", results[1].part.partName)
        assertEquals(96, results[1].percentage)
        assertEquals(TipoAlertaManutencao.CRITICO, results[1].tipoAlerta)

        // 3º: Pastilha de Freio (80%)
        assertEquals("Pastilha de Freio", results[2].part.partName)
        assertEquals(80, results[2].percentage)
        assertEquals(TipoAlertaManutencao.PREVENTIVO, results[2].tipoAlerta)
    }
}
