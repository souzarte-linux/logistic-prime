package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.formatShortName
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeEmoji
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Locale

/**
 * TELA 1 — "Entregadores Parceiros"
 * Lista de todos os entregadores cadastrados com cards estruturados contendo:
 * - Cabeçalho: Switch (ativo/inativo) à esquerda, Avatar + Nome/Sobrenome + Tipo de entrega + Estrelas à direita.
 * - Subseção "Rotas recentes": últimos 7 registros de sessões.
 * - Subseção "Sessão Ativa": badge, contagem de pacotes pendentes e botão "Fechar Sessão".
 * - O card inteiro navega para a Tela 2 ("Rotas dos Parceiros").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnersScreen(
    viewModel: DeliveryPartnersViewModel = viewModel(),
    onNavigateToPartnerRoutes: (partnerId: String) -> Unit,
    onNavigateToCreatePartner: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    val filteredPartners = remember(uiState.partners, uiState.searchQuery, uiState.statusFilter) {
        uiState.partners.filter { partner ->
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    partner.fullName.contains(uiState.searchQuery, ignoreCase = true) ||
                    (partner.phone != null && partner.phone.contains(uiState.searchQuery)) ||
                    (partner.cpf != null && partner.cpf.contains(uiState.searchQuery))

            val matchesFilter = when (uiState.statusFilter) {
                PartnerStatusFilter.ALL -> true
                PartnerStatusFilter.ACTIVE -> partner.active
                PartnerStatusFilter.INACTIVE -> !partner.active
            }

            matchesSearch && matchesFilter
        }
    }

    val routeMap = remember(uiState.routes) {
        uiState.routes.associateBy { it.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ENTREGADORES PARCEIROS",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePartner,
                containerColor = OrangeNeon,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Entregador")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // 1. Barra de Busca
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            "Buscar por nome, telefone ou CPF...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Filtros de Status (Todos, Ativos, Inativos)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.statusFilter == PartnerStatusFilter.ALL,
                        onClick = { viewModel.onStatusFilterChanged(PartnerStatusFilter.ALL) },
                        label = { Text("Todos (${uiState.partners.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                            selectedLabelColor = OrangeNeon
                        )
                    )
                    FilterChip(
                        selected = uiState.statusFilter == PartnerStatusFilter.ACTIVE,
                        onClick = { viewModel.onStatusFilterChanged(PartnerStatusFilter.ACTIVE) },
                        label = { Text("Ativos (${uiState.partners.count { it.active }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenNeon.copy(alpha = 0.2f),
                            selectedLabelColor = GreenNeon
                        )
                    )
                    FilterChip(
                        selected = uiState.statusFilter == PartnerStatusFilter.INACTIVE,
                        onClick = { viewModel.onStatusFilterChanged(PartnerStatusFilter.INACTIVE) },
                        label = { Text("Inativos (${uiState.partners.count { !it.active }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // 3. Contador
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENTREGADORES PARCEIROS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${filteredPartners.size} cadastrado(s)",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Estado de Carregamento
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
            }

            // 5. Estado Vazio
            if (!uiState.isLoading && filteredPartners.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = if (uiState.partners.isEmpty()) "Nenhum parceiro cadastrado" else "Nenhum resultado encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.partners.isEmpty())
                                    "Cadastre os motoristas e entregadores parceiros para gerenciar as rotas e repasses."
                                else "Tente buscar com outro termo ou alterar os filtros.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.partners.isEmpty()) {
                                Button(
                                    onClick = onNavigateToCreatePartner,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OrangeNeon,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adicionar Primeiro Parceiro", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6. Lista de Cards de Entregadores Parceiros
            if (!uiState.isLoading) {
                items(filteredPartners, key = { it.id }) { partner ->
                    val activeSession = uiState.activeSessionsMap[partner.id]
                    val sessions = uiState.sessionsByPartnerMap[partner.id] ?: emptyList()

                    DeliveryPartnerCard(
                        partner = partner,
                        routeMap = routeMap,
                        sessions = sessions,
                        activeSession = activeSession,
                        onToggleActive = { viewModel.togglePartnerActive(partner) },
                        onCardClick = { onNavigateToPartnerRoutes(partner.id) },
                        onOpenActiveSession = { onNavigateToPartnerRoutes(partner.id) }
                    )
                }
            }
        }
    }
}

private data class GroupedPartnerRoute(
    val routeName: String,
    val totalPackages: Int,
    val totalAmount: BigDecimal,
    val latestDate: OffsetDateTime? = null
)

/**
 * Card estruturado do Entregador Parceiro:
 * - Cabeçalho: Switch Ativar/Desativar (direita), Avatar + Nome/Sobrenome + Tipo + Estrelas (esquerda).
 * - Divisória fina cinza.
 * - Subseção "Rotas": agrupadas por nome independente do dia, exibindo quantidade de pacotes e valor total em R$.
 * - Divisória fina cinza.
 * - Subseção "Sessão Ativa": botão "Fechar Sessão".
 * - O card inteiro é clicável e navega para Tela 2.
 */
@Composable
private fun DeliveryPartnerCard(
    partner: DeliveryPartner,
    routeMap: Map<String, DeliveryRoute>,
    sessions: List<DeliveryPartnerSession>,
    activeSession: DeliveryPartnerSession?,
    onToggleActive: () -> Unit,
    onCardClick: () -> Unit,
    onOpenActiveSession: () -> Unit
) {
    val groupedRoutes = remember(sessions, routeMap) {
        sessions
            .groupBy { session ->
                session.routeId?.let { routeMap[it]?.name } ?: "Sem Rota"
            }
            .map { (routeName, sessionList) ->
                val totalPackages = sessionList.sumOf { s ->
                    if (s.endTime == null) s.scannedCount else s.deliveredCount
                }
                val totalAmount = sessionList.fold(BigDecimal.ZERO) { acc, s ->
                    acc.add(s.amountPaid)
                }
                val latestDate = sessionList.mapNotNull { it.startTime ?: it.createdAt }.maxOrNull()
                GroupedPartnerRoute(
                    routeName = routeName,
                    totalPackages = totalPackages,
                    totalAmount = totalAmount,
                    latestDate = latestDate
                )
            }
            .sortedWith(
                compareByDescending<GroupedPartnerRoute> { it.latestDate }
                    .thenByDescending { it.totalPackages }
            )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (partner.active) OrangeNeon.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------------------------------------------------------------
            // CABEÇALHO DO CARD:
            // Esquerda: Avatar + Nome e Sobrenome + Ícone tipo de entrega + Estrelas
            // Direita: Switch Ativar/Desativar (alinhado ao lado direito)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lado esquerdo: Círculo (foto ou iniciais) + Nome/Sobrenome + Ícone de entrega + Estrelas
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PartnerAvatar(
                        photoUrl = partner.photoUrl,
                        name = partner.fullName,
                        size = 46.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Nome + Segundo Nome + Ícone pequeno do tipo de entrega
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatShortName(partner.fullName),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = getDeliveryTypeEmoji(partner.deliveryType),
                                fontSize = 15.sp
                            )
                        }

                        // Classificação em estrelas (1-5, somente leitura)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= partner.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (i <= partner.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Lado direito: Switch/Slider Ativar / Desativar entregador
                Switch(
                    checked = partner.active,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = OrangeNeon,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // -------------------------------------------------------------
            // SEGUNDA SEÇÃO: Linha divisória cinza claro + "Rotas"
            // (Rotas agrupadas por nome: Nome rota | Quantidade de pacotes | Valor recebido)
            // -------------------------------------------------------------
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Rotas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (groupedRoutes.isEmpty()) {
                    Text(
                        text = "Nenhuma rota realizada.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    groupedRoutes.forEach { route ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Esquerda: Nome da rota
                            Text(
                                text = route.routeName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.3f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Centro: Quantidade de pacotes entregues
                            Text(
                                text = "${route.totalPackages} pacotes",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            // Direita: Valor recebido nessa rota
                            Text(
                                text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", route.totalAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TERCEIRA SEÇÃO: Sessão Ativa (Mostrar SOMENTE o botão "Fechar Sessão")
            // -------------------------------------------------------------
            if (activeSession != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Button(
                    onClick = onOpenActiveSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fechar Sessão", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
