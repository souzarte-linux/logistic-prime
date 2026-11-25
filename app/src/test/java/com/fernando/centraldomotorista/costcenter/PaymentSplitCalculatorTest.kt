package com.fernando.centraldomotorista.costcenter

import com.fernando.centraldomotorista.data.repository.costcenter.PaymentSplitCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PaymentSplitCalculatorTest {

    private fun bd(value: String): BigDecimal = BigDecimal(value)

    @Test
    fun splitSemValorPedidoUsaTodoSaldoDisponivelAteOTotal() {
        // total 100, saldo 60 -> 60 + 40
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("60.00")
        )
        assertEquals(bd("60.00"), split.vaultAmount)
        assertEquals(bd("40.00"), split.accountAmount)
    }

    @Test
    fun splitComSaldoMaiorQueTotalLimitaAoTotal() {
        // total 100, saldo 150 -> 100 + 0
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("150.00")
        )
        assertEquals(bd("100.00"), split.vaultAmount)
        assertEquals(bd("0.00"), split.accountAmount)
    }

    @Test
    fun splitComValorPedidoMenorQueSaldoRespeitaValorPedido() {
        // total 100, saldo 60, valor pedido 30 -> 30 + 70
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("60.00"),
            requestedVaultAmount = bd("30.00")
        )
        assertEquals(bd("30.00"), split.vaultAmount)
        assertEquals(bd("70.00"), split.accountAmount)
    }

    @Test
    fun splitComValorPedidoAcimaDoSaldoLimitaAoSaldo() {
        // total 100, saldo 60, valor pedido 80 -> limitado a 60 + 40
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("60.00"),
            requestedVaultAmount = bd("80.00")
        )
        assertEquals(bd("60.00"), split.vaultAmount)
        assertEquals(bd("40.00"), split.accountAmount)
    }

    @Test
    fun splitComSaldoZeroUsaApenasConta() {
        // saldo 0 -> 0 + 100
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("0.00")
        )
        assertEquals(bd("0.00"), split.vaultAmount)
        assertEquals(bd("100.00"), split.accountAmount)
    }

    @Test
    fun splitComSaldoNegativoOuValorPedidoNegativoNuncaProduzParteNegativa() {
        val splitSaldoNegativo = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("-25.00"),
            requestedVaultAmount = bd("20.00")
        )
        assertEquals(bd("0.00"), splitSaldoNegativo.vaultAmount)
        assertEquals(bd("100.00"), splitSaldoNegativo.accountAmount)

        val splitPedidoNegativo = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("80.00"),
            requestedVaultAmount = bd("-10.00")
        )
        assertEquals(bd("0.00"), splitPedidoNegativo.vaultAmount)
        assertEquals(bd("100.00"), splitPedidoNegativo.accountAmount)
    }

    @Test
    fun splitComValorPedidoAcimaDoTotalLimitaAoTotal() {
        val split = PaymentSplitCalculator.split(
            total = bd("100.00"),
            vaultBalance = bd("150.00"),
            requestedVaultAmount = bd("120.00")
        )
        assertEquals(bd("100.00"), split.vaultAmount)
        assertEquals(bd("0.00"), split.accountAmount)
    }
}
