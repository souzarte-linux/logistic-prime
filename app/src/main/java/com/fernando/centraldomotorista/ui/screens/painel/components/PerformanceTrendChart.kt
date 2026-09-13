package com.fernando.centraldomotorista.ui.screens.painel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.screens.painel.DailyTrendBucket
import com.fernando.centraldomotorista.ui.screens.painel.TrendRange
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PerformanceTrendChart(
    range: TrendRange,
    buckets: List<DailyTrendBucket>,
    maxTrendAmount: BigDecimal,
    selectedBucket: DailyTrendBucket?,
    onRangeSelected: (TrendRange) -> Unit,
    onBucketSelected: (DailyTrendBucket) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Título + Toggle 7D/30D
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TENDÊNCIA DE DESEMPENHO",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        TrendRange.values().forEach { r ->
                            val isSelected = range == r
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) OrangeNeon else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onRangeSelected(r) }
                            ) {
                                Text(
                                    text = r.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Container de Barras
            val safeMax = if (maxTrendAmount > BigDecimal.ZERO) maxTrendAmount else BigDecimal.ONE

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (range == TrendRange.SEVEN_DAYS) 6.dp else 2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    buckets.forEach { bucket ->
                        val isSelected = selectedBucket?.date == bucket.date
                        val ratio = bucket.totalAmount
                            .divide(safeMax, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)

                        // Altura mínima visual de 4dp para dias mesmo com 0
                        val barHeightFraction = if (bucket.totalAmount > BigDecimal.ZERO) {
                            maxOf(0.06f, ratio)
                        } else {
                            0.03f
                        }

                        val barBrush = if (isSelected) {
                            Brush.verticalGradient(
                                listOf(Color.White, OrangeNeon)
                            )
                        } else if (bucket.totalAmount > BigDecimal.ZERO) {
                            Brush.verticalGradient(
                                listOf(OrangeNeon, OrangeNeon.copy(alpha = 0.35f))
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            )
                        }

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
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(barBrush)
                                    .clickable { onBucketSelected(bucket) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rótulos de data/dia (especialmente nítidos para 7D)
            if (range == TrendRange.SEVEN_DAYS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    buckets.forEach { bucket ->
                        val isSelected = selectedBucket?.date == bucket.date
                        Text(
                            text = bucket.dayOfWeekLabel.take(3),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || bucket.isToday) FontWeight.Black else FontWeight.Normal,
                            color = if (bucket.isToday) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Modo 30D: Mostra marcadores de início, meio e fim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    buckets.firstOrNull()?.let {
                        Text(text = it.dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    buckets.getOrNull(buckets.size / 2)?.let {
                        Text(text = it.dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    buckets.lastOrNull()?.let {
                        Text(text = "Hoje (${it.dateLabel})", fontSize = 10.sp, color = OrangeNeon, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card interativo do dia selecionado
            AnimatedVisibility(
                visible = selectedBucket != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedBucket?.let { bucket ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = OrangeNeon.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${bucket.fullDayOfWeekLabel}, ${bucket.dateLabel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${bucket.packageCount} pacotes entregues",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = bucket.totalAmount.formatBrlCurrency(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangeNeon
                            )
                        }
                    }
                }
            }
        }
    }
}
