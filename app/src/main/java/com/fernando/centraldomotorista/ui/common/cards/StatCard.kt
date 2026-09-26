package com.fernando.centraldomotorista.ui.common.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon

@Composable
fun PainelStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendPositive: Boolean = true,
    progress: Float? = null, // 0.0f..1.0f
    highlight: Boolean = false,
    hint: String? = null,
    isCompact: Boolean = false,
    rightContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (highlight) {
            BorderStroke(1.5.dp, OrangeNeon)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Rótulo + Right icon opcional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (isCompact) 0.6.sp else 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                rightContent?.invoke()
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

            // Linha principal: Valor + Trend badge
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                Text(
                    text = value,
                    fontSize = if (isCompact) 19.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (highlight) MaterialTheme.colorScheme.onSurface else OrangeNeon,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (trend != null) {
                    val trendColor = if (trendPositive) GreenNeon else MaterialTheme.colorScheme.error
                    val arrow = if (trendPositive) "↑" else "↓"
                    Surface(
                        color = trendColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "$arrow $trend",
                            fontSize = if (isCompact) 9.5.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            modifier = Modifier.padding(
                                horizontal = if (isCompact) 4.dp else 6.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                }
            }

            // Barra de Progresso
            if (progress != null) {
                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 10.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    label = "stat_card_progress"
                )
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
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OrangeNeon.copy(alpha = 0.8f), OrangeNeon)
                                )
                            )
                    )
                }
            }

            // Hint / Descrição complementar
            if (!hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                Text(
                    text = hint,
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = if (isCompact) 13.sp else 15.sp,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
