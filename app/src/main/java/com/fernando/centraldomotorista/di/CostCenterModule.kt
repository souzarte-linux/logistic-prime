package com.fernando.centraldomotorista.di

import com.fernando.centraldomotorista.sync.SyncHandlerRegistry
import com.fernando.centraldomotorista.sync.handlers.BankAccountSyncHandler
import com.fernando.centraldomotorista.sync.handlers.CardAccountLinkSyncHandler
import com.fernando.centraldomotorista.sync.handlers.CardInvoicePaymentSyncHandler
import com.fernando.centraldomotorista.sync.handlers.CostCenterSettingsSyncHandler
import com.fernando.centraldomotorista.sync.handlers.ExpenseSettlementSyncHandler
import com.fernando.centraldomotorista.sync.handlers.FixedCostPaymentSyncHandler
import com.fernando.centraldomotorista.sync.handlers.FixedCostSyncHandler
import com.fernando.centraldomotorista.sync.handlers.FixedCostVersionSyncHandler
import com.fernando.centraldomotorista.sync.handlers.ReserveAllocationDaySyncHandler
import com.fernando.centraldomotorista.sync.handlers.ReserveAllocationSyncHandler
import com.fernando.centraldomotorista.sync.handlers.VaultLedgerSyncHandler
import com.fernando.centraldomotorista.sync.handlers.VaultSyncHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CostCenterModule {

    @Provides
    @Singleton
    fun provideSyncHandlerRegistry(
        bankAccountSyncHandler: BankAccountSyncHandler,
        vaultSyncHandler: VaultSyncHandler,
        costCenterSettingsSyncHandler: CostCenterSettingsSyncHandler,
        fixedCostSyncHandler: FixedCostSyncHandler,
        fixedCostVersionSyncHandler: FixedCostVersionSyncHandler,
        fixedCostPaymentSyncHandler: FixedCostPaymentSyncHandler,
        cardInvoicePaymentSyncHandler: CardInvoicePaymentSyncHandler,
        expenseSettlementSyncHandler: ExpenseSettlementSyncHandler,
        reserveAllocationSyncHandler: ReserveAllocationSyncHandler,
        reserveAllocationDaySyncHandler: ReserveAllocationDaySyncHandler,
        cardAccountLinkSyncHandler: CardAccountLinkSyncHandler,
        vaultLedgerSyncHandler: VaultLedgerSyncHandler
    ): SyncHandlerRegistry = SyncHandlerRegistry().apply {
        // Ordem topológica estrita respeitando dependências de FK
        register(bankAccountSyncHandler)
        register(vaultSyncHandler)
        register(costCenterSettingsSyncHandler)
        register(fixedCostSyncHandler)
        register(fixedCostVersionSyncHandler)
        register(fixedCostPaymentSyncHandler)
        register(cardInvoicePaymentSyncHandler)
        register(expenseSettlementSyncHandler)
        register(reserveAllocationSyncHandler)
        register(reserveAllocationDaySyncHandler)
        register(cardAccountLinkSyncHandler)
        register(vaultLedgerSyncHandler)
    }
}
