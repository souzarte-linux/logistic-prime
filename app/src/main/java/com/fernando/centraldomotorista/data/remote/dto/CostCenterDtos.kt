package com.fernando.centraldomotorista.data.remote.dto

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
import com.fernando.centraldomotorista.sync.OutboxPayloadSerializer
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

private fun parseInstantOrNow(raw: String?): Instant =
    parseInstantOrNull(raw) ?: Instant.now()

private fun parseInstantOrNull(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(raw).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(raw)
        } catch (_: Exception) {
            null
        }
    }
}

private fun parseLocalDateOrNull(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw.take(10))
    } catch (_: Exception) {
        null
    }
}

private fun BigDecimal.scale2(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
private fun BigDecimal.scale6(): BigDecimal = setScale(6, RoundingMode.HALF_UP)

fun Any.toOutboxJsonObject(): JsonObject =
    OutboxPayloadSerializer.gson.toJsonTree(this).asJsonObject

data class BankAccountDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("institution") val institution: String? = null,
    @SerializedName("agency") val agency: String? = null,
    @SerializedName("account_last_digits") val accountLastDigits: String? = null,
    @SerializedName("account_type") val accountType: String? = null,
    @SerializedName("informed_balance") val informedBalance: BigDecimal? = null,
    @SerializedName("informed_balance_at") val informedBalanceAt: String? = null,
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): BankAccount = BankAccount(
        id = id,
        userId = userId,
        name = name,
        institution = institution,
        agency = agency,
        accountLastDigits = accountLastDigits,
        accountType = AccountType.fromKeyOrNull(accountType),
        informedBalance = informedBalance?.scale2(),
        informedBalanceAt = parseInstantOrNull(informedBalanceAt),
        active = active,
        sortOrder = sortOrder,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun BankAccount.toDto(): BankAccountDto = BankAccountDto(
    id = id,
    userId = userId,
    name = name,
    institution = institution,
    agency = agency,
    accountLastDigits = accountLastDigits,
    accountType = accountType?.key,
    informedBalance = informedBalance?.scale2(),
    informedBalanceAt = informedBalanceAt?.toString(),
    active = active,
    sortOrder = sortOrder,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class VaultDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("account_id") val accountId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("purpose") val purpose: String = VaultPurpose.OUTRO.key,
    @SerializedName("goal_amount") val goalAmount: BigDecimal? = null,
    @SerializedName("initial_balance") val initialBalance: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("tracking_start_date") val trackingStartDate: String,
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): Vault = Vault(
        id = id,
        userId = userId,
        accountId = accountId,
        name = name,
        purpose = VaultPurpose.fromKey(purpose),
        goalAmount = goalAmount?.scale2(),
        initialBalance = initialBalance.scale2(),
        trackingStartDate = parseLocalDateOrNull(trackingStartDate) ?: LocalDate.now(),
        active = active,
        sortOrder = sortOrder,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun Vault.toDto(): VaultDto = VaultDto(
    id = id,
    userId = userId,
    accountId = accountId,
    name = name,
    purpose = purpose.key,
    goalAmount = goalAmount?.scale2(),
    initialBalance = initialBalance.scale2(),
    trackingStartDate = trackingStartDate.toString(),
    active = active,
    sortOrder = sortOrder,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class VaultLedgerDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("vault_id") val vaultId: String,
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("leg") val leg: Int = 0,
    @SerializedName("entry_type") val entryType: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("occurred_at") val occurredAt: String,
    @SerializedName("ref_type") val refType: String? = null,
    @SerializedName("ref_id") val refId: String? = null,
    @SerializedName("contra_vault_id") val contraVaultId: String? = null,
    @SerializedName("reverses_entry_id") val reversesEntryId: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): LedgerEntry = LedgerEntry(
        id = id,
        userId = userId,
        vaultId = vaultId,
        operationId = operationId,
        leg = leg,
        entryType = LedgerEntryType.fromKey(entryType),
        amount = amount.scale2(),
        occurredAt = parseInstantOrNow(occurredAt),
        refType = LedgerRefType.fromKeyOrNull(refType),
        refId = refId,
        contraVaultId = contraVaultId,
        reversesEntryId = reversesEntryId,
        note = note,
        createdAt = parseInstantOrNow(createdAt),
        syncState = syncState
    )
}

fun LedgerEntry.toDto(): VaultLedgerDto = VaultLedgerDto(
    id = id,
    userId = userId,
    vaultId = vaultId,
    operationId = operationId,
    leg = leg,
    entryType = entryType.key,
    amount = amount.scale2(),
    occurredAt = occurredAt.toString(),
    refType = refType?.key,
    refId = refId,
    contraVaultId = contraVaultId,
    reversesEntryId = reversesEntryId,
    note = note,
    createdAt = createdAt.toString()
)

data class ExpenseSettlementDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("expense_id") val expenseId: String,
    @SerializedName("kind") val kind: String = SettlementKind.PIX.key,
    @SerializedName("account_id") val accountId: String? = null,
    @SerializedName("vault_id") val vaultId: String? = null,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("vault_amount") val vaultAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("account_amount") val accountAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("status") val status: String = SettlementStatus.PENDENTE_DESPESA.key,
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): ExpenseSettlement = ExpenseSettlement(
        id = id,
        userId = userId,
        expenseId = expenseId,
        kind = SettlementKind.fromKey(kind),
        accountId = accountId,
        vaultId = vaultId,
        amount = amount.scale2(),
        vaultAmount = vaultAmount.scale2(),
        accountAmount = accountAmount.scale2(),
        status = SettlementStatus.fromKey(status),
        operationId = operationId,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun ExpenseSettlement.toDto(): ExpenseSettlementDto = ExpenseSettlementDto(
    id = id,
    userId = userId,
    expenseId = expenseId,
    kind = kind.key,
    accountId = accountId,
    vaultId = vaultId,
    amount = amount.scale2(),
    vaultAmount = vaultAmount.scale2(),
    accountAmount = accountAmount.scale2(),
    status = status.key,
    operationId = operationId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class FixedCostDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("catalog_key") val catalogKey: String? = null,
    @SerializedName("periodicity") val periodicity: String,
    @SerializedName("due_day") val dueDay: Int? = null,
    @SerializedName("due_month") val dueMonth: Int? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("vault_id") val vaultId: String? = null,
    @SerializedName("is_vehicle_related") val isVehicleRelated: Boolean = false,
    @SerializedName("analytical") val analytical: Boolean = false,
    @SerializedName("seeded") val seeded: Boolean = false,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): FixedCost = FixedCost(
        id = id,
        userId = userId,
        name = name,
        category = FixedCostCategory.fromKey(category),
        catalogKey = catalogKey,
        periodicity = FixedCostPeriodicity.fromKey(periodicity),
        dueDay = dueDay,
        dueMonth = dueMonth,
        startDate = parseLocalDateOrNull(startDate),
        endDate = parseLocalDateOrNull(endDate),
        vaultId = vaultId,
        isVehicleRelated = isVehicleRelated,
        analytical = analytical,
        seeded = seeded,
        notes = notes,
        active = active,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun FixedCost.toDto(): FixedCostDto = FixedCostDto(
    id = id,
    userId = userId,
    name = name,
    category = category.key,
    catalogKey = catalogKey,
    periodicity = periodicity.key,
    dueDay = dueDay,
    dueMonth = dueMonth,
    startDate = startDate?.toString(),
    endDate = endDate?.toString(),
    vaultId = vaultId,
    isVehicleRelated = isVehicleRelated,
    analytical = analytical,
    seeded = seeded,
    notes = notes,
    active = active,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class FixedCostVersionDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("fixed_cost_id") val fixedCostId: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("effective_from") val effectiveFrom: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): FixedCostVersion = FixedCostVersion(
        id = id,
        userId = userId,
        fixedCostId = fixedCostId,
        amount = amount.scale2(),
        effectiveFrom = parseLocalDateOrNull(effectiveFrom) ?: LocalDate.now(),
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        syncState = syncState
    )
}

fun FixedCostVersion.toDto(): FixedCostVersionDto = FixedCostVersionDto(
    id = id,
    userId = userId,
    fixedCostId = fixedCostId,
    amount = amount.scale2(),
    effectiveFrom = effectiveFrom.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

data class FixedCostPaymentDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("fixed_cost_id") val fixedCostId: String,
    @SerializedName("competence_date") val competenceDate: String,
    @SerializedName("paid_at") val paidAt: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("source_account_id") val sourceAccountId: String? = null,
    @SerializedName("source_vault_id") val sourceVaultId: String? = null,
    @SerializedName("vault_amount") val vaultAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("account_amount") val accountAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): FixedCostPayment = FixedCostPayment(
        id = id,
        userId = userId,
        fixedCostId = fixedCostId,
        competenceDate = parseLocalDateOrNull(competenceDate) ?: LocalDate.now(),
        paidAt = parseInstantOrNow(paidAt),
        amount = amount.scale2(),
        sourceAccountId = sourceAccountId,
        sourceVaultId = sourceVaultId,
        vaultAmount = vaultAmount.scale2(),
        accountAmount = accountAmount.scale2(),
        operationId = operationId,
        notes = notes,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun FixedCostPayment.toDto(): FixedCostPaymentDto = FixedCostPaymentDto(
    id = id,
    userId = userId,
    fixedCostId = fixedCostId,
    competenceDate = competenceDate.toString(),
    paidAt = paidAt.toString(),
    amount = amount.scale2(),
    sourceAccountId = sourceAccountId,
    sourceVaultId = sourceVaultId,
    vaultAmount = vaultAmount.scale2(),
    accountAmount = accountAmount.scale2(),
    operationId = operationId,
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class CardInvoicePaymentDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("card_id") val cardId: String,
    @SerializedName("reference_month") val referenceMonth: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("paid_at") val paidAt: String,
    @SerializedName("source_account_id") val sourceAccountId: String? = null,
    @SerializedName("source_vault_id") val sourceVaultId: String? = null,
    @SerializedName("vault_amount") val vaultAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("account_amount") val accountAmount: BigDecimal = BigDecimal.ZERO.scale2(),
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): CardInvoicePayment = CardInvoicePayment(
        id = id,
        userId = userId,
        cardId = cardId,
        referenceMonth = parseLocalDateOrNull(referenceMonth) ?: LocalDate.now().withDayOfMonth(1),
        amount = amount.scale2(),
        paidAt = parseInstantOrNow(paidAt),
        sourceAccountId = sourceAccountId,
        sourceVaultId = sourceVaultId,
        vaultAmount = vaultAmount.scale2(),
        accountAmount = accountAmount.scale2(),
        operationId = operationId,
        notes = notes,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun CardInvoicePayment.toDto(): CardInvoicePaymentDto = CardInvoicePaymentDto(
    id = id,
    userId = userId,
    cardId = cardId,
    referenceMonth = referenceMonth.toString(),
    amount = amount.scale2(),
    paidAt = paidAt.toString(),
    sourceAccountId = sourceAccountId,
    sourceVaultId = sourceVaultId,
    vaultAmount = vaultAmount.scale2(),
    accountAmount = accountAmount.scale2(),
    operationId = operationId,
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class CostCenterSettingsDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("maintenance_vault_id") val maintenanceVaultId: String? = null,
    @SerializedName("wear_provision_enabled") val wearProvisionEnabled: Boolean = true,
    @SerializedName("fixed_cost_provision_enabled") val fixedCostProvisionEnabled: Boolean = true,
    @SerializedName("reserve_mode") val reserveMode: String = ReserveMode.CALENDAR.key,
    @SerializedName("planned_weekdays") val plannedWeekdays: Int = 127,
    @SerializedName("days_off") val daysOff: List<String> = emptyList(),
    @SerializedName("seeded_catalog_version") val seededCatalogVersion: Int = 0,
    @SerializedName("dismissed_seed_keys") val dismissedSeedKeys: List<String> = emptyList(),
    @SerializedName("reminder_enabled") val reminderEnabled: Boolean = true,
    @SerializedName("reminder_offsets") val reminderOffsets: List<Int> = listOf(3, 0),
    @SerializedName("cost_center_started_on") val costCenterStartedOn: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): CostCenterSettings = CostCenterSettings(
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
        costCenterStartedOn = parseLocalDateOrNull(costCenterStartedOn),
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        syncState = syncState
    )
}

fun CostCenterSettings.toDto(): CostCenterSettingsDto = CostCenterSettingsDto(
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
    costCenterStartedOn = costCenterStartedOn?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

data class ReserveAllocationDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("billing_cycle_id") val billingCycleId: String,
    @SerializedName("vault_id") val vaultId: String,
    @SerializedName("suggested_amount") val suggestedAmount: BigDecimal,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("net_cap_applied") val netCapApplied: Boolean = false,
    @SerializedName("status") val status: String = AllocationStatus.ATIVA.key,
    @SerializedName("operation_id") val operationId: String,
    @SerializedName("undone_at") val undoneAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): ReserveAllocation = ReserveAllocation(
        id = id,
        userId = userId,
        billingCycleId = billingCycleId,
        vaultId = vaultId,
        suggestedAmount = suggestedAmount.scale2(),
        amount = amount.scale2(),
        netCapApplied = netCapApplied,
        status = AllocationStatus.fromKey(status),
        operationId = operationId,
        undoneAt = parseInstantOrNull(undoneAt),
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun ReserveAllocation.toDto(): ReserveAllocationDto = ReserveAllocationDto(
    id = id,
    userId = userId,
    billingCycleId = billingCycleId,
    vaultId = vaultId,
    suggestedAmount = suggestedAmount.scale2(),
    amount = amount.scale2(),
    netCapApplied = netCapApplied,
    status = status.key,
    operationId = operationId,
    undoneAt = undoneAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)

data class ReserveAllocationDayDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("allocation_id") val allocationId: String,
    @SerializedName("day") val day: String,
    @SerializedName("platform_id") val platformId: String? = null,
    @SerializedName("suggested_amount") val suggestedAmount: BigDecimal,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): ReserveAllocationDay = ReserveAllocationDay(
        id = id,
        userId = userId,
        allocationId = allocationId,
        day = parseLocalDateOrNull(day) ?: LocalDate.now(),
        platformId = platformId,
        suggestedAmount = suggestedAmount.scale6(),
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        syncState = syncState
    )
}

fun ReserveAllocationDay.toDto(): ReserveAllocationDayDto = ReserveAllocationDayDto(
    id = id,
    userId = userId,
    allocationId = allocationId,
    day = day.toString(),
    platformId = platformId,
    suggestedAmount = suggestedAmount.scale6(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

data class CardAccountLinkDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("card_id") val cardId: String,
    @SerializedName("account_id") val accountId: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(syncState: SyncState = SyncState.SINCRONIZADO): CardAccountLink = CardAccountLink(
        id = id,
        userId = userId,
        cardId = cardId,
        accountId = accountId,
        createdAt = parseInstantOrNow(createdAt),
        updatedAt = parseInstantOrNow(updatedAt),
        deletedAt = parseInstantOrNull(deletedAt),
        syncState = syncState
    )
}

fun CardAccountLink.toDto(): CardAccountLinkDto = CardAccountLinkDto(
    id = id,
    userId = userId,
    cardId = cardId,
    accountId = accountId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString()
)
