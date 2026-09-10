package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeEmoji
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                // 1. TOPO DA TELA: Dois números em destaque (Sessões finalizadas NO MÊS)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total de pacotes entregues no mês
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GreenNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "ENTREGUES NO MÊS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${uiState.monthDeliveredCount}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GreenNeon
                                )
                                Text(
                                    text = "pacotes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Valor total pago no mês (R$)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, OrangeNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "TOTAL PAGO NO MÊS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", uiState.monthTotalAmountPaid)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OrangeNeon,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "em repasses",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. SEÇÃO "DADOS MOTORISTA" (Somente visualização + atalho WhatsApp + Lápis)
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

                // 4. LISTA DE SESSÕES (Cards abaixo do botão)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
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
                            text = "${uiState.sessions.size} sessão(ões)",
                            fontSize = 12.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (uiState.sessions.isEmpty()) {
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(32.dp))
                                Text(
                                    text = "Nenhuma sessão registrada",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toque em 'Iniciar Sessão de Entrega' acima para abrir a primeira rota deste parceiro.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.sessions, key = { it.id }) { session ->
                        val routeName = session.routeId?.let { routeMap[it]?.name } ?: "Sem Rota"
                        PartnerSessionCard(
                            session = session,
                            routeName = routeName,
                            timeFormatter = timeFormatter,
                            onClick = {
                                if (session.endTime == null) {
                                    onNavigateToCloseSession(session.id)
                                } else {
                                    viewModel.openViewDetailSession(session)
                                }
                            },
                            onEdit = { viewModel.openEditSession(session) },
                            onDelete = { viewModel.promptDeleteSession(session) }
                        )
                    }
                }
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
    val formattedStartTime = remember(session.startTime) {
        session.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--/-- --:--"
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
            // Linha Superior: Data/Hora de Início + Badge Status + Ações (Editar, Excluir)
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
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isInProgress) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedStartTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                        Text(
                            text = "Entregues: ${session.deliveredCount} | Devolvidos: ${session.returnedCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenNeon
                        )
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
                        text = if (isInProgress) "Aberto" else "R$ ${String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)}",
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
    routes: List<com.fernando.centraldomotorista.data.model.DeliveryRoute>,
    onDismiss: () -> Unit,
    onSave: (DeliveryPartnerSession) -> Unit
) {
    var expectedText by remember { mutableStateOf(session.expectedPackageCount.toString()) }
    var deliveredText by remember { mutableStateOf(session.deliveredCount.toString()) }
    var returnedText by remember { mutableStateOf(session.returnedCount.toString()) }
    var amountPaidText by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)) }
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
                DetailRow(label = "Pacotes Entregues:", value = "${session.deliveredCount}")
                DetailRow(label = "Pacotes Devolvidos:", value = "${session.returnedCount}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Valor Total Pago:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)}",
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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

