package com.fernando.centraldomotorista.data.local.costcenter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.AllocationStatus
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.CardAccountLink
import com.fernando.centraldomotorista.data.model.CardInvoicePayment
import com.fernando.centraldomotorista.data.model.CostCenterSettings
import com.fernando.centraldomotorista.data.model.ExpenseSettlement
import com.fernando.centraldomotorista.data.model.FixedCost
import com.fernando.centraldomotorista.data.model.FixedCostCategory
import com.fernando.centraldomotorista.data.model.FixedCostPayment
import com.fernando.centraldomotorista.data.model.FixedCostPeriodicity
import com.fernando.centraldomotorista.data.model.FixedCostVersion
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.ReserveAllocation
import com.fernando.centraldomotorista.data.model.ReserveAllocationDay
import com.fernando.centraldomotorista.data.model.ReserveMode
import com.fernando.centraldomotorista.data.model.SettlementKind
import com.fernando.centraldomotorista.data.model.SettlementStatus
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "bank_accounts",
    indices = [
        Index(value = ["user_id"])
    ]
)
data class BankAccountEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    val institution: String? = null,
    val agency: String? = null,
    @ColumnInfo(name = "account_last_digits")
    val accountLastDigits: String? = null,
    @ColumnInfo(name = "account_type")
    val accountType: String? = null,
    @ColumnInfo(name = "informed_balance")
    val informedBalance: BigDecimal? = null,
    @ColumnInfo(name = "informed_balance_at")
    val informedBalanceAt: Instant? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): BankAccount = BankAccount(
        id = id,
        userId = userId,
        name = name,
        institution = institution,
        agency = agency,
        accountLastDigits = accountLastDigits,
        accountType = AccountType.fromKeyOrNull(accountType),
        informedBalance = informedBalance,
        informedBalanceAt = informedBalanceAt,
        active = active,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun BankAccount.toEntity(syncStateOverride: SyncState = syncState): BankAccountEntity = BankAccountEntity(
    id = id,
    userId = userId,
    name = name,
    institution = institution,
    agency = agency,
    accountLastDigits = accountLastDigits,
    accountType = accountType?.key,
    informedBalance = informedBalance,
    informedBalanceAt = informedBalanceAt,
    active = active,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "vaults",
    indices = [
        Index(value = ["user_id", "account_id"])
    ]
)
data class VaultEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "account_id")
    val accountId: String? = null,
    val name: String,
    val purpose: String = VaultPurpose.OUTRO.key,
    @ColumnInfo(name = "goal_amount")
    val goalAmount: BigDecimal? = null,
    @ColumnInfo(name = "initial_balance")
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "tracking_start_date")
    val trackingStartDate: LocalDate,
    val active: Boolean = true,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): Vault = Vault(
        id = id,
        userId = userId,
        accountId = accountId,
        name = name,
        purpose = VaultPurpose.fromKey(purpose),
        goalAmount = goalAmount,
        initialBalance = initialBalance,
        trackingStartDate = trackingStartDate,
        active = active,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun Vault.toEntity(syncStateOverride: SyncState = syncState): VaultEntity = VaultEntity(
    id = id,
    userId = userId,
    accountId = accountId,
    name = name,
    purpose = purpose.key,
    goalAmount = goalAmount,
    initialBalance = initialBalance,
    trackingStartDate = trackingStartDate,
    active = active,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "vault_ledger",
    indices = [
        Index(value = ["operation_id", "leg"], unique = true),
        Index(value = ["user_id", "occurred_at"]),
        Index(value = ["vault_id", "occurred_at"]),
        Index(value = ["ref_type", "ref_id"])
    ]
)
data class VaultLedgerEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "vault_id")
    val vaultId: String,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    val leg: Int = 0,
    @ColumnInfo(name = "entry_type")
    val entryType: String,
    val amount: BigDecimal,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Instant,
    @ColumnInfo(name = "ref_type")
    val refType: String? = null,
    @ColumnInfo(name = "ref_id")
    val refId: String? = null,
    @ColumnInfo(name = "contra_vault_id")
    val contraVaultId: String? = null,
    @ColumnInfo(name = "reverses_entry_id")
    val reversesEntryId: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): LedgerEntry = LedgerEntry(
        id = id,
        userId = userId,
        vaultId = vaultId,
        operationId = operationId,
        leg = leg,
        entryType = LedgerEntryType.fromKey(entryType),
        amount = amount,
        occurredAt = occurredAt,
        refType = LedgerRefType.fromKeyOrNull(refType),
        refId = refId,
        contraVaultId = contraVaultId,
        reversesEntryId = reversesEntryId,
        note = note,
        createdAt = createdAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun LedgerEntry.toEntity(syncStateOverride: SyncState = syncState): VaultLedgerEntity = VaultLedgerEntity(
    id = id,
    userId = userId,
    vaultId = vaultId,
    operationId = operationId,
    leg = leg,
    entryType = entryType.key,
    amount = amount,
    occurredAt = occurredAt,
    refType = refType?.key,
    refId = refId,
    contraVaultId = contraVaultId,
    reversesEntryId = reversesEntryId,
    note = note,
    createdAt = createdAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "expense_settlements",
    indices = [
        Index(value = ["expense_id"], unique = true),
        Index(value = ["operation_id"], unique = true),
        Index(value = ["vault_id"])
    ]
)
data class ExpenseSettlementEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "expense_id")
    val expenseId: String,
    val kind: String = SettlementKind.PIX.key,
    @ColumnInfo(name = "account_id")
    val accountId: String? = null,
    @ColumnInfo(name = "vault_id")
    val vaultId: String? = null,
    val amount: BigDecimal,
    @ColumnInfo(name = "vault_amount")
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "account_amount")
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    val status: String = SettlementStatus.PENDENTE_DESPESA.key,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): ExpenseSettlement = ExpenseSettlement(
        id = id,
        userId = userId,
        expenseId = expenseId,
        kind = SettlementKind.fromKey(kind),
        accountId = accountId,
        vaultId = vaultId,
        amount = amount,
        vaultAmount = vaultAmount,
        accountAmount = accountAmount,
        status = SettlementStatus.fromKey(status),
        operationId = operationId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun ExpenseSettlement.toEntity(syncStateOverride: SyncState = syncState): ExpenseSettlementEntity = ExpenseSettlementEntity(
    id = id,
    userId = userId,
    expenseId = expenseId,
    kind = kind.key,
    accountId = accountId,
    vaultId = vaultId,
    amount = amount,
    vaultAmount = vaultAmount,
    accountAmount = accountAmount,
    status = status.key,
    operationId = operationId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "fixed_costs",
    indices = [
        Index(value = ["user_id", "active"]),
        Index(value = ["vault_id"])
    ]
)
data class FixedCostEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    val category: String,
    @ColumnInfo(name = "catalog_key")
    val catalogKey: String? = null,
    val periodicity: String,
    @ColumnInfo(name = "due_day")
    val dueDay: Int? = null,
    @ColumnInfo(name = "due_month")
    val dueMonth: Int? = null,
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate? = null,
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate? = null,
    @ColumnInfo(name = "vault_id")
    val vaultId: String? = null,
    @ColumnInfo(name = "is_vehicle_related")
    val isVehicleRelated: Boolean = false,
    val analytical: Boolean = false,
    val seeded: Boolean = false,
    val notes: String? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): FixedCost = FixedCost(
        id = id,
        userId = userId,
        name = name,
        category = FixedCostCategory.fromKey(category),
        catalogKey = catalogKey,
        periodicity = FixedCostPeriodicity.fromKey(periodicity),
        dueDay = dueDay,
        dueMonth = dueMonth,
        startDate = startDate,
        endDate = endDate,
        vaultId = vaultId,
        isVehicleRelated = isVehicleRelated,
        analytical = analytical,
        seeded = seeded,
        notes = notes,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun FixedCost.toEntity(syncStateOverride: SyncState = syncState): FixedCostEntity = FixedCostEntity(
    id = id,
    userId = userId,
    name = name,
    category = category.key,
    catalogKey = catalogKey,
    periodicity = periodicity.key,
    dueDay = dueDay,
    dueMonth = dueMonth,
    startDate = startDate,
    endDate = endDate,
    vaultId = vaultId,
    isVehicleRelated = isVehicleRelated,
    analytical = analytical,
    seeded = seeded,
    notes = notes,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "fixed_cost_versions",
    indices = [
        Index(value = ["fixed_cost_id", "effective_from"], unique = true)
    ]
)
data class FixedCostVersionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "fixed_cost_id")
    val fixedCostId: String,
    val amount: BigDecimal,
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: LocalDate,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): FixedCostVersion = FixedCostVersion(
        id = id,
        userId = userId,
        fixedCostId = fixedCostId,
        amount = amount,
        effectiveFrom = effectiveFrom,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun FixedCostVersion.toEntity(syncStateOverride: SyncState = syncState): FixedCostVersionEntity = FixedCostVersionEntity(
    id = id,
    userId = userId,
    fixedCostId = fixedCostId,
    amount = amount,
    effectiveFrom = effectiveFrom,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "fixed_cost_payments",
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["source_vault_id"]),
        Index(value = ["fixed_cost_id", "competence_date"])
    ]
)
data class FixedCostPaymentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "fixed_cost_id")
    val fixedCostId: String,
    @ColumnInfo(name = "competence_date")
    val competenceDate: LocalDate,
    @ColumnInfo(name = "paid_at")
    val paidAt: Instant,
    val amount: BigDecimal,
    @ColumnInfo(name = "source_account_id")
    val sourceAccountId: String? = null,
    @ColumnInfo(name = "source_vault_id")
    val sourceVaultId: String? = null,
    @ColumnInfo(name = "vault_amount")
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "account_amount")
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): FixedCostPayment = FixedCostPayment(
        id = id,
        userId = userId,
        fixedCostId = fixedCostId,
        competenceDate = competenceDate,
        paidAt = paidAt,
        amount = amount,
        sourceAccountId = sourceAccountId,
        sourceVaultId = sourceVaultId,
        vaultAmount = vaultAmount,
        accountAmount = accountAmount,
        operationId = operationId,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun FixedCostPayment.toEntity(syncStateOverride: SyncState = syncState): FixedCostPaymentEntity = FixedCostPaymentEntity(
    id = id,
    userId = userId,
    fixedCostId = fixedCostId,
    competenceDate = competenceDate,
    paidAt = paidAt,
    amount = amount,
    sourceAccountId = sourceAccountId,
    sourceVaultId = sourceVaultId,
    vaultAmount = vaultAmount,
    accountAmount = accountAmount,
    operationId = operationId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "card_invoice_payments",
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["card_id", "reference_month"]),
        Index(value = ["source_vault_id"])
    ]
)
data class CardInvoicePaymentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "reference_month")
    val referenceMonth: LocalDate,
    val amount: BigDecimal,
    @ColumnInfo(name = "paid_at")
    val paidAt: Instant,
    @ColumnInfo(name = "source_account_id")
    val sourceAccountId: String? = null,
    @ColumnInfo(name = "source_vault_id")
    val sourceVaultId: String? = null,
    @ColumnInfo(name = "vault_amount")
    val vaultAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "account_amount")
    val accountAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): CardInvoicePayment = CardInvoicePayment(
        id = id,
        userId = userId,
        cardId = cardId,
        referenceMonth = referenceMonth,
        amount = amount,
        paidAt = paidAt,
        sourceAccountId = sourceAccountId,
        sourceVaultId = sourceVaultId,
        vaultAmount = vaultAmount,
        accountAmount = accountAmount,
        operationId = operationId,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun CardInvoicePayment.toEntity(syncStateOverride: SyncState = syncState): CardInvoicePaymentEntity = CardInvoicePaymentEntity(
    id = id,
    userId = userId,
    cardId = cardId,
    referenceMonth = referenceMonth,
    amount = amount,
    paidAt = paidAt,
    sourceAccountId = sourceAccountId,
    sourceVaultId = sourceVaultId,
    vaultAmount = vaultAmount,
    accountAmount = accountAmount,
    operationId = operationId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "cost_center_settings",
    indices = [
        Index(value = ["maintenance_vault_id"])
    ]
)
data class CostCenterSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "maintenance_vault_id")
    val maintenanceVaultId: String? = null,
    @ColumnInfo(name = "wear_provision_enabled")
    val wearProvisionEnabled: Boolean = true,
    @ColumnInfo(name = "fixed_cost_provision_enabled")
    val fixedCostProvisionEnabled: Boolean = true,
    @ColumnInfo(name = "reserve_mode")
    val reserveMode: String = ReserveMode.CALENDAR.key,
    @ColumnInfo(name = "planned_weekdays")
    val plannedWeekdays: Int = 127,
    @ColumnInfo(name = "days_off")
    val daysOff: List<String> = emptyList(),
    @ColumnInfo(name = "seeded_catalog_version")
    val seededCatalogVersion: Int = 0,
    @ColumnInfo(name = "dismissed_seed_keys")
    val dismissedSeedKeys: List<String> = emptyList(),
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = true,
    @ColumnInfo(name = "reminder_offsets")
    val reminderOffsets: List<Int> = listOf(3, 0),
    @ColumnInfo(name = "cost_center_started_on")
    val costCenterStartedOn: LocalDate? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): CostCenterSettings = CostCenterSettings(
        userId = userId,
        maintenanceVaultId = maintenanceVaultId,
        wearProvisionEnabled = wearProvisionEnabled,
        fixedCostProvisionEnabled = fixedCostProvisionEnabled,
        reserveMode = ReserveMode.fromKey(reserveMode),
        plannedWeekdays = plannedWeekdays,
        daysOff = daysOff,
        seededCatalogVersion = seededCatalogVersion,
        dismissedSeedKeys = dismissedSeedKeys,
        reminderEnabled = reminderEnabled,
        reminderOffsets = reminderOffsets,
        costCenterStartedOn = costCenterStartedOn,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun CostCenterSettings.toEntity(syncStateOverride: SyncState = syncState): CostCenterSettingsEntity = CostCenterSettingsEntity(
    userId = userId,
    maintenanceVaultId = maintenanceVaultId,
    wearProvisionEnabled = wearProvisionEnabled,
    fixedCostProvisionEnabled = fixedCostProvisionEnabled,
    reserveMode = reserveMode.key,
    plannedWeekdays = plannedWeekdays,
    daysOff = daysOff,
    seededCatalogVersion = seededCatalogVersion,
    dismissedSeedKeys = dismissedSeedKeys,
    reminderEnabled = reminderEnabled,
    reminderOffsets = reminderOffsets,
    costCenterStartedOn = costCenterStartedOn,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "reserve_allocations",
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["billing_cycle_id"]),
        Index(value = ["vault_id"])
    ]
)
data class ReserveAllocationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "billing_cycle_id")
    val billingCycleId: String,
    @ColumnInfo(name = "vault_id")
    val vaultId: String,
    @ColumnInfo(name = "suggested_amount")
    val suggestedAmount: BigDecimal,
    val amount: BigDecimal,
    @ColumnInfo(name = "net_cap_applied")
    val netCapApplied: Boolean = false,
    val status: String = AllocationStatus.ATIVA.key,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    @ColumnInfo(name = "undone_at")
    val undoneAt: Instant? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): ReserveAllocation = ReserveAllocation(
        id = id,
        userId = userId,
        billingCycleId = billingCycleId,
        vaultId = vaultId,
        suggestedAmount = suggestedAmount,
        amount = amount,
        netCapApplied = netCapApplied,
        status = AllocationStatus.fromKey(status),
        operationId = operationId,
        undoneAt = undoneAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun ReserveAllocation.toEntity(syncStateOverride: SyncState = syncState): ReserveAllocationEntity = ReserveAllocationEntity(
    id = id,
    userId = userId,
    billingCycleId = billingCycleId,
    vaultId = vaultId,
    suggestedAmount = suggestedAmount,
    amount = amount,
    netCapApplied = netCapApplied,
    status = status.key,
    operationId = operationId,
    undoneAt = undoneAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "reserve_allocation_days",
    indices = [
        Index(value = ["allocation_id", "day"])
    ]
)
data class ReserveAllocationDayEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "allocation_id")
    val allocationId: String,
    val day: LocalDate,
    @ColumnInfo(name = "platform_id")
    val platformId: String? = null,
    @ColumnInfo(name = "suggested_amount")
    val suggestedAmount: BigDecimal,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): ReserveAllocationDay = ReserveAllocationDay(
        id = id,
        userId = userId,
        allocationId = allocationId,
        day = day,
        platformId = platformId,
        suggestedAmount = suggestedAmount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun ReserveAllocationDay.toEntity(syncStateOverride: SyncState = syncState): ReserveAllocationDayEntity = ReserveAllocationDayEntity(
    id = id,
    userId = userId,
    allocationId = allocationId,
    day = day,
    platformId = platformId,
    suggestedAmount = suggestedAmount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncStateOverride.name
)

@Entity(
    tableName = "card_account_links",
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["card_id"])
    ]
)
data class CardAccountLinkEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ColumnInfo(name = "sync_state")
    val syncState: String = SyncState.PENDENTE.name
) {
    fun toDomain(): CardAccountLink = CardAccountLink(
        id = id,
        userId = userId,
        cardId = cardId,
        accountId = accountId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = SyncState.fromKey(syncState)
    )
}

fun CardAccountLink.toEntity(syncStateOverride: SyncState = syncState): CardAccountLinkEntity = CardAccountLinkEntity(
    id = id,
    userId = userId,
    cardId = cardId,
    accountId = accountId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncStateOverride.name
)
