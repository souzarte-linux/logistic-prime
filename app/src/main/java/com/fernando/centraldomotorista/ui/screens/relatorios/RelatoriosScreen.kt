package com.fernando.centraldomotorista.ui.screens.relatorios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.ui.common.charts.DonutChart
import com.fernando.centraldomotorista.ui.common.charts.DonutSlice
import com.fernando.centraldomotorista.ui.common.charts.FutureCashFlowChart
import com.fernando.centraldomotorista.ui.common.charts.FutureCashFlowPieChart
import com.fernando.centraldomotorista.ui.common.charts.HorizontalPlatformBars
import com.fernando.centraldomotorista.ui.common.charts.MultiLineTimelineChart
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodSelector
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    viewModel: RelatoriosViewModel,
    onNavigateToHistoricoCategory: ((String, PeriodFilter) -> Unit)? = null,
    onNavigateToEditMaintenance: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RELATÓRIOS & INSIGHTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Inteligência analítica do seu faturamento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar relatórios",
                            tint = OrangeNeon
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
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeNeon)
            }
            return@Scaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 1. SEÇÃO DE FILTROS =================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FILTROS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Filtro 1: Seletor de Período (Dia, Semana, Quinzena, Mês, Ano, Intervalo)
                PeriodSelector(
                    periodFilter = uiState.periodFilter,
                    isDropdownExpanded = uiState.isPeriodDropdownExpanded,
                    onToggleDropdown = { viewModel.togglePeriodDropdown() },
                    onSelectPreset = { preset -> viewModel.applyPeriodPreset(preset) },
                    onApplyCustomRange = { start, end -> viewModel.applyCustomPeriod(start, end) }
                )

                // Filtro 2: Seletor de Plataforma (Todas as Plataformas + lista de cadastradas)
                val chevronRotation by animateFloatAsState(
                    targetValue = if (uiState.isPlatformDropdownExpanded) 180f else 0f,
                    label = "platChevron"
                )
                val selectedPlatName = remember(uiState.selectedPlatformId, uiState.platforms) {
                    if (uiState.selectedPlatformId == "all") "Todas as Plataformas"
                    else uiState.platforms.firstOrNull { it.id == uiState.selectedPlatformId }?.name ?: "Todas as Plataformas"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isPlatformDropdownExpanded) OrangeNeon.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    onClick = { viewModel.togglePlatformDropdown() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "PLATAFORMA: ${selectedPlatName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                // Opções expansíveis de Plataforma
                AnimatedVisibility(
                    visible = uiState.isPlatformDropdownExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Opção 'Todas as Plataformas'
                            val isAllSelected = uiState.selectedPlatformId == "all"
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isAllSelected) OrangeNeon.copy(alpha = 0.15f) else Color.Transparent,
                                onClick = { viewModel.selectPlatform("all") }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Todas as Plataformas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAllSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAllSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Lista de plataformas individuais
                            uiState.platforms.forEach { plat ->
                                val isSelected = uiState.selectedPlatformId == plat.id
                                val isInactive = !plat.active

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else Color.Transparent,
                                    onClick = {
                                        if (!isInactive) {
                                            viewModel.selectPlatform(plat.id)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = plat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isInactive) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            } else if (isSelected) {
                                                OrangeNeon
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (isInactive) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "INATIVA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = OrangeNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. GRID DE 17 KPIS OPERACIONAIS E FINANCEIROS =================
            val s = uiState.stats
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Linha 1: Receita Bruta & Lucro Líquido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Receita Bruta",
                        value = s.totalRevenue.formatCurrency(),
                        icon = Icons.Default.AccountBalanceWallet,
                        tone = KpiTone.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Lucro Líquido",
                        value = s.profit.formatCurrency(),
                        icon = if (s.profit >= BigDecimal.ZERO) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        tone = if (s.profit >= BigDecimal.ZERO) KpiTone.SUCCESS else KpiTone.DESTRUCTIVE,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 2: Lucro / KM & Lucro / Hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Lucro / KM",
                        value = s.profitPerKm.formatCurrency(),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tone = KpiTone.SUCCESS,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Lucro / Hora",
                        value = s.profitPerHour.formatCurrency(),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tone = KpiTone.SUCCESS,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 3: Receita / KM & Receita / Hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Receita / KM",
                        value = s.revPerKm.formatCurrency(),
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Receita / Hora",
                        value = s.revPerHour.formatCurrency(),
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 4: KM rodados & Horas trabalhadas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "KM rodados",
                        value = "${s.totalKm.toInt()} km",
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Horas trab.",
                        value = "${s.hours}h",
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 5: Média diária de horas & Custo Op. / KM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Média diária horas",
                        value = "${s.averageDailyHours}h",
                        icon = Icons.Default.AccessTime,
                        hint = "Total de horas trabalhadas dividido pelo número de dias do período selecionado.",
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Custo Op. / KM",
                        value = s.costPerKm.formatCurrency(),
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        tone = KpiTone.DESTRUCTIVE,
                        hint = "Custo operacional total (todas as despesas do período) dividido pela quilometragem total percorrida no mesmo período.",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 6: Consumo Real (km/L) & Rotas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Consumo Real",
                        value = if (s.realConsumptionKml > BigDecimal.ZERO) "${s.realConsumptionKml} km/L" else "—",
                        icon = Icons.Default.LocalGasStation,
                        hint = "Calculado dinamicamente: KM rodados no período ÷ litros abastecidos (despesas de combustível no período pelo método tanque-a-tanque).",
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Rotas",
                        value = "${s.routeCount}",
                        icon = Icons.Default.Place,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 7: Pacotes Totais & Pacotinhos (qtd)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Pacotes Totais",
                        value = "${s.totalPackages}",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Pacotinhos",
                        value = "${s.totalSmallPackages}",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 8: Valor Pacotinhos & Volumosos (qtd)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RelatoriosKpiCard(
                        label = "Valor Pacotinhos",
                        value = s.smallPackagesValue.formatCurrency(),
                        icon = Icons.Default.AccountBalanceWallet,
                        tone = KpiTone.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    RelatoriosKpiCard(
                        label = "Volumosos",
                        value = "${s.totalLargePackages}",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 9: Valor Volumosos (card full width)
                RelatoriosKpiCard(
                    label = "Valor Volumosos",
                    value = s.largePackagesValue.formatCurrency(),
                    icon = Icons.Default.AccountBalanceWallet,
                    tone = KpiTone.PRIMARY
                )
            }

            // ================= 3. CUSTOS DE MANUTENÇÃO (PERÍODO) =================
            val mb = uiState.maintCostBreakdown
            RelatoriosSectionCard(
                title = "CUSTOS DE MANUTENÇÃO (PERÍODO)",
                icon = Icons.Default.Build
            ) {
                if (mb.total <= BigDecimal.ZERO) {
                    Text(
                        text = "Sem despesas de combustível ou manutenção no período.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    // Mini KPIs de Combustível, Óleo e Peças
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNavigateToHistoricoCategory?.invoke("combustivel", uiState.periodFilter) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Combustível", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = mb.fuel.formatCurrency(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = OrangeNeon, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNavigateToHistoricoCategory?.invoke("oleo", uiState.periodFilter) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Óleo / Filtro", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = mb.oil.formatCurrency(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2979FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNavigateToHistoricoCategory?.invoke("pecas", uiState.periodFilter) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Peças / Outros", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = mb.parts.formatCurrency(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gráfico Donut
                    val errorColor = MaterialTheme.colorScheme.error
                    val donutSlices = remember(mb, errorColor) {
                        listOf(
                            DonutSlice("combustivel", "Combustível", mb.fuel, OrangeNeon, mb.fuel.formatCurrency()),
                            DonutSlice("oleo", "Óleo / Filtros", mb.oil, Color(0xFF2979FF), mb.oil.formatCurrency()),
                            DonutSlice("pecas", "Peças / Outros", mb.parts, errorColor, mb.parts.formatCurrency())
                        ).filter { it.value > BigDecimal.ZERO }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            slices = donutSlices,
                            modifier = Modifier.size(140.dp),
                            centerLabel = "TOTAL",
                            centerValue = mb.total.formatCurrency(),
                            onSliceClick = { slice -> onNavigateToHistoricoCategory?.invoke(slice.key, uiState.periodFilter) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Toque em um card ou fatia para ver as despesas filtradas no histórico.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // ================= 4. MANUTENÇÃO PREVENTIVA =================
            RelatoriosSectionCard(
                title = "MANUTENÇÃO PREVENTIVA",
                icon = Icons.Default.Engineering
            ) {
                // Odômetro estimado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Odômetro estimado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${s.currentOdometer.toInt()} km",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.maintSchedule.isEmpty()) {
                    Text(
                        text = "Nenhuma peça cadastrada para monitoramento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.maintSchedule.forEach { item ->
                            MaintenanceScheduleCard(
                                item = item
                            )
                        }
                    }
                }
            }

            // ================= 5. DESEMPENHO NO PERÍODO (TIMELINE) =================
            RelatoriosSectionCard(
                title = "DESEMPENHO NO PERÍODO",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            ) {
                MultiLineTimelineChart(
                    buckets = uiState.timelineBuckets,
                    selectedBucket = uiState.selectedTimelineBucket,
                    onSelectBucket = { bucket -> viewModel.selectTimelineBucket(bucket) }
                )
            }

            // ================= 6. FLUXO DE CAIXA FUTURO (FATURAS A RECEBER) =================
            RelatoriosSectionCard(
                title = "FLUXO DE CAIXA FUTURO (FATURAS A RECEBER)",
                icon = Icons.Default.PieChart
            ) {
                FutureCashFlowPieChart(
                    platforms = uiState.futureCashFlowPlatforms,
                    totalAmount = uiState.futureCashFlowTotal
                )
            }

            // ================= 7. POR PLATAFORMA (RENTABILIDADE) =================
            RelatoriosSectionCard(
                title = "POR PLATAFORMA (RENTABILIDADE)",
                icon = Icons.Default.Leaderboard
            ) {
                HorizontalPlatformBars(items = uiState.platformProfitability)
            }

            // ================= 8. BONIFICAÇÕES E DESCONTOS =================
            RelatoriosSectionCard(
                title = "BONIFICAÇÕES E DESCONTOS",
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            ) {
                if (uiState.bonificacoes.isEmpty()) {
                    Text(
                        text = "Nenhum desconto ou acréscimo neste período.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.bonificacoes.forEach { group ->
                            BonificacoesCard(group = group)
                        }
                    }
                }
            }

            // ================= 9. CATEGORIAS ENTREGUES =================
            RelatoriosSectionCard(
                title = "CATEGORIAS ENTREGUES",
                icon = Icons.Default.Category,
                action = {
                    // Segmented control [R$ Valor] / [Qtd Entregue]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            val isAmount = uiState.categoryMetric == CategoryMetric.AMOUNT
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAmount) OrangeNeon else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setCategoryMetric(CategoryMetric.AMOUNT) }
                            ) {
                                Text(
                                    text = "R$ Valor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAmount) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (!isAmount) OrangeNeon else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setCategoryMetric(CategoryMetric.COUNT) }
                            ) {
                                Text(
                                    text = "Qtd Entregue",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAmount) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            ) {
                if (uiState.deliveredCategories.isEmpty()) {
                    Text(
                        text = "Sem dados de categorias entregues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
                    val isAmount = uiState.categoryMetric == CategoryMetric.AMOUNT

                    val totalCategoryAmount = remember(uiState.deliveredCategories) {
                        uiState.deliveredCategories.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amount) }
                    }
                    val totalCategoryCount = remember(uiState.deliveredCategories) {
                        uiState.deliveredCategories.sumOf { it.count }
                    }

                    val slices = remember(uiState.deliveredCategories, uiState.categoryMetric) {
                        uiState.deliveredCategories.map { c ->
                            val valBd = if (isAmount) c.amount else BigDecimal(c.count)
                            DonutSlice(
                                key = c.productType,
                                name = c.name,
                                value = valBd,
                                color = c.color,
                                formattedValue = if (isAmount) c.amount.formatCurrency() else "${c.count} un"
                            )
                        }
                    }

                    val selectedCategory = remember(selectedCategoryKey, uiState.deliveredCategories) {
                        uiState.deliveredCategories.find { it.productType == selectedCategoryKey }
                    }

                    val centerLabelText = if (selectedCategory != null) {
                        "CATEGORIA"
                    } else if (isAmount) {
                        "TOTAL R$"
                    } else {
                        "TOTAL QTD"
                    }

                    val centerValueText = if (selectedCategory != null) {
                        if (isAmount) selectedCategory.amount.formatCurrency() else "${selectedCategory.count} un"
                    } else {
                        if (isAmount) totalCategoryAmount.formatCurrency() else "$totalCategoryCount un"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gráfico Donut de Categorias com destaque de fatia ao tocar
                        DonutChart(
                            slices = slices,
                            modifier = Modifier.size(130.dp),
                            strokeWidth = 20.dp,
                            selectedSliceKey = selectedCategoryKey,
                            onSliceClick = { slice ->
                                selectedCategoryKey = if (selectedCategoryKey == slice.key) null else slice.key
                            },
                            centerLabel = centerLabelText,
                            centerValue = centerValueText
                        )

                        // Lista detalhada com legendas e realce sincronizado
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.deliveredCategories.forEach { c ->
                                val isSelected = selectedCategoryKey == c.productType

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategoryKey = if (isSelected) null else c.productType
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) c.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, c.color)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nome da categoria truncado para nunca espremer os valores
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(c.color, CircleShape)
                                            )
                                            Text(
                                                text = c.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Hierarquia de valores: inverte destaque quando métrica for Qtd Entregue
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (isAmount) {
                                                // Modo R$ Valor:
                                                // 1. Valor monetário em cima em destaque
                                                Text(
                                                    text = c.amount.formatCurrency(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OrangeNeon,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                // 2. Quantidade embaixo com menor destaque (truncada como "entr." para economizar espaço)
                                                Text(
                                                    text = "${c.count} entr.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            } else {
                                                // Modo Qtd Entregue:
                                                // 1. Quantidade entregue em cima em destaque (truncada como "entr." para não espremer os nomes)
                                                Text(
                                                    text = "${c.count} entr.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OrangeNeon,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                // 2. Valor monetário embaixo com menor destaque
                                                Text(
                                                    text = c.amount.formatCurrency(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 10. TOP ORIGENS E TOP DESTINOS =================
            PlacesRankingCard(
                title = "TOP ORIGENS",
                items = uiState.topOrigins,
                icon = Icons.Default.Place
            )

            PlacesRankingCard(
                title = "TOP DESTINOS",
                items = uiState.topDestinations,
                icon = Icons.Default.Navigation
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
