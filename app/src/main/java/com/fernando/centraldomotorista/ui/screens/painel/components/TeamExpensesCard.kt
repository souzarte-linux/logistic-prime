package com.fernando.centraldomotorista.ui.screens.painel.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
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
import com.fernando.centraldomotorista.ui.screens.painel.PartnerExpenseItem
import com.fernando.centraldomotorista.ui.theme.PurpleTeam

@Composable
fun TeamExpensesCard(
    partners: List<PartnerExpenseItem>,
    modifier: Modifier = Modifier,
    onNavigateToPartners: () -> Unit = {}
) {
    if (partners.isEmpty()) return

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPartners() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GASTO POR ENTREGADOR PARCEIRO",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = PurpleTeam,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                partners.forEach { partner ->
                    val animatedPct by animateFloatAsState(
                        targetValue = (partner.percentageOfTotal / 100f).coerceIn(0f, 1f),
                        label = "partner_bar_${partner.vendorName}"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPartners() },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = partner.vendorName.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = partner.total.formatBrlCurrency(),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleTeam
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedPct)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(PurpleTeam.copy(alpha = 0.7f), PurpleTeam)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
