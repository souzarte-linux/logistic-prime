package com.fernando.centraldomotorista.di

import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.BankAccountApi
import com.fernando.centraldomotorista.data.remote.api.CardAccountLinkApi
import com.fernando.centraldomotorista.data.remote.api.CardInvoicePaymentApi
import com.fernando.centraldomotorista.data.remote.api.CostCenterSettingsApi
import com.fernando.centraldomotorista.data.remote.api.ExpenseSettlementApi
import com.fernando.centraldomotorista.data.remote.api.FixedCostApi
import com.fernando.centraldomotorista.data.remote.api.FixedCostPaymentApi
import com.fernando.centraldomotorista.data.remote.api.FixedCostVersionApi
import com.fernando.centraldomotorista.data.remote.api.ReserveAllocationApi
import com.fernando.centraldomotorista.data.remote.api.ReserveAllocationDayApi
import com.fernando.centraldomotorista.data.remote.api.VaultApi
import com.fernando.centraldomotorista.data.remote.api.VaultLedgerApi
import com.fernando.centraldomotorista.data.remote.supabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = supabase

    @Provides
    @Singleton
    fun provideRetrofitClient(): RetrofitClient = RetrofitClient

    @Provides
    @Singleton
    fun provideBankAccountApi(): BankAccountApi = RetrofitClient.bankAccountApi

    @Provides
    @Singleton
    fun provideVaultApi(): VaultApi = RetrofitClient.vaultApi

    @Provides
    @Singleton
    fun provideVaultLedgerApi(): VaultLedgerApi = RetrofitClient.vaultLedgerApi

    @Provides
    @Singleton
    fun provideExpenseSettlementApi(): ExpenseSettlementApi = RetrofitClient.expenseSettlementApi

    @Provides
    @Singleton
    fun provideFixedCostApi(): FixedCostApi = RetrofitClient.fixedCostApi

    @Provides
    @Singleton
    fun provideFixedCostVersionApi(): FixedCostVersionApi = RetrofitClient.fixedCostVersionApi

    @Provides
    @Singleton
    fun provideFixedCostPaymentApi(): FixedCostPaymentApi = RetrofitClient.fixedCostPaymentApi

    @Provides
    @Singleton
    fun provideCardInvoicePaymentApi(): CardInvoicePaymentApi = RetrofitClient.cardInvoicePaymentApi

    @Provides
    @Singleton
    fun provideCostCenterSettingsApi(): CostCenterSettingsApi = RetrofitClient.costCenterSettingsApi

    @Provides
    @Singleton
    fun provideReserveAllocationApi(): ReserveAllocationApi = RetrofitClient.reserveAllocationApi

    @Provides
    @Singleton
    fun provideReserveAllocationDayApi(): ReserveAllocationDayApi = RetrofitClient.reserveAllocationDayApi

    @Provides
    @Singleton
    fun provideCardAccountLinkApi(): CardAccountLinkApi = RetrofitClient.cardAccountLinkApi
}
