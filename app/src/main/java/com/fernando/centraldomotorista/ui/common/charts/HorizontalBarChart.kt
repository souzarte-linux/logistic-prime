package com.fernando.centraldomotorista.ui.common.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class PlatformProfitabilityBarItem(
    val platformId: String,
    val name: String,
    val revenue: BigDecimal,
    val revPerHour: BigDecimal,
    val revPerKm: BigDecimal,
    val color: Color
)

private fun BigDecimal.formatBrl(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@Composable
fun HorizontalPlatformBars(
    items: List<PlatformProfitabilityBarItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            text = "Cadastre plataformas e lance rotas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val maxPerHour = items.maxOfOrNull { it.revPerHour } ?: BigDecimal.ONE
    val safeMax = if (maxPerHour > BigDecimal.ZERO) maxPerHour else BigDecimal.ONE

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.forEach { item ->
            val fraction = item.revPerHour
                .divide(safeMax, 4, RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0.04f, 1f)

            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600),
                label = "barFraction"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Nome da plataforma + R$/h em destaque
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(item.color, CircleShape)
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.revPerHour.formatBrl()}/h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = OrangeNeon
                        )
                        Text(
                            text = "(${item.revPerKm.formatBrl()}/km)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Barra de progresso customizada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(item.color)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Subtexto com Receita Bruta
                Text(
                    text = "Receita Bruta: ${item.revenue.formatBrl()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
