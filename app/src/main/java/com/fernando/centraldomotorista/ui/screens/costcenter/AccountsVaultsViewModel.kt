package com.fernando.centraldomotorista.ui.screens.costcenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.CostCenterSettings
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.repository.costcenter.BankAccountRepository
import com.fernando.centraldomotorista.data.repository.costcenter.CostCenterSettingsRepository
import com.fernando.centraldomotorista.data.repository.costcenter.InsufficientVaultBalance
import com.fernando.centraldomotorista.data.repository.costcenter.LedgerRepository
import com.fernando.centraldomotorista.data.repository.costcenter.VaultRepository
import com.fernando.centraldomotorista.sync.SyncEngine
import com.fernando.centraldomotorista.sync.SyncScheduler
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface SyncChipStatus {
    data object Synced : SyncChipStatus
    data class Pending(val count: Int) : SyncChipStatus
    data class Failed(val count: Int) : SyncChipStatus
}

sealed interface ActionOutcome {
    data class Success(val message: String) : ActionOutcome
    data class NoChange(val message: String) : ActionOutcome
    data class Error(val message: String) : ActionOutcome
}

data class AccountsVaultsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val accounts: List<BankAccount> = emptyList(),
    val vaults: List<Vault> = emptyList(),
    val balancesByVaultId: Map<String, BigDecimal> = emptyMap(),
    val totalInVaults: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val maintenanceVaultId: String? = null,
    val syncStatus: SyncChipStatus = SyncChipStatus.Synced,
    val statementVault: Vault? = null,
    val statementEntries: List<LedgerEntry> = emptyList(),
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsVaultsViewModel @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val vaultRepository: VaultRepository,
    private val ledgerRepository: LedgerRepository,
    private val settingsRepository: CostCenterSettingsRepository,
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsVaultsUiState())
    val uiState: StateFlow<AccountsVaultsUiState> = _uiState.asStateFlow()

    private val selectedStatementVaultId = MutableStateFlow<String?>(null)
    private val reloadTrigger = MutableStateFlow(0)

    init {
        observeMainData()
        observeSyncStatus()
        observeStatement()
    }

    private fun observeMainData() {
        viewModelScope.launch {
            reloadTrigger.flatMapLatest {
                combine(
                    bankAccountRepository.observeActive(),
                    vaultRepository.observeActive(),
                    vaultRepository.observeVaultBalances(),
                    settingsRepository.observe()
                ) { accounts, vaults, balances, settings ->
                    val balancesMap = balances.associate { it.vaultId to it.balance }
                    val total = balances.fold(BigDecimal.ZERO) { acc, item -> acc + item.balance }
                        .setScale(2, RoundingMode.HALF_UP)
                    AccountsVaultsUiState(
                        isLoading = false,
                        errorMessage = null,
                        accounts = accounts,
                        vaults = vaults,
                        balancesByVaultId = balancesMap,
                        totalInVaults = total,
                        maintenanceVaultId = settings?.maintenanceVaultId,
                        syncStatus = _uiState.value.syncStatus,
                        statementVault = vaults.find { it.id == selectedStatementVaultId.value }
                            ?: _uiState.value.statementVault,
                        statementEntries = _uiState.value.statementEntries,
                        isSubmitting = _uiState.value.isSubmitting,
                        snackbarMessage = _uiState.value.snackbarMessage
                    )
                }.catch { throwable ->
                    emit(
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Não foi possível carregar contas e caixinhas."
                        )
                    )
                }
            }.collect { nextState ->
                _uiState.value = nextState
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            userSession.userIdFlow.flatMapLatest { userId ->
                if (userId.isNullOrBlank()) {
                    flowOf(SyncChipStatus.Synced)
                } else {
                    localDatabaseProvider.current().outboxDao().observeAll().map { items ->
                        if (items.isEmpty()) {
                            SyncChipStatus.Synced
                        } else {
                            val failedCount = items.count {
                                it.attempts >= SyncEngine.MAX_ATTEMPTS ||
                                    (it.attempts > 0 && !it.lastError.isNullOrBlank())
                            }
                            if (failedCount > 0) {
                                SyncChipStatus.Failed(failedCount)
                            } else {
                                SyncChipStatus.Pending(items.size)
                            }
                        }
                    }
                }
            }.catch {
                emit(SyncChipStatus.Synced)
            }.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
    }

    private fun observeStatement() {
        viewModelScope.launch {
            selectedStatementVaultId.flatMapLatest { vaultId ->
                if (vaultId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    localDatabaseProvider.current().vaultLedgerDao().observeEntriesForVault(vaultId)
                        .map { entities -> entities.map { it.toDomain() } }
                }
            }.catch {
                emit(emptyList())
            }.collect { entries ->
                _uiState.update { it.copy(statementEntries = entries) }
            }
        }
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        reloadTrigger.update { it + 1 }
    }

    fun retrySync() {
        viewModelScope.launch {
            try {
                localDatabaseProvider.current().outboxDao().resetFailedAttempts()
                syncScheduler.scheduleOneTimeSync()
                _uiState.update { it.copy(snackbarMessage = "Sincronização agendada") }
            } catch (t: Throwable) {
                _uiState.update { it.copy(snackbarMessage = t.message ?: "Falha ao reagendar sincronização") }
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun openStatement(vault: Vault) {
        selectedStatementVaultId.value = vault.id
        _uiState.update { it.copy(statementVault = vault) }
    }

    fun closeStatement() {
        selectedStatementVaultId.value = null
        _uiState.update { it.copy(statementVault = null, statementEntries = emptyList()) }
    }

    suspend fun saveAccount(
        existing: BankAccount?,
        name: String,
        institution: String?,
        agency: String?,
        lastDigits: String?,
        accountType: AccountType?,
        informedBalance: BigDecimal?
    ): ActionOutcome {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return ActionOutcome.Error("Informe o apelido da conta")
        }

        val cleanDigits = lastDigits?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanDigits != null && (!cleanDigits.all { it.isDigit() } || cleanDigits.length > 6)) {
            return ActionOutcome.Error("Informe apenas até 6 dígitos da conta")
        }

        if (informedBalance != null && informedBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error("O saldo informado não pode ser negativo")
        }

        val userId = userSession.currentUserId()
            ?: return ActionOutcome.Error("Sessão expirada. Faça login novamente.")

        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val now = Instant.now()
            val normalizedBalance = informedBalance?.setScale(2, RoundingMode.HALF_UP)
            val balanceChanged = existing == null ||
                (normalizedBalance == null && existing.informedBalance != null) ||
                (normalizedBalance != null && existing.informedBalance?.compareTo(normalizedBalance) != 0)
            val balanceAt = when {
                normalizedBalance == null -> null
                balanceChanged -> now
                else -> existing?.informedBalanceAt ?: now
            }

            val account = if (existing != null) {
                existing.copy(
                    name = trimmedName,
                    institution = institution?.trim()?.takeIf { it.isNotEmpty() },
                    agency = agency?.trim()?.takeIf { it.isNotEmpty() },
                    accountLastDigits = cleanDigits,
                    accountType = accountType,
                    informedBalance = normalizedBalance,
                    informedBalanceAt = balanceAt
                )
            } else {
                BankAccount(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = trimmedName,
                    institution = institution?.trim()?.takeIf { it.isNotEmpty() },
                    agency = agency?.trim()?.takeIf { it.isNotEmpty() },
                    accountLastDigits = cleanDigits,
                    accountType = accountType,
                    informedBalance = normalizedBalance,
                    informedBalanceAt = balanceAt,
                    active = true,
                    createdAt = now,
                    updatedAt = now
                )
            }
            bankAccountRepository.save(account)
            val msg = if (existing != null) "Conta atualizada" else "Conta cadastrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao salvar conta")
        }
    }

    suspend fun archiveAccount(accountId: String): ActionOutcome {
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val result = bankAccountRepository.archiveOrDelete(accountId)
            val msg = if (result?.deletedAt != null) "Conta excluída" else "Conta arquivada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao arquivar conta")
        }
    }

    suspend fun saveVault(
        existing: Vault?,
        accountId: String?,
        name: String,
        purpose: VaultPurpose,
        initialBalance: BigDecimal,
        goalAmount: BigDecimal?,
        isMaintenanceVault: Boolean
    ): ActionOutcome {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return ActionOutcome.Error("Informe o nome da caixinha")
        }
        if (initialBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error("O saldo inicial não pode ser negativo")
        }
        if (goalAmount != null && goalAmount <= BigDecimal.ZERO) {
            return ActionOutcome.Error("A meta deve ser maior que zero")
        }

        val userId = userSession.currentUserId()
            ?: return ActionOutcome.Error("Sessão expirada. Faça login novamente.")

        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val now = Instant.now()
            val vault = if (existing != null) {
                existing.copy(
                    accountId = accountId,
                    name = trimmedName,
                    purpose = purpose,
                    goalAmount = goalAmount?.setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                Vault(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    accountId = accountId,
                    name = trimmedName,
                    purpose = purpose,
                    goalAmount = goalAmount?.setScale(2, RoundingMode.HALF_UP),
                    initialBalance = initialBalance.setScale(2, RoundingMode.HALF_UP),
                    trackingStartDate = LocalDate.now(),
                    active = true,
                    createdAt = now,
                    updatedAt = now
                )
            }
            val saved = vaultRepository.save(vault)

            val currentSettings = settingsRepository.get()
            if (isMaintenanceVault) {
                val updatedSettings = (currentSettings ?: CostCenterSettings(
                    userId = userId,
                    createdAt = now,
                    updatedAt = now
                )).copy(maintenanceVaultId = saved.id)
                settingsRepository.save(updatedSettings)
            } else if (currentSettings?.maintenanceVaultId == saved.id) {
                settingsRepository.save(currentSettings.copy(maintenanceVaultId = null))
            }

            val msg = if (existing != null) "Caixinha atualizada" else "Caixinha cadastrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao salvar caixinha")
        }
    }

    suspend fun archiveVault(vaultId: String): ActionOutcome {
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val result = vaultRepository.archiveOrDelete(vaultId)
            val currentSettings = settingsRepository.get()
            if (currentSettings?.maintenanceVaultId == vaultId) {
                settingsRepository.save(currentSettings.copy(maintenanceVaultId = null))
            }
            val msg = if (result?.deletedAt != null) "Caixinha excluída" else "Caixinha arquivada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao arquivar caixinha")
        }
    }

    suspend fun aportar(
        vaultId: String,
        amount: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error("Informe um valor maior que zero")
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val entry = ledgerRepository.aportar(
                vaultId = vaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = if (entry != null) "Aporte registrado" else "Operação já registrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao registrar aporte")
        }
    }

    suspend fun retirar(
        vaultId: String,
        amount: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error("Informe um valor maior que zero")
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val available = ledgerRepository.getVaultBalance(vaultId)
            if (amount.setScale(2, RoundingMode.HALF_UP) > available) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.Error(
                    "O valor máximo para retirada é o saldo existente (${available.formatCurrency()})"
                )
            }
            val entry = ledgerRepository.retirar(
                vaultId = vaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = if (entry != null) "Retirada registrada" else "Operação já registrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (e: InsufficientVaultBalance) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(
                "O valor máximo para retirada é o saldo existente (${e.available.formatCurrency()})"
            )
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao registrar retirada")
        }
    }

    suspend fun transferir(
        fromVaultId: String,
        toVaultId: String,
        amount: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (fromVaultId == toVaultId) {
            return ActionOutcome.Error("Selecione uma caixinha de destino diferente")
        }
        if (toVaultId.isBlank()) {
            return ActionOutcome.Error("Selecione a caixinha de destino")
        }
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error("Informe um valor maior que zero")
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val available = ledgerRepository.getVaultBalance(fromVaultId)
            if (amount.setScale(2, RoundingMode.HALF_UP) > available) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.Error(
                    "O valor máximo para retirada é o saldo existente (${available.formatCurrency()})"
                )
            }
            val entries = ledgerRepository.transferir(
                fromVaultId = fromVaultId,
                toVaultId = toVaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = if (entries.isNotEmpty()) "Transferência registrada" else "Operação já registrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (e: InsufficientVaultBalance) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(
                "O valor máximo para retirada é o saldo existente (${e.available.formatCurrency()})"
            )
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao transferir")
        }
    }

    suspend fun ajustarSaldo(
        vaultId: String,
        targetBalance: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (targetBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error("O saldo ajustado não pode ser negativo")
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val currentBalance = ledgerRepository.getVaultBalance(vaultId)
            val normalizedTarget = targetBalance.setScale(2, RoundingMode.HALF_UP)
            if (normalizedTarget.compareTo(currentBalance) == 0) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.NoChange("Saldo já está correto")
            }
            val entry = ledgerRepository.ajustarSaldo(
                vaultId = vaultId,
                targetBalance = normalizedTarget,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = if (entry != null) "Saldo ajustado" else "Operação já registrada"
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(t.message ?: "Erro ao ajustar saldo")
        }
    }
}
