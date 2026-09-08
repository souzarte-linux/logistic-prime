package com.fernando.centraldomotorista.data.model

import java.math.BigDecimal
import java.time.OffsetDateTime

enum class TransactionType {
    GANHO,
    DESPESA
}

enum class TransactionSourceType {
    ROUTE,
    EXPENSE,
    DAILY_TOTAL
}

data class TransactionItem(
    val id: String,
    val type: TransactionType,
    val sourceType: TransactionSourceType,
    val title: String,
    val subtitle: String,
    val amount: BigDecimal,            // Valor bruto lançado
    val netAmount: BigDecimal,         // Valor líquido (após aplicar subtract_routes quando aplicável)
    val category: String,              // Ex: "MANUTENÇÃO", "COMBUSTÍVEL", "ALIMENTAÇÃO", "ROTA", "TOTAL DO DIA"
    val occurredAt: OffsetDateTime,
    val establishment: String? = null, // Estabelecimento, posto ou plataforma
    val meta1: String? = null,         // Ex: "40 Pacs • 13.0 KM"
    val meta2: String? = null,         // Ex: "02H 30MIN TRABALHADOS"
    val tag: String? = null,           // Ex: "A RECEBER", "PAGO", "TOTAL", "MANUTENÇÃO"
    val subtractRoutes: Boolean = false,
    val rawExpense: Expense? = null,
    val rawRoute: Route? = null,
    val rawDailyTotal: DailyTotal? = null
)
