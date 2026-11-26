package com.fernando.centraldomotorista.ui.screens.costcenter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.ui.common.cards.CollapsibleSectionCard
import com.fernando.centraldomotorista.ui.screens.costcenter.components.SyncStatusChip
import com.fernando.centraldomotorista.ui.screens.costcenter.components.VaultMovementMode
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AccountsVaultsTab(
    uiState: AccountsVaultsUiState,
    onRetryLoad: () -> Unit,
    onRetrySync: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (BankAccount) -> Unit,
    onArchiveAccount: (BankAccount) -> Unit,
    onAddVault: (accountId: String?) -> Unit,
    onEditVault: (Vault) -> Unit,
    onArchiveVault: (Vault) -> Unit,
    onVaultMovement: (Vault, VaultMovementMode) -> Unit,
    onOpenStatement: (Vault) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("accounts_vaults_loading"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = OrangeNeon)
                    Text(
                        text = "Carregando contas e caixinhas...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        uiState.errorMessage != null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("accounts_vaults_error"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Erro",
                            tint = RedAlert,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = uiState.errorMessage,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = onRetryLoad,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeNeon,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Tentar novamente", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        else -> {
            val accountIds = remember(uiState.accounts) { uiState.accounts.map { it.id }.toSet() }
            val orphanVaults = remember(uiState.vaults, accountIds) {
                uiState.vaults.filter { it.accountId == null || it.accountId !in accountIds }
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                item(key = "total_card") {
                    TotalInVaultsCard(
                        total = uiState.totalInVaults,
                        syncStatus = uiState.syncStatus,
                        onRetrySync = onRetrySync
                    )
                }

                item(key = "quick_actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddAccount,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OrangeNeon)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Nova conta", color = OrangeNeon, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddVault(uiState.accounts.firstOrNull()?.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeNeon,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Nova caixinha", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.accounts.isEmpty() && uiState.vaults.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyAccountsState(onAddAccount = onAddAccount)
                    }
                } else {
                    items(uiState.accounts, key = { it.id }) { account ->
                        val accountVaults = uiState.vaults.filter { it.accountId == account.id }
                        AccountSectionCard(
                            account = account,
                            vaults = accountVaults,
                            balancesByVaultId = uiState.balancesByVaultId,
                            maintenanceVaultId = uiState.maintenanceVaultId,
                            onEditAccount = { onEditAccount(account) },
                            onArchiveAccount = { onArchiveAccount(account) },
                            onAddVault = { onAddVault(account.id) },
                            onEditVault = onEditVault,
                            onArchiveVault = onArchiveVault,
                            onVaultMovement = onVaultMovement,
                            onOpenStatement = onOpenStatement
                        )
                    }

                    if (orphanVaults.isNotEmpty()) {
                        item(key = "orphan_vaults") {
                            CollapsibleSectionCard(
                                title = "Caixinhas sem conta vinculada",
                                icon = Icons.Default.Savings,
                                initiallyExpanded = true
                            ) {
                                orphanVaults.forEach { vault ->
                                    VaultItemCard(
                                        vault = vault,
                                        balance = uiState.balancesByVaultId[vault.id] ?: vault.initialBalance,
                                        isMaintenanceVault = vault.id == uiState.maintenanceVaultId,
                                        onAportar = { onVaultMovement(vault, VaultMovementMode.APORTE) },
                                        onRetirar = { onVaultMovement(vault, VaultMovementMode.RETIRADA) },
                                        onTransferir = { onVaultMovement(vault, VaultMovementMode.TRANSFERENCIA) },
                                        onAjustar = { onVaultMovement(vault, VaultMovementMode.AJUSTE) },
                                        onExtrato = { onOpenStatement(vault) },
                                        onEditar = { onEditVault(vault) },
                                        onArquivar = { onArchiveVault(vault) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalInVaultsCard(
    total: BigDecimal,
    syncStatus: SyncChipStatus,
    onRetrySync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("total_in_vaults_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "TOTAL NAS CAIXINHAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = total.formatCurrency(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = if (total < BigDecimal.ZERO) RedAlert else GreenNeon
            )
            SyncStatusChip(
                status = syncStatus,
                onRetrySync = onRetrySync
            )
        }
    }
}

@Composable
private fun EmptyAccountsState(onAddAccount: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accounts_vaults_empty"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = OrangeNeon,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Nenhuma conta cadastrada",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cadastre sua conta bancária e organize suas caixinhas de reserva para manutenção, impostos e imprevistos.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddAccount,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cadastrar primeira conta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AccountSectionCard(
    account: BankAccount,
    vaults: List<Vault>,
    balancesByVaultId: Map<String, BigDecimal>,
    maintenanceVaultId: String?,
    onEditAccount: () -> Unit,
    onArchiveAccount: () -> Unit,
    onAddVault: () -> Unit,
    onEditVault: (Vault) -> Unit,
    onArchiveVault: (Vault) -> Unit,
    onVaultMovement: (Vault, VaultMovementMode) -> Unit,
    onOpenStatement: (Vault) -> Unit
) {
    val subtitleParts = buildList {
        account.institution?.takeIf { it.isNotBlank() }?.let { add(it) }
        account.accountLastDigits?.takeIf { it.isNotBlank() }?.let { add("•••• $it") }
    }
    val subtitle = subtitleParts.joinToString(" • ").ifBlank { account.accountType?.label }

    val informedDateText = remember(account.informedBalanceAt) {
        account.informedBalanceAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
            ?.format(DateTimeFormatter.ofPattern("dd/MM"))
    }

    CollapsibleSectionCard(
        title = account.name,
        subtitle = subtitle,
        icon = Icons.Default.AccountBalance,
        initiallyExpanded = true
    ) {
        if (account.informedBalance != null) {
            val datePart = if (informedDateText != null) " em $informedDateText" else ""
            Text(
                text = "Saldo informado: ${account.informedBalance.formatCurrency()}$datePart (não gerenciado pelo app)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onAddVault) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangeNeon)
                Spacer(modifier = Modifier.size(4.dp))
                Text("Caixinha", color = OrangeNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onEditAccount) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Editar", fontSize = 12.sp)
            }
            TextButton(onClick = onArchiveAccount) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedAlert)
                Spacer(modifier = Modifier.size(4.dp))
                Text("Arquivar", color = RedAlert, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (vaults.isEmpty()) {
            Text(
                text = "Nenhuma caixinha vinculada a esta conta.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            vaults.forEach { vault ->
                VaultItemCard(
                    vault = vault,
                    balance = balancesByVaultId[vault.id] ?: vault.initialBalance,
                    isMaintenanceVault = vault.id == maintenanceVaultId,
                    onAportar = { onVaultMovement(vault, VaultMovementMode.APORTE) },
                    onRetirar = { onVaultMovement(vault, VaultMovementMode.RETIRADA) },
                    onTransferir = { onVaultMovement(vault, VaultMovementMode.TRANSFERENCIA) },
                    onAjustar = { onVaultMovement(vault, VaultMovementMode.AJUSTE) },
                    onExtrato = { onOpenStatement(vault) },
                    onEditar = { onEditVault(vault) },
                    onArquivar = { onArchiveVault(vault) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultItemCard(
    vault: Vault,
    balance: BigDecimal,
    isMaintenanceVault: Boolean,
    onAportar: () -> Unit,
    onRetirar: () -> Unit,
    onTransferir: () -> Unit,
    onAjustar: () -> Unit,
    onExtrato: () -> Unit,
    onEditar: () -> Unit,
    onArquivar: () -> Unit
) {
    val isNegative = balance < BigDecimal.ZERO
    val balanceColor = if (isNegative) RedAlert else GreenNeon

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (isMaintenanceVault) OrangeNeon.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = vault.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = vault.purpose.label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = balance.formatCurrency(),
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = balanceColor
                )
            }

            if (isMaintenanceVault) {
                AssistChip(
                    onClick = onEditar,
                    label = { Text("Desgaste de peças", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OrangeNeon
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = OrangeNeon.copy(alpha = 0.12f),
                        labelColor = OrangeNeon
                    )
                )
            }

            val goal = vault.goalAmount
            if (goal != null && goal > BigDecimal.ZERO) {
                val progress = balance
                    .divide(goal, 4, RoundingMode.HALF_UP)
                    .toFloat()
                    .coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = OrangeNeon,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Meta: ${goal.formatCurrency()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VaultActionChip(label = "Aportar", onClick = onAportar, highlight = true)
                VaultActionChip(label = "Retirar", onClick = onRetirar)
                VaultActionChip(label = "Transferir", icon = Icons.Default.SwapHoriz, onClick = onTransferir)
                VaultActionChip(label = "Ajustar saldo", icon = Icons.Default.Tune, onClick = onAjustar)
                VaultActionChip(label = "Extrato", icon = Icons.AutoMirrored.Filled.ReceiptLong, onClick = onExtrato)
                VaultActionChip(label = "Editar", icon = Icons.Default.Edit, onClick = onEditar)
                VaultActionChip(label = "Arquivar", icon = Icons.Default.Archive, onClick = onArquivar, isDanger = true)
            }
        }
    }
}

@Composable
private fun VaultActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    highlight: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = when {
        isDanger -> RedAlert
        highlight -> OrangeNeon
        else -> MaterialTheme.colorScheme.onSurface
    }
    FilterChip(
        selected = highlight,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        },
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
        } else null
    )
}
