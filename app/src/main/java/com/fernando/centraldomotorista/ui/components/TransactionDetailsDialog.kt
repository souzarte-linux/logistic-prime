package com.fernando.centraldomotorista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import com.fernando.centraldomotorista.ui.theme.BlueInfo
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
    onEdit: (TransactionItem) -> Unit
) {
    val isPositive = transaction.type == TransactionType.GANHO
    val categoryColor = when {
        transaction.sourceType == TransactionSourceType.DAILY_TOTAL -> OrangeNeon
        isPositive -> GreenNeon
        else -> {
            val cat = transaction.rawExpense?.category?.lowercase() ?: transaction.category.lowercase()
            when {
                cat.contains("combustivel") || cat.contains("abastecimento") -> Color(0xFFFFB300)
                cat.contains("manutencao") || cat.contains("peca") -> BlueInfo
                cat.contains("alimentacao") -> GreenNeon
                else -> RedAlert
            }
        }
    }

    val zone = ZoneId.systemDefault()
    val localOccurred = transaction.occurredAt.atZoneSameInstant(zone)
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // =========================================================================
                // TOPO COM CATEGORIA/TIPO EM RETÂNGULO, TÍTULO E BOTÃO DE FECHAR E EDITAR
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Categoria/Tipo em retângulo centralizado
                        Surface(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = transaction.category.uppercase(),
                                color = categoryColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Botão Fechar à esquerda
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Botão Editar sem texto à direita (ícone lápis)
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEdit(transaction)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OrangeNeon,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Color.Black,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Título da Transação
                    Text(
                        text = transaction.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // =========================================================================
                // CARD DE VALOR (Padrão DetailSection: Ícone + Título, Linha, Conteúdo)
                // =========================================================================
                val cardTitle = if (transaction.sourceType == TransactionSourceType.DAILY_TOTAL) {
                    "VALOR TOTAL DO DIA"
                } else {
                    if (isPositive) "VALOR DA RECEITA" else "VALOR DA DESPESA"
                }
                val cardIcon = if (isPositive) Icons.Default.Payments else Icons.Default.AccountBalanceWallet

                DetailSection(title = cardTitle, icon = cardIcon, accentColor = categoryColor) {
                    Text(
                        text = "${if (isPositive) "+" else "-"}${transaction.amount.formatCurrency()}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPositive) OrangeNeon else RedAlert
                    )

                    if (transaction.sourceType == TransactionSourceType.DAILY_TOTAL && transaction.subtractRoutes) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Líquido: ${transaction.netAmount.formatCurrency()} (rotas descontadas)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )
                    }
                }

                // =========================================================================
                // SEÇÃO: DATA & HORÁRIO
                // =========================================================================
                DetailSection(title = "DATA & HORÁRIO", icon = Icons.Default.CalendarToday, accentColor = categoryColor) {
                    DetailRow(label = "Data", value = localOccurred.format(dateFormatter))
                    DetailRow(label = "Horário", value = localOccurred.format(timeFormatter))
                }

                // =========================================================================
                // SEÇÕES ESPECÍFICAS CONFORME O TIPO
                // =========================================================================
                when (transaction.sourceType) {
                    TransactionSourceType.DAILY_TOTAL -> {
                        val daily = transaction.rawDailyTotal
                        DetailSection(title = "DETALHES DA DIÁRIA", icon = Icons.Default.DateRange, accentColor = categoryColor) {
                            if (!transaction.establishment.isNullOrBlank()) {
                                DetailRow(label = "Empresa / Plataforma", value = transaction.establishment)
                            }
                            if (daily != null && daily.distanceKm > BigDecimal.ZERO) {
                                DetailRow(label = "Distância Percorrida", value = "${daily.distanceKm} KM")
                            }
                            if (daily != null) {
                                DetailRow(label = "Tipo de Produto", value = daily.productType.replaceFirstChar { it.uppercase() })
                                DetailRow(label = "Descontar Rotas", value = if (daily.subtractRoutes) "Sim" else "Não")
                            }
                        }

                        if (!daily?.notes.isNullOrBlank()) {
                            DetailSection(title = "OBSERVAÇÕES", icon = Icons.AutoMirrored.Filled.Notes, accentColor = categoryColor) {
                                Text(
                                    text = daily.notes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    TransactionSourceType.EXPENSE -> {
                        val expense = transaction.rawExpense
                        val cat = expense?.category?.lowercase() ?: transaction.category.lowercase()

                        when {
                            // 1. Combustível / Abastecimento
                            cat.contains("combustivel") || cat.contains("abastecimento") -> {
                                DetailSection(title = "DETALHES DO ABASTECIMENTO", icon = Icons.Default.LocalGasStation, accentColor = categoryColor) {
                                    val posto = expense?.vendor ?: transaction.establishment
                                    if (!posto.isNullOrBlank()) {
                                        DetailRow(label = "Posto", value = posto)
                                    }
                                    if (!expense?.fuelType.isNullOrBlank()) {
                                        DetailRow(label = "Combustível", value = expense.fuelType.uppercase())
                                    }
                                    if (expense?.liters != null && expense.liters > BigDecimal.ZERO) {
                                        DetailRow(label = "Litros", value = "${expense.liters} L")
                                    }
                                    if (expense?.pricePerLiter != null && expense.pricePerLiter > BigDecimal.ZERO) {
                                        DetailRow(label = "Preço por Litro", value = expense.pricePerLiter.formatCurrency())
                                    }
                                    if (expense?.odometerKm != null && expense.odometerKm > BigDecimal.ZERO) {
                                        DetailRow(label = "Odômetro", value = "${expense.odometerKm} KM")
                                    }
                                    DetailRow(label = "Tanque Cheio", value = if (expense?.isFullTank == true) "Sim" else "Não")
                                }
                            }

                            // 2. Peças / Manutenção Mecânica
                            cat.contains("manutencao") || cat.contains("peca") -> {
                                DetailSection(title = "DETALHES DA MANUTENÇÃO", icon = Icons.Default.Build, accentColor = categoryColor) {
                                    val oficina = expense?.vendor ?: transaction.establishment
                                    if (!oficina.isNullOrBlank()) {
                                        DetailRow(label = "Oficina / Loja", value = oficina)
                                    }
                                    if (!expense?.partBrand.isNullOrBlank()) {
                                        DetailRow(label = "Marca da Peça", value = expense.partBrand)
                                    }
                                    if (!expense?.partModel.isNullOrBlank()) {
                                        DetailRow(label = "Modelo", value = expense.partModel)
                                    }
                                    if (expense?.odometerKm != null && expense.odometerKm > BigDecimal.ZERO) {
                                        DetailRow(label = "Odômetro na Troca", value = "${expense.odometerKm} KM")
                                    }
                                }
                            }

                            // 3. Alimentação
                            cat.contains("alimentacao") -> {
                                DetailSection(title = "DETALHES DA REFEIÇÃO", icon = Icons.Default.Restaurant, accentColor = categoryColor) {
                                    val restaurante = expense?.vendor ?: transaction.establishment
                                    if (!restaurante.isNullOrBlank()) {
                                        DetailRow(label = "Local / Estabelecimento", value = restaurante)
                                    }
                                    if (!expense?.mealType.isNullOrBlank()) {
                                        DetailRow(label = "Tipo de Refeição", value = expense.mealType.replaceFirstChar { it.uppercase() })
                                    }
                                }
                            }

                            // 4. Outras Despesas
                            else -> {
                                DetailSection(title = "DETALHES DA DESPESA", icon = Icons.Default.Receipt, accentColor = categoryColor) {
                                    val local = expense?.vendor ?: transaction.establishment
                                    if (!local.isNullOrBlank()) {
                                        DetailRow(label = "Estabelecimento", value = local)
                                    }
                                    if (!expense?.description.isNullOrBlank()) {
                                        DetailRow(label = "Descrição", value = expense.description)
                                    }
                                }
                            }
                        }

                        // Forma de Pagamento
                        if (expense != null) {
                            DetailSection(title = "PAGAMENTO & DOCUMENTO", icon = Icons.Default.CreditCard, accentColor = categoryColor) {
                                DetailRow(label = "Forma de Pagamento", value = expense.paymentMethod.uppercase())
                                if (expense.installmentTotal != null && expense.installmentTotal > 1) {
                                    DetailRow(label = "Parcelamento", value = "${expense.installmentNumber ?: 1} de ${expense.installmentTotal}")
                                }
                                if (!expense.cardBrand.isNullOrBlank()) {
                                    DetailRow(label = "Bandeira do Cartão", value = expense.cardBrand)
                                }
                                if (!expense.receiptNumber.isNullOrBlank()) {
                                    DetailRow(label = "Nº do Cupom", value = expense.receiptNumber)
                                }
                                if (!expense.invoiceNumber.isNullOrBlank()) {
                                    DetailRow(label = "Nota Fiscal", value = expense.invoiceNumber)
                                }
                            }
                        }

                        // Observações adicionais
                        if (!expense?.description.isNullOrBlank() && !cat.contains("outr")) {
                            DetailSection(title = "OBSERVAÇÕES", icon = Icons.AutoMirrored.Filled.Notes, accentColor = categoryColor) {
                                Text(
                                    text = expense.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    TransactionSourceType.ROUTE -> {
                        // Caso acidentalmente caia aqui
                        DetailSection(title = "INFORMAÇÃO", icon = Icons.Default.Info, accentColor = categoryColor) {
                            DetailRow(label = "Tipo", value = "Rota de Entrega")
                        }
                    }
                }

                // Botão Fechar
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    icon: ImageVector,
    accentColor: Color = OrangeNeon,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
