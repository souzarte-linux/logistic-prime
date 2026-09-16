package com.fernando.centraldomotorista.ui.screens.apps

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
import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
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
fun PlatformsScreen(
    viewModel: PlatformsViewModel = viewModel(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GESTOR DE PLATAFORMAS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
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
                    IconButton(onClick = { viewModel.openFilterModal() }) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = OrangeNeon,
                                        contentColor = Color.Black
                                    ) {
                                        Text(
                                            text = uiState.activeFilterCount.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filtros & Ordenação",
                                tint = if (uiState.activeFilterCount > 0) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.loadPlatforms() }) {
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
                onClick = { viewModel.openAddDialog() },
                containerColor = OrangeNeon,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Plataforma")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val filteredPlatforms = uiState.filteredAndSortedPlatforms

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // 1. Banner Principal de Identidade
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "GESTOR DE\nPLATAFORMAS",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        color = OrangeNeon
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Plataformas com as quais você trabalha. Acompanhe seus ganhos e calendário de pagamentos.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Barra de Busca
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            "Buscar por nome, segmento ou ciclo...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
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

            // 3. Barra de Contagem & Botão de Filtros
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredPlatforms.size} ${if (filteredPlatforms.size == 1) "PLATAFORMA" else "PLATAFORMAS"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    AssistChip(
                        onClick = { viewModel.openFilterModal() },
                        label = {
                            Text(
                                text = if (uiState.activeFilterCount > 0)
                                    "Filtros & Ordenação (${uiState.activeFilterCount})"
                                else
                                    "Filtros & Ordenação",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = OrangeNeon
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = OrangeNeon
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (uiState.activeFilterCount > 0) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
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
            if (!uiState.isLoading && filteredPlatforms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (uiState.platforms.isEmpty()) "Nenhuma plataforma cadastrada" else "Nenhum resultado com os filtros selecionados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (uiState.platforms.isEmpty())
                                    "Adicione os apps onde você trabalha para organizar repasses, faturamento e ciclos."
                                else
                                    "Tente alterar a busca ou redefina os filtros.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.platforms.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearFilters() }) {
                                    Text("Limpar filtros", color = OrangeNeon, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "SUGESTÕES RÁPIDAS:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(POPULAR_PLATFORMS) { (name, segment, cycle) ->
                                        SuggestionChip(
                                            onClick = { viewModel.openAddDialog(name, segment, cycle) },
                                            label = { Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            border = SuggestionChipDefaults.suggestionChipBorder(
                                                enabled = true,
                                                borderColor = OrangeNeon.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Lista de Plataformas
            items(filteredPlatforms, key = { it.id }) { platform ->
                val monthEarnings = uiState.earningsMap[platform.id] ?: BigDecimal.ZERO
                PlatformCardItem(
                    platform = platform,
                    monthEarnings = monthEarnings,
                    onEditClick = { viewModel.startEditing(platform) },
                    onToggleActive = { viewModel.togglePlatformActive(platform) }
                )
            }

            // 7. Botão inferior tracejado "+ ADICIONAR NOVA PLATAFORMA"
            item {
                Surface(
                    onClick = { viewModel.openAddDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(2.dp, OrangeNeon.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADICIONAR NOVA PLATAFORMA",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )
                    }
                }
            }
        }
    }

    // Modal de Filtros & Ordenação
    if (uiState.isFilterModalOpen) {
        FilterSortModal(
            uiState = uiState,
            onDismiss = { viewModel.closeFilterModal() },
            onStatusChange = { viewModel.onStatusFilterChanged(it) },
            onSegmentChange = { viewModel.onSegmentFilterChanged(it) },
            onSortChange = { viewModel.onSortByChanged(it) },
            onClear = { viewModel.clearFilters() }
        )
    }

    // Modal de Adicionar / Editar Plataforma
    if (uiState.isFormOpen) {
        PlatformFormModal(
            uiState = uiState,
            onDismiss = { viewModel.closeForm() },
            onNameChange = { viewModel.onNameChanged(it) },
            onSegmentChange = { viewModel.onSegmentChanged(it) },
            onPaymentModelChange = { viewModel.onPaymentModelChanged(it) },
            onCycleChange = { viewModel.onCycleChanged(it) },
            onPaymentDayChange = { viewModel.onPaymentDayChanged(it) },
            onFixedPayDelayChange = { viewModel.onFixedPayDelayChanged(it) },
            onActiveChange = { viewModel.onActiveChanged(it) },
            onBankNameChange = { viewModel.onBankNameChanged(it) },
            onBankAgencyChange = { viewModel.onBankAgencyChanged(it) },
            onBankAccountChange = { viewModel.onBankAccountChanged(it) },
            onPixKeyTypeChange = { viewModel.onPixKeyTypeChanged(it) },
            onPixKeyChange = { viewModel.onPixKeyChanged(it) },
            onAddCycleEntry = { viewModel.addCycleEntry() },
            onRemoveCycleEntry = { viewModel.removeCycleEntry(it) },
            onUpdateCycleEntry = { idx, cut, delay -> viewModel.updateCycleEntry(idx, cut, delay) },
            onSave = { viewModel.savePlatform() },
            onDelete = { platformId -> viewModel.deletePlatform(platformId) }
        )
    }
}

@Composable
fun PlatformCardItem(
    platform: Platform,
    monthEarnings: BigDecimal,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val isLogistica = platform.segment.equals("logistica", ignoreCase = true)
    val segmentBadgeColor = if (isLogistica) BlueInfo else GreenNeon
    val segmentLabel = if (isLogistica) "Logística" else "Delivery"
    val cycleLabel = BillingCycleCalculator.getCycleDisplayLabel(platform.cycle, platform.paymentDay)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (platform.active)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ícone da plataforma
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (platform.active) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (platform.active) OrangeNeon.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLogistica) Icons.Default.LocalShipping else Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = if (platform.active) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Detalhes da Plataforma
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = platform.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (platform.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Switch Ativa / Inativa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Switch(
                            checked = platform.active,
                            onCheckedChange = { onToggleActive() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = GreenNeon,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = RedAlert.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                        Text(
                            text = if (platform.active) "ATIVA" else "INATIVA",
                            fontSize = 10.sp,
                            color = if (platform.active) GreenNeon else RedAlert,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Tags de Categoria e Ciclo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = segmentBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, segmentBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = segmentLabel,
                            color = segmentBadgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = cycleLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Estimativa de Pagamento do Mês
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Est. de pagamento",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = monthEarnings.formatCurrency(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangeNeon
                    )
                }
            }

            // Botão Configurações / Edição
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Editar ${platform.name}",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortModal(
    uiState: PlatformsUiState,
    onDismiss: () -> Unit,
    onStatusChange: (PlatformStatusFilter) -> Unit,
    onSegmentChange: (PlatformSegmentFilter) -> Unit,
    onSortChange: (PlatformSortBy) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabeçalho do modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILTRAR & ORDENAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Personalize a exibição das suas plataformas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // 1. Status da Plataforma
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STATUS DA PLATAFORMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformStatusFilter.entries.forEach { status ->
                        val isSelected = uiState.statusFilter == status
                        OutlinedButton(
                            onClick = { onStatusChange(status) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.Transparent
                            )
                        ) {
                            Text(status.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Categoria (Segmento)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TIPO DE CATEGORIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformSegmentFilter.entries.forEach { segment ->
                        val isSelected = uiState.segmentFilter == segment
                        OutlinedButton(
                            onClick = { onSegmentChange(segment) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.Transparent
                            )
                        ) {
                            Text(segment.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Ordenação
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ORDENAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PlatformSortBy.entries.forEach { sort ->
                    val isSelected = uiState.sortBy == sort
                    Surface(
                        onClick = { onSortChange(sort) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) OrangeNeon else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sort.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Ações
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onClear()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Limpar Filtros", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformFormModal(
    uiState: PlatformsUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onSegmentChange: (String) -> Unit,
    onPaymentModelChange: (String) -> Unit,
    onCycleChange: (String) -> Unit,
    onPaymentDayChange: (String) -> Unit,
    onFixedPayDelayChange: (Int) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onBankNameChange: (String) -> Unit,
    onBankAgencyChange: (String) -> Unit,
    onBankAccountChange: (String) -> Unit,
    onPixKeyTypeChange: (String) -> Unit,
    onPixKeyChange: (String) -> Unit,
    onAddCycleEntry: () -> Unit,
    onRemoveCycleEntry: (Int) -> Unit,
    onUpdateCycleEntry: (Int, Int?, Int?) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pixTypeExpanded by remember { mutableStateOf(false) }
    val isEditing = uiState.editingPlatformId != null

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "EDITAR PLATAFORMA" else "NOVA PLATAFORMA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangeNeon
                )
                if (isEditing) {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedAlert)
                    }
                }
            }

            // 1. Campo Nome
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("Nome da Plataforma") },
                placeholder = { Text("Ex: iFood, Loggi, Mercado Envios") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeNeon,
                    focusedLabelColor = OrangeNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Segmento de Operação
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SEGMENTO DE OPERAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLogistica = uiState.segment == "logistica"
                    OutlinedButton(
                        onClick = { onSegmentChange("logistica") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isLogistica) BlueInfo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isLogistica) BlueInfo else MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLogistica) BlueInfo else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logística", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    val isDelivery = uiState.segment == "delivery"
                    OutlinedButton(
                        onClick = { onSegmentChange("delivery") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDelivery) GreenNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDelivery) GreenNeon else MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDelivery) GreenNeon else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delivery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 3. Modelo de Pagamento
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MODELO DE PAGAMENTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isProducao = uiState.paymentModel == "producao"
                    OutlinedButton(
                        onClick = { onPaymentModelChange("producao") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isProducao) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isProducao) OrangeNeon else MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isProducao) OrangeNeon else Color.Transparent
                        )
                    ) {
                        Text("Produção (Pacote)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    val isDiaria = uiState.paymentModel == "diaria"
                    OutlinedButton(
                        onClick = { onPaymentModelChange("diaria") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDiaria) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDiaria) OrangeNeon else MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDiaria) OrangeNeon else Color.Transparent
                        )
                    ) {
                        Text("Diária (Fixo)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // 4. Ciclo de Pagamento (Grid 2x2: SEMANAL, QUINZENAL, MENSAL, VARIÁVEL)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CICLO DE PAGAMENTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val cycles = listOf(
                    "semanal" to "SEMANAL",
                    "quinzenal" to "QUINZENAL",
                    "mensal" to "MENSAL",
                    "misto" to "VARIÁVEL"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..1) {
                            val (key, label) = cycles[i]
                            val isSelected = uiState.cycle == key
                            OutlinedButton(
                                onClick = { onCycleChange(key) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) OrangeNeon else Color.Transparent
                                )
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 2..3) {
                            val (key, label) = cycles[i]
                            val isSelected = uiState.cycle == key
                            OutlinedButton(
                                onClick = { onCycleChange(key) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) OrangeNeon else Color.Transparent
                                )
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 5. Configuração Específica do Ciclo Selecionado
            when (uiState.cycle) {
                "semanal" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dia de fechamento semanal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WEEK_DAYS.forEach { day ->
                                val isSelected = uiState.paymentDay.equals(day, ignoreCase = true)
                                Surface(
                                    onClick = { onPaymentDayChange(day) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        OutlinedTextField(
                            value = uiState.fixedPayDelay.toString(),
                            onValueChange = { str ->
                                val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                onFixedPayDelayChange(num)
                            },
                            label = { Text("Prazo de pagamento (dias após fechamento)") },
                            suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "Ex: se fechar toda ${uiState.paymentDay} e o prazo for ${uiState.fixedPayDelay} dias, o repasse cai na ${uiState.paymentDay} seguinte da semana.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "quinzenal", "mensal" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.fixedPayDelay.toString(),
                            onValueChange = { str ->
                                val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                onFixedPayDelayChange(num)
                            },
                            label = { Text("Prazo de pagamento (dias após fechamento)") },
                            suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = if (uiState.cycle == "quinzenal")
                                "Fechamento automático no dia 15 e no último dia do mês. O pagamento é realizado ${uiState.fixedPayDelay} dias após cada fechamento."
                            else
                                "Fechamento automático no último dia do mês. O pagamento é realizado ${uiState.fixedPayDelay} dias após o fechamento.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "misto", "variavel" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ciclos de pagamento",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Configure cada corte e os dias até o repasse:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onAddCycleEntry) {
                                Text("+ Adicionar ciclo", color = OrangeNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        uiState.cycleEntries.forEachIndexed { index, entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CICLO ${index + 1}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = OrangeNeon,
                                            letterSpacing = 1.sp
                                        )
                                        if (uiState.cycleEntries.size > 1) {
                                            IconButton(
                                                onClick = { onRemoveCycleEntry(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remover ciclo",
                                                    tint = RedAlert,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = entry.cut.toString(),
                                            onValueChange = { str ->
                                                val v = str.filter { it.isDigit() }.toIntOrNull()
                                                onUpdateCycleEntry(index, v, null)
                                            },
                                            label = { Text("Fechamento (dia)") },
                                            placeholder = { Text("1-28") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangeNeon,
                                                focusedLabelColor = OrangeNeon
                                            ),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = entry.payDelay.toString(),
                                            onValueChange = { str ->
                                                val v = str.filter { it.isDigit() }.toIntOrNull()
                                                onUpdateCycleEntry(index, null, v)
                                            },
                                            label = { Text("Pagamento (dias)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangeNeon,
                                                focusedLabelColor = OrangeNeon
                                            ),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }

                                    val payDayEst = entry.cut + entry.payDelay
                                    val feedback = if (payDayEst > 28)
                                        "Fecha dia ${entry.cut} → paga dia ${payDayEst - 28} do mês seguinte"
                                    else
                                        "Fecha dia ${entry.cut} → paga dia $payDayEst"

                                    Surface(
                                        color = OrangeNeon.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = feedback,
                                            fontSize = 11.sp,
                                            color = OrangeNeon,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Dados de Recebimento Bancário
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DADOS DE RECEBIMENTO (OPCIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.bankName,
                    onValueChange = onBankNameChange,
                    label = { Text("Instituição financeira") },
                    placeholder = { Text("Ex: Nubank, Itaú, Bradesco...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.bankAgency,
                        onValueChange = onBankAgencyChange,
                        label = { Text("Agência") },
                        placeholder = { Text("0001") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = uiState.bankAccount,
                        onValueChange = onBankAccountChange,
                        label = { Text("Conta") },
                        placeholder = { Text("123456-7") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 7. Chave PIX
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CHAVE PIX (OPCIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = pixTypeExpanded,
                        onExpandedChange = { pixTypeExpanded = !pixTypeExpanded },
                        modifier = Modifier.width(120.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.pixKeyType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pixTypeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = pixTypeExpanded,
                            onDismissRequest = { pixTypeExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            PIX_KEY_TYPES.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        onPixKeyTypeChange(type)
                                        pixTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.pixKey,
                        onValueChange = onPixKeyChange,
                        label = { Text("Chave PIX") },
                        placeholder = { Text("Digite a chave") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 8. Status da Plataforma (Ativa / Inativa)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Status da Plataforma",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Habilitar para novos lançamentos de rota",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.active,
                    onCheckedChange = onActiveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GreenNeon,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = RedAlert.copy(alpha = 0.6f)
                    )
                )
            }

            // 9. Botão Salvar
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && uiState.name.isNotBlank(),
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "SALVAR ALTERAÇÕES" else "VINCULAR PLATAFORMA",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmação de Exclusão
    if (showDeleteConfirmDialog && uiState.editingPlatformId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    "Excluir Plataforma?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja excluir '${uiState.name}'? Corridas e lançamentos já registrados não serão excluídos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(uiState.editingPlatformId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
