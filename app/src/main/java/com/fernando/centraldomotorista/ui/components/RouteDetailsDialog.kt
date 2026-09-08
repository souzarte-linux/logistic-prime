package com.fernando.centraldomotorista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.GreenNeon
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
fun RouteDetailsDialog(
    route: Route,
    platformName: String? = null,
    onDismiss: () -> Unit,
    onEdit: (Route) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val localOccurred = route.occurredAt.atZoneSameInstant(zone)
    val localStarted = route.startedAt?.atZoneSameInstant(zone)
    val localEnded = route.endedAt?.atZoneSameInstant(zone)

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

    // Cálculo do tempo trabalhado
    val workedTimeStr: String = if (localStarted != null && localEnded != null) {
        val ms = route.endedAt!!.toInstant().toEpochMilli() - route.startedAt!!.toInstant().toEpochMilli()
        val totalMins = maxOf(0L, (ms / 60000) - route.breakMinutes)
        val h = totalMins / 60
        val m = totalMins % 60
        String.format(Locale.getDefault(), "%02dh %02dmin", h, m)
    } else {
        "—"
    }

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
                // 1. Cabeçalho com Título, Tag de Plataforma e Botão de Editar no Topo Direito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = OrangeNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = (platformName ?: "ROTA").uppercase(),
                                    color = OrangeNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            val origin = route.origin?.ifBlank { "Origem" } ?: "Origem"
                            val destination = route.destination?.ifBlank { "Destino" } ?: "Destino"
                            Text(
                                text = "$origin ➔ $destination",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Botão "EDITAR" na parte direita superior
                    Button(
                        onClick = {
                            onDismiss()
                            onEdit(route)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EDITAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 2. Card de Valor Total
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "VALOR TOTAL DA ROTA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = route.amount.formatCurrency(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangeNeon
                        )

                        // Discriminativo de adicionais
                        if (route.tip > BigDecimal.ZERO || route.bonus > BigDecimal.ZERO) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (route.tip > BigDecimal.ZERO) {
                                    Text(
                                        text = "+ Gorjeta: ${route.tip.formatCurrency()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenNeon
                                    )
                                }
                                if (route.bonus > BigDecimal.ZERO) {
                                    Text(
                                        text = "+ Bônus: ${route.bonus.formatCurrency()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenNeon
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Seção: Data e Horários
                DetailSection(title = "DATA & HORÁRIOS", icon = Icons.Default.CalendarToday) {
                    DetailRow(label = "Data da Rota", value = localOccurred.format(dateFormatter))
                    DetailRow(
                        label = "Horário",
                        value = if (localStarted != null && localEnded != null) {
                            "${localStarted.format(timeFormatter)} às ${localEnded.format(timeFormatter)}"
                        } else {
                            "—"
                        }
                    )
                    DetailRow(label = "Tempo Trabalhado", value = workedTimeStr)
                    if (route.breakMinutes > 0) {
                        DetailRow(label = "Tempo de Pausa", value = "${route.breakMinutes} minutos")
                    }
                }

                // 4. Seção: Pacotes e Entregas
                val totalPkgs = (route.smallPackagesCount + route.largePackagesCount).let {
                    if (it > 0) it else route.packageCount
                }
                DetailSection(title = "PACOTES & VOLUMES", icon = Icons.Default.Inventory2) {
                    DetailRow(label = "Total de Pacotes", value = "$totalPkgs pacotes")
                    if (route.smallPackagesCount > 0) {
                        val smallSubtotal = BigDecimal(route.smallPackagesCount).multiply(route.packageUnitPrice)
                        DetailRow(
                            label = "Pacotes Pequenos",
                            value = "${route.smallPackagesCount}x (${route.packageUnitPrice.formatCurrency()}) = ${smallSubtotal.formatCurrency()}"
                        )
                    }
                    if (route.largePackagesCount > 0) {
                        val largeTotal = route.largePackagesPrices.fold(BigDecimal.ZERO, BigDecimal::add)
                        DetailRow(
                            label = "Pacotes Grandes / Volumosos",
                            value = "${route.largePackagesCount} volumes = ${largeTotal.formatCurrency()}"
                        )
                    }
                    DetailRow(label = "Tipo de Produto", value = route.productType.replaceFirstChar { it.uppercase() })
                }

                // 5. Seção: Deslocamento e Odômetro
                DetailSection(title = "DISTÂNCIA & ODÔMETRO", icon = Icons.Default.DirectionsCar) {
                    DetailRow(label = "Distância Percorrida", value = "${route.distanceKm} KM")
                    if (route.startKm > BigDecimal.ZERO || route.endKm > BigDecimal.ZERO) {
                        DetailRow(
                            label = "Odômetro",
                            value = "${route.startKm} KM (Inicial) ➔ ${route.endKm} KM (Final)"
                        )
                    }
                }

                // 6. Observações (se houver)
                if (!route.notes.isNullOrBlank()) {
                    DetailSection(title = "OBSERVAÇÕES", icon = Icons.Default.Notes) {
                        Text(
                            text = route.notes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
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
                    tint = OrangeNeon,
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
