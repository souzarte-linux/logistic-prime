package com.fernando.centraldomotorista.sync.handlers

import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.BankAccountEntity
import com.fernando.centraldomotorista.data.local.costcenter.CardAccountLinkEntity
import com.fernando.centraldomotorista.data.local.costcenter.CardInvoicePaymentEntity
import com.fernando.centraldomotorista.data.local.costcenter.CostCenterSettingsEntity
import com.fernando.centraldomotorista.data.local.costcenter.ExpenseSettlementEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostPaymentEntity
import com.fernando.centraldomotorista.data.local.costcenter.FixedCostVersionEntity
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationDayEntity
import com.fernando.centraldomotorista.data.local.costcenter.ReserveAllocationEntity
import com.fernando.centraldomotorista.data.local.costcenter.VaultEntity
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.SyncState
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
import com.fernando.centraldomotorista.sync.OutboxItem
import com.fernando.centraldomotorista.sync.OutboxPayloadSerializer
import com.fernando.centraldomotorista.sync.PostgrestErrorClassifier
import com.fernando.centraldomotorista.sync.PullResult
import com.fernando.centraldomotorista.sync.PushResult
import com.fernando.centraldomotorista.sync.SyncEngine
import com.fernando.centraldomotorista.sync.TableSyncHandler
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.Lazy
import retrofit2.Response
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private fun extractPostgrestSqlState(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null
    return try {
        val obj = JsonParser.parseString(errorBody).asJsonObject
        obj.get("code")?.takeIf { !it.isJsonNull }?.asString
    } catch (_: Exception) {
        null
    }
}

abstract class BaseMutableSyncHandler<Dto, Entity>(
    protected val dbProvider: LocalDatabaseProvider,
    protected val userSession: UserSession,
    private val syncEngine: Lazy<SyncEngine>
) : TableSyncHandler {

    protected abstract suspend fun remoteUpsert(body: JsonObject): Response<List<Dto>>
    protected abstract suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?): Response<List<Dto>>
    protected abstract fun dtoToEntity(dto: Dto): Entity
    protected abstract fun entityId(entity: Entity): String
    protected abstract fun entitySyncState(entity: Entity): String
    protected abstract fun entityUpdatedAt(entity: Entity): Instant
    protected abstract suspend fun getLocalById(id: String): Entity?
    protected abstract suspend fun upsertLocal(entity: Entity)
    protected abstract suspend fun markLocalSynced(rowId: String)

    override suspend fun push(item: OutboxItem): PushResult {
        val body = OutboxPayloadSerializer.deserialize(item.payloadJson)
        val response = remoteUpsert(body)

        if (response.isSuccessful) {
            markLocalSynced(item.rowId)
            return PushResult.Success
        }

        val errorBody = response.errorBody()?.string()
        val sqlState = extractPostgrestSqlState(errorBody)
        if (response.code() == 409 || PostgrestErrorClassifier.isIdempotentDuplicate(sqlState)) {
            markLocalSynced(item.rowId)
            return PushResult.AlreadyApplied
        }

        return PushResult.Failure(
            IllegalStateException("HTTP ${response.code()} em $table: ${errorBody ?: response.message()}")
        )
    }

    override suspend fun pull(since: Instant?): PullResult {
        val userId = userSession.currentUserId()
            ?: return PullResult.Failure(IllegalStateException("Sem sessão ativa para sincronizar $table"))

        val cursorFilter = since?.let { "gt.$it" }
        val response = remoteGetAll(userIdFilter = "eq.$userId", updatedAtFilter = cursorFilter)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            return PullResult.Failure(
                IllegalStateException("HTTP ${response.code()} ao buscar $table: ${errorBody ?: response.message()}")
            )
        }

        val dtos = response.body().orEmpty()
        var maxUpdatedAt = since

        for (dto in dtos) {
            val remoteEntity = dtoToEntity(dto)
            val id = entityId(remoteEntity)
            val remoteUpdatedAt = entityUpdatedAt(remoteEntity)

            val local = getLocalById(id)
            val shouldWrite = if (local != null && entitySyncState(local) == SyncState.PENDENTE.name) {
                syncEngine.get().resolveMutableConflict(
                    localUpdatedAt = entityUpdatedAt(local),
                    remoteUpdatedAt = remoteUpdatedAt
                ) == SyncEngine.ConflictResolution.USE_REMOTE
            } else {
                true
            }

            if (shouldWrite) {
                upsertLocal(remoteEntity)
            }

            if (maxUpdatedAt == null || remoteUpdatedAt.isAfter(maxUpdatedAt)) {
                maxUpdatedAt = remoteUpdatedAt
            }
        }

        return PullResult.Success(newCursor = maxUpdatedAt, recordsCount = dtos.size)
    }
}

@Singleton
class BankAccountSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: BankAccountApi
) : BaseMutableSyncHandler<BankAccountDto, BankAccountEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "bank_accounts"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: BankAccountDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: BankAccountEntity) = entity.id
    override fun entitySyncState(entity: BankAccountEntity) = entity.syncState
    override fun entityUpdatedAt(entity: BankAccountEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().bankAccountDao().getById(id)
    override suspend fun upsertLocal(entity: BankAccountEntity) = dbProvider.current().bankAccountDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().bankAccountDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class VaultSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: VaultApi
) : BaseMutableSyncHandler<VaultDto, VaultEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "vaults"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: VaultDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: VaultEntity) = entity.id
    override fun entitySyncState(entity: VaultEntity) = entity.syncState
    override fun entityUpdatedAt(entity: VaultEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().vaultDao().getById(id)
    override suspend fun upsertLocal(entity: VaultEntity) = dbProvider.current().vaultDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().vaultDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class CostCenterSettingsSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: CostCenterSettingsApi
) : BaseMutableSyncHandler<CostCenterSettingsDto, CostCenterSettingsEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "cost_center_settings"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: CostCenterSettingsDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: CostCenterSettingsEntity) = entity.userId
    override fun entitySyncState(entity: CostCenterSettingsEntity) = entity.syncState
    override fun entityUpdatedAt(entity: CostCenterSettingsEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().costCenterSettingsDao().getByUserId(id)
    override suspend fun upsertLocal(entity: CostCenterSettingsEntity) =
        dbProvider.current().costCenterSettingsDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().costCenterSettingsDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class FixedCostSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: FixedCostApi
) : BaseMutableSyncHandler<FixedCostDto, FixedCostEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "fixed_costs"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: FixedCostDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: FixedCostEntity) = entity.id
    override fun entitySyncState(entity: FixedCostEntity) = entity.syncState
    override fun entityUpdatedAt(entity: FixedCostEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().fixedCostDao().getById(id)
    override suspend fun upsertLocal(entity: FixedCostEntity) = dbProvider.current().fixedCostDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().fixedCostDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class FixedCostVersionSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: FixedCostVersionApi
) : BaseMutableSyncHandler<FixedCostVersionDto, FixedCostVersionEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "fixed_cost_versions"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: FixedCostVersionDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: FixedCostVersionEntity) = entity.id
    override fun entitySyncState(entity: FixedCostVersionEntity) = entity.syncState
    override fun entityUpdatedAt(entity: FixedCostVersionEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().fixedCostVersionDao().getById(id)
    override suspend fun upsertLocal(entity: FixedCostVersionEntity) =
        dbProvider.current().fixedCostVersionDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().fixedCostVersionDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class FixedCostPaymentSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: FixedCostPaymentApi
) : BaseMutableSyncHandler<FixedCostPaymentDto, FixedCostPaymentEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "fixed_cost_payments"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: FixedCostPaymentDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: FixedCostPaymentEntity) = entity.id
    override fun entitySyncState(entity: FixedCostPaymentEntity) = entity.syncState
    override fun entityUpdatedAt(entity: FixedCostPaymentEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().fixedCostPaymentDao().getById(id)
    override suspend fun upsertLocal(entity: FixedCostPaymentEntity) =
        dbProvider.current().fixedCostPaymentDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().fixedCostPaymentDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class CardInvoicePaymentSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: CardInvoicePaymentApi
) : BaseMutableSyncHandler<CardInvoicePaymentDto, CardInvoicePaymentEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "card_invoice_payments"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: CardInvoicePaymentDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: CardInvoicePaymentEntity) = entity.id
    override fun entitySyncState(entity: CardInvoicePaymentEntity) = entity.syncState
    override fun entityUpdatedAt(entity: CardInvoicePaymentEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().cardInvoicePaymentDao().getById(id)
    override suspend fun upsertLocal(entity: CardInvoicePaymentEntity) =
        dbProvider.current().cardInvoicePaymentDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().cardInvoicePaymentDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class ExpenseSettlementSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: ExpenseSettlementApi
) : BaseMutableSyncHandler<ExpenseSettlementDto, ExpenseSettlementEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "expense_settlements"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: ExpenseSettlementDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: ExpenseSettlementEntity) = entity.id
    override fun entitySyncState(entity: ExpenseSettlementEntity) = entity.syncState
    override fun entityUpdatedAt(entity: ExpenseSettlementEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().expenseSettlementDao().getById(id)
    override suspend fun upsertLocal(entity: ExpenseSettlementEntity) =
        dbProvider.current().expenseSettlementDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().expenseSettlementDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class ReserveAllocationSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: ReserveAllocationApi
) : BaseMutableSyncHandler<ReserveAllocationDto, ReserveAllocationEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "reserve_allocations"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: ReserveAllocationDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: ReserveAllocationEntity) = entity.id
    override fun entitySyncState(entity: ReserveAllocationEntity) = entity.syncState
    override fun entityUpdatedAt(entity: ReserveAllocationEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().reserveAllocationDao().getById(id)
    override suspend fun upsertLocal(entity: ReserveAllocationEntity) =
        dbProvider.current().reserveAllocationDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().reserveAllocationDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class ReserveAllocationDaySyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: ReserveAllocationDayApi
) : BaseMutableSyncHandler<ReserveAllocationDayDto, ReserveAllocationDayEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "reserve_allocation_days"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: ReserveAllocationDayDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: ReserveAllocationDayEntity) = entity.id
    override fun entitySyncState(entity: ReserveAllocationDayEntity) = entity.syncState
    override fun entityUpdatedAt(entity: ReserveAllocationDayEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().reserveAllocationDayDao().getById(id)
    override suspend fun upsertLocal(entity: ReserveAllocationDayEntity) =
        dbProvider.current().reserveAllocationDayDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().reserveAllocationDayDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class CardAccountLinkSyncHandler @Inject constructor(
    dbProvider: LocalDatabaseProvider,
    userSession: UserSession,
    syncEngine: Lazy<SyncEngine>,
    private val api: CardAccountLinkApi
) : BaseMutableSyncHandler<CardAccountLinkDto, CardAccountLinkEntity>(dbProvider, userSession, syncEngine) {
    override val table: String = "card_account_links"
    override suspend fun remoteUpsert(body: JsonObject) = api.upsert(body = body)
    override suspend fun remoteGetAll(userIdFilter: String, updatedAtFilter: String?) =
        api.getAll(userIdFilter = userIdFilter, updatedAtFilter = updatedAtFilter)
    override fun dtoToEntity(dto: CardAccountLinkDto) = dto.toDomain(SyncState.SINCRONIZADO).toEntity()
    override fun entityId(entity: CardAccountLinkEntity) = entity.id
    override fun entitySyncState(entity: CardAccountLinkEntity) = entity.syncState
    override fun entityUpdatedAt(entity: CardAccountLinkEntity) = entity.updatedAt
    override suspend fun getLocalById(id: String) = dbProvider.current().cardAccountLinkDao().getById(id)
    override suspend fun upsertLocal(entity: CardAccountLinkEntity) =
        dbProvider.current().cardAccountLinkDao().upsert(entity)
    override suspend fun markLocalSynced(rowId: String) =
        dbProvider.current().cardAccountLinkDao().updateSyncState(rowId, SyncState.SINCRONIZADO.name)
}

@Singleton
class VaultLedgerSyncHandler @Inject constructor(
    private val dbProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val api: VaultLedgerApi
) : TableSyncHandler {
    override val table: String = SyncEngine.LEDGER_TABLE

    override suspend fun push(item: OutboxItem): PushResult {
        val body = OutboxPayloadSerializer.deserialize(item.payloadJson)
        val response = api.insert(body = body)

        if (response.isSuccessful) {
            val inserted = response.body().orEmpty()
            dbProvider.current().vaultLedgerDao().updateSyncState(item.rowId, SyncState.SINCRONIZADO.name)
            return if (inserted.isEmpty()) PushResult.AlreadyApplied else PushResult.Success
        }

        val errorBody = response.errorBody()?.string()
        val sqlState = extractPostgrestSqlState(errorBody)
        if (response.code() == 409 || PostgrestErrorClassifier.isIdempotentDuplicate(sqlState)) {
            dbProvider.current().vaultLedgerDao().updateSyncState(item.rowId, SyncState.SINCRONIZADO.name)
            return PushResult.AlreadyApplied
        }

        return PushResult.Failure(
            IllegalStateException("HTTP ${response.code()} em $table: ${errorBody ?: response.message()}")
        )
    }

    override suspend fun pull(since: Instant?): PullResult {
        val userId = userSession.currentUserId()
            ?: return PullResult.Failure(IllegalStateException("Sem sessão ativa para sincronizar $table"))

        val cursorFilter = since?.let { "gt.$it" }
        val response = api.getAll(userIdFilter = "eq.$userId", createdAtFilter = cursorFilter)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            return PullResult.Failure(
                IllegalStateException("HTTP ${response.code()} ao buscar $table: ${errorBody ?: response.message()}")
            )
        }

        val dtos = response.body().orEmpty()
        if (dtos.isEmpty()) {
            return PullResult.Success(newCursor = since, recordsCount = 0)
        }

        val entities = dtos.map { it.toDomain(SyncState.SINCRONIZADO).toEntity() }
        // Razão imutável: INSERT IGNORE, nunca sobrescreve linha existente
        dbProvider.current().vaultLedgerDao().insertAllIgnore(entities)

        var maxCreatedAt = since
        for (entity in entities) {
            if (maxCreatedAt == null || entity.createdAt.isAfter(maxCreatedAt)) {
                maxCreatedAt = entity.createdAt
            }
        }

        return PullResult.Success(newCursor = maxCreatedAt, recordsCount = entities.size)
    }
}
