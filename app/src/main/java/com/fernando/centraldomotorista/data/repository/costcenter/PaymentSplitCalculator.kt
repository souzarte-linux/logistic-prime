package com.fernando.centraldomotorista.data.repository.costcenter

import java.math.BigDecimal
import java.math.RoundingMode

data class PaymentSplit(
    val vaultAmount: BigDecimal,
    val accountAmount: BigDecimal
)

object PaymentSplitCalculator {

    private val zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

    /**
     * Regra R13 — Pagamento dividido:
     * parteCaixinha = min(valorEscolhido, saldoDaCaixinha, valorDoPagamento), nunca negativa;
     * parteConta = valorDoPagamento - parteCaixinha.
     */
    fun split(
        total: BigDecimal,
        vaultBalance: BigDecimal,
        requestedVaultAmount: BigDecimal? = null
    ): PaymentSplit {
        val safeTotal = total.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        val safeBalance = vaultBalance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        val maxFromVault = safeBalance.min(safeTotal)

        val chosenVault = requestedVaultAmount
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.coerceIn(zero, maxFromVault)
            ?: maxFromVault

        val accountAmount = (safeTotal - chosenVault).setScale(2, RoundingMode.HALF_UP)
        return PaymentSplit(
            vaultAmount = chosenVault,
            accountAmount = accountAmount
        )
    }
}
