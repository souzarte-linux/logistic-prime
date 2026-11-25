package com.fernando.centraldomotorista.data.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class AccountType(val key: String, val label: String) {
    CORRENTE("corrente", "Conta corrente"),
    POUPANCA("poupanca", "Poupança"),
    PAGAMENTO("pagamento", "Conta de pagamento"),
    OUTRA("outra", "Outra");

    companion object {
        fun fromKey(key: String?): AccountType =
            entries.find { it.key == key } ?: OUTRA

        fun fromKeyOrNull(key: String?): AccountType? =
            if (key.isNullOrBlank()) null else fromKey(key)
    }
}

enum class VaultPurpose(val key: String, val label: String) {
    MANUTENCAO("manutencao", "Manutenção"),
    IMPOSTOS("impostos", "Impostos"),
    GARAGEM("garagem", "Garagem"),
    SEGURO("seguro", "Seguro"),
    CUSTOS_FIXOS("custos_fixos", "Custos fixos"),
    VIAGEM("viagem", "Viagem"),
    EMERGENCIA("emergencia", "Emergência"),
    OUTRO("outro", "Outro");

    companion object {
        fun fromKey(key: String?): VaultPurpose =
            entries.find { it.key == key } ?: OUTRO
    }
}

enum class LedgerEntryType(val key: String, val label: String) {
    APORTE("aporte", "Aporte"),
    RETIRADA("retirada", "Retirada"),
    TRANSFERENCIA("transferencia", "Transferência"),
    AJUSTE("ajuste", "Ajuste"),
    PAGAMENTO_DESPESA("pagamento_despesa", "Pagamento de despesa"),
    PAGAMENTO_CUSTO_FIXO("pagamento_custo_fixo", "Pagamento de custo fixo"),
    PAGAMENTO_FATURA("pagamento_fatura", "Pagamento de fatura"),
    ESTORNO("estorno", "Estorno");

    companion object {
        fun fromKey(key: String?): LedgerEntryType =
            entries.find { it.key == key } ?: AJUSTE
    }
}

enum class LedgerRefType(val key: String, val label: String) {
    DESPESA("despesa", "Despesa"),
    CUSTO_FIXO("custo_fixo", "Custo fixo"),
    FATURA("fatura", "Fatura"),
    MANUAL("manual", "Manual"),
    BAIXA_FATURA("baixa_fatura", "Baixa de faturamento");

    companion object {
        fun fromKey(key: String?): LedgerRefType =
            entries.find { it.key == key } ?: MANUAL

        fun fromKeyOrNull(key: String?): LedgerRefType? =
            if (key.isNullOrBlank()) null else fromKey(key)
    }
}

enum class SettlementKind(val key: String, val label: String) {
    PIX("pix", "PIX"),
    DEBITO("debito", "Débito");

    companion object {
        fun fromKey(key: String?): SettlementKind =
            entries.find { it.key == key } ?: PIX
    }
}

enum class SettlementStatus(val key: String, val label: String) {
    PENDENTE_DESPESA("PENDENTE_DESPESA", "Pendente"),
    CONFIRMADA("CONFIRMADA", "Confirmada"),
    ABORTADA("ABORTADA", "Abortada");

    companion object {
        fun fromKey(key: String?): SettlementStatus =
            entries.find { it.key == key } ?: PENDENTE_DESPESA
    }
}

enum class FixedCostCategory(val key: String, val label: String) {
    VEICULO_TRIBUTO("veiculo_tributo", "Tributos do veículo"),
    VEICULO_PROTECAO("veiculo_protecao", "Seguro e proteção"),
    GARAGEM("garagem", "Garagem e estacionamento"),
    FINANCIAMENTO("financiamento", "Financiamento"),
    COMUNICACAO("comunicacao", "Celular e internet"),
    TRABALHO_FISCAL("trabalho_fiscal", "MEI e contabilidade"),
    DOCUMENTOS("documentos", "Documentos e CNH"),
    PROTECAO_PESSOAL("protecao_pessoal", "Proteção pessoal"),
    EQUIPAMENTO("equipamento", "Equipamentos"),
    OUTRO("outro", "Outro");

    companion object {
        fun fromKey(key: String?): FixedCostCategory =
            entries.find { it.key == key } ?: OUTRO
    }
}

enum class FixedCostPeriodicity(val key: String, val label: String) {
    SEMANAL("semanal", "Semanal"),
    MENSAL("mensal", "Mensal"),
    BIMESTRAL("bimestral", "Bimestral"),
    TRIMESTRAL("trimestral", "Trimestral"),
    SEMESTRAL("semestral", "Semestral"),
    ANUAL("anual", "Anual");

    companion object {
        fun fromKey(key: String?): FixedCostPeriodicity =
            entries.find { it.key == key } ?: MENSAL
    }
}

enum class ReserveMode(val key: String, val label: String) {
    CALENDAR("CALENDAR", "Dias corridos"),
    WORKING_DAYS("WORKING_DAYS", "Dias planejados");

    companion object {
        fun fromKey(key: String?): ReserveMode =
            entries.find { it.key == key } ?: CALENDAR
    }
}

enum class AllocationStatus(val key: String, val label: String) {
    ATIVA("ATIVA", "Ativa"),
    DESFEITA("DESFEITA", "Desfeita"),
    DISPENSADA("DISPENSADA", "Dispensada");

    companion object {
        fun fromKey(key: String?): AllocationStatus =
            entries.find { it.key == key } ?: ATIVA
    }
}

enum class SyncState(val key: String, val label: String) {
    PENDENTE("PENDENTE", "Pendente"),
    SINCRONIZADO("SINCRONIZADO", "Sincronizado"),
    ERRO("ERRO", "Erro de sincronização");

    companion object {
        fun fromKey(key: String?): SyncState =
            entries.find { it.key == key } ?: PENDENTE
    }
}

data class BankAccount(
    val id: String,
    val userId: String,
    val name: String,
    val institution: String? = null,
    val agency: String? = null,
    val accountLastDigits: String? = null,
    val accountType: AccountType? = null,
    val informedBalance: BigDecimal? = null,
    val informedBalanceAt: Instant? = null,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class Vault(
    val id: String,
    val userId: String,
    val accountId: String? = null,
    val name: String,
    val purpose: VaultPurpose = VaultPurpose.OUTRO,
    val goalAmount: BigDecimal? = null,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val trackingStartDate: LocalDate,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class VaultBalance(
    val vaultId: String,
    val userId: String,
    val accountId: String?,
    val balance: BigDecimal
)

data class LedgerEntry(
    val id: String,
    val userId: String,
    val vaultId: String,
    val operationId: String,
    val leg: Int = 0,
    val entryType: LedgerEntryType,
    val amount: BigDecimal,
    val occurredAt: Instant,
    val refType: LedgerRefType? = null,
    val refId: String? = null,
    val contraVaultId: String? = null,
    val reversesEntryId: String? = null,
    val note: String? = null,
    val createdAt: Instant,
    val syncState: SyncState = SyncState.PENDENTE
)

data class ExpenseSettlement(
    val id: String,
    val userId: String,
    val expenseId: String,
    val kind: SettlementKind = SettlementKind.PIX,
    val accountId: String? = null,
    val vaultId: String? = null,
    val amount: BigDecimal,
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    val status: SettlementStatus = SettlementStatus.PENDENTE_DESPESA,
    val operationId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class FixedCost(
    val id: String,
    val userId: String,
    val name: String,
    val category: FixedCostCategory,
    val catalogKey: String? = null,
    val periodicity: FixedCostPeriodicity,
    val dueDay: Int? = null,
    val dueMonth: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val vaultId: String? = null,
    val isVehicleRelated: Boolean = false,
    val analytical: Boolean = false,
    val seeded: Boolean = false,
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class FixedCostVersion(
    val id: String,
    val userId: String,
    val fixedCostId: String,
    val amount: BigDecimal,
    val effectiveFrom: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.PENDENTE
)

data class FixedCostPayment(
    val id: String,
    val userId: String,
    val fixedCostId: String,
    val competenceDate: LocalDate,
    val paidAt: Instant,
    val amount: BigDecimal,
    val sourceAccountId: String? = null,
    val sourceVaultId: String? = null,
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    val operationId: String,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class CardInvoicePayment(
    val id: String,
    val userId: String,
    val cardId: String,
    val referenceMonth: LocalDate,
    val amount: BigDecimal,
    val paidAt: Instant,
    val sourceAccountId: String? = null,
    val sourceVaultId: String? = null,
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    val operationId: String,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class ReserveAllocation(
    val id: String,
    val userId: String,
    val billingCycleId: String,
    val vaultId: String,
    val suggestedAmount: BigDecimal,
    val amount: BigDecimal,
    val netCapApplied: Boolean = false,
    val status: AllocationStatus = AllocationStatus.ATIVA,
    val operationId: String,
    val undoneAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class ReserveAllocationDay(
    val id: String,
    val userId: String,
    val allocationId: String,
    val day: LocalDate,
    val platformId: String? = null,
    val suggestedAmount: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.PENDENTE
)

data class CardAccountLink(
    val id: String,
    val userId: String,
    val cardId: String,
    val accountId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDENTE
)

data class CostCenterSettings(
    val userId: String,
    val maintenanceVaultId: String? = null,
    val wearProvisionEnabled: Boolean = true,
    val fixedCostProvisionEnabled: Boolean = true,
    val reserveMode: ReserveMode = ReserveMode.CALENDAR,
    val plannedWeekdays: Int = 127,
    val daysOff: List<String> = emptyList(),
    val seededCatalogVersion: Int = 0,
    val dismissedSeedKeys: List<String> = emptyList(),
    val reminderEnabled: Boolean = true,
    val reminderOffsets: List<Int> = listOf(3, 0),
    val costCenterStartedOn: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.PENDENTE
)
