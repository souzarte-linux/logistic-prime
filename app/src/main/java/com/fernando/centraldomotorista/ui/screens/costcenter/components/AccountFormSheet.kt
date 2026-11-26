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
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.ui.screens.costcenter.ActionOutcome
import com.fernando.centraldomotorista.ui.screens.costcenter.UiText
import com.fernando.centraldomotorista.ui.screens.costcenter.asString
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    existing: BankAccount?,
    isSubmitting: Boolean,
    onSave: suspend (
        name: String,
        institution: String?,
        agency: String?,
        lastDigits: String?,
        accountType: AccountType?,
        informedBalance: BigDecimal?
    ) -> ActionOutcome,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var institution by rememberSaveable(existing?.id) { mutableStateOf(existing?.institution.orEmpty()) }
    var agency by rememberSaveable(existing?.id) { mutableStateOf(existing?.agency.orEmpty()) }
    var lastDigits by rememberSaveable(existing?.id) { mutableStateOf(existing?.accountLastDigits.orEmpty()) }
    var selectedType by rememberSaveable(existing?.id) { mutableStateOf(existing?.accountType) }
    var informedBalanceText by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.informedBalance?.toPlainString()?.replace('.', ',').orEmpty()
        )
    }
    var typeExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<UiText?>(null) }
    val scope = rememberCoroutineScope()

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
                            if (existing == null) R.string.account_form_title_new else R.string.account_form_title_edit
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
                    label = { Text(stringResource(R.string.account_form_name_label)) },
                    placeholder = { Text(stringResource(R.string.account_form_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text(stringResource(R.string.account_form_institution_label)) },
                    placeholder = { Text(stringResource(R.string.account_form_institution_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = agency,
                        onValueChange = { agency = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) },
                        label = { Text(stringResource(R.string.account_form_agency_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = lastDigits,
                        onValueChange = { input ->
                            lastDigits = input.filter { it.isDigit() }.take(6)
                            errorText = null
                        },
                        label = { Text(stringResource(R.string.account_form_digits_label)) },
                        placeholder = { Text(stringResource(R.string.account_form_digits_placeholder)) },
                        supportingText = { Text(stringResource(R.string.account_form_digits_supporting)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType?.label ?: stringResource(R.string.account_form_type_none),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.account_form_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_form_type_none)) },
                            onClick = {
                                selectedType = null
                                typeExpanded = false
                            }
                        )
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = informedBalanceText,
                    onValueChange = { input ->
                        informedBalanceText = cleanCurrencyInput(input)
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.account_form_balance_label)) },
                    placeholder = { Text(stringResource(R.string.cost_center_currency_placeholder)) },
                    supportingText = { Text(stringResource(R.string.account_form_balance_supporting)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
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
                        val trimmedBalance = informedBalanceText.trim()
                        val parsedBalance = if (trimmedBalance.isEmpty()) {
                            null
                        } else {
                            parseCurrency(trimmedBalance)
                        }
                        scope.launch {
                            when (val outcome = onSave(
                                name,
                                institution,
                                agency,
                                lastDigits,
                                selectedType,
                                parsedBalance
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
                            if (isSubmitting) R.string.cost_center_saving else R.string.account_form_save
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
