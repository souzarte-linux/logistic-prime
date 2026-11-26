package com.fernando.centraldomotorista.ui.screens.costcenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.ui.screens.costcenter.components.AccountFormSheet
import com.fernando.centraldomotorista.ui.screens.costcenter.components.VaultFormSheet
import com.fernando.centraldomotorista.ui.screens.costcenter.components.VaultMovementMode
import com.fernando.centraldomotorista.ui.screens.costcenter.components.VaultMovementSheet
import com.fernando.centraldomotorista.ui.screens.costcenter.components.VaultStatementSheet
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class CentroCustosTab(val label: String) {
    CONTAS("Contas & Caixinhas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentroCustosScreen(
    initialTab: String? = null,
    aporteVaultId: String? = null,
    aporteAmount: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AccountsVaultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable(initialTab) {
        mutableStateOf(
            CentroCustosTab.entries.find { it.name.equals(initialTab, ignoreCase = true) }
                ?: CentroCustosTab.CONTAS
        )
    }

    var showAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }

    var showVaultSheet by remember { mutableStateOf(false) }
    var editingVault by remember { mutableStateOf<Vault?>(null) }
    var preselectedAccountId by remember { mutableStateOf<String?>(null) }

    var movementTarget by remember { mutableStateOf<Pair<Vault, VaultMovementMode>?>(null) }
    var movementInitialAmount by remember { mutableStateOf<String?>(null) }
    var handledAporteRoute by rememberSaveable(aporteVaultId, aporteAmount) { mutableStateOf(false) }

    var pendingArchiveAccount by remember { mutableStateOf<BankAccount?>(null) }
    var pendingArchiveVault by remember { mutableStateOf<Vault?>(null) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(aporteVaultId, aporteAmount, uiState.vaults) {
        if (!handledAporteRoute && !aporteVaultId.isNullOrBlank()) {
            val targetVault = uiState.vaults.find { it.id == aporteVaultId }
            if (targetVault != null) {
                selectedTab = CentroCustosTab.CONTAS
                movementInitialAmount = aporteAmount
                movementTarget = targetVault to VaultMovementMode.APORTE
                handledAporteRoute = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CENTRO DE CUSTOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val selectedIndex = CentroCustosTab.entries.indexOf(selectedTab).coerceAtLeast(0)
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangeNeon,
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = OrangeNeon
                        )
                    }
                }
            ) {
                CentroCustosTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = OrangeNeon,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                CentroCustosTab.CONTAS -> {
                    AccountsVaultsTab(
                        uiState = uiState,
                        onRetryLoad = { viewModel.retryLoad() },
                        onRetrySync = { viewModel.retrySync() },
                        onAddAccount = {
                            editingAccount = null
                            showAccountSheet = true
                        },
                        onEditAccount = { account ->
                            editingAccount = account
                            showAccountSheet = true
                        },
                        onArchiveAccount = { account ->
                            pendingArchiveAccount = account
                        },
                        onAddVault = { accountId ->
                            editingVault = null
                            preselectedAccountId = accountId
                            showVaultSheet = true
                        },
                        onEditVault = { vault ->
                            editingVault = vault
                            preselectedAccountId = vault.accountId
                            showVaultSheet = true
                        },
                        onArchiveVault = { vault ->
                            pendingArchiveVault = vault
                        },
                        onVaultMovement = { vault, mode ->
                            movementInitialAmount = null
                            movementTarget = vault to mode
                        },
                        onOpenStatement = { vault ->
                            viewModel.openStatement(vault)
                        }
                    )
                }
            }
        }
    }

    if (showAccountSheet) {
        AccountFormSheet(
            existing = editingAccount,
            isSubmitting = uiState.isSubmitting,
            onSave = { name, institution, agency, lastDigits, accountType, informedBalance ->
                viewModel.saveAccount(
                    existing = editingAccount,
                    name = name,
                    institution = institution,
                    agency = agency,
                    lastDigits = lastDigits,
                    accountType = accountType,
                    informedBalance = informedBalance
                )
            },
            onDismiss = {
                showAccountSheet = false
                editingAccount = null
            }
        )
    }

    if (showVaultSheet) {
        val currentVault = editingVault
        VaultFormSheet(
            existing = currentVault,
            initialAccountId = preselectedAccountId,
            accounts = uiState.accounts,
            isCurrentMaintenanceVault = currentVault != null && currentVault.id == uiState.maintenanceVaultId,
            isSubmitting = uiState.isSubmitting,
            onSave = { accountId, name, purpose, initialBalance, goalAmount, isMaintenanceVault ->
                viewModel.saveVault(
                    existing = currentVault,
                    accountId = accountId,
                    name = name,
                    purpose = purpose,
                    initialBalance = initialBalance,
                    goalAmount = goalAmount,
                    isMaintenanceVault = isMaintenanceVault
                )
            },
            onDismiss = {
                showVaultSheet = false
                editingVault = null
                preselectedAccountId = null
            }
        )
    }

    movementTarget?.let { (vault, mode) ->
        val currentBalance = uiState.balancesByVaultId[vault.id] ?: vault.initialBalance
        val otherVaults = uiState.vaults.filter { it.id != vault.id }
        VaultMovementSheet(
            mode = mode,
            vault = vault,
            currentBalance = currentBalance,
            otherVaults = otherVaults,
            initialAmount = movementInitialAmount,
            isSubmitting = uiState.isSubmitting,
            onSubmit = { amountOrTarget, toVaultId, note, operationId ->
                when (mode) {
                    VaultMovementMode.APORTE -> viewModel.aportar(vault.id, amountOrTarget, note, operationId)
                    VaultMovementMode.RETIRADA -> viewModel.retirar(vault.id, amountOrTarget, note, operationId)
                    VaultMovementMode.TRANSFERENCIA -> viewModel.transferir(
                        fromVaultId = vault.id,
                        toVaultId = toVaultId.orEmpty(),
                        amount = amountOrTarget,
                        note = note,
                        operationId = operationId
                    )
                    VaultMovementMode.AJUSTE -> viewModel.ajustarSaldo(vault.id, amountOrTarget, note, operationId)
                }
            },
            onDismiss = {
                movementTarget = null
                movementInitialAmount = null
            }
        )
    }

    uiState.statementVault?.let { vault ->
        val vaultNames = remember(uiState.vaults) { uiState.vaults.associate { it.id to it.name } }
        VaultStatementSheet(
            vault = vault,
            currentBalance = uiState.balancesByVaultId[vault.id] ?: vault.initialBalance,
            entries = uiState.statementEntries,
            vaultNamesById = vaultNames,
            onDismiss = { viewModel.closeStatement() }
        )
    }

    pendingArchiveAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingArchiveAccount = null },
            title = { Text("Arquivar conta?") },
            text = {
                Text("A conta \"${account.name}\" será arquivada (ou removida se não possuir movimentações vinculadas).")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingArchiveAccount = null
                        scope.launch { viewModel.archiveAccount(account.id) }
                    }
                ) {
                    Text("Arquivar", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveAccount = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    pendingArchiveVault?.let { vault ->
        AlertDialog(
            onDismissRequest = { pendingArchiveVault = null },
            title = { Text("Arquivar caixinha?") },
            text = {
                Text("A caixinha \"${vault.name}\" será arquivada (ou removida se não possuir movimentações vinculadas).")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingArchiveVault = null
                        scope.launch { viewModel.archiveVault(vault.id) }
                    }
                ) {
                    Text("Arquivar", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveVault = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
