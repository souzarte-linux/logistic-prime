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
                .padding(16.dp)
        ) {
            // Header: Rótulo + Right icon opcional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                rightContent?.invoke()
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Linha principal: Valor + Trend badge
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (highlight) MaterialTheme.colorScheme.onSurface else OrangeNeon,
                    letterSpacing = (-0.5).sp
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Barra de Progresso
            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
