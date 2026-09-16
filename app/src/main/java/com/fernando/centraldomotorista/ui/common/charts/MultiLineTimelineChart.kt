package com.fernando.centraldomotorista.ui.common.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

data class PerformanceTimelineBucket(
    val key: String,
    val label: String,
    val receita: BigDecimal = BigDecimal.ZERO,
    val despesa: BigDecimal = BigDecimal.ZERO,
    val lucro: BigDecimal = BigDecimal.ZERO
)

private fun BigDecimal.formatBrl(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

val TimelineReceitaColor = Color(0xFFFFD54F) // Amarelo/Dourado Neon
val TimelineDespesaColor = Color(0xFFFF5252) // Vermelho Coral
val TimelineLucroColor = Color(0xFF00E676)   // Verde Esmeralda Neon

@Composable
fun MultiLineTimelineChart(
    buckets: List<PerformanceTimelineBucket>,
    selectedBucket: PerformanceTimelineBucket?,
    onSelectBucket: (PerformanceTimelineBucket?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sem dados no período.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Calcula valor máximo e mínimo para enquadramento das curvas
    val maxVal = remember(buckets) {
        val maxReceita = buckets.maxOfOrNull { it.receita } ?: BigDecimal.ZERO
        val maxLucro = buckets.maxOfOrNull { it.lucro } ?: BigDecimal.ZERO
        val maxDespesa = buckets.maxOfOrNull { it.despesa } ?: BigDecimal.ZERO
        val absoluteMax = maxOf(maxReceita, maxLucro, maxDespesa)
        if (absoluteMax > BigDecimal.ZERO) absoluteMax else BigDecimal("100")
    }

    val minVal = remember(buckets) {
        val minLucro = buckets.minOfOrNull { it.lucro } ?: BigDecimal.ZERO
        if (minLucro < BigDecimal.ZERO) minLucro else BigDecimal.ZERO
    }

    val range = remember(maxVal, minVal) {
        val diff = maxVal.subtract(minVal)
        if (diff > BigDecimal.ZERO) diff else BigDecimal.ONE
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legenda Superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(label = "Receita", color = TimelineReceitaColor)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(label = "Despesa", color = TimelineDespesaColor)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(label = "Lucro Líq.", color = TimelineLucroColor)
        }

        // Área do Gráfico Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(buckets) {
                        detectTapGestures { tapOffset ->
                            val count = buckets.size
                            if (count > 0) {
                                val stepX = size.width / if (count > 1) (count - 1) else 1
                                val index = (tapOffset.x / stepX).toInt().coerceIn(0, count - 1)
                                val tapped = buckets[index]
                                onSelectBucket(if (selectedBucket?.key == tapped.key) null else tapped)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val count = buckets.size
                val stepX = if (count > 1) w / (count - 1) else w / 2f

                val rangeD = range.toDouble()
                val minD = minVal.toDouble()

                fun getY(valBd: BigDecimal): Float {
                    val norm = (valBd.toDouble() - minD) / rangeD
                    return (h - (norm * h * 0.85f) - (h * 0.08f)).toFloat().coerceIn(4f, h - 4f)
                }

                // Grid tracejada horizontal (Topo, Meio, Base/Zero)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val gridColor = Color.Gray.copy(alpha = 0.2f)

                drawLine(gridColor, Offset(0f, getY(maxVal)), Offset(w, getY(maxVal)), strokeWidth = 1f, pathEffect = dashEffect)
                drawLine(gridColor, Offset(0f, getY(BigDecimal.ZERO)), Offset(w, getY(BigDecimal.ZERO)), strokeWidth = 1.5f)
                if (minVal < BigDecimal.ZERO) {
                    drawLine(gridColor, Offset(0f, getY(minVal)), Offset(w, getY(minVal)), strokeWidth = 1f, pathEffect = dashEffect)
                }

                // Construção dos Paths das 3 linhas
                val pathReceita = Path()
                val pathDespesa = Path()
                val pathLucro = Path()

                buckets.forEachIndexed { i, b ->
                    val x = if (count > 1) i * stepX else w / 2f
                    val yRec = getY(b.receita)
                    val yDesp = getY(b.despesa)
                    val yLucro = getY(b.lucro)

                    if (i == 0) {
                        pathReceita.moveTo(x, yRec)
                        pathDespesa.moveTo(x, yDesp)
                        pathLucro.moveTo(x, yLucro)
                    } else {
                        pathReceita.lineTo(x, yRec)
                        pathDespesa.lineTo(x, yDesp)
                        pathLucro.lineTo(x, yLucro)
                    }
                }

                // Desenha linhas
                drawPath(pathReceita, TimelineReceitaColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(pathDespesa, TimelineDespesaColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(pathLucro, TimelineLucroColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Desenha marcadores de pontos
                buckets.forEachIndexed { i, b ->
                    val x = if (count > 1) i * stepX else w / 2f
                    val isSelected = selectedBucket?.key == b.key

                    // Se selecionado, desenha linha vertical indicadora
                    if (isSelected) {
                        drawLine(
                            color = OrangeNeon.copy(alpha = 0.6f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.5f,
                            pathEffect = dashEffect
                        )
                    }

                    val pointRadius = if (isSelected) 5.5f else 3f
                    drawCircle(TimelineReceitaColor, radius = pointRadius, center = Offset(x, getY(b.receita)))
                    drawCircle(TimelineDespesaColor, radius = pointRadius, center = Offset(x, getY(b.despesa)))
                    drawCircle(TimelineLucroColor, radius = pointRadius, center = Offset(x, getY(b.lucro)))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Eixo X: Rótulos temporais
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = remember(buckets.size) {
                when {
                    buckets.size <= 7 -> buckets.indices.toList()
                    buckets.size <= 14 -> listOf(0, buckets.size / 2, buckets.size - 1)
                    else -> listOf(0, buckets.size / 4, buckets.size / 2, (buckets.size * 3) / 4, buckets.size - 1)
                }
            }

            labelIndices.forEach { idx ->
                val bucket = buckets.getOrNull(idx)
                if (bucket != null) {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Card interativo de inspeção do dia/mês selecionado
        AnimatedVisibility(
            visible = selectedBucket != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedBucket?.let { bucket ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lucro: ${bucket.lucro.formatBrl()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (bucket.lucro >= BigDecimal.ZERO) GreenNeon else TimelineDespesaColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Receita: ${bucket.receita.formatBrl()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TimelineReceitaColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Despesa: ${bucket.despesa.formatBrl()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TimelineDespesaColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
