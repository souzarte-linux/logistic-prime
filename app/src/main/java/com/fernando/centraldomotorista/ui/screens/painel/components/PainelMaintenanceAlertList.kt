package com.fernando.centraldomotorista.ui.screens.painel.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.screens.painel.PartMaintenanceAlertItem
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.YellowGold
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PainelMaintenanceAlertList(
    alerts: List<PartMaintenanceAlertItem>,
    onResetClick: (PartMaintenanceAlertItem) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        alerts.forEach { item ->
            val alertColor = if (item.isOverdue) RedAlert else YellowGold
            val bgColor = alertColor.copy(alpha = 0.08f)
            val strokeColor = alertColor.copy(alpha = 0.35f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.dp, strokeColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(alertColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.isOverdue) Icons.Default.Warning else Icons.Default.Build,
                                contentDescription = null,
                                tint = alertColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.isOverdue) {
                                    "MANUTENÇÃO ATRASADA: ${item.part.partName.uppercase()}"
                                } else {
                                    "TROCA DE PEÇA PRÓXIMA: ${item.part.partName.uppercase()}"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = alertColor,
                                letterSpacing = 0.4.sp
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            val kmDescription = if (item.isOverdue) {
                                "Você ultrapassou em ${item.overdueKm.setScale(0, RoundingMode.HALF_UP).toPlainString()} KM o limite de ${item.lifeKm.setScale(0, RoundingMode.HALF_UP).toPlainString()} KM."
                            } else {
                                "Faltam ${item.remainingKm.setScale(0, RoundingMode.HALF_UP).toPlainString()} KM para o limite de ${item.lifeKm.setScale(0, RoundingMode.HALF_UP).toPlainString()} KM."
                            }

                            Text(
                                text = kmDescription,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barra de desgaste
                    val progressFraction = (item.wearPercentage.toFloat() / 100f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(alertColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botões de ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onResetClick(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MARCAR TROCA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToHistory,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HISTÓRICO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResetPartMaintenanceDialog(
    item: PartMaintenanceAlertItem,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    val estimatedKm = (item.lastChangeKm.add(item.drivenKm)).setScale(0, RoundingMode.HALF_UP)
    var kmText by remember { mutableStateOf(estimatedKm.toPlainString()) }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Registrar Troca de Peça",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Informe o odômetro (KM) no momento da troca de \"${item.part.partName}\":",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = kmText,
                    onValueChange = {
                        kmText = it.filter { ch -> ch.isDigit() }
                        inputError = null
                    },
                    label = { Text("Quilometragem (KM)") },
                    isError = inputError != null,
                    supportingText = inputError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = kmText.toBigDecimalOrNull()
                    if (parsed == null || parsed <= BigDecimal.ZERO) {
                        inputError = "Informe uma quilometragem válida"
                    } else {
                        onConfirm(parsed)
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancelar")
            }
        }
    )
}
