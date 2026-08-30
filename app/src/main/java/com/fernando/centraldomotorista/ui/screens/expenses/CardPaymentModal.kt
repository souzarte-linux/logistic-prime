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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CardPaymentData(
    val cardId: String,
    val cardBrand: String? = null,
    val cardOperator: String? = null,
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
    initialData: CardPaymentData?,
    purchaseDate: LocalDateTime = LocalDateTime.now(),
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

    var isInstallment by remember {
        mutableStateOf(initialData?.isInstallment ?: false)
    }

    var installmentsCountText by remember {
        mutableStateOf(initialData?.installmentTotal?.toString() ?: "2")
    }

    // Cálculo automático da 1ª Parcela com base na data da compra e dia de fechamento do cartão
    val computedFirstInstallmentMonth = remember(selectedCard, purchaseDate) {
        if (selectedCard != null) {
            val purchaseDay = purchaseDate.dayOfMonth
            val closingDay = selectedCard.closingDay
            val targetMonth = if (purchaseDay < closingDay) purchaseDate else purchaseDate.plusMonths(1)
            targetMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"))
        } else {
            purchaseDate.format(DateTimeFormatter.ofPattern("MM/yyyy"))
        }
    }

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

            // 1. Seletor de Cartão com Botão "+" Fixo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeCards.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    onNavigateToManageCards()
                                },
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Nenhum cartão cadastrado",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Toque no '+' ao lado para cadastrar",
                                    color = OrangeNeon,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        val currentDisplay = selectedCard?.let { "${it.nickname} (•••• ${it.lastFour})" } ?: "Selecione o Cartão *"
                        ExposedDropdownMenuBox(
                            expanded = cardExpanded,
                            onExpandedChange = { cardExpanded = !cardExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cartão Cadastrado *") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = OrangeNeon) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = cardExpanded,
                                onDismissRequest = { cardExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                activeCards.forEach { card ->
                                    val brandIssuerText = listOfNotNull(card.brandName, card.issuerName).joinToString(" • ")
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("${card.nickname} •••• ${card.lastFour}", color = Color.White, fontWeight = FontWeight.Bold)
                                                if (brandIssuerText.isNotBlank()) {
                                                    Text(brandIssuerText, color = OrangeNeon, fontSize = 11.sp)
                                                }
                                                Text("Fechamento: Dia ${card.closingDay} • Vencimento: Dia ${card.dueDay}", color = Color.Gray, fontSize = 11.sp)
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

                    // Botão "+" Fixo para cadastrar ou gerenciar cartões
                    IconButton(
                        onClick = {
                            onDismiss()
                            onNavigateToManageCards()
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Novo Cartão / Gerenciar",
                            tint = OrangeNeon,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 2. Informação Somente Leitura da Bandeira e Emissor do Cartão Selecionado
            if (selectedCard != null) {
                val brandIssuerInfo = listOfNotNull(
                    selectedCard.brandName?.takeIf { it.isNotBlank() },
                    selectedCard.issuerName?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                if (brandIssuerInfo.isNotBlank()) {
                    item {
                        Surface(
                            color = SurfaceDarkAlt.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Bandeira / Emissor: $brandIssuerInfo",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Toggle "À Vista" / "A Prazo"
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

            // 4. Se "A Prazo": Parcelas (editável) e 1ª Parcela (somente leitura / calculado)
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = computedFirstInstallmentMonth,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("1ª Parcela (Mês/Ano)") },
                            singleLine = true,
                            supportingText = {
                                val closing = selectedCard?.closingDay
                                Text(
                                    text = if (closing != null) "Fechamento: Dia $closing" else "Automático",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = GreenNeon,
                                unfocusedTextColor = GreenNeon,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            // 5. Vencimento Fatura Cartão (Somente Visualização / Não editável)
            item {
                val dueDayDisplay = selectedCard?.let { "Todo dia ${it.dueDay} de cada mês" } ?: "Selecione um cartão acima"
                OutlinedTextField(
                    value = dueDayDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Venc. Fatura Cartão") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = OrangeNeon,
                        unfocusedTextColor = Color.LightGray,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Botão Confirmar
            item {
                Button(
                    onClick = {
                        val dueDay = selectedCard?.dueDay ?: 10
                        val totalInst = if (isInstallment) (installmentsCountText.toIntOrNull() ?: 1) else 1
                        val groupId = if (isInstallment) UUID.randomUUID().toString() else null

                        val result = CardPaymentData(
                            cardId = selectedCardId,
                            cardBrand = selectedCard?.brandName,
                            cardOperator = selectedCard?.issuerName,
                            cardDueDay = dueDay,
                            isInstallment = isInstallment,
                            installmentTotal = if (isInstallment) totalInst else null,
                            firstInstallmentMonth = if (isInstallment) computedFirstInstallmentMonth else null,
                            installmentGroupId = groupId
                        )
                        onConfirm(result)
                    },
                    enabled = selectedCard != null,
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
}
