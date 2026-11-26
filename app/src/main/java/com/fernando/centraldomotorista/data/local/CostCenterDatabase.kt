package com.fernando.centraldomotorista.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fernando.centraldomotorista.data.local.costcenter.BankAccountDao
import com.fernando.centraldomotorista.data.local.costcenter.BankAccountEntity
import com.fernando.centraldomotorista.data.local.costcenter.CardAccountLinkDao
import com.fernando.centraldomotorista.data.local.costcenter.CardAccountLinkEntity
import com.fernando.centraldomotorista.data.local.costcenter.CardInvoicePaymentDao
import com.fernando.centraldomotorista.data.local.costcenter.CardInvoicePaymentEntity
import com.fernando.centraldomotorista.data.local.costcenter.CostCenterSettingsDao
import com.fernando.centraldomotorista.data.local.costcenter.CostCenterSettingsEntity
import com.fernando.centraldomotorista.data.local.costcenter.CostCenterTypeConverters
import com.fernando.centraldomotorista.data.local.costcenter.ExpenseSettlementDao
import com.fernando.centraldomotorista.data.local.costcenter.ExpenseSettlementEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostDao
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostPaymentDao
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostPaymentEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostVersionDao
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostVersionEntity
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationDao
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationDayDao
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationDayEntity
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationEntity
import com.fernando.centraldomotorista.data.local.costcenter.VaultDao
import com.fernando.centraldomotorista.data.local.costcenter.VaultEntity
import com.fernando.centraldomotorista.data.local.costcenter.VaultLedgerDao
import com.fernando.centraldomotorista.data.local.costcenter.VaultLedgerEntity
import com.fernando.centraldomotorista.data.local.dao.OutboxDao
import com.fernando.centraldomotorista.data.local.dao.SyncCursorDao
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity

@Database(
    entities = [
        OutboxEntity::class,
        SyncCursorEntity::class,
        BankAccountEntity::class,
        VaultEntity::class,
        VaultLedgerEntity::class,
        ExpenseSettlementEntity::class,
        FixedCostEntity::class,
        FixedCostVersionEntity::class,
        FixedCostPaymentEntity::class,
        CardInvoicePaymentEntity::class,
        CostCenterSettingsEntity::class,
        ReserveAllocationEntity::class,
        ReserveAllocationDayEntity::class,
        CardAccountLinkEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(CostCenterTypeConverters::class)
abstract class CostCenterDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun vaultDao(): VaultDao
    abstract fun vaultLedgerDao(): VaultLedgerDao
    abstract fun expenseSettlementDao(): ExpenseSettlementDao
    abstract fun fixedCostDao(): FixedCostDao
    abstract fun fixedCostVersionDao(): FixedCostVersionDao
    abstract fun fixedCostPaymentDao(): FixedCostPaymentDao
    abstract fun cardInvoicePaymentDao(): CardInvoicePaymentDao
    abstract fun costCenterSettingsDao(): CostCenterSettingsDao
    abstract fun reserveAllocationDao(): ReserveAllocationDao
    abstract fun reserveAllocationDayDao(): ReserveAllocationDayDao
    abstract fun cardAccountLinkDao(): CardAccountLinkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN last_attempt_at INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bank_accounts` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `institution` TEXT,
                        `agency` TEXT,
                        `account_last_digits` TEXT,
                        `account_type` TEXT,
                        `informed_balance` TEXT,
                        `informed_balance_at` TEXT,
                        `active` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bank_accounts_user_id` ON `bank_accounts` (`user_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vaults` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `account_id` TEXT,
                        `name` TEXT NOT NULL,
                        `purpose` TEXT NOT NULL,
                        `goal_amount` TEXT,
                        `initial_balance` TEXT NOT NULL,
                        `tracking_start_date` TEXT NOT NULL,
                        `active` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vaults_user_id_account_id` ON `vaults` (`user_id`, `account_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vault_ledger` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `vault_id` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `leg` INTEGER NOT NULL,
                        `entry_type` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `occurred_at` TEXT NOT NULL,
                        `ref_type` TEXT,
                        `ref_id` TEXT,
                        `contra_vault_id` TEXT,
                        `reverses_entry_id` TEXT,
                        `note` TEXT,
                        `created_at` TEXT NOT NULL,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_vault_ledger_operation_id_leg` ON `vault_ledger` (`operation_id`, `leg`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_ledger_user_id_occurred_at` ON `vault_ledger` (`user_id`, `occurred_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_ledger_vault_id_occurred_at` ON `vault_ledger` (`vault_id`, `occurred_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vault_ledger_ref_type_ref_id` ON `vault_ledger` (`ref_type`, `ref_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_settlements` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `expense_id` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `account_id` TEXT,
                        `vault_id` TEXT,
                        `amount` TEXT NOT NULL,
                        `vault_amount` TEXT NOT NULL,
                        `account_amount` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_settlements_expense_id` ON `expense_settlements` (`expense_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_settlements_operation_id` ON `expense_settlements` (`operation_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_settlements_vault_id` ON `expense_settlements` (`vault_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_costs` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `catalog_key` TEXT,
                        `periodicity` TEXT NOT NULL,
                        `due_day` INTEGER,
                        `due_month` INTEGER,
                        `start_date` TEXT,
                        `end_date` TEXT,
                        `vault_id` TEXT,
                        `is_vehicle_related` INTEGER NOT NULL,
                        `analytical` INTEGER NOT NULL,
                        `seeded` INTEGER NOT NULL,
                        `notes` TEXT,
                        `active` INTEGER NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_costs_user_id_active` ON `fixed_costs` (`user_id`, `active`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_costs_vault_id` ON `fixed_costs` (`vault_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_cost_versions` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `fixed_cost_id` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `effective_from` TEXT NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fixed_cost_versions_fixed_cost_id_effective_from` ON `fixed_cost_versions` (`fixed_cost_id`, `effective_from`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_cost_payments` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `fixed_cost_id` TEXT NOT NULL,
                        `competence_date` TEXT NOT NULL,
                        `paid_at` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `source_account_id` TEXT,
                        `source_vault_id` TEXT,
                        `vault_amount` TEXT NOT NULL,
                        `account_amount` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `notes` TEXT,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fixed_cost_payments_operation_id` ON `fixed_cost_payments` (`operation_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_cost_payments_source_vault_id` ON `fixed_cost_payments` (`source_vault_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_cost_payments_fixed_cost_id_competence_date` ON `fixed_cost_payments` (`fixed_cost_id`, `competence_date`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_invoice_payments` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `card_id` TEXT NOT NULL,
                        `reference_month` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `paid_at` TEXT NOT NULL,
                        `source_account_id` TEXT,
                        `source_vault_id` TEXT,
                        `vault_amount` TEXT NOT NULL,
                        `account_amount` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `notes` TEXT,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_card_invoice_payments_operation_id` ON `card_invoice_payments` (`operation_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_invoice_payments_card_id_reference_month` ON `card_invoice_payments` (`card_id`, `reference_month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_invoice_payments_source_vault_id` ON `card_invoice_payments` (`source_vault_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cost_center_settings` (
                        `user_id` TEXT NOT NULL,
                        `maintenance_vault_id` TEXT,
                        `wear_provision_enabled` INTEGER NOT NULL,
                        `fixed_cost_provision_enabled` INTEGER NOT NULL,
                        `reserve_mode` TEXT NOT NULL,
                        `planned_weekdays` INTEGER NOT NULL,
                        `days_off` TEXT NOT NULL,
                        `seeded_catalog_version` INTEGER NOT NULL,
                        `dismissed_seed_keys` TEXT NOT NULL,
                        `reminder_enabled` INTEGER NOT NULL,
                        `reminder_offsets` TEXT NOT NULL,
                        `cost_center_started_on` TEXT,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`user_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_center_settings_maintenance_vault_id` ON `cost_center_settings` (`maintenance_vault_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reserve_allocations` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `billing_cycle_id` TEXT NOT NULL,
                        `vault_id` TEXT NOT NULL,
                        `suggested_amount` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `net_cap_applied` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `undone_at` TEXT,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reserve_allocations_operation_id` ON `reserve_allocations` (`operation_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reserve_allocations_billing_cycle_id` ON `reserve_allocations` (`billing_cycle_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reserve_allocations_vault_id` ON `reserve_allocations` (`vault_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reserve_allocation_days` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `allocation_id` TEXT NOT NULL,
                        `day` TEXT NOT NULL,
                        `platform_id` TEXT,
                        `suggested_amount` TEXT NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reserve_allocation_days_allocation_id_day` ON `reserve_allocation_days` (`allocation_id`, `day`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_account_links` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `card_id` TEXT NOT NULL,
                        `account_id` TEXT NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        `deleted_at` TEXT,
                        `sync_state` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_account_links_account_id` ON `card_account_links` (`account_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_account_links_card_id` ON `card_account_links` (`card_id`)")
            }
        }
    }
}
