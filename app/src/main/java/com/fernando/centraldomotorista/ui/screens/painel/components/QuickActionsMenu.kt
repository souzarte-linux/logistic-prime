package com.fernando.centraldomotorista.ui.screens.painel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.OrangeNeon

@Composable
fun QuickActionsMenu(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onNavigateToCreateRoute: () -> Unit,
    onNavigateToCreateDailyTotal: () -> Unit,
    onNavigateToFuelExpense: () -> Unit,
    onNavigateToMaintenanceExpense: () -> Unit,
    onNavigateToMealExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (isOpen) 45f else 0f, label = "fab_rotation")

    // Backdrop quando aberto
    if (isOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onToggle() }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(bottom = 80.dp, end = 16.dp)
        ) {
            // Menu de Ações Animado
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Grupo 1: RECEITA
                    Text(
                        text = "RECEITA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = OrangeNeon
                    )

                    QuickActionRow(
                        icon = Icons.Default.Navigation,
                        title = "Lançar Ganhos por Rota",
                        onClick = {
                            onToggle()
                            onNavigateToCreateRoute()
                        }
                    )

                    QuickActionRow(
                        icon = Icons.Default.CalendarToday,
                        title = "Lançar Ganho Total do Dia",
                        onClick = {
                            onToggle()
                            onNavigateToCreateDailyTotal()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // Grupo 2: DESPESAS
                    Text(
                        text = "DESPESAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = OrangeNeon
                    )

                    QuickActionRow(
                        icon = Icons.Default.LocalGasStation,
                        title = "Combustível",
                        onClick = {
                            onToggle()
                            onNavigateToFuelExpense()
                        }
                    )

                    QuickActionRow(
                        icon = Icons.Default.Build,
                        title = "Manutenção",
                        onClick = {
                            onToggle()
                            onNavigateToMaintenanceExpense()
                        }
                    )

                    QuickActionRow(
                        icon = Icons.Default.Restaurant,
                        title = "Alimentação",
                        onClick = {
                            onToggle()
                            onNavigateToMealExpense()
                        }
                    )
                }
            }

            // Floating Action Button principal
            FloatingActionButton(
                onClick = onToggle,
                containerColor = OrangeNeon,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isOpen) "Fechar ações" else "Ações rápidas",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
