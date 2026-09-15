package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.ui.screens.deliverypartners.SessionCalculationHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class SessionEditCalculationTest {

    @Test
    fun testParseAmountWithVariousFormats() {
        assertEquals(BigDecimal.ZERO, SessionCalculationHelper.parseAmount(""))
        assertEquals(BigDecimal.ZERO, SessionCalculationHelper.parseAmount("   "))
        assertEquals(BigDecimal("150.00"), SessionCalculationHelper.parseAmount("150.00"))
        assertEquals(BigDecimal("150.00"), SessionCalculationHelper.parseAmount("150,00"))
        assertEquals(BigDecimal("2450.50"), SessionCalculationHelper.parseAmount("R$ 2.450,50"))
        assertEquals(BigDecimal("12.34"), SessionCalculationHelper.parseAmount("abc 12,34 xyz"))
    }

    @Test
    fun testCalculateAmountDirect() {
        // 50 entregues * R$ 3,00 + R$ 0,00 = R$ 150,00
        val total1 = SessionCalculationHelper.calculateAmount(
            deliveredCount = 50,
            packageRate = BigDecimal("3.00"),
            defaultBonus = BigDecimal.ZERO
        )
        assertEquals(BigDecimal("150.00"), total1)

        // 80 entregues * R$ 2,50 + R$ 30,00 = 200,00 + 30,00 = R$ 230,00
        val total2 = SessionCalculationHelper.calculateAmount(
            deliveredCount = 80,
            packageRate = BigDecimal("2.50"),
            defaultBonus = BigDecimal("30.00")
        )
        assertEquals(BigDecimal("230.00"), total2)

        // 0 entregues com bônus de diária = R$ 50,00
        val total3 = SessionCalculationHelper.calculateAmount(
            deliveredCount = 0,
            packageRate = BigDecimal("2.50"),
            defaultBonus = BigDecimal("50.00")
        )
        assertEquals(BigDecimal("50.00"), total3)

        // 0 entregues sem bônus = R$ 0,00
        val total4 = SessionCalculationHelper.calculateAmount(
            deliveredCount = 0,
            packageRate = BigDecimal("2.50"),
            defaultBonus = BigDecimal.ZERO
        )
        assertEquals(BigDecimal.ZERO, total4)
    }

    @Test
    fun testCalculateDeliveredFromAmountReverseBilateral() {
        // R$ 150,00 total / R$ 3,00 taxa (sem bônus) = 50 pacotes
        val del1 = SessionCalculationHelper.calculateDeliveredFromAmount(
            amount = BigDecimal("150.00"),
            packageRate = BigDecimal("3.00"),
            defaultBonus = BigDecimal.ZERO
        )
        assertEquals(50, del1)

        // R$ 230,00 total - R$ 30,00 bônus = R$ 200,00 / R$ 2,50 taxa = 80 pacotes
        val del2 = SessionCalculationHelper.calculateDeliveredFromAmount(
            amount = BigDecimal("230.00"),
            packageRate = BigDecimal("2.50"),
            defaultBonus = BigDecimal("30.00")
        )
        assertEquals(80, del2)

        // Taxa zerada não deve causar divisão por zero
        val delZeroRate = SessionCalculationHelper.calculateDeliveredFromAmount(
            amount = BigDecimal("100.00"),
            packageRate = BigDecimal.ZERO,
            defaultBonus = BigDecimal.ZERO
        )
        assertEquals(0, delZeroRate)

        // Valor menor que o bônus retorna 0
        val delLowAmount = SessionCalculationHelper.calculateDeliveredFromAmount(
            amount = BigDecimal("20.00"),
            packageRate = BigDecimal("3.00"),
            defaultBonus = BigDecimal("50.00")
        )
        assertEquals(0, delLowAmount)

        // Arredondamento HALF_UP: 100 / 3 = 33.33 -> 33
        val delRounding = SessionCalculationHelper.calculateDeliveredFromAmount(
            amount = BigDecimal("100.00"),
            packageRate = BigDecimal("3.00"),
            defaultBonus = BigDecimal.ZERO
        )
        assertEquals(33, delRounding)
    }

    @Test
    fun testCalculateReturnedAndDeliveredHarmonization() {
        // Expedidos 50, Entregues 48 -> Devolvidos 2
        val ret = SessionCalculationHelper.calculateReturned(expected = 50, delivered = 48)
        assertEquals(2, ret)

        // Expedidos 50, Devolvidos 5 -> Entregues 45
        val del = SessionCalculationHelper.calculateDelivered(expected = 50, returned = 5)
        assertEquals(45, del)

        // Entregues maior que expedidos -> Devolvidos coagido a 0
        val retOverflow = SessionCalculationHelper.calculateReturned(expected = 50, delivered = 55)
        assertEquals(0, retOverflow)
    }

    @Test
    fun testFullBilateralEditingLifecycle() {
        val rate = BigDecimal("2.50")
        val bonus = BigDecimal("20.00")
        var expected = 60
        var delivered = 60
        var returned = 0

        // Início: 60 entregues -> 60 * 2.50 + 20 = 170.00
        var total = SessionCalculationHelper.calculateAmount(delivered, rate, bonus)
        assertEquals(BigDecimal("170.00"), total)

        // 1. Usuário altera Entregues para 50:
        delivered = 50
        returned = SessionCalculationHelper.calculateReturned(expected, delivered) // 60 - 50 = 10
        total = SessionCalculationHelper.calculateAmount(delivered, rate, bonus) // 50 * 2.50 + 20 = 145.00
        assertEquals(10, returned)
        assertEquals(BigDecimal("145.00"), total)

        // 2. Usuário altera Expedidos para 70:
        expected = 70
        delivered = SessionCalculationHelper.calculateDelivered(expected, returned) // 70 - 10 = 60
        total = SessionCalculationHelper.calculateAmount(delivered, rate, bonus) // 60 * 2.50 + 20 = 170.00
        assertEquals(60, delivered)
        assertEquals(BigDecimal("170.00"), total)

        // 3. Usuário altera Devolvidos para 5:
        returned = 5
        delivered = SessionCalculationHelper.calculateDelivered(expected, returned) // 70 - 5 = 65
        total = SessionCalculationHelper.calculateAmount(delivered, rate, bonus) // 65 * 2.50 + 20 = 182.50
        assertEquals(65, delivered)
        assertEquals(BigDecimal("182.50"), total)

        // 4. Sentido Reverso (Bilateral): Usuário altera Valor Total Pago para 220.00:
        val newAmount = BigDecimal("220.00")
        delivered = SessionCalculationHelper.calculateDeliveredFromAmount(newAmount, rate, bonus) // (220 - 20) / 2.50 = 80
        returned = SessionCalculationHelper.calculateReturned(expected, delivered) // max(0, 70 - 80) = 0
        assertEquals(80, delivered)
        assertEquals(0, returned)
    }
}
