package com.fernando.centraldomotorista.ui.screens.apps

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.theme.*

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
                        text = "APPS & PLATAFORMAS",
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
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPlatforms() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = OrangeNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
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
        containerColor = BackgroundDark
    ) { padding ->
        val filteredPlatforms = remember(uiState.platforms, uiState.searchQuery, uiState.selectedSegmentFilter) {
            uiState.platforms.filter { platform ->
                val matchesSearch = uiState.searchQuery.isBlank() ||
                        platform.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        platform.cycle.contains(uiState.searchQuery, ignoreCase = true) ||
                        (platform.paymentDay?.contains(uiState.searchQuery, ignoreCase = true) == true)

                val matchesSegment = uiState.selectedSegmentFilter == null ||
                        platform.segment.equals(uiState.selectedSegmentFilter, ignoreCase = true)

                matchesSearch && matchesSegment
            }
        }

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
                    placeholder = { Text("Buscar plataforma, ciclo ou dia...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Filtros de Segmento
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedSegmentFilter == null,
                        onClick = { viewModel.onSegmentFilterChanged(null) },
                        label = { Text("Todas (${uiState.platforms.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeNeon,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceDark,
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = uiState.selectedSegmentFilter == "logistica",
                        onClick = { viewModel.onSegmentFilterChanged("logistica") },
                        label = { Text("Logística (${uiState.platforms.count { it.segment == "logistica" }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueInfo,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = uiState.selectedSegmentFilter == "delivery",
                        onClick = { viewModel.onSegmentFilterChanged("delivery") },
                        label = { Text("Delivery (${uiState.platforms.count { it.segment == "delivery" }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenNeon,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = Color.White
                        )
                    )
                }
            }

            // 3. Loading state
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

            // 4. Empty State
            if (!uiState.isLoading && filteredPlatforms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
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
                                text = if (uiState.platforms.isEmpty()) "Nenhuma plataforma cadastrada" else "Nenhum resultado para a busca",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.platforms.isEmpty())
                                    "Adicione os apps onde você trabalha para organizar repasses e faturamento."
                                else "Tente buscar por outro termo ou limpe os filtros.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.platforms.isEmpty()) {
                                Text(
                                    text = "SUGESTÕES RÁPIDAS:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(POPULAR_PLATFORMS) { (name, segment, cycle) ->
                                        SuggestionChip(
                                            onClick = { viewModel.openAddDialog(name, segment, cycle) },
                                            label = { Text(name, fontSize = 12.sp, color = Color.White) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = SurfaceDarkAlt
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

            // 5. Lista de Plataformas
            items(filteredPlatforms, key = { it.id }) { platform ->
                PlatformCardItem(
                    platform = platform,
                    onEditClick = { viewModel.startEditing(platform) },
                    onToggleActive = { viewModel.togglePlatformActive(platform) }
                )
            }
        }
    }

    // Modal de Adicionar / Editar Plataforma
    if (uiState.isFormOpen) {
        PlatformFormModal(
            uiState = uiState,
            onDismiss = { viewModel.closeForm() },
            onNameChange = { viewModel.onNameChanged(it) },
            onSegmentChange = { viewModel.onSegmentChanged(it) },
            onCycleChange = { viewModel.onCycleChanged(it) },
            onPaymentDayChange = { viewModel.onPaymentDayChanged(it) },
            onPaymentModelChange = { viewModel.onPaymentModelChanged(it) },
            onActiveChange = { viewModel.onActiveChanged(it) },
            onSave = { viewModel.savePlatform() },
            onDelete = { platformId -> viewModel.deletePlatform(platformId) }
        )
    }
}

@Composable
fun PlatformCardItem(
    platform: Platform,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val isLogistica = platform.segment.equals("logistica", ignoreCase = true)
    val segmentBadgeColor = if (isLogistica) BlueInfo else GreenNeon
    val segmentLabel = if (isLogistica) "Logística" else "Delivery"
    val cycleLabel = when (platform.cycle.lowercase()) {
        "semanal" -> "Semanal"
        "quinzenal" -> "Quinzenal"
        "misto" -> "Misto"
        "mensal" -> "Mensal"
        "diario" -> "Diário"
        else -> platform.cycle
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = if (platform.active)
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.4f))
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
                    .size(44.dp)
                    .background(
                        color = if (platform.active) OrangeNeon.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.3f),
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
                    tint = if (platform.active) OrangeNeon else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Detalhes da Plataforma
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = platform.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (platform.active) Color.White else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Ciclo: $cycleLabel",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Dia de pagamento
                if (!platform.paymentDay.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Repasse: ${platform.paymentDay}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Switch Ativa/Inativa
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Switch(
                    checked = platform.active,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = OrangeNeon,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceDarkAlt
                    )
                )
                Text(
                    text = if (platform.active) "Ativa" else "Inativa",
                    fontSize = 10.sp,
                    color = if (platform.active) OrangeNeon else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
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
    onCycleChange: (String) -> Unit,
    onPaymentDayChange: (String) -> Unit,
    onPaymentModelChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var segmentExpanded by remember { mutableStateOf(false) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var paymentDayExpanded by remember { mutableStateOf(false) }
    var paymentModelExpanded by remember { mutableStateOf(false) }

    val isEditing = uiState.editingPlatformId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho do Modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Plataforma" else "Nova Plataforma",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
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
                label = { Text("Nome da Plataforma (ex: iFood, Loggi)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeNeon,
                    focusedLabelColor = OrangeNeon,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Dropdown Categoria (Segmento)
            ExposedDropdownMenuBox(
                expanded = segmentExpanded,
                onExpandedChange = { segmentExpanded = !segmentExpanded }
            ) {
                OutlinedTextField(
                    value = PLATFORM_SEGMENTS.find { it.first == uiState.segment }?.second ?: uiState.segment,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria (Segmento)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = segmentExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = segmentExpanded,
                    onDismissRequest = { segmentExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    PLATFORM_SEGMENTS.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = {
                                onSegmentChange(key)
                                segmentExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Dropdown Ciclo de Pagamento
            ExposedDropdownMenuBox(
                expanded = cycleExpanded,
                onExpandedChange = { cycleExpanded = !cycleExpanded }
            ) {
                OutlinedTextField(
                    value = PAYMENT_CYCLES.find { it.first == uiState.cycle }?.second ?: uiState.cycle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ciclo de Pagamento") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = cycleExpanded,
                    onDismissRequest = { cycleExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    PAYMENT_CYCLES.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = {
                                onCycleChange(key)
                                cycleExpanded = false
                            }
                        )
                    }
                }
            }

            // 4. Campo Dia de Pagamento
            ExposedDropdownMenuBox(
                expanded = paymentDayExpanded,
                onExpandedChange = { paymentDayExpanded = !paymentDayExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.paymentDay,
                    onValueChange = onPaymentDayChange,
                    label = { Text("Dia do Pagamento / Repasse") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDayExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = paymentDayExpanded,
                    onDismissRequest = { paymentDayExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    COMMON_PAYMENT_DAYS.forEach { dayOption ->
                        DropdownMenuItem(
                            text = { Text(dayOption, color = Color.White) },
                            onClick = {
                                onPaymentDayChange(dayOption)
                                paymentDayExpanded = false
                            }
                        )
                    }
                }
            }

            // 5. Switch Ativa / Inativa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Status da Plataforma", fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Habilitar para novos lançamentos de rota", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = uiState.active,
                    onCheckedChange = onActiveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = OrangeNeon
                    )
                )
            }

            // Botão Salvar
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
                    .height(50.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isEditing) "Salvar Alterações" else "Cadastrar Plataforma",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
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
            title = { Text("Excluir Plataforma?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir '${uiState.name}'? Rotas já registradas não serão afetadas.", color = Color.LightGray) },
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
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
