package com.fernando.centraldomotorista.ui.screens.costcenter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.R
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import com.fernando.centraldomotorista.ui.theme.BlueInfo
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultStatementSheet(
    vault: Vault,
    currentBalance: BigDecimal,
    entries: List<LedgerEntry>,
    vaultNamesById: Map<String, String>,
    onDismiss: () -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val entriesById = remember(entries) { entries.associateBy { it.id } }

    val groupedByDay = remember(entries) {
        entries.groupBy { entry ->
            entry.occurredAt.atZone(zoneId).toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vault_statement_title, vault.name.uppercase()),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = OrangeNeon,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = stringResource(R.string.vault_statement_current_balance, currentBalance.formatCurrency()),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentBalance < BigDecimal.ZERO) RedAlert else GreenNeon
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cost_center_action_close),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.vault_statement_empty),
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedByDay.forEach { (day, dayEntries) ->
                    item(key = "day_$day") {
                        Text(
                            text = day.format(dateFormatter),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    items(dayEntries, key = { it.id }) { entry ->
                        LedgerEntryRow(
                            entry = entry,
                            originalEntry = entry.reversesEntryId?.let { entriesById[it] },
                            contraVaultName = entry.contraVaultId?.let { vaultNamesById[it] },
                            timeText = entry.occurredAt.atZone(zoneId).format(timeFormatter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryRow(
    entry: LedgerEntry,
    originalEntry: LedgerEntry?,
    contraVaultName: String?,
    timeText: String
) {
    val isPositive = entry.amount >= BigDecimal.ZERO
    val amountColor = if (isPositive) GreenNeon else RedAlert
    val formattedAmount = if (isPositive) {
        "+${entry.amount.formatCurrency()}"
    } else {
        entry.amount.formatCurrency()
    }
    val icon = entryTypeIcon(entry.entryType)
    val iconTint = when (entry.entryType) {
        LedgerEntryType.ESTORNO -> OrangeNeon
        LedgerEntryType.TRANSFERENCIA -> BlueInfo
        else -> amountColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = entry.entryType.label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = entry.entryType.label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (entry.entryType == LedgerEntryType.ESTORNO || entry.reversesEntryId != null) {
                val originalSummary = if (originalEntry != null) {
                    stringResource(
                        R.string.vault_statement_reversal_with_original,
                        originalEntry.entryType.label.lowercase(),
                        originalEntry.amount.formatCurrency()
                    )
                } else {
                    stringResource(
                        R.string.vault_statement_reversal_fallback,
                        entry.reversesEntryId?.take(8).orEmpty()
                    )
                }
                Text(
                    text = originalSummary,
                    fontSize = 12.sp,
                    color = OrangeNeon,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!contraVaultName.isNullOrBlank()) {
                val direction = if (isPositive) {
                    stringResource(R.string.vault_statement_transfer_from, contraVaultName)
                } else {
                    stringResource(R.string.vault_statement_transfer_to, contraVaultName)
                }
                Text(
                    text = direction,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!entry.note.isNullOrBlank()) {
                Text(
                    text = entry.note,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = timeText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formattedAmount,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = amountColor
        )
    }
}

private fun entryTypeIcon(type: LedgerEntryType): ImageVector = when (type) {
    LedgerEntryType.APORTE -> Icons.Default.AddCircle
    LedgerEntryType.RETIRADA -> Icons.Default.RemoveCircle
    LedgerEntryType.TRANSFERENCIA -> Icons.Default.SwapHoriz
    LedgerEntryType.AJUSTE -> Icons.Default.Tune
    LedgerEntryType.PAGAMENTO_DESPESA,
    LedgerEntryType.PAGAMENTO_CUSTO_FIXO,
    LedgerEntryType.PAGAMENTO_FATURA -> Icons.Default.Payments
    LedgerEntryType.ESTORNO -> Icons.AutoMirrored.Filled.Undo
}
