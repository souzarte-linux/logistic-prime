package com.fernando.centraldomotorista.ui.screens.faturas

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                            color = Color.White
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
            FaturasTab.ABERTO -> uiState.openCycles
            FaturasTab.PAGO -> uiState.paidCycles
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // 1. Cards de Resumo (KPIs)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total a Receber
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueInfo.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "A RECEBER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = BlueInfo
                            )
                            Text(
                                text = uiState.totalAReceber.formatCurrency(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.openCycles.size} faturas em aberto",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Total Recebido
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenNeon.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "RECEBIDO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = GreenNeon
                            )
                            Text(
                                text = uiState.totalRecebido.formatCurrency(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.paidCycles.size} faturas baixadas",
                                fontSize = 11.sp,
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

            // 3. Alternador de Abas (Em Aberto vs Recebidas)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FaturasTab.entries.forEach { tab ->
                        val isSelected = uiState.activeTab == tab
                        val count = if (tab == FaturasTab.ABERTO) uiState.openCycles.size else uiState.paidCycles.size
                        Surface(
                            onClick = { viewModel.onTabChanged(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tab.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                imageVector = if (uiState.activeTab == FaturasTab.ABERTO) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = GreenNeon,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (uiState.activeTab == FaturasTab.ABERTO) "Tudo em dia!" else "Nenhuma fatura recebida",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.activeTab == FaturasTab.ABERTO)
                                    "Nenhuma fatura em aberto encontrada com os filtros atuais."
                                else
                                    "As faturas que você liquidar e der baixa aparecerão aqui.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.activeTab == FaturasTab.ABERTO) {
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
            } else {
                items(currentCycles, key = { it.cycle.id }) { cycleItem ->
                    FaturaCardItem(
                        item = cycleItem,
                        onConfirm = { viewModel.confirmCycle(cycleItem) },
                        onPay = { viewModel.openPayModal(cycleItem) },
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
                            .padding(vertical = 18.dp),
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
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )
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
            onExpectedDateSelected = { viewModel.onNewCycleExpectedDateChanged(it) },
            onSubmit = { viewModel.createBillingCycle() }
        )
    }

    // Modal Liquidar / Baixar Pagamento
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
}

@Composable
fun FaturaCardItem(
    item: BillingCycleWithTotals,
    onConfirm: () -> Unit,
    onPay: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val status = item.cycle.status
    val isPendingConfirmation = status == "pendente_confirmacao"
    val isPaid = status == "pago"
    val isOpen = !isPaid && !isPendingConfirmation

    val statusBadgeColor = when {
        isPaid -> GreenNeon
        isPendingConfirmation -> Color(0xFFFFB300) // Amber
        else -> BlueInfo
    }
    val statusLabel = when (status) {
        "pendente_confirmacao" -> "Pendente Confirmação"
        "pago" -> "Recebido"
        "open" -> "A receber"
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
            // Linha 1: Nome da Plataforma e Valor Total
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
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Período: ${item.cycle.periodStart.format(dateFormatter)} a ${item.cycle.periodEnd.format(dateFormatter)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.totalAmount.formatCurrency(),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = OrangeNeon
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Linha 2: Vencimento e Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vencimento: ${item.cycle.expectedPaymentDate.format(dateFormatter)}",
                    fontSize = 12.sp,
                    fontWeight = if (item.isOverdue) FontWeight.Black else FontWeight.Bold,
                    color = if (item.isOverdue) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant
                )

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

            // Linha 3: Botões de Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPendingConfirmation) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar Fatura", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else if (isOpen) {
                    Button(
                        onClick = onPay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenNeon,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Liquidar / Baixar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = if (isPaid) Modifier.weight(1f) else Modifier
                ) {
                    Text("Detalhes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedAlert.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
                    "Deseja excluir a fatura de ${item.platformName}?\nAs corridas vinculadas serão desassociadas e voltarão a ficar disponíveis.",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaFaturaModal(
    uiState: FaturasUiState,
    onDismiss: () -> Unit,
    onPlatformSelected: (String) -> Unit,
    onPeriodStartSelected: (LocalDate) -> Unit,
    onPeriodEndSelected: (LocalDate) -> Unit,
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
                        text = "O sistema vai varrer todas as suas rotas e totais da plataforma dentro deste período e vinculá-las a esta fatura para calcular o valor exato a receber.",
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
                    Text("GERAR FATURA E VINCULAR CORRIDAS", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                            text = "Valor: ${cycle.totalAmount.formatCurrency()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
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
                        color = Color.White
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
                        Text("${cycle.cycle.periodStart.format(dateFormatter)} ➔ ${cycle.cycle.periodEnd.format(dateFormatter)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Previsão Pagamento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cycle.cycle.expectedPaymentDate.format(dateFormatter), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotas vinculadas (${cycle.routeCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cycle.routeAmount.add(cycle.tipTotal).formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Totais diários (${cycle.dailyCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(cycle.dailyAmount.formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (cycle.adjustmentsTotal != BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Acréscimos / Descontos (${cycle.adjustmentsCount})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cycle.adjustmentsTotal.formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (cycle.adjustmentsTotal > BigDecimal.ZERO) GreenNeon else RedAlert)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Líquido", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(cycle.totalAmount.formatCurrency(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = OrangeNeon)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
