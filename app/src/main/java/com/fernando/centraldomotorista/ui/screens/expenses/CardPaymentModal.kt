package com.fernando.centraldomotorista.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CardPaymentData(
    val cardId: String,
    val cardBrand: String?,
    val cardOperator: String?,
    val cardDueDay: Int?,
    val isInstallment: Boolean,
    val installmentTotal: Int?,
    val firstInstallmentMonth: String?,
    val installmentGroupId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPaymentModal(
    availableCards: List<CreditCard>,
    availableBrands: List<CardBrand>,
    availableOperators: List<CardOperator>,
    initialData: CardPaymentData?,
    onAddBrand: (String) -> Unit,
    onAddOperator: (String) -> Unit,
    onNavigateToManageCards: () -> Unit,
    onConfirm: (CardPaymentData) -> Unit,
    onDismiss: () -> Unit
) {
    val activeCards = remember(availableCards) { availableCards.filter { it.active } }

    var selectedCardId by remember {
        mutableStateOf(initialData?.cardId ?: activeCards.firstOrNull()?.id ?: "")
    }

    val selectedCard = remember(selectedCardId, activeCards) {
        activeCards.firstOrNull { it.id == selectedCardId }
    }

    var selectedBrandId by remember {
        mutableStateOf(
            selectedCard?.brandId ?: availableBrands.firstOrNull()?.id
        )
    }

    var selectedIssuerId by remember {
        mutableStateOf(
            selectedCard?.issuerId ?: availableOperators.firstOrNull()?.id
        )
    }

    var isInstallment by remember {
        mutableStateOf(initialData?.isInstallment ?: false)
    }

    var installmentsCountText by remember {
        mutableStateOf(initialData?.installmentTotal?.toString() ?: "2")
    }

    val currentMonthYear = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"))
    }
    var firstInstallmentMonthText by remember {
        mutableStateOf(initialData?.firstInstallmentMonth ?: currentMonthYear)
    }

    var dueDayText by remember {
        mutableStateOf(initialData?.cardDueDay?.toString() ?: selectedCard?.dueDay?.toString() ?: "10")
    }

    // Update due day when selected card changes
    LaunchedEffect(selectedCardId) {
        selectedCard?.let { card ->
            dueDayText = card.dueDay.toString()
            if (card.brandId != null) selectedBrandId = card.brandId
            if (card.issuerId != null) selectedIssuerId = card.issuerId
        }
    }

    var showAddBrandDialog by remember { mutableStateOf(false) }
    var showAddOperatorDialog by remember { mutableStateOf(false) }

    var brandExpanded by remember { mutableStateOf(false) }
    var issuerExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DADOS DO CARTÃO",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = OrangeNeon,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text("Fechar", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // 1. Bandeira (Dropdown + Botão "+")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val brandName = availableBrands.firstOrNull { it.id == selectedBrandId }?.name ?: "Selecione a Bandeira"
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = !brandExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = brandName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bandeira") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            availableBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand.name, color = Color.White) },
                                    onClick = {
                                        selectedBrandId = brand.id
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showAddBrandDialog = true },
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Nova Bandeira", tint = OrangeNeon)
                    }
                }
            }

            // 2. Emissor (Dropdown + Botão "+")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val operatorName = availableOperators.firstOrNull { it.id == selectedIssuerId }?.name ?: "Selecione o Emissor"
                    ExposedDropdownMenuBox(
                        expanded = issuerExpanded,
                        onExpandedChange = { issuerExpanded = !issuerExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = operatorName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Emissor / Banco") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issuerExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = issuerExpanded,
                            onDismissRequest = { issuerExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            availableOperators.forEach { op ->
                                DropdownMenuItem(
                                    text = { Text(op.name, color = Color.White) },
                                    onClick = {
                                        selectedIssuerId = op.id
                                        issuerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showAddOperatorDialog = true },
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Novo Emissor", tint = OrangeNeon)
                    }
                }
            }

            // 3. Seletor de Cartão
            item {
                if (activeCards.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhum cartão cadastrado",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = {
                                onDismiss()
                                onNavigateToManageCards()
                            }) {
                                Text("Cadastrar Cartão Agora ➔", color = OrangeNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val currentDisplay = selectedCard?.let { "${it.nickname} (•••• ${it.lastFour})" } ?: "Selecione um Cartão"
                    ExposedDropdownMenuBox(
                        expanded = cardExpanded,
                        onExpandedChange = { cardExpanded = !cardExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecione o Cartão Cadastrado *") },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = OrangeNeon) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = cardExpanded,
                            onDismissRequest = { cardExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            activeCards.forEach { card ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${card.nickname} •••• ${card.lastFour}", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Vencimento: Dia ${card.dueDay}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedCardId = card.id
                                        cardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Toggle "À Vista" / "A Prazo"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isInstallment = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isInstallment) OrangeNeon else SurfaceDarkAlt,
                            contentColor = if (!isInstallment) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("À Vista", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { isInstallment = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInstallment) OrangeNeon else SurfaceDarkAlt,
                            contentColor = if (isInstallment) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("A Prazo (Parcelado)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Se "A Prazo": Parcelas e 1ª Parcela
            if (isInstallment) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = installmentsCountText,
                            onValueChange = { installmentsCountText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Nº de Parcelas") },
                            placeholder = { Text("ex: 3") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = firstInstallmentMonthText,
                            onValueChange = { firstInstallmentMonthText = it.take(7) },
                            label = { Text("1ª Parcela (Mês/Ano)") },
                            placeholder = { Text("MM/AAAA") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Vencimento Fatura Cartão
            item {
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Venc. Fatura Cartão (Dia do mês)") },
                    placeholder = { Text("ex: 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 7. Botão Confirmar
            item {
                Button(
                    onClick = {
                        val brandName = availableBrands.firstOrNull { it.id == selectedBrandId }?.name
                        val operatorName = availableOperators.firstOrNull { it.id == selectedIssuerId }?.name
                        val dueDay = dueDayText.toIntOrNull() ?: selectedCard?.dueDay ?: 10
                        val totalInst = if (isInstallment) (installmentsCountText.toIntOrNull() ?: 1) else 1
                        val groupId = if (isInstallment) UUID.randomUUID().toString() else null

                        val result = CardPaymentData(
                            cardId = selectedCardId,
                            cardBrand = brandName,
                            cardOperator = operatorName,
                            cardDueDay = dueDay,
                            isInstallment = isInstallment,
                            installmentTotal = if (isInstallment) totalInst else null,
                            firstInstallmentMonth = if (isInstallment) firstInstallmentMonthText else null,
                            installmentGroupId = groupId
                        )
                        onConfirm(result)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Confirmar Dados do Cartão", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Dialog Novo Emissor
    if (showAddOperatorDialog) {
        var opName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddOperatorDialog = false },
            title = { Text("Cadastrar Novo Emissor", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = opName,
                    onValueChange = { opName = it },
                    label = { Text("Nome do Emissor / Banco") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddOperator(opName)
                        showAddOperatorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOperatorDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog Nova Bandeira
    if (showAddBrandDialog) {
        var bName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBrandDialog = false },
            title = { Text("Cadastrar Nova Bandeira", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = bName,
                    onValueChange = { bName = it },
                    label = { Text("Nome da Bandeira") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBrand(bName)
                        showAddBrandDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBrandDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = SurfaceDark
        )
    }
}
