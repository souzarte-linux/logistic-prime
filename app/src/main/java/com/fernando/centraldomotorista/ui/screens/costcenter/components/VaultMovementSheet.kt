package com.fernando.centraldomotorista.ui.screens.costcenter.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.ui.screens.costcenter.ActionOutcome
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

enum class VaultMovementMode(val title: String, val actionLabel: String) {
    APORTE("APORTAR NA CAIXINHA", "Confirmar aporte"),
    RETIRADA("RETIRAR DA CAIXINHA", "Confirmar retirada"),
    TRANSFERENCIA("TRANSFERIR ENTRE CAIXINHAS", "Confirmar transferência"),
    AJUSTE("AJUSTAR SALDO", "Confirmar ajuste")
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
    var feedbackError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
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
                        text = mode.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = OrangeNeon,
                        letterSpacing = 0.8.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Text(
                    text = "${vault.name} • Saldo atual: ${currentBalance.formatCurrency()}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (mode == VaultMovementMode.TRANSFERENCIA) {
                item {
                    if (otherVaults.isEmpty()) {
                        Text(
                            text = "Cadastre outra caixinha ativa para realizar transferências.",
                            color = RedAlert,
                            fontSize = 13.sp
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = targetExpanded,
                            onExpandedChange = { targetExpanded = !targetExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedTargetVault?.name ?: "Selecione o destino",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Caixinha de destino *") },
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
                            feedbackError = "O saldo ajustado não pode ser negativo"
                            return@OutlinedTextField
                        }
                        val cleaned = cleanCurrencyInput(raw)
                        val parsed = if (cleaned.isBlank()) BigDecimal.ZERO else parseCurrency(cleaned)
                        if (isWithdrawalLike && parsed > currentBalance) {
                            feedbackError = "O valor máximo para retirada é o saldo existente (${currentBalance.formatCurrency()})"
                            return@OutlinedTextField
                        }
                        amountText = cleaned
                        feedbackError = null
                        infoMessage = null
                    },
                    label = {
                        Text(
                            if (mode == VaultMovementMode.AJUSTE) "Saldo correto (R$) *"
                            else "Valor (R$) *"
                        )
                    },
                    placeholder = { Text("0,00") },
                    supportingText = if (isWithdrawalLike) {
                        { Text("Máximo disponível: ${currentBalance.formatCurrency()}") }
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
                    label = { Text("Observação (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (feedbackError != null) {
                item {
                    Text(
                        text = feedbackError.orEmpty(),
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (infoMessage != null) {
                item {
                    Text(
                        text = infoMessage.orEmpty(),
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
                            feedbackError = "O saldo ajustado não pode ser negativo"
                            return@Button
                        }
                        val parsed = parseCurrency(trimmed)
                        if (isWithdrawalLike && parsed > currentBalance) {
                            feedbackError = "O valor máximo para retirada é o saldo existente (${currentBalance.formatCurrency()})"
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
                        text = if (isSubmitting) "Processando..." else mode.actionLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
