package com.fernando.centraldomotorista.ui.screens.pecas

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.ui.screens.empresas.EmpresasViewModel
import com.fernando.centraldomotorista.ui.theme.*

private val WhatsAppGreen = Color(0xFF25D366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartMaintenanceScreen(
    viewModel: PartMaintenanceViewModel = viewModel(),
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
                        text = "MONITORAMENTO DE PEÇAS",
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
                    IconButton(onClick = { viewModel.loadData() }) {
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Peça")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        val filteredParts = remember(uiState.parts, uiState.searchQuery) {
            if (uiState.searchQuery.isBlank()) {
                uiState.parts
            } else {
                uiState.parts.filter {
                    it.partName.contains(uiState.searchQuery, ignoreCase = true)
                }
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
                    placeholder = { Text("Buscar peça ou manutenção...", color = Color.Gray, fontSize = 14.sp) },
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

            // 2. Contador de Peças
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PEÇAS & COMPONENTES",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${filteredParts.size} cadastrada(s)",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Loading
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
            if (!uiState.isLoading && filteredParts.isEmpty()) {
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
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (uiState.parts.isEmpty()) "Nenhuma peça em monitoramento" else "Nenhum resultado encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.parts.isEmpty())
                                    "Cadastre peças e componentes do seu veículo para acompanhar a vida útil e receber alertas de manutenção."
                                else "Tente buscar por outro termo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.parts.isEmpty()) {
                                Text(
                                    text = "SUGESTÕES DE PEÇAS:",
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
                                    items(SUGGESTED_PARTS) { partName ->
                                        SuggestionChip(
                                            onClick = { viewModel.openAddDialog(partName) },
                                            label = { Text(partName, fontSize = 12.sp, color = Color.White) },
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

            // 5. Lista de Peças
            items(filteredParts, key = { it.id }) { part ->
                val linkedCompany = remember(uiState.companies, part.companyId) {
                    uiState.companies.firstOrNull { it.id == part.companyId }
                }

                PartMaintenanceCard(
                    part = part,
                    company = linkedCompany,
                    onEdit = { viewModel.startEditing(part) },
                    onContactPhone = { phone, isWhatsapp ->
                        openCompanyContact(context, phone, isWhatsapp)
                    },
                    onOpenMap = { company ->
                        openCompanyAddress(context, company)
                    }
                )
            }
        }
    }

    // Modal de Cadastro / Edição de Peça
    if (uiState.isFormOpen) {
        val isEditing = uiState.editingPartId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
        var companyDropdownExpanded by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            containerColor = SurfaceDark,
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "EDITAR PEÇA" else "NOVA PEÇA / MANUTENÇÃO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Controle de durabilidade e trocas periódicas",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Peça", tint = RedAlert)
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                // Nome da Peça *
                OutlinedTextField(
                    value = uiState.partName,
                    onValueChange = { viewModel.onPartNameChanged(it) },
                    label = { Text("Nome da Peça / Componente *") },
                    placeholder = { Text("Ex: Óleo do Motor, Pneu Traseiro, Pastilhas") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Build, contentDescription = null, tint = OrangeNeon)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.DarkGray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Vida Útil em KM * e KM da Última Troca *
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.lifeKm,
                        onValueChange = { viewModel.onLifeKmChanged(it) },
                        label = { Text("Vida Útil (KM) *") },
                        placeholder = { Text("Ex: 5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = OrangeNeon)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.lastChangeKm,
                        onValueChange = { viewModel.onLastChangeKmChanged(it) },
                        label = { Text("KM Última Troca *") },
                        placeholder = { Text("Ex: 12500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = OrangeNeon)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. Empresa/Oficina (opcional) — Dropdown + Botão "+"
                Text(
                    text = "VÍNCULO COM EMPRESA / OFICINA",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectedCompanyName = uiState.companies.firstOrNull { it.id == uiState.selectedCompanyId }?.name
                        ?: "Nenhuma empresa vinculada"

                    ExposedDropdownMenuBox(
                        expanded = companyDropdownExpanded,
                        onExpandedChange = { companyDropdownExpanded = !companyDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCompanyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Empresa / Oficina (opcional)") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, tint = OrangeNeon)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = companyDropdownExpanded,
                            onDismissRequest = { companyDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("(Nenhuma empresa)", color = Color.Gray) },
                                onClick = {
                                    viewModel.onCompanySelected(null)
                                    companyDropdownExpanded = false
                                }
                            )
                            uiState.companies.forEach { company ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(company.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            if (!company.street.isNullOrBlank()) {
                                                Text(
                                                    text = "${company.street}, ${company.number ?: ""}",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.onCompanySelected(company.id)
                                        companyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Botão "+" para cadastro rápido de nova empresa
                    IconButton(
                        onClick = { viewModel.openAddCompanyDialog() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Nova Empresa",
                            tint = OrangeNeon,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão Salvar
                Button(
                    onClick = { viewModel.savePartMaintenance() },
                    enabled = !uiState.isSaving && uiState.partName.isNotBlank() && uiState.lifeKm.isNotBlank(),
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
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Peça",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Confirmação de exclusão
        if (showDeleteConfirmDialog && uiState.editingPartId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Peça?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir o monitoramento de '${uiState.partName}'?", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deletePartMaintenance(uiState.editingPartId!!)
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

    // Diálogo de Cadastro Rápido de Empresa
    if (uiState.isAddCompanyDialogOpen) {
        var quickCompanyName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeAddCompanyDialog() },
            title = { Text("Nova Empresa / Oficina", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cadastre o nome da empresa para vinculá-la a esta peça:", color = Color.LightGray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = quickCompanyName,
                        onValueChange = { quickCompanyName = it },
                        label = { Text("Nome da Empresa") },
                        placeholder = { Text("Ex: Oficina Moto Prime, Dinho Motos") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, tint = OrangeNeon)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addQuickCompany(quickCompanyName)
                    },
                    enabled = quickCompanyName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar Empresa")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddCompanyDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun PartMaintenanceCard(
    part: PartMaintenance,
    company: Company?,
    onEdit: () -> Unit,
    onContactPhone: (String, Boolean) -> Unit,
    onOpenMap: (Company) -> Unit
) {
    val nextDueKm = part.lastChangeKm + part.lifeKm

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header do Card: Nome e Ícone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, OrangeNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.partName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Vida útil: ${part.lifeKm} KM",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Métricas de Troca (Última Troca vs Próxima Troca)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDarkAlt, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ÚLTIMA TROCA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "${part.lastChangeKm} KM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PRÓXIMA TROCA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon
                    )
                    Text(
                        text = "$nextDueKm KM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon
                    )
                }
            }

            // 6. Empresa Vinculada + Ações Rápidas (Telefone / WhatsApp / Google Maps)
            if (company != null) {
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Identificação da Empresa
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = company.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val companyAddr = listOfNotNull(
                                company.street?.takeIf { it.isNotBlank() },
                                company.number?.takeIf { it.isNotBlank() }
                            ).joinToString(", ")
                            if (companyAddr.isNotBlank()) {
                                Text(
                                    text = companyAddr,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Ações Rápidas: Contato & Endereço
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Telefone / WhatsApp
                        if (!company.phone.isNullOrBlank()) {
                            val isWpp = company.isWhatsapp
                            FilledTonalIconButton(
                                onClick = { onContactPhone(company.phone, isWpp) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isWpp) WhatsAppGreen.copy(alpha = 0.2f) else OrangeNeon.copy(alpha = 0.2f),
                                    contentColor = if (isWpp) WhatsAppGreen else OrangeNeon
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWpp) Icons.Default.ChatBubble else Icons.Default.Phone,
                                    contentDescription = if (isWpp) "WhatsApp" else "Ligar",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Botão Google Maps
                        val hasAddress = !company.street.isNullOrBlank() || !company.cep.isNullOrBlank()
                        if (hasAddress) {
                            FilledTonalIconButton(
                                onClick = { onOpenMap(company) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Ver no Mapa",
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

private fun openCompanyContact(context: Context, phone: String, isWhatsapp: Boolean) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isBlank()) {
        Toast.makeText(context, "Telefone não informado", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        if (isWhatsapp) {
            val fullPhone = if (digits.startsWith("55")) digits else "55$digits"
            val uri = Uri.parse("https://wa.me/$fullPhone")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o aplicativo de contato", Toast.LENGTH_SHORT).show()
    }
}

private fun openCompanyAddress(context: Context, company: Company) {
    val addressParts = listOfNotNull(
        company.street?.takeIf { it.isNotBlank() },
        company.number?.takeIf { it.isNotBlank() },
        company.cep?.takeIf { it.isNotBlank() },
        company.name.takeIf { it.isNotBlank() }
    ).joinToString(", ")

    if (addressParts.isBlank()) {
        Toast.makeText(context, "Endereço não informado", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(addressParts))
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserUri = Uri.parse("https://maps.google.com/?q=" + Uri.encode(addressParts))
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    } catch (e: Exception) {
        try {
            val browserUri = Uri.parse("https://maps.google.com/?q=" + Uri.encode(addressParts))
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        } catch (err: Exception) {
            Toast.makeText(context, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
        }
    }
}
