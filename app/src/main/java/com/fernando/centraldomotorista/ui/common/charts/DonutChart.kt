package com.fernando.centraldomotorista.ui.common.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import kotlin.math.atan2

data class DonutSlice(
    val key: String,
    val name: String,
    val value: BigDecimal,
    val color: Color,
    val formattedValue: String = ""
)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
    centerLabel: String? = null,
    centerValue: String? = null,
    onSliceClick: ((DonutSlice) -> Unit)? = null
) {
    val total = remember(slices) {
        slices.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.value) }
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(strokeWidth / 2)
                .pointerInput(slices, total) {
                    if (onSliceClick != null && total > BigDecimal.ZERO) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // Ajusta para o ângulo inicial de -90 graus (topo)
                            var sweepAngleFromTop = (angle + 90f) % 360f

                            var currentStart = 0f
                            for (slice in slices) {
                                if (slice.value <= BigDecimal.ZERO) continue
                                val sliceSweep = (slice.value.toDouble() / total.toDouble() * 360f).toFloat()
                                if (sweepAngleFromTop >= currentStart && sweepAngleFromTop <= currentStart + sliceSweep) {
                                    onSliceClick(slice)
                                    break
                                }
                                currentStart += sliceSweep
                            }
                        }
                    }
                }
        ) {
            val strokePx = strokeWidth.toPx()
            val diameter = minOf(size.width, size.height)
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            if (total <= BigDecimal.ZERO || slices.isEmpty()) {
                // Desenha anel de fallback vazio
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )
            } else {
                var startAngle = -90f
                val totalDouble = total.toDouble()

                slices.forEach { slice ->
                    if (slice.value > BigDecimal.ZERO) {
                        val sliceFraction = (slice.value.toDouble() / totalDouble).toFloat()
                        val sweepAngle = sliceFraction * 360f * animationProgress.value

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                        )
                        startAngle += sliceFraction * 360f
                    }
                }
            }
        }

        // Conteúdo central opcional
        if (!centerLabel.isNullOrBlank() || !centerValue.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!centerLabel.isNullOrBlank()) {
                    Text(
                        text = centerLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                if (!centerValue.isNullOrBlank()) {
                    Text(
                        text = centerValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
