package com.fernando.centraldomotorista.ui.screens.historico

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    viewModel: HistoricoViewModel = viewModel(),
    onNavigateToEdit: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Barra de Busca Arredondada
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Segmented Control (Pílulas)
            TabsSegmentedControl(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setTab(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && uiState.allTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangeNeon)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 3. Card "SALDO DE HOJE"
                    item {
                        SaldoDeHojeCard(
                            saldoHoje = uiState.saldoHoje,
                            entradasHoje = uiState.entradasHoje,
                            saidasHoje = uiState.saidasHoje,
                            metaDiaria = uiState.metaDiaria,
                            metaPercent = uiState.metaPercent,
                            faltamParaMeta = uiState.faltamParaMeta,
                            onEditGoalClick = { viewModel.openEditGoalDialog() }
                        )
                    }

                    // 4. Lista Hierárquica de Meses / Semanas / Dias
                    if (uiState.monthGroups.isEmpty()) {
                        item {
                            EmptyHistoryState()
                        }
                    } else {
                        items(uiState.monthGroups, key = { it.monthKey }) { monthGroup ->
                            val isExpanded = uiState.expandedMonths.contains(monthGroup.monthKey)
                            MonthAccordionItem(
                                month = monthGroup,
                                isExpanded = isExpanded,
                                expandedWeeks = uiState.expandedWeeks,
                                onToggleMonth = { viewModel.toggleMonth(monthGroup.monthKey) },
                                onToggleWeek = { viewModel.toggleWeek(it) },
                                onEditTransaction = { tx ->
                                    val editRoute = viewModel.getEditRoute(tx)
                                    onNavigateToEdit(editRoute)
                                },
                                onDeleteTransaction = { tx ->
                                    viewModel.promptDelete(tx)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de Confirmação de Exclusão
    uiState.itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = {
                Text(
                    text = "Excluir Transação",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Deseja realmente excluir '${item.title}' no valor de ${item.amount.formatCurrency()}? Esta ação não pode ser desfeita.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Modal de Edição de Meta Diária
    if (uiState.isEditGoalDialogOpen) {
        var goalText by remember { mutableStateOf(uiState.metaDiaria.toPlainString()) }
        AlertDialog(
            onDismissRequest = { viewModel.closeEditGoalDialog() },
            title = {
                Text(
                    text = "Editar Meta Diária",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Defina sua meta diária de ganhos (R$):",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { str ->
                            goalText = str.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            cursorColor = OrangeNeon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = goalText.toBigDecimalOrNull()
                        if (parsed != null && parsed > BigDecimal.ZERO) {
                            viewModel.saveDailyGoal(parsed)
                        } else {
                            Toast.makeText(context, "Informe um valor válido maior que zero.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeEditGoalDialog() }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// -----------------------------------------------------------------------------------------
// Componentes de Interface do Histórico
// -----------------------------------------------------------------------------------------

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "BUSCAR TRANSAÇÕES",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(OrangeNeon),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar busca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = Icons.Default.FilterAlt,
                contentDescription = "Filtro",
                tint = OrangeNeon,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TabsSegmentedControl(
    selectedTab: HistoricoTab,
    onTabSelected: (HistoricoTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoricoTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.surface,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                onClick = { onTabSelected(tab) }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SaldoDeHojeCard(
    saldoHoje: BigDecimal,
    entradasHoje: BigDecimal,
    saidasHoje: BigDecimal,
    metaDiaria: BigDecimal,
    metaPercent: Int,
    faltamParaMeta: BigDecimal,
    onEditGoalClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Label superior
            Text(
                text = "SALDO DE HOJE",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Valor Grande em Itálico Negrito Laranja
            Text(
                text = saldoHoje.formatCurrency(),
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = OrangeNeon
                )
            )

            // Strip de Entradas vs Saídas
            if (entradasHoje > BigDecimal.ZERO || saidasHoje > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Entradas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GreenNeon.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = GreenNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "ENTRADAS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+ ${entradasHoje.formatCurrency()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon
                            )
                        }
                    }

                    // Divisor
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Saídas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RedAlert.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = RedAlert,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "SAÍDAS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "- ${saidasHoje.formatCurrency()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de Progresso / Termômetro
            val progressFraction = (metaPercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF3B30),
                                    Color(0xFFFF9500),
                                    Color(0xFFFFCC00),
                                    Color(0xFF34C759)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linha Inferior com Meta e Percentual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onEditGoalClick() }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = "META: ${metaDiaria.formatCurrency()}",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar meta",
                        tint = OrangeNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }

                if (metaPercent >= 100) {
                    Text(
                        text = "Meta Atingida! 🎉",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                    )
                } else {
                    Text(
                        text = "$metaPercent% ATINGIDO",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MonthAccordionItem(
    month: MonthGroup,
    isExpanded: Boolean,
    expandedWeeks: Set<String>,
    onToggleMonth: () -> Unit,
    onToggleWeek: (String) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabeçalho do Mês
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isExpanded) OrangeNeon.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            onClick = onToggleMonth
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = month.label,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = OrangeNeon
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = month.balance.formatCurrency(),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (month.balance >= BigDecimal.ZERO) OrangeNeon else RedAlert
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher mês" else "Expandir mês",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Conteúdo do Mês (Semanas)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                month.weeks.forEach { week ->
                    val isWeekExpanded = expandedWeeks.contains(week.weekKey)
                    WeekAccordionItem(
                        week = week,
                        isExpanded = isWeekExpanded,
                        onToggleWeek = { onToggleWeek(week.weekKey) },
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction
                    )
                }

                // Faixa Laranja Vibrante de Saldo do Mês
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = OrangeNeon
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALDO DO MÊS (${month.label})",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        )
                        Text(
                            text = month.balance.formatCurrency(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekAccordionItem(
    week: WeekGroup,
    isExpanded: Boolean,
    onToggleWeek: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Cabeçalho da Semana
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isExpanded) OrangeNeon.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isExpanded) OrangeNeon.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            onClick = onToggleWeek
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = week.label,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = week.balance.formatCurrency(),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (week.balance >= BigDecimal.ZERO) OrangeNeon else RedAlert
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher semana" else "Expandir semana",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Conteúdo da Semana (Dias)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                week.days.forEach { day ->
                    DaySection(
                        day = day,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction
                    )
                }

                // Faixa de Fechamento da Semana
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = OrangeNeon.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FECHAMENTO DA SEMANA",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangeNeon
                            )
                        )
                        Text(
                            text = week.balance.formatCurrency(),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (week.balance >= BigDecimal.ZERO) OrangeNeon else RedAlert
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaySection(
    day: DayGroup,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Título do Dia: | TERÇA-FEIRA, 15 DE SET
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = day.label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Lista de Transações do Dia
        day.items.forEach { item ->
            TransactionCard(
                item = item,
                onEdit = { onEditTransaction(item) },
                onDelete = { onDeleteTransaction(item) }
            )
        }

        // Linha "SALDO DO DIA"
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SALDO DO DIA",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = day.balance.formatCurrency(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (day.balance >= BigDecimal.ZERO) OrangeNeon else RedAlert
                    )
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    item: TransactionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPositive = item.type == TransactionType.GANHO

    // Configuração de Ícone e Cor de Categoria
    val (iconVector, iconBgColor, iconTintColor) = when (item.sourceType) {
        TransactionSourceType.ROUTE -> Triple(Icons.Default.Inventory2, OrangeNeon.copy(alpha = 0.15f), OrangeNeon)
        TransactionSourceType.DAILY_TOTAL -> Triple(Icons.Default.CalendarToday, OrangeNeon.copy(alpha = 0.15f), OrangeNeon)
        TransactionSourceType.EXPENSE -> {
            val cat = item.rawExpense?.category?.lowercase() ?: item.category.lowercase()
            when {
                cat.contains("combustivel") || cat.contains("abastecimento") -> Triple(Icons.Default.LocalGasStation, Color(0xFFFFB300).copy(alpha = 0.15f), Color(0xFFFFB300))
                cat.contains("manutencao") || cat.contains("peca") -> Triple(Icons.Default.Build, BlueInfo.copy(alpha = 0.15f), BlueInfo)
                cat.contains("alimentacao") -> Triple(Icons.Default.Restaurant, GreenNeon.copy(alpha = 0.15f), GreenNeon)
                else -> Triple(Icons.Default.Receipt, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone da Categoria
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Detalhes da Transação
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.subtitle,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.meta1.isNullOrBlank()) {
                    Text(
                        text = item.meta1,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Observação de subtractRoutes caso aplicável
                if (item.sourceType == TransactionSourceType.DAILY_TOTAL && item.subtractRoutes) {
                    Text(
                        text = "Líquido: ${item.netAmount.formatCurrency()} (rotas descontadas)",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = OrangeNeon
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!item.meta2.isNullOrBlank()) {
                    Text(
                        text = item.meta2,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Coluna da Direita: Valor, Botões de Ação e Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Valor formatado
                Text(
                    text = "${if (isPositive) "+" else "-"}${item.amount.formatCurrency()}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPositive) OrangeNeon else RedAlert
                    )
                )

                // Botões de Ação: Editar (Lápis) e Excluir (Lixeira)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onEdit() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar transação",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onDelete() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir transação",
                                tint = RedAlert,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Tag Badge
                if (!item.tag.isNullOrBlank()) {
                    val tagBg = when {
                        item.tag == "PAGO" -> GreenNeon.copy(alpha = 0.15f)
                        item.tag == "A RECEBER" -> YellowGold.copy(alpha = 0.15f)
                        isPositive -> GreenNeon.copy(alpha = 0.15f)
                        else -> RedAlert.copy(alpha = 0.15f)
                    }
                    val tagText = when {
                        item.tag == "PAGO" -> GreenNeon
                        item.tag == "A RECEBER" -> YellowGold
                        isPositive -> GreenNeon
                        else -> RedAlert
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagBg
                    ) {
                        Text(
                            text = item.tag,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagText
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Nenhuma transação encontrada.",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
