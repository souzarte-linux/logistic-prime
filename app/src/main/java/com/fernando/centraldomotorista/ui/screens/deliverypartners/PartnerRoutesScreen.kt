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
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeEmoji
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.common.cards.PainelStatCard
import com.fernando.centraldomotorista.ui.common.charts.PerformanceTrendChart
import com.fernando.centraldomotorista.ui.common.period.PeriodSelector
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.util.WhatsAppHelper
import com.fernando.centraldomotorista.util.WhatsAppVariant
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PerformanceDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val LocalPlatformMap = staticCompositionLocalOf<Map<String, Platform>> { emptyMap() }

enum class PartnerRoutesTab(val label: String) {
    SESSOES("Sessões"),
    DESEMPENHO("Desempenho"),
    FINANCAS("Finanças")
}

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
    val platformMap = remember(uiState.platforms) { uiState.platforms.associateBy { it.id } }
    var selectedTab by remember { mutableStateOf(PartnerRoutesTab.SESSOES) }
    var showWhatsAppChooserPhone by remember { mutableStateOf<String?>(null) }

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
                                        text = "Excluir também a despesa vinculada no histórico financeiro",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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
                        viewModel.dismissDeleteSession()
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

    // Tela Cheia de Edição de Dados da Sessão
    val editingSession = uiState.editingSession
    if (editingSession != null) {
        SessionEditScreen(
            session = editingSession,
            routes = uiState.routes,
            partner = partner,
            platforms = uiState.platforms,
            onDismiss = { viewModel.closeEditSession() },
            onSave = { updated -> viewModel.saveEditedSession(updated) }
        )
        return
    }

    // Diálogo de Detalhes da Sessão Concluída (Modo Visualização e Edição)
    val viewDetailSession = uiState.viewDetailSession
    if (viewDetailSession != null) {
        SessionDetailDialog(
            session = viewDetailSession,
            routeName = viewDetailSession.routeId?.let { routeMap[it]?.name } ?: "Sem rota definida",
            partnerName = partner?.fullName ?: "",
            platformName = viewDetailSession.platformId?.let { platformMap[it]?.name },
            timeFormatter = timeFormatter,
            onDismiss = { viewModel.closeViewDetailSession() },
            onEdit = {
                viewModel.openEditSession(viewDetailSession)
            },
            onDelete = {
                viewModel.closeViewDetailSession()
                viewModel.promptDeleteSession(viewDetailSession)
            }
        )
    }

    // Diálogo de Seleção de WhatsApp (Pessoal vs Business)
    val phoneForWhatsApp = showWhatsAppChooserPhone
    if (phoneForWhatsApp != null) {
        AlertDialog(
            onDismissRequest = { showWhatsAppChooserPhone = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = GreenNeon,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Escolha o WhatsApp", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Identificamos mais de um aplicativo do WhatsApp instalado. Por qual deles você deseja abrir a conversa?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                WhatsAppHelper.openWhatsApp(context, phoneForWhatsApp, WhatsAppVariant.STANDARD.packageName)
                                showWhatsAppChooserPhone = null
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = GreenNeon)
                            Column {
                                Text("WhatsApp Pessoal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Aplicativo padrão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                WhatsAppHelper.openWhatsApp(context, phoneForWhatsApp, WhatsAppVariant.BUSINESS.packageName)
                                showWhatsAppChooserPhone = null
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = GreenNeon)
                            Column {
                                Text("WhatsApp Business", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Conta comercial", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showWhatsAppChooserPhone = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    CompositionLocalProvider(LocalPlatformMap provides platformMap) {
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
            val activePlatforms = remember(uiState.platforms) {
                uiState.platforms.filter { it.active }
            }

            val availableMonths = remember(uiState.sessions) {
                uiState.sessions.mapNotNull { s ->
                    (s.startTime ?: s.createdAt)?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()?.let { YearMonth.from(it) }
                }.distinct().sortedDescending()
            }

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
                                            val installed = WhatsAppHelper.getInstalledWhatsAppVariants(context)
                                            if (installed.size > 1) {
                                                showWhatsAppChooserPhone = phoneDigits
                                            } else if (installed.isNotEmpty()) {
                                                WhatsAppHelper.openWhatsApp(context, phoneDigits, installed.first().packageName)
                                            } else {
                                                WhatsAppHelper.openWhatsApp(context, phoneDigits, null)
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

                // 2. SELETOR DE PERÍODO (PRESETS + INTERVALO PERSONALIZADO)
                // Posicionado acima das abas para refletir na aba que estiver ativa
                item {
                    PeriodSelector(
                        periodFilter = uiState.periodFilter,
                        isDropdownExpanded = uiState.isPeriodDropdownExpanded,
                        onToggleDropdown = { viewModel.togglePeriodDropdown() },
                        onSelectPreset = { preset -> viewModel.applyPeriodPreset(preset) },
                        onApplyCustomRange = { start, end -> viewModel.applyCustomPeriod(start, end) }
                    )
                }

                // 3. SELETOR DE ABAS LOCAL (Sessões vs Desempenho vs Painel)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = OrangeNeon,
                            divider = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PartnerRoutesTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                Tab(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    modifier = Modifier.height(44.dp),
                                    text = {
                                        Text(
                                            text = tab.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedTab == PartnerRoutesTab.SESSOES) {
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
                            onEditSession = { session -> viewModel.openViewDetailSession(session) },
                            onDeleteSession = { session -> viewModel.promptDeleteSession(session) },
                            onShareSession = { session, rName, startStr, endStr, durStr ->
                                SessionShareHelper.shareSessionImage(
                                    context = context,
                                    session = session,
                                    partnerName = partner?.fullName ?: "",
                                    routeName = rName,
                                    startTimeStr = startStr,
                                    endTimeStr = endStr,
                                    durationStr = durStr
                                )
                            },
                            routeMap = routeMap,
                            timeFormatter = timeFormatter
                        )
                    }
                }
                } else if (selectedTab == PartnerRoutesTab.DESEMPENHO) {
                    // ABA "DESEMPENHO" (deste parceiro específico)
                    if (uiState.sessions.isEmpty()) {
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
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Nenhum dado de desempenho",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Este parceiro ainda não concluiu sessões de entrega para gerar relatórios analíticos.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (uiState.performanceMetrics.totalSessions == 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Nenhum dado no período selecionado",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Não há sessões de entrega concluídas no período (${uiState.periodFilter.preset.label}). Altere o filtro acima para visualizar o desempenho de outros períodos.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.resetPeriodFilter() },
                                        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f))
                                    ) {
                                        Text("Redefinir para Esta Semana", color = OrangeNeon, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        val metrics = uiState.performanceMetrics

                        // 1. Card: Total Ganho Acumulado
                        item {
                            PainelStatCard(
                                label = "Total Ganho pelo Parceiro",
                                value = metrics.totalEarnings.formatCurrency(),
                                highlight = true,
                                hint = "Mês atual: ${metrics.currentMonthEarnings.formatCurrency()} repassados",
                                rightContent = {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = GreenNeon,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }

                        // 2. Cards Simétricos: Volumetria e Taxa de Devolução
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Entregues vs Devolvidos
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "PACOTES TOTAIS",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${metrics.totalDelivered}",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GreenNeon
                                            )
                                            Text(
                                                text = "entregues",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = "${metrics.totalReturned} devoluções registradas",
                                            fontSize = 11.5.sp,
                                            color = if (metrics.totalReturned > 0) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Taxa de Devolução
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "TAXA DE DEVOLUÇÃO",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${metrics.returnRate.toPlainString()}%",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (metrics.returnRate.compareTo(BigDecimal("5.00")) >= 0) RedAlert else GreenNeon
                                        )
                                        Text(
                                            text = if (metrics.returnRate.compareTo(BigDecimal("5.00")) >= 0) "Atenção: índice elevado" else "Excelente aproveitamento",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Card: Dia de Maior Desempenho (Critério financeiro R$)
                        item {
                            val best = metrics.bestDay
                            if (best != null) {
                                PainelStatCard(
                                    label = "Dia de Maior Desempenho",
                                    value = best.totalEarnings.formatCurrency(),
                                    highlight = false,
                                    hint = "${best.date.format(PerformanceDateFormatter)} • ${best.deliveredCount} pacotes entregues • ${best.returnedCount} devolvidos (${best.sessionCount} rota(s))",
                                    rightContent = {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                )
                            }
                        }

                        // 4. Card: Resumo Operacional
                        item {
                            PainelStatCard(
                                label = "Média por Sessão",
                                value = metrics.averageEarningsPerSession.formatCurrency(),
                                highlight = false,
                                hint = "${metrics.totalSessions} sessões executadas até o momento",
                                rightContent = {
                                    Icon(
                                        imageVector = Icons.Default.AltRoute,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            )
                        }

                        // 4.1 Card de Tendência de Desempenho (movido do Painel para Desempenho)
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

                        // 5. Card: Tempo de Duração de Cada Rota
                        item {
                            val sessionsMap = remember(uiState.sessions) {
                                uiState.sessions.associateBy { it.id }
                            }
                            RouteDurationCard(
                                durationMetrics = metrics.durationMetrics,
                                routeMap = routeMap,
                                partnerName = partner?.fullName ?: "",
                                sessionsMap = sessionsMap,
                                timeFormatter = timeFormatter,
                                dateFormatter = PerformanceDateFormatter,
                                onSessionClick = { session ->
                                    viewModel.openViewDetailSession(session)
                                },
                                onShareSession = { session, rName, startStr, endStr, durStr ->
                                    SessionShareHelper.shareSessionImage(
                                        context = context,
                                        session = session,
                                        partnerName = partner?.fullName ?: "",
                                        routeName = rName,
                                        startTimeStr = startStr,
                                        endTimeStr = endStr,
                                        durationStr = durStr
                                    )
                                }
                            )
                        }
                    }
                } else if (selectedTab == PartnerRoutesTab.FINANCAS) {
                    // ABA "FINANÇAS" (por Plataformas Ativas e Gráfico de Ganhos)
                    if (activePlatforms.isEmpty()) {
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
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Nenhuma Plataforma Ativa",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Cadastre ou ative plataformas para visualizar os repasses e gráficos financeiros detalhados.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // 1. Seção: PLATAFORMAS (Cards individuais com filtros em alto relevo)
                        item {
                            Text(
                                text = "PLATAFORMAS ATIVAS (${activePlatforms.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OrangeNeon,
                                modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }

                        activePlatforms.forEach { platform ->
                            item(key = "platform-finance-${platform.id}") {
                                var platformFilter by remember(platform.id) {
                                    mutableStateOf(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.MES_CORRENTE))
                                }

                                PlatformFinanceCard(
                                    platform = platform,
                                    sessions = uiState.sessions,
                                    filter = platformFilter,
                                    availableMonths = availableMonths,
                                    onFilterChange = { newFilter -> platformFilter = newFilter },
                                    onSessionClick = { s -> viewModel.openViewDetailSession(s) },
                                    routesMap = routeMap
                                )
                            }
                        }

                        // 2. Seção: GRÁFICO DE LINHAS DE GANHOS (Abaixo das plataformas)
                        item(key = "platform-finance-line-chart") {
                            PlatformEarningsLineChart(
                                activePlatforms = activePlatforms,
                                sessions = uiState.sessions
                            )
                        }
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
    onShareSession: (session: DeliveryPartnerSession, routeName: String, startTimeStr: String, endTimeStr: String, durationStr: String) -> Unit,
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
                        onShareSession = onShareSession,
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
    onShareSession: (session: DeliveryPartnerSession, routeName: String, startTimeStr: String, endTimeStr: String, durationStr: String) -> Unit,
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
                            onShareSession = onShareSession,
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
    onShareSession: (session: DeliveryPartnerSession, routeName: String, startTimeStr: String, endTimeStr: String, durationStr: String) -> Unit,
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
            val startStr = session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--:--"
            val endStr = session.endTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--:--"
            val durStr = formatDuration(session.startTime, session.endTime)

            PartnerSessionCard(
                session = session,
                routeName = routeName,
                timeFormatter = timeFormatter,
                onClick = { onSessionClick(session) },
                onEdit = { onEditSession(session) },
                onDelete = { onDeleteSession(session) },
                onShare = { onShareSession(session, routeName, startStr, endStr, durStr) }
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
    onDelete: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val platformMap = LocalPlatformMap.current
    val platformName = session.platformId?.let { platformMap[it]?.name }
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
            // Linha Superior: Data + Horas embaixo + Badge Status + Ações (Compartilhar, Editar, Excluir)
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                    // Botão Compartilhar (mesmo layout e funcionalidade da aba Desempenho)
                    if (!isInProgress && onShare != null) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar Rota",
                                tint = OrangeNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Botão Editar / Ver Detalhes (mesmo fluxo de edição do Desempenho)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ver Detalhes e Editar",
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

            // Linha do Meio: Rota + Duração destacada (igual à aba Desempenho)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(15.dp))
                    Text(
                        text = if (!platformName.isNullOrBlank()) "$routeName • $platformName" else routeName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isInProgress) {
                    val durationStr = formatDuration(session.startTime, session.endTime)
                    Surface(
                        color = OrangeNeon.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = durationStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
 * Diálogo de exibição de detalhes completos da sessão finalizada.
 */
@Composable
private fun SessionDetailDialog(
    session: DeliveryPartnerSession,
    routeName: String,
    partnerName: String,
    platformName: String? = null,
    timeFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null
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
                if (!platformName.isNullOrBlank()) {
                    DetailRow(label = "Plataforma:", value = platformName)
                }
                DetailRow(label = "Início:", value = startTimeStr)
                DetailRow(label = "Término:", value = endTimeStr)
                DetailRow(label = "Duração Rota:", value = durationStr)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                DetailRow(label = "Pacotes Expedidos:", value = "${session.expectedPackageCount}")
                DetailRow(label = "Pacotes Bipados:", value = "${session.scannedCount}")
                DetailRow(label = "Pacotes Entregues:", value = "${session.deliveredCount}", valueColor = GreenNeon)
                DetailRow(label = "Pacotes Devolvidos:", value = "${session.returnedCount}", valueColor = RedAlert)
                if (session.packageRate > java.math.BigDecimal.ZERO) {
                    DetailRow(label = "Taxa por Pacote:", value = session.packageRate.formatCurrency())
                }
                if (session.defaultBonus > java.math.BigDecimal.ZERO) {
                    DetailRow(label = "Bônus / Diária Fixa:", value = session.defaultBonus.formatCurrency())
                }
                if (session.scannedBarcodes.isNotEmpty()) {
                    DetailRow(label = "Códigos Bipados:", value = "${session.scannedBarcodes.size} registrado(s)")
                }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RedAlert
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir", color = RedAlert)
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Fechar")
                }
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

internal fun formatDuration(startTime: OffsetDateTime?, endTime: OffsetDateTime?): String {
    if (startTime == null || endTime == null) return "--:--"
    val totalMinutes = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}

@Composable
private fun RouteDurationCard(
    durationMetrics: PartnerDurationMetrics,
    routeMap: Map<String, DeliveryRoute>,
    partnerName: String = "",
    sessionsMap: Map<String, DeliveryPartnerSession> = emptyMap(),
    timeFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
    onSessionClick: (DeliveryPartnerSession) -> Unit = {},
    onShareSession: (session: DeliveryPartnerSession, routeName: String, startTimeStr: String, endTimeStr: String, durationStr: String) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TEMPO DE DURAÇÃO DAS ROTAS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (durationMetrics.totalTimedRoutes > 0)
                                "${durationMetrics.totalTimedRoutes} rota(s) com cronometragem"
                            else "Sem horários registrados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (durationMetrics.totalTimedRoutes == 0) {
                Text(
                    text = "As sessões deste período não possuem registro completo de início e término para calcular as durações.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Linha de Destaque: Média e Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Duração Média por Rota",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = durationMetrics.formattedAverage,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangeNeon
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tempo Total em Rota",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = durationMetrics.formattedTotal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Trio de Métricas: Mais rápida, Mais longa, Ritmo por pct
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Mais rápida",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = durationMetrics.formattedShortest,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏳ Mais longa",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = durationMetrics.formattedLongest,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "📦 Ritmo por pct",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = durationMetrics.averageMinutesPerPackage?.let { "$it min/pct" } ?: "--",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )
                    }
                }

                // Detalhamento de cada rota
                if (durationMetrics.timedRoutes.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETALHAMENTO POR ROTA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (durationMetrics.timedRoutes.size > 3) {
                            Text(
                                text = if (isExpanded) "Recolher" else "Ver todas (${durationMetrics.timedRoutes.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )
                        }
                    }

                    val routesToShow = if (isExpanded || durationMetrics.timedRoutes.size <= 3) {
                        durationMetrics.timedRoutes
                    } else {
                        durationMetrics.timedRoutes.take(3)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        routesToShow.forEach { item ->
                            val targetSession = item.session ?: sessionsMap[item.sessionId]
                            val rName = item.routeId?.let { routeMap[it]?.name } ?: "Sem rota definida"
                            val dateStr = item.date.format(dateFormatter)
                            val startStr = item.startTime.format(timeFormatter)
                            val endStr = item.endTime.format(timeFormatter)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = targetSession != null) {
                                        targetSession?.let { onSessionClick(it) }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = dateStr,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = rName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$startStr às $endStr • ${item.deliveredCount} entregues",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = OrangeNeon.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.formattedDuration,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangeNeon,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        if (targetSession != null) {
                                            IconButton(
                                                onClick = {
                                                    val durStr = formatDuration(targetSession.startTime, targetSession.endTime)
                                                    onShareSession(targetSession, rName, startStr, endStr, durStr)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Compartilhar Rota",
                                                    tint = OrangeNeon,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Ver Detalhes e Editar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
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
}



