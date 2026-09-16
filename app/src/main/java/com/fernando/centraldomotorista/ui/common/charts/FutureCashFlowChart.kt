package com.fernando.centraldomotorista.ui.common.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class FutureCashFlowItem(
    val date: String,
    val label: String,
    val amount: BigDecimal
)

private fun BigDecimal.formatBrl(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@Composable
fun FutureCashFlowChart(
    items: List<FutureCashFlowItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            text = "Nenhuma fatura em aberto com data de pagamento futura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val maxAmount = items.maxOfOrNull { it.amount } ?: BigDecimal.ONE
    val safeMax = if (maxAmount > BigDecimal.ZERO) maxAmount else BigDecimal.ONE
    val totalProjected = remember(items) {
        items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.amount) }
    }

    var selectedItem by remember { mutableStateOf<FutureCashFlowItem?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Container de Barras Verticais
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { item ->
                    val isSelected = selectedItem?.date == item.date
                    val fraction = item.amount
                        .divide(safeMax, 4, RoundingMode.HALF_UP)
                        .toFloat()
                        .coerceIn(0.08f, 1f)

                    val animatedHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(500),
                        label = "cashFlowBarHeight"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedHeight)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (isSelected) listOf(Color.White, OrangeNeon)
                                        else listOf(OrangeNeon, OrangeNeon.copy(alpha = 0.5f))
                                    )
                                )
                                .clickable {
                                    selectedItem = if (selectedItem?.date == item.date) null else item
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rótulos de Data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = if (selectedItem?.date == item.date) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedItem?.date == item.date) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Destaque do item selecionado
        selectedItem?.let { sel ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp),
                color = OrangeNeon.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vencimento: ${sel.label}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sel.amount.formatBrl(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = OrangeNeon
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card Total Projetado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OrangeNeon.copy(alpha = 0.12f))
                .border(1.dp, OrangeNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL PROJETADO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = OrangeNeon,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = totalProjected.formatBrl(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = OrangeNeon
                )
            }
        }
    }
}
