package com.fernando.centraldomotorista.ui.screens.relatorios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

enum class KpiTone {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    DESTRUCTIVE
}

@Composable
fun RelatoriosKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: KpiTone = KpiTone.DEFAULT,
    hint: String? = null,
    onClick: (() -> Unit)? = null
) {
    var showHintDialog by remember { mutableStateOf(false) }

    val valueColor = when (tone) {
        KpiTone.PRIMARY -> OrangeNeon
        KpiTone.SUCCESS -> GreenNeon
        KpiTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
        KpiTone.DEFAULT -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = OrangeNeon,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!hint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable { showHintDialog = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = valueColor,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showHintDialog && !hint.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("OK", color = OrangeNeon, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RelatoriosSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título no topo e ao centro, na cor laranja neon e em maiúsculo, com ícone alinhado ao início da primeira linha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = OrangeNeon,
                    textAlign = TextAlign.Center
                )
            }

            // Qualquer outro elemento (como botões de ação ou filtros específicos) deve estar ABAIXO
            if (action != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    action()
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
fun PlacesRankingCard(
    title: String,
    items: List<RankedPlaceItem>,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    RelatoriosSectionCard(title = title, icon = icon, modifier = modifier) {
        if (items.isEmpty()) {
            Text(
                text = "Lance rotas com origem/destino.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            return@RelatoriosSectionCard
        }

        val maxCount = items.maxOfOrNull { it.count } ?: 1
        val safeMax = if (maxCount > 0) maxCount else 1

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                val fraction = (item.count.toFloat() / safeMax.toFloat()).coerceIn(0.05f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(500),
                    label = "rankingBarFraction"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${item.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(OrangeNeon, Color(0xFFFFB300))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceScheduleCard(
    item: MaintScheduleItem,
    modifier: Modifier = Modifier
) {
    var showTooltipDialog by remember { mutableStateOf(false) }

    val (statusLabel, statusColor, statusBg, statusIcon) = when (item.status) {
        MaintScheduleStatus.CRITICAL -> Quadruple(
            "Atrasado",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            Icons.Default.Warning
        )
        MaintScheduleStatus.WARN -> Quadruple(
            "Atenção",
            Color(0xFFFFB300),
            Color(0xFFFFB300).copy(alpha = 0.12f),
            Icons.Default.Warning
        )
        MaintScheduleStatus.OK -> Quadruple(
            "OK",
            GreenNeon,
            GreenNeon.copy(alpha = 0.12f),
            Icons.Default.CheckCircle
        )
    }

    val isAlert = item.status == MaintScheduleStatus.CRITICAL || item.status == MaintScheduleStatus.WARN

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = statusBg,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Modo exclusivamente exibição: sem botão de edição.
                // Ícone de alerta com tooltip dialog ao clicar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (isAlert) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showTooltipDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    } else {
                        Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    }
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = if (isAlert) "Toque para ver aviso de manutenção" else null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ÚLTIMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.lastKm.toInt()} km",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = "PRÓXIMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.nextKm.toInt()} km",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RESTANTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val remainingText = if (item.remainingKm <= BigDecimal.ZERO) {
                        "-${item.remainingKm.abs().toInt()} km"
                    } else {
                        "${item.remainingKm.toInt()} km"
                    }
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }
        }
    }

    if (showTooltipDialog) {
        val alertTitle = when (item.status) {
            MaintScheduleStatus.CRITICAL -> "Atenção: Manutenção Atrasada"
            MaintScheduleStatus.WARN -> "Alerta: Manutenção Próxima"
            MaintScheduleStatus.OK -> "Manutenção em Dia"
        }
        val alertMessage = when (item.status) {
            MaintScheduleStatus.CRITICAL ->
                "A vida útil recomendada para \"${item.name}\" foi ultrapassada!\n\n" +
                "• Próxima troca prevista: ${item.nextKm.toInt()} km\n" +
                "• Situação: Atrasado em ${(-item.remainingKm.toInt()).coerceAtLeast(0)} km\n\n" +
                "É necessária a realização da manutenção preventiva desta peça/serviço para segurança e bom funcionamento do veículo."
            MaintScheduleStatus.WARN ->
                "A vida útil recomendada para \"${item.name}\" está próxima do limite.\n\n" +
                "• Próxima troca prevista: ${item.nextKm.toInt()} km\n" +
                "• Restam apenas: ${item.remainingKm.toInt()} km\n\n" +
                "Programe a manutenção preventiva desta peça/serviço em breve."
            MaintScheduleStatus.OK ->
                "O item \"${item.name}\" está dentro do período regular de uso.\n\n" +
                "• Próxima troca prevista: ${item.nextKm.toInt()} km\n" +
                "• Restam: ${item.remainingKm.toInt()} km"
        }

        AlertDialog(
            onDismissRequest = { showTooltipDialog = false },
            icon = {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = alertTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = alertMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showTooltipDialog = false }) {
                    Text("OK, ENTENDI", color = OrangeNeon, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun BonificacoesCard(
    group: PlatformAdjustmentGroup,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Nome da plataforma + Totais de descontos e acréscimos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (group.descontosTotal > BigDecimal.ZERO) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "-${group.descontosTotal.formatCurrency()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (group.acrescimosTotal > BigDecimal.ZERO) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = GreenNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+${group.acrescimosTotal.formatCurrency()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Itens de detalhe discriminados
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                group.details.forEach { det ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = det.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (det.isDiscount) "-" else "+"}${det.amount.formatCurrency()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (det.isDiscount) MaterialTheme.colorScheme.error else GreenNeon
                            )
                        }
                    }
                }
            }
        }
    }
}
