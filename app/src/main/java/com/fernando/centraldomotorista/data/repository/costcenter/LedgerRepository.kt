package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.sync.SyncEngine
import com.fernando.centraldomotorista.sync.SyncScheduler
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class InsufficientVaultBalance(
    val vaultId: String,
    val available: BigDecimal,
    val required: BigDecimal
) : IllegalStateException(
    "Saldo insuficiente na caixinha $vaultId: disponível $available, necessário $required"
)

@Singleton
class LedgerRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {

    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

    suspend fun getVaultBalance(vaultId: String): BigDecimal {
        val db = localDatabaseProvider.current()
        return computeVaultBalance(db, vaultId)
    }

    internal suspend fun computeVaultBalance(db: CostCenterDatabase, vaultId: String): BigDecimal {
        val vault = db.vaultDao().getById(vaultId)
            ?: throw IllegalArgumentException("Caixinha não encontrada: $vaultId")
        val entries = db.vaultLedgerDao().getEntriesForVault(vaultId)
        return entries.fold(vault.initialBalance) { acc, entry -> acc + entry.amount }.money()
    }

    suspend fun aportar(
        vaultId: String,
        amount: BigDecimal,
        occurredAt: Instant = Instant.now(),
        refType: LedgerRefType? = LedgerRefType.MANUAL,
        refId: String? = null,
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val normalizedAmount = amount.money()
        require(normalizedAmount > BigDecimal.ZERO) { "Valor do aporte deve ser maior que zero" }

        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            aportarInTransaction(db, userId, vaultId, normalizedAmount, occurredAt, refType, refId, note, operationId)
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    internal suspend fun aportarInTransaction(
        db: CostCenterDatabase,
        userId: String,
        vaultId: String,
        amount: BigDecimal,
        occurredAt: Instant,
        refType: LedgerRefType?,
        refId: String?,
        note: String?,
        operationId: String
    ): LedgerEntry? {
        val normalizedAmount = amount.money()
        require(normalizedAmount > BigDecimal.ZERO) { "Valor do aporte deve ser maior que zero" }
        val ledgerDao = db.vaultLedgerDao()
        if (ledgerDao.existsByOperationId(operationId)) return null

        val entry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            vaultId = vaultId,
            operationId = operationId,
            leg = 0,
            entryType = LedgerEntryType.APORTE,
            amount = normalizedAmount,
            occurredAt = occurredAt,
            refType = refType,
            refId = refId,
            note = note,
            createdAt = Instant.now(),
            syncState = SyncState.PENDENTE
        )
        insertLedgerAndOutbox(db, entry)
        return entry
    }

    suspend fun retirar(
        vaultId: String,
        amount: BigDecimal,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val normalizedAmount = amount.money()
        require(normalizedAmount > BigDecimal.ZERO) { "Valor da retirada deve ser maior que zero" }

        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            val ledgerDao = db.vaultLedgerDao()
            if (ledgerDao.existsByOperationId(operationId)) return@withTransaction null

            val balance = computeVaultBalance(db, vaultId)
            if (balance < normalizedAmount) {
                throw InsufficientVaultBalance(vaultId, balance, normalizedAmount)
            }

            val entry = LedgerEntry(
                id = UUID.randomUUID().toString(),
                userId = userId,
                vaultId = vaultId,
                operationId = operationId,
                leg = 0,
                entryType = LedgerEntryType.RETIRADA,
                amount = normalizedAmount.negate(),
                occurredAt = occurredAt,
                refType = LedgerRefType.MANUAL,
                note = note,
                createdAt = Instant.now(),
                syncState = SyncState.PENDENTE
            )
            insertLedgerAndOutbox(db, entry)
            entry
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    suspend fun transferir(
        fromVaultId: String,
        toVaultId: String,
        amount: BigDecimal,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): List<LedgerEntry> {
        val userId = requireUserId()
        require(fromVaultId != toVaultId) { "Caixinha de origem e destino devem ser diferentes" }
        val normalizedAmount = amount.money()
        require(normalizedAmount > BigDecimal.ZERO) { "Valor da transferência deve ser maior que zero" }

        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            val ledgerDao = db.vaultLedgerDao()
            if (ledgerDao.existsByOperationId(operationId)) return@withTransaction emptyList()

            val balance = computeVaultBalance(db, fromVaultId)
            if (balance < normalizedAmount) {
                throw InsufficientVaultBalance(fromVaultId, balance, normalizedAmount)
            }
            // Garante que a caixinha de destino existe localmente
            db.vaultDao().getById(toVaultId)
                ?: throw IllegalArgumentException("Caixinha de destino não encontrada: $toVaultId")

            val now = Instant.now()
            val outEntry = LedgerEntry(
                id = UUID.randomUUID().toString(),
                userId = userId,
                vaultId = fromVaultId,
                operationId = operationId,
                leg = 0,
                entryType = LedgerEntryType.TRANSFERENCIA,
                amount = normalizedAmount.negate(),
                occurredAt = occurredAt,
                refType = LedgerRefType.MANUAL,
                contraVaultId = toVaultId,
                note = note,
                createdAt = now,
                syncState = SyncState.PENDENTE
            )
            val inEntry = LedgerEntry(
                id = UUID.randomUUID().toString(),
                userId = userId,
                vaultId = toVaultId,
                operationId = operationId,
                leg = 1,
                entryType = LedgerEntryType.TRANSFERENCIA,
                amount = normalizedAmount,
                occurredAt = occurredAt,
                refType = LedgerRefType.MANUAL,
                contraVaultId = fromVaultId,
                note = note,
                createdAt = now,
                syncState = SyncState.PENDENTE
            )
            insertLedgerAndOutbox(db, outEntry)
            insertLedgerAndOutbox(db, inEntry)
            listOf(outEntry, inEntry)
        }
        if (inserted.isNotEmpty()) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    suspend fun ajustarSaldo(
        vaultId: String,
        targetBalance: BigDecimal,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val normalizedTarget = targetBalance.money()
        require(normalizedTarget >= BigDecimal.ZERO) { "Saldo ajustado não pode ser negativo" }

        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            val ledgerDao = db.vaultLedgerDao()
            if (ledgerDao.existsByOperationId(operationId)) return@withTransaction null

            val currentBalance = computeVaultBalance(db, vaultId)
            val diff = (normalizedTarget - currentBalance).money()
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                return@withTransaction null
            }

            val entry = LedgerEntry(
                id = UUID.randomUUID().toString(),
                userId = userId,
                vaultId = vaultId,
                operationId = operationId,
                leg = 0,
                entryType = LedgerEntryType.AJUSTE,
                amount = diff,
                occurredAt = occurredAt,
                refType = LedgerRefType.MANUAL,
                note = note,
                createdAt = Instant.now(),
                syncState = SyncState.PENDENTE
            )
            insertLedgerAndOutbox(db, entry)
            entry
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    suspend fun registrarPagamento(
        refType: LedgerRefType,
        refId: String,
        vaultId: String,
        vaultAmount: BigDecimal,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            registrarPagamentoInTransaction(
                db = db,
                userId = userId,
                refType = refType,
                refId = refId,
                vaultId = vaultId,
                vaultAmount = vaultAmount,
                occurredAt = occurredAt,
                note = note,
                operationId = operationId
            )
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    internal suspend fun registrarPagamentoInTransaction(
        db: CostCenterDatabase,
        userId: String,
        refType: LedgerRefType,
        refId: String,
        vaultId: String,
        vaultAmount: BigDecimal,
        occurredAt: Instant,
        note: String?,
        operationId: String
    ): LedgerEntry? {
        val normalizedAmount = vaultAmount.money()
        require(normalizedAmount > BigDecimal.ZERO) { "Valor do pagamento na caixinha deve ser maior que zero" }

        val ledgerDao = db.vaultLedgerDao()
        if (ledgerDao.existsByOperationId(operationId)) return null

        val balance = computeVaultBalance(db, vaultId)
        if (balance < normalizedAmount) {
            throw InsufficientVaultBalance(vaultId, balance, normalizedAmount)
        }

        val entryType = when (refType) {
            LedgerRefType.DESPESA -> LedgerEntryType.PAGAMENTO_DESPESA
            LedgerRefType.CUSTO_FIXO -> LedgerEntryType.PAGAMENTO_CUSTO_FIXO
            LedgerRefType.FATURA -> LedgerEntryType.PAGAMENTO_FATURA
            LedgerRefType.MANUAL, LedgerRefType.BAIXA_FATURA -> LedgerEntryType.RETIRADA
        }

        val entry = LedgerEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            vaultId = vaultId,
            operationId = operationId,
            leg = 0,
            entryType = entryType,
            amount = normalizedAmount.negate(),
            occurredAt = occurredAt,
            refType = refType,
            refId = refId,
            note = note,
            createdAt = Instant.now(),
            syncState = SyncState.PENDENTE
        )
        insertLedgerAndOutbox(db, entry)
        return entry
    }

    suspend fun estornar(
        entryId: String,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            estornarInTransaction(db, userId, entryId, occurredAt, note, operationId)
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    internal suspend fun estornarInTransaction(
        db: CostCenterDatabase,
        userId: String,
        entryId: String,
        occurredAt: Instant,
        note: String?,
        operationId: String
    ): LedgerEntry? {
        val ledgerDao = db.vaultLedgerDao()
        if (ledgerDao.existsByOperationId(operationId)) return null
        if (ledgerDao.existsReversalForEntry(entryId)) return null

        val original = ledgerDao.getById(entryId)
            ?: throw IllegalArgumentException("Lançamento não encontrado para estorno: $entryId")

        val reversal = LedgerEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            vaultId = original.vaultId,
            operationId = operationId,
            leg = 0,
            entryType = LedgerEntryType.ESTORNO,
            amount = original.amount.negate().money(),
            occurredAt = occurredAt,
            refType = LedgerRefType.fromKeyOrNull(original.refType),
            refId = original.refId,
            reversesEntryId = entryId,
            note = note ?: "Estorno de ${original.id}",
            createdAt = Instant.now(),
            syncState = SyncState.PENDENTE
        )
        insertLedgerAndOutbox(db, reversal)
        return reversal
    }

    suspend fun estornarPorReferencia(
        refType: LedgerRefType,
        refId: String,
        occurredAt: Instant = Instant.now(),
        note: String? = null,
        operationId: String = UUID.randomUUID().toString()
    ): LedgerEntry? {
        val userId = requireUserId()
        val db = localDatabaseProvider.current()
        val inserted = db.withTransaction {
            estornarPorReferenciaInTransaction(db, userId, refType, refId, occurredAt, note, operationId)
        }
        if (inserted != null) {
            notifyAfterMutation(syncScheduler)
        }
        return inserted
    }

    internal suspend fun estornarPorReferenciaInTransaction(
        db: CostCenterDatabase,
        userId: String,
        refType: LedgerRefType,
        refId: String,
        occurredAt: Instant,
        note: String?,
        operationId: String
    ): LedgerEntry? {
        val ledgerDao = db.vaultLedgerDao()
        if (ledgerDao.existsByOperationId(operationId)) return null

        val entries = ledgerDao.getEntriesByReference(refType.key, refId)
        if (entries.isEmpty()) return null

        val net = entries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }.money()
        if (net.compareTo(BigDecimal.ZERO) == 0) return null

        val lastEntry = entries.last()
        val reversal = LedgerEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            vaultId = lastEntry.vaultId,
            operationId = operationId,
            leg = 0,
            entryType = LedgerEntryType.ESTORNO,
            amount = net.negate().money(),
            occurredAt = occurredAt,
            refType = refType,
            refId = refId,
            reversesEntryId = lastEntry.id,
            note = note ?: "Estorno por referência ${refType.key}:$refId",
            createdAt = Instant.now(),
            syncState = SyncState.PENDENTE
        )
        insertLedgerAndOutbox(db, reversal)
        return reversal
    }

    private suspend fun insertLedgerAndOutbox(db: CostCenterDatabase, entry: LedgerEntry) {
        val entity = entry.toEntity(SyncState.PENDENTE)
        db.vaultLedgerDao().insertIgnore(entity)
        db.outboxDao().insert(
            buildOutboxEntity(
                tableName = SyncEngine.LEDGER_TABLE,
                rowId = entry.id,
                operation = "INSERT",
                dto = entry.toDto()
            )
        )
    }
}
