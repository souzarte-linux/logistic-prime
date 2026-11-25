package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.BankAccountDto
import com.fernando.centraldomotorista.data.remote.dto.CardAccountLinkDto
import com.fernando.centraldomotorista.data.remote.dto.CardInvoicePaymentDto
import com.fernando.centraldomotorista.data.remote.dto.CostCenterSettingsDto
import com.fernando.centraldomotorista.data.remote.dto.ExpenseSettlementDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostPaymentDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostVersionDto
import com.fernando.centraldomotorista.data.remote.dto.ReserveAllocationDayDto
import com.fernando.centraldomotorista.data.remote.dto.ReserveAllocationDto
import com.fernando.centraldomotorista.data.remote.dto.VaultDto
import com.fernando.centraldomotorista.data.remote.dto.VaultLedgerDto
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface BankAccountApi {
    @GET("bank_accounts")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<BankAccountDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("bank_accounts")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<BankAccountDto>>
}

interface VaultApi {
    @GET("vaults")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<VaultDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("vaults")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<VaultDto>>
}

interface VaultLedgerApi {
    @GET("vault_ledger")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("created_at") createdAtFilter: String? = null,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<VaultLedgerDto>>

    @Headers("Prefer: resolution=ignore-duplicates,return=representation")
    @POST("vault_ledger")
    suspend fun insert(
        @Query("on_conflict") onConflict: String = "operation_id,leg",
        @Body body: JsonObject
    ): Response<List<VaultLedgerDto>>
}

interface ExpenseSettlementApi {
    @GET("expense_settlements")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<ExpenseSettlementDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("expense_settlements")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<ExpenseSettlementDto>>
}

interface FixedCostApi {
    @GET("fixed_costs")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<FixedCostDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("fixed_costs")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<FixedCostDto>>
}

interface FixedCostVersionApi {
    @GET("fixed_cost_versions")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<FixedCostVersionDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("fixed_cost_versions")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<FixedCostVersionDto>>
}

interface FixedCostPaymentApi {
    @GET("fixed_cost_payments")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<FixedCostPaymentDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("fixed_cost_payments")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<FixedCostPaymentDto>>
}

interface CardInvoicePaymentApi {
    @GET("card_invoice_payments")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<CardInvoicePaymentDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("card_invoice_payments")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<CardInvoicePaymentDto>>
}

interface CostCenterSettingsApi {
    @GET("cost_center_settings")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<CostCenterSettingsDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("cost_center_settings")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "user_id",
        @Body body: JsonObject
    ): Response<List<CostCenterSettingsDto>>
}

interface ReserveAllocationApi {
    @GET("reserve_allocations")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<ReserveAllocationDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("reserve_allocations")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<ReserveAllocationDto>>
}

interface ReserveAllocationDayApi {
    @GET("reserve_allocation_days")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<ReserveAllocationDayDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("reserve_allocation_days")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<ReserveAllocationDayDto>>
}

interface CardAccountLinkApi {
    @GET("card_account_links")
    suspend fun getAll(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<CardAccountLinkDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("card_account_links")
    suspend fun upsert(
        @Query("on_conflict") onConflict: String = "id",
        @Body body: JsonObject
    ): Response<List<CardAccountLinkDto>>
}
