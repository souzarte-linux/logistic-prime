package com.fernando.centraldomotorista.ui.screens.costcenter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.R
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.ui.screens.costcenter.ActionOutcome
import com.fernando.centraldomotorista.ui.screens.costcenter.UiText
import com.fernando.centraldomotorista.ui.screens.costcenter.asString
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultFormSheet(
    existing: Vault?,
    initialAccountId: String?,
    accounts: List<BankAccount>,
    isCurrentMaintenanceVault: Boolean,
    isSubmitting: Boolean,
    onSave: suspend (
        accountId: String?,
        name: String,
        purpose: VaultPurpose,
        initialBalance: BigDecimal,
        goalAmount: BigDecimal?,
        isMaintenanceVault: Boolean
    ) -> ActionOutcome,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var selectedAccountId by rememberSaveable(existing?.id, initialAccountId) {
        mutableStateOf(existing?.accountId ?: initialAccountId)
    }
    var selectedPurpose by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.purpose ?: VaultPurpose.OUTRO)
    }
    var initialBalanceText by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.initialBalance?.toPlainString()?.replace('.', ',') ?: "0,00"
        )
    }
    var goalAmountText by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.goalAmount?.toPlainString()?.replace('.', ',').orEmpty()
        )
    }
    var isMaintenanceVault by rememberSaveable(existing?.id, isCurrentMaintenanceVault) {
        mutableStateOf(isCurrentMaintenanceVault)
    }
    var accountExpanded by remember { mutableStateOf(false) }
    var purposeExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<UiText?>(null) }
    val scope = rememberCoroutineScope()

    val trackingDate = existing?.trackingStartDate ?: LocalDate.now()
    val trackingDateText = remember(trackingDate) {
        trackingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.vault_form_title_new else R.string.vault_form_title_edit
                        ),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = OrangeNeon,
                        letterSpacing = 1.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cost_center_action_close),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.vault_form_name_label)) },
                    placeholder = { Text(stringResource(R.string.vault_form_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: stringResource(R.string.vault_form_account_none),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.vault_form_account_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_form_account_none)) },
                            onClick = {
                                selectedAccountId = null
                                accountExpanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = purposeExpanded,
                    onExpandedChange = { purposeExpanded = !purposeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPurpose.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.vault_form_purpose_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = purposeExpanded,
                        onDismissRequest = { purposeExpanded = false }
                    ) {
                        VaultPurpose.entries.forEach { purpose ->
                            DropdownMenuItem(
                                text = { Text(purpose.label) },
                                onClick = {
                                    selectedPurpose = purpose
                                    purposeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (existing == null) {
                item {
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = {
                            initialBalanceText = cleanCurrencyInput(it)
                            errorText = null
                        },
                        label = { Text(stringResource(R.string.vault_form_initial_balance_label)) },
                        placeholder = { Text(stringResource(R.string.cost_center_currency_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = goalAmountText,
                    onValueChange = {
                        goalAmountText = cleanCurrencyInput(it)
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.vault_form_goal_label)) },
                    placeholder = { Text(stringResource(R.string.vault_form_goal_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.vault_form_tracking_start, trackingDateText),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.vault_form_maintenance_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.vault_form_maintenance_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMaintenanceVault,
                        onCheckedChange = { isMaintenanceVault = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = OrangeNeon
                        )
                    )
                }
            }

            errorText?.let { err ->
                item {
                    Text(
                        text = err.asString(),
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val initialBal = if (initialBalanceText.isBlank()) {
                            BigDecimal.ZERO
                        } else {
                            parseCurrency(initialBalanceText)
                        }
                        val goal = if (goalAmountText.isBlank()) {
                            null
                        } else {
                            parseCurrency(goalAmountText)
                        }
                        scope.launch {
                            when (val outcome = onSave(
                                selectedAccountId,
                                name,
                                selectedPurpose,
                                initialBal,
                                goal,
                                isMaintenanceVault
                            )) {
                                is ActionOutcome.Success, is ActionOutcome.NoChange -> onDismiss()
                                is ActionOutcome.Error -> errorText = outcome.message
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (isSubmitting) R.string.cost_center_saving else R.string.vault_form_save
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
