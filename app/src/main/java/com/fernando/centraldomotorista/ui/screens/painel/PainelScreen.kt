package com.fernando.centraldomotorista.ui.screens.painel

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.ui.screens.painel.components.*
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(
    viewModel: PainelViewModel,
    onNavigateToCreateRoute: () -> Unit,
    onNavigateToCreateDailyTotal: () -> Unit,
    onNavigateToFuelExpense: () -> Unit,
    onNavigateToMaintenanceExpense: () -> Unit,
    onNavigateToMealExpense: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPartners: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PAINEL",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar dados",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.profile != null,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp)
                ) {
                    // 1. Loading linear quando carregando em background
                    if (uiState.isLoading && uiState.profile != null) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = OrangeNeon,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // 2. Mensagem de Erro
                    if (uiState.error != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedAlert)
                                    Text(
                                        text = uiState.error ?: "",
                                        color = RedAlert,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.refresh() }) {
                                        Text("Recarregar", color = OrangeNeon, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Alertas de Manutenção de Peças (Desgaste >= 90%)
                    if (uiState.maintenanceAlerts.isNotEmpty()) {
                        item {
                            PainelMaintenanceAlertList(
                                alerts = uiState.maintenanceAlerts,
                                onResetClick = { item -> viewModel.openResetPartDialog(item) },
                                onNavigateToHistory = onNavigateToHistory
                            )
                        }
                    }

                    // 4. StatCard: Lucro Diário
                    item {
                        PainelStatCard(
                            label = "Lucro Diário",
                            value = uiState.dailyEarnings.formatBrlCurrency(),
                            trend = uiState.dailyProgressPct?.let { "${it.toPlainString()}%" },
                            trendPositive = true,
                            progress = uiState.dailyProgressPct?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) },
                            hint = "${uiState.dailyPackages} pacotes hoje"
                        )
                    }

                    // 5. StatCard: Lucro Semanal
                    item {
                        PainelStatCard(
                            label = "Lucro Semanal",
                            value = uiState.weeklyEarnings.formatBrlCurrency(),
                            trend = uiState.weeklyProgressPct?.let { "${it.toPlainString()}%" },
                            trendPositive = true,
                            progress = uiState.weeklyProgressPct?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) },
                            hint = "${uiState.weeklyPackages} pacotes esta semana"
                        )
                    }

                    // 6. StatCard: Meta Mensal (Destaque visual)
                    item {
                        val hasMonthlyGoal = uiState.monthlyGoal > BigDecimal.ZERO
                        val monthlyValueText = if (hasMonthlyGoal) {
                            uiState.monthlyGoal.formatBrlCurrency()
                        } else {
                            "Não definida"
                        }

                        val monthlyHintText = if (hasMonthlyGoal) {
                            val pctStr = uiState.monthlyProgressPct?.toPlainString() ?: "0"
                            "Progresso $pctStr% • ${uiState.monthlyEarnings.formatBrlCurrency()} • ${uiState.monthlyPackages} pacotes este mês"
                        } else {
                            "${uiState.monthlyEarnings.formatBrlCurrency()} faturados este mês"
                        }

                        PainelStatCard(
                            label = "Meta Mensal",
                            value = monthlyValueText,
                            highlight = true,
                            progress = uiState.monthlyProgressPct?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) },
                            hint = monthlyHintText,
                            rightContent = {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        )
                    }

                    // 7. Ganhos por Plataforma
                    item {
                        PlatformEarningsCard(
                            platforms = uiState.platformEarnings,
                            onNavigateToApps = onNavigateToApps
                        )
                    }

                    // 8. Despesas Operacionais do Mês (Combustível, Manutenção, Alimentação, Equipe)
                    item {
                        ExpenseSummaryCard(
                            expenses = uiState.expensesByCategory,
                            onNavigateToFuelExpense = onNavigateToFuelExpense,
                            onNavigateToMaintenanceExpense = onNavigateToMaintenanceExpense,
                            onNavigateToMealExpense = onNavigateToMealExpense,
                            onNavigateToPartners = onNavigateToPartners
                        )
                    }

                    // 8.1 Gasto por Entregador Parceiro (quando houver pagamentos de equipe no mês)
                    item {
                        TeamExpensesCard(
                            partners = uiState.teamExpensesByPartner,
                            onNavigateToPartners = onNavigateToPartners
                        )
                    }

                    // 9. Tendência de Desempenho (7D / 30D)
                    item {
                        PerformanceTrendChart(
                            range = uiState.trendRange,
                            buckets = uiState.trendBuckets,
                            maxTrendAmount = uiState.maxTrendAmount,
                            selectedBucket = uiState.selectedTrendDay,
                            onRangeSelected = { viewModel.setTrendRange(it) },
                            onBucketSelected = { viewModel.selectTrendDay(it) }
                        )
                    }
                }
            }

            // 10. Menu de Ações Rápidas (FAB)
            QuickActionsMenu(
                isOpen = uiState.isQuickActionsOpen,
                onToggle = { viewModel.toggleQuickActions() },
                onNavigateToCreateRoute = onNavigateToCreateRoute,
                onNavigateToCreateDailyTotal = onNavigateToCreateDailyTotal,
                onNavigateToFuelExpense = onNavigateToFuelExpense,
                onNavigateToMaintenanceExpense = onNavigateToMaintenanceExpense,
                onNavigateToMealExpense = onNavigateToMealExpense
            )

            // 11. Diálogo para Registro de Troca de Peça
            uiState.partToReset?.let { alertItem ->
                ResetPartMaintenanceDialog(
                    item = alertItem,
                    isSaving = uiState.isUpdatingPart,
                    onDismiss = { viewModel.closeResetPartDialog() },
                    onConfirm = { newKm ->
                        viewModel.confirmResetPart(alertItem.part.id, newKm)
                    }
                )
            }
        }
    }
}
