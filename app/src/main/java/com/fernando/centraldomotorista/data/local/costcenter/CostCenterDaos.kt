package com.fernando.centraldomotorista.data.local.costcenter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE user_id = :userId AND deleted_at IS NULL AND active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeActive(userId: String): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE user_id = :userId AND deleted_at IS NULL ORDER BY sort_order ASC, name ASC")
    fun observeAll(userId: String): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BankAccountEntity?

    @Upsert
    suspend fun upsert(entity: BankAccountEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BankAccountEntity>)

    @Query("UPDATE bank_accounts SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)

    @Query("UPDATE bank_accounts SET deleted_at = :deletedAt, updated_at = :updatedAt, sync_state = :syncState WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Instant, updatedAt: Instant, syncState: String)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults WHERE user_id = :userId AND deleted_at IS NULL AND active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeActive(userId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE user_id = :userId AND deleted_at IS NULL ORDER BY sort_order ASC, name ASC")
    fun observeAll(userId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE user_id = :userId AND account_id = :accountId AND deleted_at IS NULL ORDER BY sort_order ASC, name ASC")
    fun observeByAccount(userId: String, accountId: String): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM vaults WHERE account_id = :accountId AND deleted_at IS NULL)")
    suspend fun existsByAccountId(accountId: String): Boolean

    @Upsert
    suspend fun upsert(entity: VaultEntity)

    @Upsert
    suspend fun upsertAll(entities: List<VaultEntity>)

    @Query("UPDATE vaults SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)

    @Query("UPDATE vaults SET deleted_at = :deletedAt, updated_at = :updatedAt, sync_state = :syncState WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Instant, updatedAt: Instant, syncState: String)
}

@Dao
interface VaultLedgerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: VaultLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entries: List<VaultLedgerEntity>): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM vault_ledger WHERE operation_id = :operationId)")
    suspend fun existsByOperationId(operationId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM vault_ledger WHERE reverses_entry_id = :entryId)")
    suspend fun existsReversalForEntry(entryId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM vault_ledger WHERE vault_id = :vaultId OR contra_vault_id = :vaultId)")
    suspend fun existsForVault(vaultId: String): Boolean

    @Query("SELECT * FROM vault_ledger WHERE ref_type = :refType AND ref_id = :refId ORDER BY occurred_at ASC")
    suspend fun getEntriesByReference(refType: String, refId: String): List<VaultLedgerEntity>

    @Query("SELECT * FROM vault_ledger WHERE vault_id = :vaultId ORDER BY occurred_at DESC, created_at DESC")
    suspend fun getEntriesForVault(vaultId: String): List<VaultLedgerEntity>

    @Query("SELECT * FROM vault_ledger WHERE vault_id = :vaultId ORDER BY occurred_at DESC, created_at DESC")
    fun observeEntriesForVault(vaultId: String): Flow<List<VaultLedgerEntity>>

    @Query("SELECT * FROM vault_ledger WHERE user_id = :userId ORDER BY occurred_at DESC, created_at DESC")
    fun observeAll(userId: String): Flow<List<VaultLedgerEntity>>

    @Query("SELECT * FROM vault_ledger WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultLedgerEntity?

    @Query("UPDATE vault_ledger SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface ExpenseSettlementDao {
    @Query("SELECT * FROM expense_settlements WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(userId: String): Flow<List<ExpenseSettlementEntity>>

    @Query("SELECT * FROM expense_settlements WHERE expense_id = :expenseId AND deleted_at IS NULL LIMIT 1")
    suspend fun getByExpenseId(expenseId: String): ExpenseSettlementEntity?

    @Query("SELECT * FROM expense_settlements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseSettlementEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM expense_settlements WHERE account_id = :accountId AND deleted_at IS NULL)")
    suspend fun existsByAccountId(accountId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM expense_settlements WHERE vault_id = :vaultId AND deleted_at IS NULL)")
    suspend fun existsByVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: ExpenseSettlementEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExpenseSettlementEntity>)

    @Query("UPDATE expense_settlements SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface FixedCostDao {
    @Query("SELECT * FROM fixed_costs WHERE user_id = :userId AND deleted_at IS NULL AND active = 1 ORDER BY name ASC")
    fun observeActive(userId: String): Flow<List<FixedCostEntity>>

    @Query("SELECT * FROM fixed_costs WHERE user_id = :userId AND deleted_at IS NULL ORDER BY name ASC")
    fun observeAll(userId: String): Flow<List<FixedCostEntity>>

    @Query("SELECT * FROM fixed_costs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FixedCostEntity?

    @Query("SELECT * FROM fixed_costs WHERE user_id = :userId AND catalog_key = :catalogKey AND deleted_at IS NULL LIMIT 1")
    suspend fun getByCatalogKey(userId: String, catalogKey: String): FixedCostEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM fixed_costs WHERE vault_id = :vaultId AND deleted_at IS NULL)")
    suspend fun existsByVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: FixedCostEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FixedCostEntity>)

    @Query("UPDATE fixed_costs SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface FixedCostVersionDao {
    @Query("SELECT * FROM fixed_cost_versions WHERE fixed_cost_id = :fixedCostId ORDER BY effective_from DESC")
    fun observeByFixedCost(fixedCostId: String): Flow<List<FixedCostVersionEntity>>

    @Query("SELECT * FROM fixed_cost_versions WHERE user_id = :userId ORDER BY effective_from DESC")
    fun observeAll(userId: String): Flow<List<FixedCostVersionEntity>>

    @Query("SELECT * FROM fixed_cost_versions WHERE fixed_cost_id = :fixedCostId ORDER BY effective_from DESC")
    suspend fun getByFixedCost(fixedCostId: String): List<FixedCostVersionEntity>

    @Query("SELECT * FROM fixed_cost_versions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FixedCostVersionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM fixed_cost_versions WHERE fixed_cost_id = :fixedCostId)")
    suspend fun existsByFixedCostId(fixedCostId: String): Boolean

    @Upsert
    suspend fun upsert(entity: FixedCostVersionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FixedCostVersionEntity>)

    @Query("UPDATE fixed_cost_versions SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface FixedCostPaymentDao {
    @Query("SELECT * FROM fixed_cost_payments WHERE user_id = :userId AND deleted_at IS NULL ORDER BY paid_at DESC")
    fun observeAll(userId: String): Flow<List<FixedCostPaymentEntity>>

    @Query("SELECT * FROM fixed_cost_payments WHERE fixed_cost_id = :fixedCostId AND deleted_at IS NULL ORDER BY competence_date DESC")
    fun observeByFixedCost(fixedCostId: String): Flow<List<FixedCostPaymentEntity>>

    @Query("SELECT * FROM fixed_cost_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FixedCostPaymentEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM fixed_cost_payments WHERE fixed_cost_id = :fixedCostId AND deleted_at IS NULL)")
    suspend fun existsByFixedCostId(fixedCostId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM fixed_cost_payments WHERE source_account_id = :accountId AND deleted_at IS NULL)")
    suspend fun existsByAccountId(accountId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM fixed_cost_payments WHERE source_vault_id = :vaultId AND deleted_at IS NULL)")
    suspend fun existsByVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: FixedCostPaymentEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FixedCostPaymentEntity>)

    @Query("UPDATE fixed_cost_payments SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface CardInvoicePaymentDao {
    @Query("SELECT * FROM card_invoice_payments WHERE user_id = :userId AND deleted_at IS NULL ORDER BY paid_at DESC")
    fun observeAll(userId: String): Flow<List<CardInvoicePaymentEntity>>

    @Query("SELECT * FROM card_invoice_payments WHERE card_id = :cardId AND deleted_at IS NULL ORDER BY reference_month DESC")
    fun observeByCard(cardId: String): Flow<List<CardInvoicePaymentEntity>>

    @Query("SELECT * FROM card_invoice_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CardInvoicePaymentEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM card_invoice_payments WHERE source_account_id = :accountId AND deleted_at IS NULL)")
    suspend fun existsByAccountId(accountId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM card_invoice_payments WHERE source_vault_id = :vaultId AND deleted_at IS NULL)")
    suspend fun existsByVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: CardInvoicePaymentEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CardInvoicePaymentEntity>)

    @Query("UPDATE card_invoice_payments SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface CostCenterSettingsDao {
    @Query("SELECT * FROM cost_center_settings WHERE user_id = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<CostCenterSettingsEntity?>

    @Query("SELECT * FROM cost_center_settings WHERE user_id = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): CostCenterSettingsEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM cost_center_settings WHERE maintenance_vault_id = :vaultId)")
    suspend fun existsByMaintenanceVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: CostCenterSettingsEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CostCenterSettingsEntity>)

    @Query("UPDATE cost_center_settings SET sync_state = :syncState WHERE user_id = :userId")
    suspend fun updateSyncState(userId: String, syncState: String)
}

@Dao
interface ReserveAllocationDao {
    @Query("SELECT * FROM reserve_allocations WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(userId: String): Flow<List<ReserveAllocationEntity>>

    @Query("SELECT * FROM reserve_allocations WHERE billing_cycle_id = :billingCycleId AND deleted_at IS NULL")
    fun observeByCycle(billingCycleId: String): Flow<List<ReserveAllocationEntity>>

    @Query("SELECT * FROM reserve_allocations WHERE billing_cycle_id = :billingCycleId AND deleted_at IS NULL")
    suspend fun getByCycle(billingCycleId: String): List<ReserveAllocationEntity>

    @Query("SELECT * FROM reserve_allocations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReserveAllocationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM reserve_allocations WHERE vault_id = :vaultId AND deleted_at IS NULL)")
    suspend fun existsByVaultId(vaultId: String): Boolean

    @Upsert
    suspend fun upsert(entity: ReserveAllocationEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ReserveAllocationEntity>)

    @Query("UPDATE reserve_allocations SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface ReserveAllocationDayDao {
    @Query("SELECT * FROM reserve_allocation_days WHERE allocation_id = :allocationId ORDER BY day ASC")
    fun observeByAllocation(allocationId: String): Flow<List<ReserveAllocationDayEntity>>

    @Query("SELECT * FROM reserve_allocation_days WHERE allocation_id = :allocationId ORDER BY day ASC")
    suspend fun getByAllocation(allocationId: String): List<ReserveAllocationDayEntity>

    @Query("SELECT * FROM reserve_allocation_days WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReserveAllocationDayEntity?

    @Upsert
    suspend fun upsert(entity: ReserveAllocationDayEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ReserveAllocationDayEntity>)

    @Query("UPDATE reserve_allocation_days SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}

@Dao
interface CardAccountLinkDao {
    @Query("SELECT * FROM card_account_links WHERE user_id = :userId AND deleted_at IS NULL")
    fun observeAll(userId: String): Flow<List<CardAccountLinkEntity>>

    @Query("SELECT * FROM card_account_links WHERE card_id = :cardId AND deleted_at IS NULL LIMIT 1")
    suspend fun getByCardId(cardId: String): CardAccountLinkEntity?

    @Query("SELECT * FROM card_account_links WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CardAccountLinkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM card_account_links WHERE account_id = :accountId AND deleted_at IS NULL)")
    suspend fun existsByAccountId(accountId: String): Boolean

    @Upsert
    suspend fun upsert(entity: CardAccountLinkEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CardAccountLinkEntity>)

    @Query("UPDATE card_account_links SET sync_state = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)
}
