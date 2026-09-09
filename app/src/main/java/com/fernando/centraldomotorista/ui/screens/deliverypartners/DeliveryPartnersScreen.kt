package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.*
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnersScreen(
    viewModel: DeliveryPartnersViewModel = viewModel(),
    onNavigateToNewSession: (partnerId: String) -> Unit = {},
    onNavigateToCloseSession: (sessionId: String) -> Unit = {},
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

    // Intercept hardware back button when form is open
    BackHandler(enabled = uiState.isFormOpen) {
        viewModel.requestCloseForm()
    }

    if (uiState.isFormOpen) {
        DeliveryPartnerFormView(
            uiState = uiState,
            onFullNameChanged = { viewModel.onFullNameChanged(it) },
            onCepChanged = { viewModel.onCepChanged(it) },
            onStreetChanged = { viewModel.onStreetChanged(it) },
            onNumberChanged = { viewModel.onNumberChanged(it) },
            onNeighborhoodChanged = { viewModel.onNeighborhoodChanged(it) },
            onCityChanged = { viewModel.onCityChanged(it) },
            onStateChanged = { viewModel.onStateChanged(it) },
            onPhoneChanged = { viewModel.onPhoneChanged(it) },
            onIsWhatsappChanged = { viewModel.onIsWhatsappChanged(it) },
            onSocialMediaChanged = { viewModel.onSocialMediaChanged(it) },
            onPixKeyChanged = { viewModel.onPixKeyChanged(it) },
            onPixBankChanged = { viewModel.onPixBankChanged(it) },
            onCpfChanged = { viewModel.onCpfChanged(it) },
            onPreferredRouteChanged = { viewModel.onPreferredRouteChanged(it) },
            onPackageRateChanged = { viewModel.onPackageRateChanged(it) },
            onDefaultBonusChanged = { viewModel.onDefaultBonusChanged(it) },
            onDeliveryTypeChanged = { viewModel.onDeliveryTypeChanged(it) },
            onRatingChanged = { viewModel.onRatingChanged(it) },
            onPaymentCycleTypeChanged = { viewModel.onPaymentCycleTypeChanged(it) },
            onPaymentCycleFixedChanged = { viewModel.onPaymentCycleFixedChanged(it) },
            onAddVariableDay = { viewModel.addVariableCycleDay(it) },
            onRemoveVariableDay = { viewModel.removeVariableCycleDay(it) },
            onMoveVariableDay = { from, to -> viewModel.moveVariableCycleDay(from, to) },
            onActiveChanged = { viewModel.onActiveChanged(it) },
            onSave = { viewModel.savePartner() },
            onDelete = { uiState.formData.id?.let { viewModel.deletePartner(it) } },
            onNavigateToNewSession = onNavigateToNewSession,
            onNavigateToCloseSession = onNavigateToCloseSession,
            onClose = { viewModel.requestCloseForm() },
            onConfirmDiscard = { viewModel.forceCloseForm() },
            onDismissDiscard = { viewModel.dismissDiscardAlert() }
        )
    } else {
        DeliveryPartnersListView(
            uiState = uiState,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onStatusFilterChanged = { viewModel.onStatusFilterChanged(it) },
            onRefresh = { viewModel.loadData() },
            onAddPartner = { viewModel.openCreateForm() },
            onEditPartner = { viewModel.openEditForm(it) },
            onToggleActive = { viewModel.togglePartnerActive(it) },
            onNavigateToNewSession = onNavigateToNewSession,
            onNavigateToCloseSession = onNavigateToCloseSession,
            onNavigateBack = onNavigateBack
        )
    }
}

// -----------------------------------------------------------------------------
// LIST VIEW
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryPartnersListView(
    uiState: DeliveryPartnersUiState,
    onSearchQueryChanged: (String) -> Unit,
    onStatusFilterChanged: (PartnerStatusFilter) -> Unit,
    onRefresh: () -> Unit,
    onAddPartner: () -> Unit,
    onEditPartner: (DeliveryPartner) -> Unit,
    onToggleActive: (DeliveryPartner) -> Unit,
    onNavigateToNewSession: (partnerId: String) -> Unit,
    onNavigateToCloseSession: (sessionId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
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
                    IconButton(onClick = onRefresh) {
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
                onClick = onAddPartner,
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
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Buscar por nome, telefone ou CPF...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onClick = { onStatusFilterChanged(PartnerStatusFilter.ALL) },
                        label = { Text("Todos (${uiState.partners.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                            selectedLabelColor = OrangeNeon
                        )
                    )
                    FilterChip(
                        selected = uiState.statusFilter == PartnerStatusFilter.ACTIVE,
                        onClick = { onStatusFilterChanged(PartnerStatusFilter.ACTIVE) },
                        label = { Text("Ativos (${uiState.partners.count { it.active }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenNeon.copy(alpha = 0.2f),
                            selectedLabelColor = GreenNeon
                        )
                    )
                    FilterChip(
                        selected = uiState.statusFilter == PartnerStatusFilter.INACTIVE,
                        onClick = { onStatusFilterChanged(PartnerStatusFilter.INACTIVE) },
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

            // 4. Loading state
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

            // 5. Empty State
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
                                    "Cadastre os motoristas e entregadores parceiros com taxas por pacote, rota preferida e ciclo de repasse."
                                else "Tente buscar com outro termo ou alterar os filtros.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.partners.isEmpty()) {
                                Button(
                                    onClick = onAddPartner,
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

            // 6. Lista de Cards de Entregadores
            if (!uiState.isLoading) {
                items(filteredPartners, key = { it.id }) { partner ->
                    val preferredRouteName = partner.preferredRouteId?.let { routeMap[it]?.name } ?: "Sem rota definida"
                    val activeSession = uiState.activeSessionsMap[partner.id]
                    DeliveryPartnerCard(
                        partner = partner,
                        preferredRouteName = preferredRouteName,
                        activeSession = activeSession,
                        onStartSession = { onNavigateToNewSession(partner.id) },
                        onCloseSession = { sessionId -> onNavigateToCloseSession(sessionId) },
                        onClick = { onEditPartner(partner) },
                        onToggleActive = { onToggleActive(partner) }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PARTNER CARD
// -----------------------------------------------------------------------------
@Composable
private fun DeliveryPartnerCard(
    partner: DeliveryPartner,
    preferredRouteName: String,
    activeSession: DeliveryPartnerSession? = null,
    onStartSession: () -> Unit = {},
    onCloseSession: (String) -> Unit = {},
    onClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val deliveryIcon = getDeliveryTypeIcon(partner.deliveryType)
    val deliveryLabel = getDeliveryTypeLabel(partner.deliveryType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            // Linha Superior: Nome, Estrelas, Toggle Ativo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (partner.active) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = deliveryIcon,
                            contentDescription = deliveryLabel,
                            tint = if (partner.active) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = partner.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Estrelas de Avaliação
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= partner.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (i <= partner.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = deliveryLabel,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Switch Ativo/Inativo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Linha Inferior: Rota Preferida + Valores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rota Preferida
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = "Rota",
                        tint = OrangeNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = preferredRouteName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Taxa por pacote + bônus
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", partner.packageRate)} / pct",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon
                    )
                    if (partner.defaultBonus > BigDecimal.ZERO) {
                        Text(
                            text = "+ R$ ${String.format(Locale("pt", "BR"), "%.2f", partner.defaultBonus)} bônus",
                            fontSize = 10.sp,
                            color = GreenNeon,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Ação de Sessão (Ativa ou Iniciar)
            if (activeSession != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Surface(
                    color = OrangeNeon.copy(alpha = 0.15f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(OrangeNeon, CircleShape)
                            )
                            Text(
                                text = "Sessão Ativa (${activeSession.scannedCount} pct)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon
                            )
                        }
                        Button(
                            onClick = { onCloseSession(activeSession.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Fechar Sessão", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (partner.active) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                OutlinedButton(
                    onClick = onStartSession,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeNeon),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Iniciar Sessão", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPLETE FORM VIEW
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryPartnerFormView(
    uiState: DeliveryPartnersUiState,
    onFullNameChanged: (String) -> Unit,
    onCepChanged: (String) -> Unit,
    onStreetChanged: (String) -> Unit,
    onNumberChanged: (String) -> Unit,
    onNeighborhoodChanged: (String) -> Unit,
    onCityChanged: (String) -> Unit,
    onStateChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onIsWhatsappChanged: (Boolean) -> Unit,
    onSocialMediaChanged: (String) -> Unit,
    onPixKeyChanged: (String) -> Unit,
    onPixBankChanged: (String) -> Unit,
    onCpfChanged: (String) -> Unit,
    onPreferredRouteChanged: (String?) -> Unit,
    onPackageRateChanged: (String) -> Unit,
    onDefaultBonusChanged: (String) -> Unit,
    onDeliveryTypeChanged: (String) -> Unit,
    onRatingChanged: (Int) -> Unit,
    onPaymentCycleTypeChanged: (String) -> Unit,
    onPaymentCycleFixedChanged: (String) -> Unit,
    onAddVariableDay: (Int) -> Unit,
    onRemoveVariableDay: (Int) -> Unit,
    onMoveVariableDay: (Int, Int) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToNewSession: (partnerId: String) -> Unit,
    onNavigateToCloseSession: (sessionId: String) -> Unit,
    onClose: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit
) {
    val form = uiState.formData
    val isEditing = form.id != null
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var routeDropdownExpanded by remember { mutableStateOf(false) }
    var cycleFixedDropdownExpanded by remember { mutableStateOf(false) }
    var customDayInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val selectedRouteName = remember(form.preferredRouteId, uiState.routes) {
        uiState.routes.firstOrNull { it.id == form.preferredRouteId }?.name ?: "Nenhuma (Sem preferência)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDITAR ENTREGADOR" else "NOVO ENTREGADOR",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = RedAlert
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onSave,
                        enabled = !uiState.isSaving && form.fullName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isEditing) "Salvar Alterações" else "Cadastrar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // SEÇÃO: IDENTIFICAÇÃO E CONTATO
            // -------------------------------------------------------------
            SectionHeader(title = "DADOS PESSOAIS & CONTATO", icon = Icons.Default.Person)

            // Nome Completo *
            OutlinedTextField(
                value = form.fullName,
                onValueChange = onFullNameChanged,
                label = { Text("Nome Completo *") },
                singleLine = true,
                isError = form.fullName.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // CPF (Opcional com máscara e validação)
            OutlinedTextField(
                value = form.cpf,
                onValueChange = onCpfChanged,
                label = { Text("CPF (Opcional)") },
                placeholder = { Text("000.000.000-00") },
                singleLine = true,
                visualTransformation = CpfVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                isError = uiState.cpfError != null,
                supportingText = {
                    if (uiState.cpfError != null) {
                        Text(uiState.cpfError!!, color = RedAlert)
                    } else {
                        Text("Apenas números, com validação de dígitos", fontSize = 11.sp)
                    }
                },
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Telefone / Contato com máscara e WhatsApp
            OutlinedTextField(
                value = form.phone,
                onValueChange = onPhoneChanged,
                label = { Text("Celular / Contato") },
                placeholder = { Text("(00) 00000-0000") },
                singleLine = true,
                visualTransformation = PhoneVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIsWhatsappChanged(!form.isWhatsapp) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = form.isWhatsapp,
                    onCheckedChange = onIsWhatsappChanged,
                    colors = CheckboxDefaults.colors(checkedColor = OrangeNeon)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Este número é WhatsApp",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Rede Social
            OutlinedTextField(
                value = form.socialMedia,
                onValueChange = onSocialMediaChanged,
                label = { Text("Rede Social (Instagram, etc.)") },
                placeholder = { Text("@usuario ou link") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // -------------------------------------------------------------
            // SEÇÃO: ENDEREÇO (ViaCEP)
            // -------------------------------------------------------------
            SectionHeader(title = "ENDEREÇO (VIACEP)", icon = Icons.Default.LocationOn)

            // CEP
            OutlinedTextField(
                value = form.cep,
                onValueChange = onCepChanged,
                label = { Text("CEP") },
                placeholder = { Text("00000-000") },
                singleLine = true,
                visualTransformation = CepVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                trailingIcon = {
                    if (uiState.isSearchingCep) {
                        CircularProgressIndicator(
                            color = OrangeNeon,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Logradouro / Rua
            OutlinedTextField(
                value = form.street,
                onValueChange = onStreetChanged,
                label = { Text("Logradouro / Endereço") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.number,
                    onValueChange = onNumberChanged,
                    label = { Text("Número") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = form.neighborhood,
                    onValueChange = onNeighborhoodChanged,
                    label = { Text("Bairro") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.city,
                    onValueChange = onCityChanged,
                    label = { Text("Cidade") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(2f)
                )

                OutlinedTextField(
                    value = form.state,
                    onValueChange = onStateChanged,
                    label = { Text("UF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // -------------------------------------------------------------
            // SEÇÃO: DADOS FINANCEIROS / PIX
            // -------------------------------------------------------------
            SectionHeader(title = "DADOS BANCÁRIOS / PIX", icon = Icons.Default.AccountBalanceWallet)

            OutlinedTextField(
                value = form.pixKey,
                onValueChange = onPixKeyChanged,
                label = { Text("Chave PIX") },
                placeholder = { Text("CPF, Celular, E-mail ou Aleatória") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.pixBank,
                onValueChange = onPixBankChanged,
                label = { Text("Instituição Financeira da Chave PIX") },
                placeholder = { Text("ex: Nubank, Banco do Brasil, Inter") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = outlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // -------------------------------------------------------------
            // SEÇÃO: CONFIGURAÇÕES DE ENTREGA & VEÍCULO
            // -------------------------------------------------------------
            SectionHeader(title = "ENTREGA & VEÍCULO", icon = Icons.Default.DeliveryDining)

            // Tipo de Entrega com Ícones
            Text(
                text = "Tipo de Entrega:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            val deliveryTypes = listOf(
                "a_pe" to ("A pé" to Icons.Default.DirectionsWalk),
                "bike" to ("Bike" to Icons.Default.DirectionsBike),
                "moto" to ("Moto" to Icons.Default.TwoWheeler),
                "carro" to ("Carro" to Icons.Default.DirectionsCar),
                "utilitario" to ("Utilitário" to Icons.Default.LocalShipping)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(deliveryTypes) { (typeKey, pair) ->
                    val (label, icon) = pair
                    val isSelected = form.deliveryType == typeKey
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDeliveryTypeChanged(typeKey) }
                            .border(
                                1.5.dp,
                                if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Rota de Preferência (Dropdown alimentado por delivery_routes)
            ExposedDropdownMenuBox(
                expanded = routeDropdownExpanded,
                onExpandedChange = { routeDropdownExpanded = !routeDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedRouteName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rota de Preferência") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeDropdownExpanded) },
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = routeDropdownExpanded,
                    onDismissRequest = { routeDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma (Sem preferência)") },
                        onClick = {
                            onPreferredRouteChanged(null)
                            routeDropdownExpanded = false
                        }
                    )
                    uiState.routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                onPreferredRouteChanged(route.id)
                                routeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Classificação em Estrelas (1 a 5 tocáveis)
            Text(
                text = "Classificação (${form.rating} estrelas):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (star in 1..5) {
                    IconButton(
                        onClick = { onRatingChanged(star) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (star <= form.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star estrelas",
                            tint = if (star <= form.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SEÇÃO: TAXAS & REMUNERAÇÃO (BigDecimal)
            // -------------------------------------------------------------
            SectionHeader(title = "TAXAS & REMUNERAÇÃO", icon = Icons.Default.Payments)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Valor por Pacote
                OutlinedTextField(
                    value = form.packageRateText,
                    onValueChange = onPackageRateChanged,
                    label = { Text("Valor por Pacote") },
                    singleLine = true,
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                // Bonificação Padrão
                OutlinedTextField(
                    value = form.defaultBonusText,
                    onValueChange = onDefaultBonusChanged,
                    label = { Text("Bonificação Padrão") },
                    singleLine = true,
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = outlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // -------------------------------------------------------------
            // SEÇÃO: CICLO DE PAGAMENTO (Fixo / Variável)
            // -------------------------------------------------------------
            SectionHeader(title = "CICLO DE PAGAMENTO", icon = Icons.Default.Schedule)

            // Segmented toggle Fixo vs Variável
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isFixed = form.paymentCycleType == "fixed"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPaymentCycleTypeChanged("fixed") },
                    color = if (isFixed) OrangeNeon else Color.Transparent
                ) {
                    Text(
                        text = "Ciclo Fixo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isFixed) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPaymentCycleTypeChanged("variable") },
                    color = if (!isFixed) OrangeNeon else Color.Transparent
                ) {
                    Text(
                        text = "Ciclo Variável",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!isFixed) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            if (form.paymentCycleType == "fixed") {
                // Dropdown Fixo
                ExposedDropdownMenuBox(
                    expanded = cycleFixedDropdownExpanded,
                    onExpandedChange = { cycleFixedDropdownExpanded = !cycleFixedDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fixedLabel = when (form.paymentCycleFixed) {
                        "semanal" -> "Semanal"
                        "quinzenal" -> "Quinzenal"
                        "mensal" -> "Mensal"
                        else -> "Semanal"
                    }
                    OutlinedTextField(
                        value = fixedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Periodicidade Fixa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleFixedDropdownExpanded) },
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = cycleFixedDropdownExpanded,
                        onDismissRequest = { cycleFixedDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semanal (a cada 7 dias)") },
                            onClick = {
                                onPaymentCycleFixedChanged("semanal")
                                cycleFixedDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quinzenal (a cada 15 dias)") },
                            onClick = {
                                onPaymentCycleFixedChanged("quinzenal")
                                cycleFixedDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mensal (a cada 30 dias)") },
                            onClick = {
                                onPaymentCycleFixedChanged("mensal")
                                cycleFixedDropdownExpanded = false
                            }
                        )
                    }
                }
            } else {
                // Interface Construtora de Ciclo Variável
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sequência de Ciclos (Repetição Cíclica)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Monte a sequência de dias de cada período. Ao concluir o último período, o ciclo se repete automaticamente do início.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Lista sequencial de períodos
                        if (form.paymentCycleVariableDays.isEmpty()) {
                            Text(
                                text = "Nenhum período adicionado. Use os botões abaixo para montar a sequência.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                form.paymentCycleVariableDays.forEachIndexed { index, days ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Badge(containerColor = OrangeNeon, contentColor = Color.Black) {
                                                    Text("${index + 1}º", fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = "$days dias",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Mover para cima
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = { onMoveVariableDay(index, index - 1) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                // Mover para baixo
                                                if (index < form.paymentCycleVariableDays.size - 1) {
                                                    IconButton(
                                                        onClick = { onMoveVariableDay(index, index + 1) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descer", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                // Excluir
                                                IconButton(
                                                    onClick = { onRemoveVariableDay(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = RedAlert, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Botões de adição rápida
                        Text(
                            text = "ADICIONAR PERÍODO:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(7, 10, 14, 15, 30).forEach { days ->
                                AssistChip(
                                    onClick = { onAddVariableDay(days) },
                                    label = { Text("+$days d", fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }

                        // Entrada personalizada
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customDayInput,
                                onValueChange = { customDayInput = it.filter { c -> c.isDigit() }.take(3) },
                                placeholder = { Text("Dias...", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    val d = customDayInput.toIntOrNull()
                                    if (d != null && d > 0) {
                                        onAddVariableDay(d)
                                        customDayInput = ""
                                    }
                                },
                                enabled = customDayInput.toIntOrNull() != null && customDayInput.toInt() > 0,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                            ) {
                                Text("Adicionar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // STATUS ATIVO / INATIVO
            // -------------------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Entregador Ativo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (form.active) "Disponível para rotas e entregas" else "Entregador temporariamente inativo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = form.active,
                        onCheckedChange = onActiveChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = OrangeNeon
                        )
                    )
                }
            }

            // -------------------------------------------------------------
            // OPERAÇÃO & SESSÕES DE TRABALHO
            // -------------------------------------------------------------
            if (isEditing && form.id != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Button(
                    onClick = { onNavigateToNewSession(form.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Sessão de Trabalho", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                PartnerRecentSessionsSection(
                    sessions = uiState.partnerSessions,
                    routes = uiState.routes,
                    isLoading = uiState.isLoadingSessions,
                    onOpenCloseSession = { sessionId -> onNavigateToCloseSession(sessionId) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de confirmação de exclusão
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Entregador") },
            text = { Text("Deseja realmente excluir \"${form.fullName}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("Excluir", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de descarte de alterações
    if (uiState.showDiscardAlert) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = { Text("Descartar Alterações?") },
            text = { Text("Você possui alterações não salvas. Deseja sair sem salvar?") },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Descartar", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) {
                    Text("Continuar Editando")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPOSABLES & FUNCTIONS
// -----------------------------------------------------------------------------
@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = OrangeNeon
        )
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeNeon,
    focusedLabelColor = OrangeNeon,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

private fun getDeliveryTypeIcon(deliveryType: String): ImageVector {
    return when (deliveryType.lowercase()) {
        "a_pe" -> Icons.Default.DirectionsWalk
        "bike" -> Icons.Default.DirectionsBike
        "moto" -> Icons.Default.TwoWheeler
        "carro" -> Icons.Default.DirectionsCar
        "utilitario" -> Icons.Default.LocalShipping
        else -> Icons.Default.TwoWheeler
    }
}

private fun getDeliveryTypeLabel(deliveryType: String): String {
    return when (deliveryType.lowercase()) {
        "a_pe" -> "A pé"
        "bike" -> "Bike"
        "moto" -> "Moto"
        "carro" -> "Carro"
        "utilitario" -> "Utilitário"
        else -> "Moto"
    }
}

@Composable
private fun PartnerRecentSessionsSection(
    sessions: List<DeliveryPartnerSession>,
    routes: List<DeliveryRoute>,
    isLoading: Boolean,
    onOpenCloseSession: (String) -> Unit
) {
    val routeMap = remember(routes) { routes.associateBy { it.id } }
    val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }

    SectionHeader(title = "SESSÕES RECENTES", icon = Icons.Default.History)

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangeNeon, modifier = Modifier.size(32.dp))
        }
    } else if (sessions.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "Nenhuma sessão registrada para este entregador até o momento.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sessions.forEach { session ->
                val routeName = session.routeId?.let { routeMap[it]?.name } ?: "Sem rota definida"
                val isInProgress = session.endTime == null

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isInProgress) {
                            onOpenCloseSession(session.id)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isInProgress) OrangeNeon.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.startTime?.format(timeFormatter) ?: "Data não informada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                color = if (isInProgress) OrangeNeon.copy(alpha = 0.15f) else GreenNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isInProgress) "EM ANDAMENTO" else "FINALIZADA",
                                    color = if (isInProgress) OrangeNeon else GreenNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Rota
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = routeName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Pacotes e Valores
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isInProgress) "Pendente" else "R$ ${String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInProgress) OrangeNeon else GreenNeon
                                )
                                if (isInProgress) {
                                    Text(
                                        text = "Toque para fechar",
                                        fontSize = 10.sp,
                                        color = OrangeNeon,
                                        fontWeight = FontWeight.Medium
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
