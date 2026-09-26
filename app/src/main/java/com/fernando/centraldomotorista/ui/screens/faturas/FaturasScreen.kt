package com.fernando.centraldomotorista.ui.screens.faturas

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.DailyTotal
import com.fernando.centraldomotorista.data.model.FinancialAdjustmentSubtype
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun showDatePicker(
    context: Context,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturasScreen(
    viewModel: FaturasViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CONTAS A RECEBER",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FATURAS & CICLOS DE REPASSE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = OrangeNeon
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openNewCycleModal() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nova Fatura",
                            tint = OrangeNeon
                        )
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
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
    ) { padding ->
        val currentCycles = when (uiState.activeTab) {
            FaturasTab.EM_ABERTO -> uiState.emAbertoCycles
            FaturasTab.A_VENCER -> uiState.aVencerCycles
            FaturasTab.PAGO -> uiState.pagoCycles
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                // 1. Cards de Resumo (KPIs em 3 Fases)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Em Aberto
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BlueInfo.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "EM ABERTO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = BlueInfo
                                )
                                Text(
                                    text = uiState.totalEmAberto.formatCurrency(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.emAbertoCycles.size} faturas",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Total A Vencer
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "A VENCER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = OrangeNeon
                                )
                                Text(
                                    text = uiState.totalAVencer.formatCurrency(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.aVencerCycles.size} faturas",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Total Recebido / Pago
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenNeon.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "PAGO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = GreenNeon
                                )
                                Text(
                                    text = uiState.totalPago.formatCurrency(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.pagoCycles.size} faturas",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Filtro Horizontal de Plataformas
                if (uiState.platforms.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedPlatformFilter == "all",
                                    onClick = { viewModel.onPlatformFilterChanged("all") },
                                    label = { Text("Todas (${uiState.cycles.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangeNeon,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            items(uiState.platforms) { platform ->
                                val count = uiState.cycles.count { it.cycle.platformId == platform.id }
                                FilterChip(
                                    selected = uiState.selectedPlatformFilter == platform.id,
                                    onClick = { viewModel.onPlatformFilterChanged(platform.id) },
                                    label = { Text("${platform.name} ($count)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangeNeon,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. 3 Abas Nativas (TabRow)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = uiState.activeTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = OrangeNeon,
                            indicator = {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(uiState.activeTab.ordinal),
                                    color = OrangeNeon
                                )
                            },
                            divider = {}
                        ) {
                            FaturasTab.entries.forEach { tab ->
                                val isSelected = uiState.activeTab == tab
                                val count = when (tab) {
                                    FaturasTab.EM_ABERTO -> uiState.emAbertoCycles.size
                                    FaturasTab.A_VENCER -> uiState.aVencerCycles.size
                                    FaturasTab.PAGO -> uiState.pagoCycles.size
                                }
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.onTabChanged(tab) },
                                    text = {
                                        Text(
                                            text = "${tab.label} ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Lista de Faturas
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = OrangeNeon)
                        }
                    }
                } else if (currentCycles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (uiState.activeTab) {
                                        FaturasTab.PAGO -> Icons.AutoMirrored.Filled.ReceiptLong
                                        FaturasTab.A_VENCER -> Icons.Default.PendingActions
                                        FaturasTab.EM_ABERTO -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when (uiState.activeTab) {
                                        FaturasTab.PAGO -> GreenNeon
                                        FaturasTab.A_VENCER -> OrangeNeon
                                        FaturasTab.EM_ABERTO -> BlueInfo
                                    },
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = when (uiState.activeTab) {
                                        FaturasTab.EM_ABERTO -> "Nenhum ciclo em aberto"
                                        FaturasTab.A_VENCER -> "Nenhum ciclo a vencer"
                                        FaturasTab.PAGO -> "Nenhum ciclo pago"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (uiState.activeTab) {
                                        FaturasTab.EM_ABERTO -> "Você não possui ciclos abertos dentro do período selecionado."
                                        FaturasTab.A_VENCER -> "Não há faturas fechadas aguardando quitação/repasse."
                                        FaturasTab.PAGO -> "As faturas liquidadas e baixadas aparecerão aqui."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (uiState.activeTab != FaturasTab.PAGO) {
                                    Button(
                                        onClick = { viewModel.openNewCycleModal() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = OrangeNeon,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fechar Ciclo / Nova Fatura", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (uiState.activeTab == FaturasTab.PAGO) {
                    // 4b. Cascata em Accordion na aba 'Ciclo Pago' (Mês -> Semana -> Itens)
                    uiState.pagoMonthGroups.forEach { monthGroup ->
                        val ymKey = monthGroup.yearMonth.toString()
                        val isMonthExpanded = uiState.expandedMonths.contains(ymKey)

                        // Nível 1: Cabeçalho do Mês
                        item(key = "pago_month_$ymKey") {
                            PagoMonthHeaderCard(
                                monthGroup = monthGroup,
                                isExpanded = isMonthExpanded,
                                onToggle = { viewModel.toggleMonthExpanded(ymKey) }
                            )
                        }

                        // Nível 2: Semanas do Mês
                        if (isMonthExpanded) {
                            monthGroup.weeks.forEach { weekGroup ->
                                val weekKey = "${ymKey}_${weekGroup.weekNumber}"
                                val isWeekExpanded = uiState.expandedWeeks.contains(weekKey)

                                item(key = "pago_week_$weekKey") {
                                    PagoWeekHeaderCard(
                                        weekGroup = weekGroup,
                                        isExpanded = isWeekExpanded,
                                        onToggle = { viewModel.toggleWeekExpanded(weekKey) }
                                    )
                                }

                                // Nível 3: Itens da Semana (ordenados alfabeticamente pela empresa e por data)
                                if (isWeekExpanded) {
                                    items(weekGroup.items, key = { "pago_item_${it.cycle.id}" }) { cycleItem ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp)
                                        ) {
                                            FaturaCardItem(
                                                item = cycleItem,
                                                onPay = { viewModel.openPayModal(cycleItem) },
                                                onEditItems = { viewModel.openEditCycleModal(cycleItem) },
                                                onAdjustments = { viewModel.openAdjustmentModal(cycleItem) },
                                                onViewDetails = { viewModel.openDetailsModal(cycleItem) },
                                                onDelete = { viewModel.deleteCycle(cycleItem) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(currentCycles, key = { it.cycle.id }) { cycleItem ->
                        FaturaCardItem(
                            item = cycleItem,
                            onPay = { viewModel.openPayModal(cycleItem) },
                            onEditItems = { viewModel.openEditCycleModal(cycleItem) },
                            onAdjustments = { viewModel.openAdjustmentModal(cycleItem) },
                            onViewDetails = { viewModel.openDetailsModal(cycleItem) },
                            onDelete = { viewModel.deleteCycle(cycleItem) }
                        )
                    }
                }

                // 5. Botão "+ Nova Fatura" inferior
                item {
                    Surface(
                        onClick = { viewModel.openNewCycleModal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, OrangeNeon.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FECHAR CICLO / NOVA FATURA",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = OrangeNeon
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Nova Fatura
    if (uiState.isNewCycleModalOpen) {
        NovaFaturaModal(
            uiState = uiState,
            onDismiss = { viewModel.closeNewCycleModal() },
            onPlatformSelected = { viewModel.onNewCyclePlatformChanged(it) },
            onPeriodStartSelected = { viewModel.onNewCyclePeriodStartChanged(it) },
            onPeriodEndSelected = { viewModel.onNewCyclePeriodEndChanged(it) },
            onIncludeEndDateChanged = { viewModel.onNewCycleIncludeEndDateChanged(it) },
            onExpectedDateSelected = { viewModel.onNewCycleExpectedDateChanged(it) },
            onSubmit = { viewModel.createBillingCycle() }
        )
    }

    // Modal Liquidar / Baixar Pagamento (Especialmente na aba "A Vencer")
    if (uiState.payingCycle != null) {
        LiquidarFaturaModal(
            cycle = uiState.payingCycle!!,
            paymentDate = uiState.paymentReceivedDate,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.closePayModal() },
            onDateSelected = { viewModel.onPaymentReceivedDateChanged(it) },
            onConfirm = { viewModel.confirmPayment() }
        )
    }

    // Modal Detalhes da Fatura
    if (uiState.viewingCycle != null) {
        DetalhesFaturaModal(
            cycle = uiState.viewingCycle!!,
            onDismiss = { viewModel.closeDetailsModal() }
        )
    }

    // Modal Edição de Itens (Valores / Pacotes em todas as fases com recálculo BigDecimal)
    if (uiState.editingCycle != null) {
        EditarItensFaturaModal(
            cycle = uiState.editingCycle!!,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.closeEditCycleModal() },
            onSaveRoute = { route, newPackages, newAmount ->
                viewModel.updateRouteItem(route, newPackages, newAmount)
            },
            onSaveDaily = { daily, newAmount ->
                viewModel.updateDailyTotalItem(daily, newAmount)
            }
        )
    }

    // Modal de Ajustes Financeiros com os 9 Subtipos
    if (uiState.adjustingCycle != null) {
        AjustesFaturaModal(
            cycle = uiState.adjustingCycle!!,
            uiState = uiState,
            onDismiss = { viewModel.closeAdjustmentModal() },
            onSubtypeChanged = { viewModel.onAdjustmentSubtypeChanged(it) },
            onAmountChanged = { viewModel.onAdjustmentAmountChanged(it) },
            onDescriptionChanged = { viewModel.onAdjustmentDescriptionChanged(it) },
            onNotesChanged = { viewModel.onAdjustmentNotesChanged(it) },
            onDateChanged = { viewModel.onAdjustmentDateChanged(it) },
            onSaveAdjustment = { viewModel.saveAdjustment() },
            onDeleteAdjustment = { viewModel.deleteAdjustment(it) }
        )
    }
}

@Composable
fun FaturaCardItem(
    item: BillingCycleWithTotals,
    onPay: () -> Unit,
    onEditItems: () -> Unit,
    onAdjustments: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val status = item.cycle.status
    val isPaid = status == "pago"
    val isAVencer = status == "a_vencer"
    val isEmAberto = status == "em_aberto"

    val statusBadgeColor = when {
        isPaid -> GreenNeon
        isAVencer -> OrangeNeon
        else -> BlueInfo
    }
    val statusLabel = when (status) {
        "pago" -> "Pago / Recebido"
        "a_vencer" -> "A Vencer"
        "em_aberto" -> "Em Aberto"
        "atrasado" -> "Atrasado"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isOverdue) RedAlert else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha 1: Nome da Plataforma e Valor Total Líquido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.platformName,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Período: ${item.cycle.periodStart.format(dateFormatter)} a ${item.cycle.periodEnd.format(dateFormatter)}${if (!item.cycle.includeEndDate) " (excl.)" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.totalAmount.formatCurrency(),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isPaid) GreenNeon else OrangeNeon
                )
            }

            // Linha 2: Resumo de Pacotes e Corridas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${item.routeCount} corridas (${item.packageCount} pacotes)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.dailyCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.dailyCount} diárias",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.sessionsCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.sessionsCount} sessões",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.adjustmentsCount > 0) {
                    Surface(
                        color = if (item.adjustmentsTotal >= BigDecimal.ZERO) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${item.adjustmentsCount} ajustes (${item.adjustmentsTotal.formatCurrency()})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.adjustmentsTotal >= BigDecimal.ZERO) GreenNeon else RedAlert,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Linha 3: Vencimento ou Recebimento e Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaid && item.cycle.paymentReceivedDate != null) {
                    Text(
                        text = "Recebido em: ${item.cycle.paymentReceivedDate.format(dateFormatter)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenNeon
                    )
                } else {
                    Text(
                        text = "Previsão: ${item.cycle.expectedPaymentDate.format(dateFormatter)}",
                        fontSize = 12.sp,
                        fontWeight = if (item.isOverdue) FontWeight.Black else FontWeight.Bold,
                        color = if (item.isOverdue) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Linha 4: Botões de Ação Estruturados e Padronizados (Design System)
            if (isAVencer) {
                // Aba "A Vencer": Botão primário full-width [ Liquidar / Baixar Repasse ] em GreenNeon
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liquidar / Baixar Repasse",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linha secundária de ações com altura fixa de 38.dp e padding compacto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Detalhes
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Detalhes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Editar
                    OutlinedButton(
                        onClick = onEditItems,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OrangeNeon
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Ajustes
                    OutlinedButton(
                        onClick = onAdjustments,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BlueInfo
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Ajustes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 4. Excluir (OutlinedIconButton Harmonizado)
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = RedAlert.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f))
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir fatura",
                                tint = RedAlert,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (isEmAberto) {
                // Aba "Em Aberto": Botão primário full-width [ Liquidar / Baixar Ciclo ] em OrangeNeon
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liquidar / Baixar Ciclo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linha secundária de ações com altura fixa de 38.dp e padding compacto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Detalhes
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Detalhes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Editar
                    OutlinedButton(
                        onClick = onEditItems,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OrangeNeon
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Ajustes
                    OutlinedButton(
                        onClick = onAdjustments,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BlueInfo
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Ajustes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 4. Excluir (OutlinedIconButton Harmonizado)
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = RedAlert.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f))
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir fatura",
                                tint = RedAlert,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Aba "Pago" (ou outros status): Linha secundária de ações com altura fixa de 38.dp e padding compacto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Detalhes
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Detalhes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Editar
                    OutlinedButton(
                        onClick = onEditItems,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OrangeNeon
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Ajustes
                    OutlinedButton(
                        onClick = onAdjustments,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BlueInfo
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Ajustes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 4. Excluir (OutlinedIconButton Harmonizado)
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = RedAlert.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f))
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir fatura",
                                tint = RedAlert,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Fatura?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Deseja excluir a fatura de ${item.platformName}?\nAs corridas e diárias vinculadas retornarão ao estado desvinculado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun PagoMonthHeaderCard(
    monthGroup: FaturasPaidMonthGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenNeon.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monthGroup.monthLabel,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${monthGroup.totalInvoices} ${if (monthGroup.totalInvoices == 1) "fatura" else "faturas"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = monthGroup.totalAmount.formatCurrency(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = GreenNeon
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Recolher mês" else "Expandir mês",
                    tint = GreenNeon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PagoWeekHeaderCard(
    weekGroup: FaturasPaidWeekGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekGroup.weekLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${weekGroup.totalItems} ${if (weekGroup.totalItems == 1) "fatura" else "faturas"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = weekGroup.totalAmount.formatCurrency(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher semana" else "Expandir semana",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaFaturaModal(
    uiState: FaturasUiState,
    onDismiss: () -> Unit,
    onPlatformSelected: (String) -> Unit,
    onPeriodStartSelected: (LocalDate) -> Unit,
    onPeriodEndSelected: (LocalDate) -> Unit,
    onIncludeEndDateChanged: (Boolean) -> Unit,
    onExpectedDateSelected: (LocalDate) -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    var platformExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FECHAR CICLO / NOVA FATURA",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = OrangeNeon
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // Seletor de Plataforma
            ExposedDropdownMenuBox(
                expanded = platformExpanded,
                onExpandedChange = { platformExpanded = !platformExpanded }
            ) {
                val selectedPlat = uiState.platforms.find { it.id == uiState.newCyclePlatformId }
                OutlinedTextField(
                    value = selectedPlat?.name ?: "Selecione a plataforma",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Plataforma") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = platformExpanded,
                    onDismissRequest = { platformExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    uiState.platforms.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                onPlatformSelected(p.id)
                                platformExpanded = false
                            }
                        )
                    }
                }
            }

            // Período Início e Fim
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newCyclePeriodStart.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Início do Período") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePicker(context, uiState.newCyclePeriodStart) { onPeriodStartSelected(it) }
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showDatePicker(context, uiState.newCyclePeriodStart) { onPeriodStartSelected(it) }
                        },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.newCyclePeriodEnd.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fim do Período") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePicker(context, uiState.newCyclePeriodEnd) { onPeriodEndSelected(it) }
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showDatePicker(context, uiState.newCyclePeriodEnd) { onPeriodEndSelected(it) }
                        },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Switch: Incluir valores da data final no cálculo?
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Incluir valores da data final no cálculo?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.newCycleIncludeEndDate) "Data final inclusiva (até às 23:59)" else "Data final exclusiva (corte às 00:00)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.newCycleIncludeEndDate,
                        onCheckedChange = { onIncludeEndDateChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrangeNeon,
                            checkedTrackColor = OrangeNeon.copy(alpha = 0.4f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Data Prevista para Pagamento
            OutlinedTextField(
                value = uiState.newCycleExpectedDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Data Prevista para Pagamento") },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePicker(context, uiState.newCycleExpectedDate) { onExpectedDateSelected(it) }
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker(context, uiState.newCycleExpectedDate) { onExpectedDateSelected(it) }
                    },
                shape = RoundedCornerShape(12.dp)
            )

            // Caixa de Informação Explicativa
            Surface(
                color = BlueInfo.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BlueInfo.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BlueInfo, modifier = Modifier.size(20.dp))
                    Text(
                        text = "O sistema sincroniza rotas, diárias e ajustes do período especificado recalculando o valor líquido com precisão estrita em BigDecimal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Botão Submit
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSaving && uiState.newCyclePlatformId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("GERAR FATURA E VINCULAR TRANSAÇÕES", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LiquidarFaturaModal(
    cycle: BillingCycleWithTotals,
    paymentDate: LocalDate,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Liquidar Fatura", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Confirme o recebimento do repasse de ${cycle.platformName}:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Valor Total: ${cycle.totalAmount.formatCurrency()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = GreenNeon
                        )
                        Text(
                            text = "Período: ${cycle.cycle.periodStart.format(dateFormatter)} a ${cycle.cycle.periodEnd.format(dateFormatter)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = paymentDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data do Recebimento") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePicker(context, paymentDate) { onDateSelected(it) }
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GreenNeon, contentColor = Color.Black)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Confirmar Recebimento", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesFaturaModal(
    cycle: BillingCycleWithTotals,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DETALHES DA FATURA",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = OrangeNeon
                    )
                    Text(
                        text = cycle.platformName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Período de Apuração", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${cycle.cycle.periodStart.format(dateFormatter)} ➔ ${cycle.cycle.periodEnd.format(dateFormatter)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Previsão Pagamento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            cycle.cycle.expectedPaymentDate.format(dateFormatter),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (cycle.cycle.paymentReceivedDate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data Efetiva de Recebimento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                cycle.cycle.paymentReceivedDate.format(dateFormatter),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotas (${cycle.routeCount}) • Pacotes (${cycle.packageCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            cycle.routeAmount.add(cycle.tipTotal).add(cycle.bonusTotal).formatCurrency(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (cycle.dailyCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Totais diários (${cycle.dailyCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                cycle.dailyAmount.formatCurrency(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (cycle.sessionsCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sessões de Parceiros (${cycle.sessionsCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                cycle.sessionAmount.formatCurrency(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (cycle.adjustmentsCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ganhos Extras / Bônus", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "+ ${cycle.adjustmentsCredit.formatCurrency()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descontos / Extravios", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "- ${cycle.adjustmentsDebit.formatCurrency()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Líquido", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text(cycle.totalAmount.formatCurrency(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = OrangeNeon)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- Modal Edição de Itens (Valores / Pacotes) ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarItensFaturaModal(
    cycle: BillingCycleWithTotals,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveRoute: (Route, Int, BigDecimal) -> Unit,
    onSaveDaily: (DailyTotal, BigDecimal) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EDITAR ITENS DA FATURA",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = OrangeNeon
                    )
                    Text(
                        text = "Altere valores e pacotes com recálculo imediato (BigDecimal)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // Seção de Rotas
            if (cycle.routes.isNotEmpty()) {
                Text(
                    text = "CORRIDAS / ROTAS VINCULADAS (${cycle.routes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                cycle.routes.forEach { route ->
                    RouteEditItemCard(route = route, isSaving = isSaving, onSave = { pkgs, amt -> onSaveRoute(route, pkgs, amt) })
                }
            }

            // Seção de Diárias
            if (cycle.dailyTotals.isNotEmpty()) {
                Text(
                    text = "DIÁRIAS VINCULADAS (${cycle.dailyTotals.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                cycle.dailyTotals.forEach { daily ->
                    DailyEditItemCard(daily = daily, isSaving = isSaving, onSave = { amt -> onSaveDaily(daily, amt) })
                }
            }

            if (cycle.routes.isEmpty() && cycle.dailyTotals.isEmpty()) {
                Text(
                    text = "Nenhuma corrida ou diária vinculada diretamente nesta fatura.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Concluir Edição", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RouteEditItemCard(
    route: Route,
    isSaving: Boolean,
    onSave: (Int, BigDecimal) -> Unit
) {
    var packageText by remember(route.id, route.packageCount) { mutableStateOf(route.packageCount.toString()) }
    var amountText by remember(route.id, route.amount) { mutableStateOf(route.amount.toPlainString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${route.occurredAt.toLocalDate().format(dateFormatter)} • ${route.origin ?: "Rota"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = route.amount.formatCurrency(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangeNeon
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = packageText,
                    onValueChange = { packageText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Pacotes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.') },
                    label = { Text("Valor Total (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = {
                        val pkgs = packageText.toIntOrNull() ?: route.packageCount
                        val amt = amountText.toBigDecimalOrNull() ?: route.amount
                        onSave(pkgs, amt)
                    },
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salvar", tint = GreenNeon)
                }
            }
        }
    }
}

@Composable
private fun DailyEditItemCard(
    daily: DailyTotal,
    isSaving: Boolean,
    onSave: (BigDecimal) -> Unit
) {
    var amountText by remember(daily.id, daily.amount) { mutableStateOf(daily.amount.toPlainString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${daily.occurredAt.toLocalDate().format(dateFormatter)} • Diária",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = daily.amount.formatCurrency(),
                    fontSize = 12.sp,
                    color = OrangeNeon,
                    fontWeight = FontWeight.Black
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(130.dp),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = {
                    val amt = amountText.toBigDecimalOrNull() ?: daily.amount
                    onSave(amt)
                },
                enabled = !isSaving
            ) {
                Icon(Icons.Default.Save, contentDescription = "Salvar", tint = GreenNeon)
            }
        }
    }
}

// ---------------- Modal Ajustes Financeiros (9 Tipos) ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesFaturaModal(
    cycle: BillingCycleWithTotals,
    uiState: FaturasUiState,
    onDismiss: () -> Unit,
    onSubtypeChanged: (FinancialAdjustmentSubtype) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onSaveAdjustment: () -> Unit,
    onDeleteAdjustment: (String) -> Unit
) {
    val context = LocalContext.current
    var isDescontoTab by remember { mutableStateOf(!uiState.newAdjustmentSubtype.isCredit) }

    val subtypesOptions = if (isDescontoTab) {
        listOf(
            FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO,
            FinancialAdjustmentSubtype.DESCONTO_PREVIDENCIARIO,
            FinancialAdjustmentSubtype.DESCONTO_MULTA,
            FinancialAdjustmentSubtype.OUTROS_DESCONTOS
        )
    } else {
        listOf(
            FinancialAdjustmentSubtype.BONUS,
            FinancialAdjustmentSubtype.GRATIFICACAO,
            FinancialAdjustmentSubtype.INCENTIVO,
            FinancialAdjustmentSubtype.METAS,
            FinancialAdjustmentSubtype.OUTROS_GANHOS
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AJUSTES FINANCEIROS",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = OrangeNeon
                    )
                    Text(
                        text = "Lançamentos específicos na fatura de ${cycle.platformName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // Lista de Ajustes Já Lançados
            if (cycle.adjustments.isNotEmpty()) {
                Text(
                    text = "AJUSTES LANÇADOS NESTA FATURA (${cycle.adjustments.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                cycle.adjustments.forEach { adj ->
                    val subtypeObj = FinancialAdjustmentSubtype.fromKey(adj.subtype)
                    val isCred = subtypeObj?.isCredit ?: (adj.type.lowercase() in listOf("credito", "bonus", "acrescimo"))
                    val badgeColor = if (isCred) GreenNeon else RedAlert

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = subtypeObj?.label ?: adj.type.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = badgeColor
                                )
                                Text(
                                    text = adj.description ?: "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!adj.notes.isNullOrBlank()) {
                                    Text(
                                        text = "Rastreio / Obs: ${adj.notes}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = (if (isCred) "+ " else "- ") + adj.amount.formatCurrency(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = badgeColor
                                )
                                IconButton(
                                    onClick = { onDeleteAdjustment(adj.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedAlert, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Alternador Descontos (-) vs Ganhos (+)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        isDescontoTab = true
                        onSubtypeChanged(FinancialAdjustmentSubtype.OUTROS_DESCONTOS)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDescontoTab) RedAlert.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDescontoTab) RedAlert else Color.Transparent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Descontos (-)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDescontoTab) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = {
                        isDescontoTab = false
                        onSubtypeChanged(FinancialAdjustmentSubtype.BONUS)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isDescontoTab) GreenNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (!isDescontoTab) GreenNeon else Color.Transparent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ganhos (+)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (!isDescontoTab) GreenNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Seletor dos Subtipos
            Text(
                text = "TIPO DE LANÇAMENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subtypesOptions.forEach { subtype ->
                    val isSelected = uiState.newAdjustmentSubtype == subtype
                    val activeColor = if (subtype.isCredit) GreenNeon else RedAlert

                    Surface(
                        onClick = { onSubtypeChanged(subtype) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) activeColor else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subtype.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = activeColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Campo de Rastreio (se for PRODUTO_EXTRAVIADO)
            if (uiState.newAdjustmentSubtype == FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO) {
                OutlinedTextField(
                    value = uiState.newAdjustmentNotes,
                    onValueChange = { onNotesChanged(it) },
                    label = { Text("Código de Rastreio / Detalhes do Extravio *") },
                    placeholder = { Text("Ex: BR123456789 - Pacote não entregue") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedAlert,
                        focusedLabelColor = RedAlert,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Campos Valor e Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newAdjustmentAmount,
                    onValueChange = { onAmountChanged(it) },
                    label = { Text("Valor (R$) *") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.newAdjustmentDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePicker(context, uiState.newAdjustmentDate) { onDateChanged(it) }
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker(context, uiState.newAdjustmentDate) { onDateChanged(it) } },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Descrição adicional
            OutlinedTextField(
                value = uiState.newAdjustmentDescription,
                onValueChange = { onDescriptionChanged(it) },
                label = { Text("Descrição / Motivo (Opcional)") },
                placeholder = { Text("Ex: Meta quinzenal atingida, avaria leve...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeNeon,
                    focusedLabelColor = OrangeNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Botão Adicionar Ajuste
            Button(
                onClick = onSaveAdjustment,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDescontoTab) RedAlert else GreenNeon,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDescontoTab) "LANÇAR DESCONTO" else "LANÇAR GANHO",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
