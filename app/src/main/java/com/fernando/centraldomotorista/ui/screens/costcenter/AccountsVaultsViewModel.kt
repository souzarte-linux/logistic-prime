package com.fernando.centraldomotorista.ui.screens.costcenter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.R
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
import kotlin.coroutines.cancellation.CancellationException

sealed interface SyncChipStatus {
    data object Synced : SyncChipStatus
    data class Pending(val count: Int) : SyncChipStatus
    data class Failed(val count: Int) : SyncChipStatus
    data object Unknown : SyncChipStatus
}

sealed interface ActionOutcome {
    data class Success(val message: UiText) : ActionOutcome
    data class NoChange(val message: UiText) : ActionOutcome
    data class Error(val message: UiText) : ActionOutcome
}

data class AccountsVaultsUiState(
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val accounts: List<BankAccount> = emptyList(),
    val vaults: List<Vault> = emptyList(),
    val balancesByVaultId: Map<String, BigDecimal> = emptyMap(),
    val totalInVaults: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val maintenanceVaultId: String? = null,
    val syncStatus: SyncChipStatus = SyncChipStatus.Synced,
    val statementVault: Vault? = null,
    val statementEntries: List<LedgerEntry> = emptyList(),
    val isSubmitting: Boolean = false,
    val snackbarMessage: UiText? = null
)

private data class MainData(
    val accounts: List<BankAccount>,
    val vaults: List<Vault>,
    val balancesByVaultId: Map<String, BigDecimal>,
    val totalInVaults: BigDecimal,
    val maintenanceVaultId: String?
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

    private companion object {
        const val TAG = "AccountsVaultsVM"
    }

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
                    MainData(
                        accounts = accounts,
                        vaults = vaults,
                        balancesByVaultId = balancesMap,
                        totalInVaults = total,
                        maintenanceVaultId = settings?.maintenanceVaultId
                    )
                }.catch { throwable ->
                    Log.w(TAG, "observeMainData falhou: ${throwable::class.java.simpleName}")
                    val msg = UiText.Res(R.string.vm_error_load_accounts_vaults)
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }.collect { data ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = null,
                        accounts = data.accounts,
                        vaults = data.vaults,
                        balancesByVaultId = data.balancesByVaultId,
                        totalInVaults = data.totalInVaults,
                        maintenanceVaultId = data.maintenanceVaultId,
                        statementVault = current.statementVault?.let { sv ->
                            data.vaults.find { v -> v.id == sv.id } ?: sv
                        }
                    )
                }
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
                            val failedCount = items.count { it.attempts >= SyncEngine.MAX_ATTEMPTS }
                            if (failedCount > 0) {
                                SyncChipStatus.Failed(failedCount)
                            } else {
                                SyncChipStatus.Pending(items.size)
                            }
                        }
                    }
                }
            }.catch {
                emit(SyncChipStatus.Unknown)
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
                localDatabaseProvider.current().outboxDao().resetFailedAttempts(SyncEngine.MAX_ATTEMPTS)
                syncScheduler.scheduleOneTimeSync()
                _uiState.update { it.copy(snackbarMessage = UiText.Res(R.string.vm_sync_scheduled)) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "retrySync falhou: ${t::class.java.simpleName}")
                _uiState.update { it.copy(snackbarMessage = UiText.Res(R.string.vm_sync_reschedule_failed)) }
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
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_account_name_required))
        }

        val cleanDigits = lastDigits?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanDigits != null && (!cleanDigits.all { it.isDigit() } || cleanDigits.length > 6)) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_account_digits_max))
        }

        if (informedBalance != null && informedBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_account_balance_negative))
        }

        val userId = userSession.currentUserId()
            ?: return ActionOutcome.Error(UiText.Res(R.string.vm_error_session_expired))

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
            val msg = UiText.Res(
                if (existing != null) R.string.vm_account_updated else R.string.vm_account_created
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "saveAccount falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_save_account))
        }
    }

    suspend fun archiveAccount(accountId: String): ActionOutcome {
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val result = bankAccountRepository.archiveOrDelete(accountId)
            val msg = UiText.Res(
                if (result?.deletedAt != null) R.string.vm_account_deleted else R.string.vm_account_archived
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "archiveAccount falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_archive_account))
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
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_vault_name_required))
        }
        if (initialBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_vault_initial_negative))
        }
        if (goalAmount != null && goalAmount <= BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_vault_goal_positive))
        }

        val userId = userSession.currentUserId()
            ?: return ActionOutcome.Error(UiText.Res(R.string.vm_error_session_expired))

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

            val msg = UiText.Res(
                if (existing != null) R.string.vm_vault_updated else R.string.vm_vault_created
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "saveVault falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_save_vault))
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
            val msg = UiText.Res(
                if (result?.deletedAt != null) R.string.vm_vault_deleted else R.string.vm_vault_archived
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "archiveVault falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_archive_vault))
        }
    }

    suspend fun aportar(
        vaultId: String,
        amount: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_amount_positive))
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val entry = ledgerRepository.aportar(
                vaultId = vaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = UiText.Res(
                if (entry != null) R.string.vm_deposit_recorded else R.string.vm_operation_already_recorded
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "deposit falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_deposit))
        }
    }

    suspend fun retirar(
        vaultId: String,
        amount: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_amount_positive))
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val available = ledgerRepository.getVaultBalance(vaultId)
            if (amount.setScale(2, RoundingMode.HALF_UP) > available) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.Error(
                    UiText.Res(R.string.vm_error_withdraw_max_balance, available.formatCurrency())
                )
            }
            val entry = ledgerRepository.retirar(
                vaultId = vaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = UiText.Res(
                if (entry != null) R.string.vm_withdraw_recorded else R.string.vm_operation_already_recorded
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (e: InsufficientVaultBalance) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(
                UiText.Res(R.string.vm_error_withdraw_max_balance, e.available.formatCurrency())
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "withdraw falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_withdraw))
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
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_transfer_same_vault))
        }
        if (toVaultId.isBlank()) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_transfer_target_required))
        }
        if (amount <= BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_amount_positive))
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val available = ledgerRepository.getVaultBalance(fromVaultId)
            if (amount.setScale(2, RoundingMode.HALF_UP) > available) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.Error(
                    UiText.Res(R.string.vm_error_withdraw_max_balance, available.formatCurrency())
                )
            }
            val entries = ledgerRepository.transferir(
                fromVaultId = fromVaultId,
                toVaultId = toVaultId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = UiText.Res(
                if (entries.isNotEmpty()) R.string.vm_transfer_recorded else R.string.vm_operation_already_recorded
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (e: InsufficientVaultBalance) {
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(
                UiText.Res(R.string.vm_error_withdraw_max_balance, e.available.formatCurrency())
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "transfer falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_transfer))
        }
    }

    suspend fun ajustarSaldo(
        vaultId: String,
        targetBalance: BigDecimal,
        note: String?,
        operationId: String
    ): ActionOutcome {
        if (targetBalance < BigDecimal.ZERO) {
            return ActionOutcome.Error(UiText.Res(R.string.vm_error_adjust_negative))
        }
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            val currentBalance = ledgerRepository.getVaultBalance(vaultId)
            val normalizedTarget = targetBalance.setScale(2, RoundingMode.HALF_UP)
            if (normalizedTarget.compareTo(currentBalance) == 0) {
                _uiState.update { it.copy(isSubmitting = false) }
                return ActionOutcome.NoChange(UiText.Res(R.string.vm_adjust_no_change))
            }
            val entry = ledgerRepository.ajustarSaldo(
                vaultId = vaultId,
                targetBalance = normalizedTarget,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                operationId = operationId
            )
            val msg = UiText.Res(
                if (entry != null) R.string.vm_adjust_recorded else R.string.vm_operation_already_recorded
            )
            _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            ActionOutcome.Success(msg)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "adjust falhou: ${t::class.java.simpleName}")
            _uiState.update { it.copy(isSubmitting = false) }
            ActionOutcome.Error(UiText.Res(R.string.vm_error_adjust))
        }
    }
}
