package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeEmoji
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerRoutesScreen(
    partnerId: String,
    viewModel: PartnerRoutesViewModel = viewModel(),
    onNavigateToNewSession: (partnerId: String) -> Unit,
    onNavigateToCloseSession: (sessionId: String) -> Unit,
    onNavigateToEditPartner: (partnerId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    LaunchedEffect(partnerId) {
        viewModel.loadData(partnerId)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    val partner = uiState.partner
    val routeMap = remember(uiState.routes) { uiState.routes.associateBy { it.id } }

    // Diálogo de Confirmação de Exclusão de Sessão
    val deletingSession = uiState.deletingSession
    if (deletingSession != null) {
        val hasExpense = !deletingSession.expenseId.isNullOrBlank()
        var deleteExpenseAlso by remember { mutableStateOf(hasExpense) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteSession() },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAlert, modifier = Modifier.size(36.dp))
            },
            title = { Text("Excluir Sessão de Entrega", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Deseja realmente excluir o registro desta sessão de entrega?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (hasExpense) {
                        Surface(
                            color = OrangeNeon.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Esta sessão gerou um lançamento financeiro no histórico de despesas.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { deleteExpenseAlso = !deleteExpenseAlso }
                                ) {
                                    Checkbox(
                                        checked = deleteExpenseAlso,
                                        onCheckedChange = { deleteExpenseAlso = it },
                                        colors = CheckboxDefaults.colors(checkedColor = OrangeNeon)
                                    )
                                    Text(
                                        text = "Excluir também a despesa vinculada",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDeleteSession(deletingSession, deleteExpenseAlso)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDeleteSession() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edição de Dados da Sessão
    val editingSession = uiState.editingSession
    if (editingSession != null) {
        EditSessionDialog(
            session = editingSession,
            routes = uiState.routes,
            onDismiss = { viewModel.closeEditSession() },
            onSave = { updated -> viewModel.saveEditedSession(updated) }
        )
    }

    // Diálogo de Detalhes da Sessão Concluída (Modo Visualização)
    val viewDetailSession = uiState.viewDetailSession
    if (viewDetailSession != null) {
        SessionDetailDialog(
            session = viewDetailSession,
            routeName = viewDetailSession.routeId?.let { routeMap[it]?.name } ?: "Sem rota definida",
            partnerName = partner?.fullName ?: "",
            timeFormatter = timeFormatter,
            onDismiss = { viewModel.closeViewDetailSession() },
            onEdit = {
                viewModel.closeViewDetailSession()
                viewModel.openEditSession(viewDetailSession)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ROTAS DOS PARCEIROS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                    IconButton(onClick = { viewModel.loadData(partnerId) }) {
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeNeon)
            }
        } else if (partner == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entregador não encontrado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
            ) {
                // 1. SEÇÃO "DADOS MOTORISTA" (Visualização + atalho WhatsApp + Lápis de edição)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PartnerAvatar(
                                        photoUrl = partner.photoUrl,
                                        name = partner.fullName,
                                        size = 54.dp
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = partner.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = getDeliveryTypeEmoji(partner.deliveryType),
                                                fontSize = 16.sp
                                            )
                                        }

                                        // Classificação em estrelas
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            for (i in 1..5) {
                                                Icon(
                                                    imageVector = if (i <= partner.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = null,
                                                    tint = if (i <= partner.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = getDeliveryTypeLabel(partner.deliveryType),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Ícone de lápis para editar dados do entregador
                                IconButton(
                                    onClick = { onNavigateToEditPartner(partner.id) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(OrangeNeon.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar Dados Entregador",
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Linha de Contato com Celular tocável para abrir WhatsApp
                            val phoneDigits = partner.phone?.filter { it.isDigit() } ?: ""
                            if (phoneDigits.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val fullNumber = if (phoneDigits.startsWith("55")) phoneDigits else "55$phoneDigits"
                                            val url = "https://wa.me/$fullNumber"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "WhatsApp",
                                            tint = GreenNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Conversar no WhatsApp",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatPhone(partner.phone ?: phoneDigits),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = GreenNeon
                                            )
                                        }
                                    }

                                    Surface(
                                        color = GreenNeon.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(12.dp))
                                            Text("Abrir Conversa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenNeon)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhum telefone celular cadastrado.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. MÉTRICAS DO PERÍODO (Cards simétricos - com BigDecimal.formatCurrency())
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total de pacotes entregues no período
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, GreenNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenNeon,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "ENTREGUES NO PERÍODO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${uiState.monthDeliveredCount}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GreenNeon,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "pacotes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        // Valor total pago no período (R$) em BigDecimal
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, OrangeNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "TOTAL PAGO NO PERÍODO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.monthTotalAmountPaid.formatCurrency(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OrangeNeon,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "em repasses",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3. BOTÃO "INICIAR SESSÃO DE ENTREGA"
                item {
                    Button(
                        onClick = { onNavigateToNewSession(partner.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar Sessão de Entrega",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }

                // 4. SELETOR DE PERÍODO (PRESETS + INTERVALO PERSONALIZADO)
                item {
                    PartnerPeriodSelector(
                        periodFilter = uiState.periodFilter,
                        isDropdownExpanded = uiState.isPeriodDropdownExpanded,
                        onToggleDropdown = { viewModel.togglePeriodDropdown() },
                        onSelectPreset = { preset -> viewModel.applyPeriodPreset(preset) },
                        onApplyCustomRange = { start, end -> viewModel.applyCustomPeriod(start, end) }
                    )
                }

                // 5. CABEÇALHO DA LISTA
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSÕES DESTE PARCEIRO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.monthGroups.sumOf { it.weeks.sumOf { w -> w.days.sumOf { d -> d.sessions.size } } }} sessão(ões)",
                            fontSize = 12.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 6. CASCATA HIERÁRQUICA MULTINÍVEL (Mês ➔ Semana ➔ Dia ➔ Sessão)
                if (uiState.monthGroups.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(32.dp))
                                Text(
                                    text = if (uiState.sessions.isEmpty()) "Nenhuma sessão registrada" else "Nenhuma sessão encontrada no período selecionado",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.sessions.isEmpty()) {
                                        "Toque em 'Iniciar Sessão de Entrega' acima para abrir a primeira rota deste parceiro."
                                    } else {
                                        "Altere o período de exibição acima para visualizar outros lançamentos."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                if (uiState.sessions.isNotEmpty() && uiState.periodFilter.preset != PartnerPeriodPreset.SEMANA) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetPeriodFilter() },
                                        border = BorderStroke(1.dp, OrangeNeon)
                                    ) {
                                        Text("Redefinir para Esta Semana", color = OrangeNeon, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.monthGroups, key = { it.monthKey }) { monthGroup ->
                        val isExpanded = uiState.expandedMonths.contains(monthGroup.monthKey)
                        PartnerMonthAccordionItem(
                            month = monthGroup,
                            isExpanded = isExpanded,
                            expandedWeeks = uiState.expandedWeeks,
                            onToggleMonth = { viewModel.toggleMonth(monthGroup.monthKey) },
                            onToggleWeek = { viewModel.toggleWeek(it) },
                            onSessionClick = { session ->
                                if (session.endTime == null) {
                                    onNavigateToCloseSession(session.id)
                                } else {
                                    viewModel.openViewDetailSession(session)
                                }
                            },
                            onEditSession = { session -> viewModel.openEditSession(session) },
                            onDeleteSession = { session -> viewModel.promptDeleteSession(session) },
                            routeMap = routeMap,
                            timeFormatter = timeFormatter
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente de seleção de período (Presets + Intervalo Customizado).
 */
@Composable
private fun PartnerPeriodSelector(
    periodFilter: PartnerPeriodFilter,
    isDropdownExpanded: Boolean,
    onToggleDropdown: () -> Unit,
    onSelectPreset: (PartnerPeriodPreset) -> Unit,
    onApplyCustomRange: (LocalDate, LocalDate) -> Unit
) {
    val context = LocalContext.current
    val chevronRotation by animateFloatAsState(targetValue = if (isDropdownExpanded) 180f else 0f)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabeçalho do seletor (Barra clicável)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isDropdownExpanded) OrangeNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            onClick = onToggleDropdown
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filtro de Período",
                        tint = OrangeNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PERÍODO: ${periodFilter.preset.label.uppercase()}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isDropdownExpanded) "Recolher opções" else "Expandir opções",
                    tint = OrangeNeon,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
        }

        // Grade de Presets (Dia, Semana, Quinzena, Mês, Ano, Intervalo)
        AnimatedVisibility(
            visible = isDropdownExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selecione o intervalo de exibição:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = PartnerPeriodPreset.values()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (rowPresets in presets.toList().chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    val isSelected = preset == periodFilter.preset
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        onClick = { onSelectPreset(preset) }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = preset.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
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

        // Quando INTERVALO (Personalizado) está ativo: exibe dois campos de data lado a lado
        if (periodFilter.preset == PartnerPeriodPreset.PERSONALIZADO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Campo "De"
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showNativeDatePicker(
                                context = context,
                                currentDate = periodFilter.customStart,
                                maxDate = periodFilter.customEnd
                            ) { newStart ->
                                onApplyCustomRange(newStart, periodFilter.customEnd)
                            }
                        },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "De",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = periodFilter.customStart.format(dateFormatter),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Campo "Até"
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showNativeDatePicker(
                                context = context,
                                currentDate = periodFilter.customEnd,
                                minDate = periodFilter.customStart,
                                maxDate = LocalDate.now()
                            ) { newEnd ->
                                onApplyCustomRange(periodFilter.customStart, newEnd)
                            }
                        },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Até",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = periodFilter.customEnd.format(dateFormatter),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
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

/**
 * Item acordeão de Nível Mês.
 */
@Composable
private fun PartnerMonthAccordionItem(
    month: PartnerSessionMonthGroup,
    isExpanded: Boolean,
    expandedWeeks: Set<String>,
    onToggleMonth: () -> Unit,
    onToggleWeek: (String) -> Unit,
    onSessionClick: (DeliveryPartnerSession) -> Unit,
    onEditSession: (DeliveryPartnerSession) -> Unit,
    onDeleteSession: (DeliveryPartnerSession) -> Unit,
    routeMap: Map<String, DeliveryRoute>,
    timeFormatter: DateTimeFormatter
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
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
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = "${month.totalDelivered} pacs • ${month.totalAmountPaid.formatCurrency()}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher mês" else "Expandir mês",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation)
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
                    PartnerWeekAccordionItem(
                        week = week,
                        isExpanded = isWeekExpanded,
                        onToggleWeek = { onToggleWeek(week.weekKey) },
                        onSessionClick = onSessionClick,
                        onEditSession = onEditSession,
                        onDeleteSession = onDeleteSession,
                        routeMap = routeMap,
                        timeFormatter = timeFormatter
                    )
                }

                // Card de Fechamento do Mês
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = OrangeNeon
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TOTAL DO MÊS (${month.label}): ${month.totalDelivered} PACOTES • ${month.totalAmountPaid.formatCurrency()}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Item acordeão de Nível Semana com linha guia vertical.
 */
@Composable
private fun PartnerWeekAccordionItem(
    week: PartnerSessionWeekGroup,
    isExpanded: Boolean,
    onToggleWeek: () -> Unit,
    onSessionClick: (DeliveryPartnerSession) -> Unit,
    onEditSession: (DeliveryPartnerSession) -> Unit,
    onDeleteSession: (DeliveryPartnerSession) -> Unit,
    routeMap: Map<String, DeliveryRoute>,
    timeFormatter: DateTimeFormatter
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
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
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = "${week.totalDelivered} pacs • ${week.totalAmountPaid.formatCurrency()}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher semana" else "Expandir semana",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                }
            }
        }

        // Conteúdo da Semana (Dias) com Ramificação em Cascata (Linha Conectora Vertical)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 10.dp, end = 2.dp)
            ) {
                // Linha Guia Vertical
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    week.days.forEach { day ->
                        PartnerDaySection(
                            day = day,
                            onSessionClick = onSessionClick,
                            onEditSession = onEditSession,
                            onDeleteSession = onDeleteSession,
                            routeMap = routeMap,
                            timeFormatter = timeFormatter
                        )
                    }

                    // Card de Fechamento da Semana
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
                                text = "${week.totalDelivered} pacotes • ${week.totalAmountPaid.formatCurrency()}",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OrangeNeon
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Seção de Nível Dia.
 */
@Composable
private fun PartnerDaySection(
    day: PartnerSessionDayGroup,
    onSessionClick: (DeliveryPartnerSession) -> Unit,
    onEditSession: (DeliveryPartnerSession) -> Unit,
    onDeleteSession: (DeliveryPartnerSession) -> Unit,
    routeMap: Map<String, DeliveryRoute>,
    timeFormatter: DateTimeFormatter
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Indicador visual em bullet vertical
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
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

        // Cards de sessão do dia
        day.sessions.forEach { session ->
            val routeName = session.routeId?.let { routeMap[it]?.name } ?: "Sem Rota"
            PartnerSessionCard(
                session = session,
                routeName = routeName,
                timeFormatter = timeFormatter,
                onClick = { onSessionClick(session) },
                onEdit = { onEditSession(session) },
                onDelete = { onDeleteSession(session) }
            )
        }

        // Linha "Total do Dia"
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
                    text = "Total do Dia:",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "${day.totalDelivered} pacotes • ${day.totalAmountPaid.formatCurrency()}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangeNeon
                    )
                )
            }
        }
    }
}

/**
 * Card individual de sessão de entrega do parceiro.
 */
@Composable
private fun PartnerSessionCard(
    session: DeliveryPartnerSession,
    routeName: String,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isInProgress = session.endTime == null
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }
    val hourFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR")) }

    val startDate = remember(session.startTime) {
        session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(dateFormatter) ?: "--/--/----"
    }
    val startHour = remember(session.startTime) {
        session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(hourFormatter) ?: "--:--"
    }
    val endHour = remember(session.endTime) {
        session.endTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(hourFormatter) ?: "--:--"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isInProgress) OrangeNeon.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha Superior: Data + Horas embaixo + Badge Status + Ações (Editar, Excluir)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isInProgress) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = startDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isInProgress) startHour else "$startHour - $endHour",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Status
                    Surface(
                        color = if (isInProgress) OrangeNeon.copy(alpha = 0.15f) else GreenNeon.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isInProgress) "Em Andamento" else "Concluído",
                            color = if (isInProgress) OrangeNeon else GreenNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    // Botão Editar
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Sessão",
                            tint = OrangeNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Botão Excluir
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Sessão",
                            tint = RedAlert,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Linha do Meio: Rota
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(15.dp))
                Text(
                    text = routeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Linha Inferior: Contagem de Pacotes e Valor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expedidos: ${session.expectedPackageCount} | Bipados: ${session.scannedCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isInProgress) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Entregues: ${session.deliveredCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = GreenNeon
                            )
                            Text(
                                text = " | ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Devolvidos: ${session.returnedCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = RedAlert
                            )
                        }
                        val durationStr = formatDuration(session.startTime, session.endTime)
                        Text(
                            text = "Duração Rota: $durationStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isInProgress) "Aberto" else session.amountPaid.formatCurrency(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isInProgress) OrangeNeon else GreenNeon
                    )
                    if (isInProgress) {
                        Text(
                            text = "Toque para fechar",
                            fontSize = 10.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para edição de campos de uma sessão existente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionDialog(
    session: DeliveryPartnerSession,
    routes: List<DeliveryRoute>,
    onDismiss: () -> Unit,
    onSave: (DeliveryPartnerSession) -> Unit
) {
    var expectedText by remember { mutableStateOf(session.expectedPackageCount.toString()) }
    var deliveredText by remember { mutableStateOf(session.deliveredCount.toString()) }
    var returnedText by remember { mutableStateOf(session.returnedCount.toString()) }
    var amountPaidText by remember { mutableStateOf(session.amountPaid.toPlainString()) }
    var selectedRouteId by remember { mutableStateOf(session.routeId) }
    var routeDropdownOpen by remember { mutableStateOf(false) }

    val selectedRouteName = remember(selectedRouteId, routes) {
        routes.firstOrNull { it.id == selectedRouteId }?.name ?: "Sem Rota"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Dados da Sessão", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rota
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { routeDropdownOpen = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedRouteName, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OrangeNeon)
                        }
                    }
                    DropdownMenu(
                        expanded = routeDropdownOpen,
                        onDismissRequest = { routeDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem Rota") },
                            onClick = {
                                selectedRouteId = null
                                routeDropdownOpen = false
                            }
                        )
                        routes.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name) },
                                onClick = {
                                    selectedRouteId = r.id
                                    routeDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Pacotes Expedidos
                OutlinedTextField(
                    value = expectedText,
                    onValueChange = { expectedText = it.filter { c -> c.isDigit() } },
                    label = { Text("Pacotes Expedidos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Pacotes Entregues e Devolvidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deliveredText,
                        onValueChange = { deliveredText = it.filter { c -> c.isDigit() } },
                        label = { Text("Entregues") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = returnedText,
                        onValueChange = { returnedText = it.filter { c -> c.isDigit() } },
                        label = { Text("Devolvidos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Valor Pago (R$)
                OutlinedTextField(
                    value = amountPaidText,
                    onValueChange = { amountPaidText = it },
                    label = { Text("Valor Pago (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exp = expectedText.toIntOrNull() ?: session.expectedPackageCount
                    val del = deliveredText.toIntOrNull() ?: session.deliveredCount
                    val ret = returnedText.toIntOrNull() ?: session.returnedCount
                    val amt = parseAmount(amountPaidText)
                    val updated = session.copy(
                        routeId = selectedRouteId,
                        expectedPackageCount = exp,
                        deliveredCount = del,
                        returnedCount = ret,
                        amountPaid = amt
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
            ) {
                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo de exibição de detalhes completos da sessão finalizada.
 */
@Composable
private fun SessionDetailDialog(
    session: DeliveryPartnerSession,
    routeName: String,
    partnerName: String,
    timeFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val startTimeStr = session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--"
    val endTimeStr = session.endTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--"
    val durationStr = formatDuration(session.startTime, session.endTime)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = OrangeNeon)
                    Text(
                        text = "Detalhes da Sessão",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        SessionShareHelper.shareSessionImage(
                            context = context,
                            session = session,
                            partnerName = partnerName,
                            routeName = routeName,
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr,
                            durationStr = durationStr
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar Imagem",
                        tint = OrangeNeon
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Entregador:", value = partnerName)
                DetailRow(label = "Rota:", value = routeName)
                DetailRow(label = "Início:", value = startTimeStr)
                DetailRow(label = "Término:", value = endTimeStr)
                DetailRow(label = "Duração Rota:", value = durationStr)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                DetailRow(label = "Pacotes Expedidos:", value = "${session.expectedPackageCount}")
                DetailRow(label = "Pacotes Bipados:", value = "${session.scannedCount}")
                DetailRow(label = "Pacotes Entregues:", value = "${session.deliveredCount}", valueColor = GreenNeon)
                DetailRow(label = "Pacotes Devolvidos:", value = "${session.returnedCount}", valueColor = RedAlert)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Valor Total Pago:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = session.amountPaid.formatCurrency(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = GreenNeon
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
            ) {
                Text("Editar Dados", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = valueColor)
    }
}

private fun formatPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 11) {
        "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
    } else if (digits.length == 10) {
        "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
    } else {
        raw
    }
}

private fun parseAmount(text: String): BigDecimal {
    val clean = text.filter { it.isDigit() || it == ',' || it == '.' }.trim()
    if (clean.isBlank()) return BigDecimal.ZERO
    val normalized = if (clean.contains(',')) {
        clean.replace(".", "").replace(',', '.')
    } else {
        clean
    }
    return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

private fun formatDuration(startTime: OffsetDateTime?, endTime: OffsetDateTime?): String {
    if (startTime == null || endTime == null) return "--:--"
    val totalMinutes = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}

private fun showNativeDatePicker(
    context: android.content.Context,
    currentDate: LocalDate,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDateSelected: (LocalDate) -> Unit
) {
    val dialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        currentDate.year,
        currentDate.monthValue - 1,
        currentDate.dayOfMonth
    )
    minDate?.let {
        dialog.datePicker.minDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    maxDate?.let {
        dialog.datePicker.maxDate = it.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    dialog.show()
}
