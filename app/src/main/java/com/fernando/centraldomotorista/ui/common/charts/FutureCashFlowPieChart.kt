package com.fernando.centraldomotorista.ui.common.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.screens.relatorios.PlatformReceivableShare
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.util.Locale

@Composable
fun FutureCashFlowPieChart(
    platforms: List<PlatformReceivableShare>,
    totalAmount: BigDecimal,
    modifier: Modifier = Modifier
) {
    if (platforms.isEmpty() || totalAmount <= BigDecimal.ZERO) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nenhuma fatura a receber na aba 'Ciclo A Vencer'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedPlatformKey by remember { mutableStateOf<String?>(null) }

    val slices = remember(platforms) {
        platforms.map { p ->
            DonutSlice(
                key = p.platformId.ifBlank { p.platformName },
                name = p.platformName,
                value = p.amount,
                color = p.color,
                formattedValue = p.amount.formatCurrency()
            )
        }
    }

    val selectedItem = remember(selectedPlatformKey, platforms) {
        platforms.find { (it.platformId.ifBlank { it.platformName }) == selectedPlatformKey }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gráfico Donut (Pizza) à esquerda com suporte a clique e borda de destaque
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                slices = slices,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 20.dp,
                selectedSliceKey = selectedPlatformKey,
                onSliceClick = { slice ->
                    selectedPlatformKey = if (selectedPlatformKey == slice.key) null else slice.key
                },
                centerLabel = if (selectedItem != null) "PLATAFORMA" else "A RECEBER",
                centerValue = selectedItem?.amount?.formatCurrency() ?: totalAmount.formatCurrency()
            )
        }

        // Legenda detalhada ao lado do gráfico com nome, valor e percentual
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { item ->
                val itemKey = item.platformId.ifBlank { item.platformName }
                val isSelected = selectedPlatformKey == itemKey

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlatformKey = if (isSelected) null else itemKey
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) item.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) {
                        BorderStroke(1.5.dp, item.color)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(item.color, CircleShape)
                            )
                            Text(
                                text = item.platformName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = item.amount.formatCurrency(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${String.format(Locale("pt", "BR"), "%.1f", item.percentage)}% do total",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
