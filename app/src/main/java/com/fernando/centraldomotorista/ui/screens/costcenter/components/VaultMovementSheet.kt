package com.fernando.centraldomotorista.ui.screens.costcenter.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.ui.screens.costcenter.ActionOutcome
import com.fernando.centraldomotorista.ui.screens.costcenter.UiText
import com.fernando.centraldomotorista.ui.screens.costcenter.asString
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

enum class VaultMovementMode(
    @StringRes val titleRes: Int,
    @StringRes val actionLabelRes: Int
) {
    APORTE(R.string.vault_movement_title_deposit, R.string.vault_movement_action_deposit),
    RETIRADA(R.string.vault_movement_title_withdraw, R.string.vault_movement_action_withdraw),
    TRANSFERENCIA(R.string.vault_movement_title_transfer, R.string.vault_movement_action_transfer),
    AJUSTE(R.string.vault_movement_title_adjust, R.string.vault_movement_action_adjust)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMovementSheet(
    mode: VaultMovementMode,
    vault: Vault,
    currentBalance: BigDecimal,
    otherVaults: List<Vault>,
    initialAmount: String? = null,
    isSubmitting: Boolean,
    onSubmit: suspend (
        amountOrTarget: BigDecimal,
        toVaultId: String?,
        note: String?,
        operationId: String
    ) -> ActionOutcome,
    onDismiss: () -> Unit
) {
    // Gera um operationId único ao abrir o sheet e reutiliza em qualquer reenvio do mesmo formulário
    val operationId = rememberSaveable(mode, vault.id) { UUID.randomUUID().toString() }

    var amountText by rememberSaveable(mode, vault.id, initialAmount) {
        mutableStateOf(
            when {
                !initialAmount.isNullOrBlank() -> cleanCurrencyInput(initialAmount)
                mode == VaultMovementMode.AJUSTE -> currentBalance.toPlainString().replace('.', ',')
                else -> ""
            }
        )
    }
    var toVaultId by rememberSaveable(mode, vault.id) {
        mutableStateOf(otherVaults.firstOrNull()?.id.orEmpty())
    }
    var note by rememberSaveable(mode, vault.id) { mutableStateOf("") }
    var targetExpanded by remember { mutableStateOf(false) }
    var feedbackError by remember { mutableStateOf<UiText?>(null) }
    var infoMessage by remember { mutableStateOf<UiText?>(null) }
    val scope = rememberCoroutineScope()

    val isWithdrawalLike = mode == VaultMovementMode.RETIRADA || mode == VaultMovementMode.TRANSFERENCIA
    val selectedTargetVault = otherVaults.find { it.id == toVaultId }

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
                        text = stringResource(mode.titleRes),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = OrangeNeon,
                        letterSpacing = 0.8.sp
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
                Text(
                    text = stringResource(
                        R.string.vault_movement_current_balance,
                        vault.name,
                        currentBalance.formatCurrency()
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (mode == VaultMovementMode.TRANSFERENCIA) {
                item {
                    if (otherVaults.isEmpty()) {
                        Text(
                            text = stringResource(R.string.vault_movement_no_other_vaults),
                            color = RedAlert,
                            fontSize = 13.sp
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = targetExpanded,
                            onExpandedChange = { targetExpanded = !targetExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedTargetVault?.name
                                    ?: stringResource(R.string.vault_movement_select_target),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.vault_movement_target_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false }
                            ) {
                                otherVaults.forEach { target ->
                                    DropdownMenuItem(
                                        text = { Text(target.name) },
                                        onClick = {
                                            toVaultId = target.id
                                            targetExpanded = false
                                            feedbackError = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        if (mode == VaultMovementMode.AJUSTE && raw.contains('-')) {
                            feedbackError = UiText.Res(R.string.vm_error_adjust_negative)
                            return@OutlinedTextField
                        }
                        val cleaned = cleanCurrencyInput(raw)
                        val parsed = if (cleaned.isBlank()) BigDecimal.ZERO else parseCurrency(cleaned)
                        if (isWithdrawalLike && parsed > currentBalance) {
                            feedbackError = UiText.Res(
                                R.string.vm_error_withdraw_max_balance,
                                currentBalance.formatCurrency()
                            )
                            return@OutlinedTextField
                        }
                        amountText = cleaned
                        feedbackError = null
                        infoMessage = null
                    },
                    label = {
                        Text(
                            stringResource(
                                if (mode == VaultMovementMode.AJUSTE) {
                                    R.string.vault_movement_amount_adjust_label
                                } else {
                                    R.string.vault_movement_amount_label
                                }
                            )
                        )
                    },
                    placeholder = { Text(stringResource(R.string.cost_center_currency_placeholder)) },
                    supportingText = if (isWithdrawalLike) {
                        {
                            Text(
                                stringResource(
                                    R.string.vault_movement_max_available,
                                    currentBalance.formatCurrency()
                                )
                            )
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.vault_movement_note_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            feedbackError?.let { err ->
                item {
                    Text(
                        text = err.asString(),
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            infoMessage?.let { info ->
                item {
                    Text(
                        text = info.asString(),
                        color = OrangeNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val trimmed = amountText.trim()
                        if (trimmed.startsWith("-")) {
                            feedbackError = UiText.Res(R.string.vm_error_adjust_negative)
                            return@Button
                        }
                        val parsed = parseCurrency(trimmed)
                        if (isWithdrawalLike && parsed > currentBalance) {
                            feedbackError = UiText.Res(
                                R.string.vm_error_withdraw_max_balance,
                                currentBalance.formatCurrency()
                            )
                            return@Button
                        }
                        scope.launch {
                            when (val outcome = onSubmit(
                                parsed,
                                if (mode == VaultMovementMode.TRANSFERENCIA) toVaultId else null,
                                note,
                                operationId
                            )) {
                                is ActionOutcome.Success -> onDismiss()
                                is ActionOutcome.NoChange -> infoMessage = outcome.message
                                is ActionOutcome.Error -> feedbackError = outcome.message
                            }
                        }
                    },
                    enabled = !isSubmitting && (mode != VaultMovementMode.TRANSFERENCIA || otherVaults.isNotEmpty()),
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
                            if (isSubmitting) R.string.vault_movement_processing else mode.actionLabelRes
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
