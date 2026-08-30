package com.fernando.centraldomotorista.ui.screens.partproducts

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
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.data.model.PartType
import com.fernando.centraldomotorista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartProductsScreen(
    viewModel: PartProductsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var partTypeDropdownExpanded by remember { mutableStateOf(false) }
    var selectedTypeForOptions by remember { mutableStateOf<PartType?>(null) }
    var showTypeOptionsMenu by remember { mutableStateOf(false) }

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
                        text = "PRODUTOS & MARCAS",
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
                onClick = { viewModel.openCreateProductDialog() },
                containerColor = OrangeNeon,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Produto")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        val filteredProducts = remember(uiState.partProducts, uiState.selectedFilterTypeId, uiState.searchQuery) {
            uiState.partProducts.filter { prod ->
                val matchesType = uiState.selectedFilterTypeId == null || prod.partTypeId == uiState.selectedFilterTypeId
                val matchesQuery = uiState.searchQuery.isBlank() ||
                        prod.brand.contains(uiState.searchQuery, ignoreCase = true) ||
                        (prod.model?.contains(uiState.searchQuery, ignoreCase = true) == true)
                matchesType && matchesQuery
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
                    placeholder = { Text("Buscar marca ou modelo...", color = Color.Gray, fontSize = 14.sp) },
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

            // 2. Filtro por Chips de Tipos de Peça
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIPO DE PEÇA",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { viewModel.openAddTypeDialog() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo Tipo", color = OrangeNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Chip "Todos"
                        item {
                            FilterChip(
                                selected = uiState.selectedFilterTypeId == null,
                                onClick = { viewModel.onSelectFilterType(null) },
                                label = { Text("Todos", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeNeon,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceDark,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedFilterTypeId == null,
                                    borderColor = if (uiState.selectedFilterTypeId == null) OrangeNeon else Color.DarkGray
                                )
                            )
                        }

                        // Chips de cada PartType
                        items(uiState.partTypes, key = { it.id }) { type ->
                            val isSelected = uiState.selectedFilterTypeId == type.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        // Se já está selecionado e clica novamente, abre opções de renomear/excluir
                                        selectedTypeForOptions = type
                                        showTypeOptionsMenu = true
                                    } else {
                                        viewModel.onSelectFilterType(type.id)
                                    }
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(type.name, fontSize = 12.sp)
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Opções do tipo",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeNeon,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceDark,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) OrangeNeon else Color.DarkGray
                                )
                            )
                        }
                    }
                }
            }

            // 3. Contador de Produtos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRODUTOS CADASTRADOS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${filteredProducts.size} produto(s)",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Loading
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

            // Empty State
            if (!uiState.isLoading && filteredProducts.isEmpty()) {
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
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (uiState.partProducts.isEmpty()) "Nenhum produto cadastrado" else "Nenhum produto com este filtro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.partProducts.isEmpty())
                                    "Cadastre as marcas e modelos específicos de peças (ex: Mobil Super 20W50, Cobreq Orgânica) com sua vida útil padrão."
                                else "Selecione outro tipo ou adicione um novo produto.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.openCreateProductDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cadastrar Produto", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Lista de Produtos
            items(filteredProducts, key = { it.id }) { product ->
                val typeName = uiState.partTypes.firstOrNull { it.id == product.partTypeId }?.name ?: "Peça"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.openEditProductDialog(product) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, OrangeNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = OrangeNeon.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = typeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeNeon,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = if (!product.model.isNullOrBlank()) "${product.brand} - ${product.model}" else product.brand,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )

                            Text(
                                text = "Vida útil padrão: ${product.defaultLifeKm} KM",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { viewModel.openEditProductDialog(product) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de Formulário do Produto (Adicionar / Editar)
    if (uiState.isProductFormOpen) {
        val isEditing = uiState.editingProductId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeProductDialog() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "EDITAR PRODUTO" else "NOVO PRODUTO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Marca, modelo e durabilidade estimada",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Produto", tint = RedAlert)
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                // Tipo de Peça (Dropdown + Botão "+")
                Text(
                    text = "TIPO DE PEÇA *",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectedTypeName = uiState.partTypes.firstOrNull { it.id == uiState.selectedFormTypeId }?.name
                        ?: "Selecione o Tipo de Peça"

                    ExposedDropdownMenuBox(
                        expanded = partTypeDropdownExpanded,
                        onExpandedChange = { partTypeDropdownExpanded = !partTypeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedTypeName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Peça *") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = OrangeNeon)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = partTypeDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = partTypeDropdownExpanded,
                            onDismissRequest = { partTypeDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            uiState.partTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name, color = Color.White) },
                                    onClick = {
                                        viewModel.onFormTypeSelected(type.id)
                                        partTypeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Botão "+" para adicionar novo tipo de peça na hora
                    IconButton(
                        onClick = { viewModel.openAddTypeDialog() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Novo Tipo de Peça",
                            tint = OrangeNeon,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Marca *
                OutlinedTextField(
                    value = uiState.brand,
                    onValueChange = { viewModel.onBrandChanged(it) },
                    label = { Text("Marca *") },
                    placeholder = { Text("Ex: Mobil, Motul, Cobreq, Riffel") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null, tint = OrangeNeon)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.DarkGray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Modelo (opcional)
                OutlinedTextField(
                    value = uiState.model,
                    onValueChange = { viewModel.onModelChanged(it) },
                    label = { Text("Modelo (opcional)") },
                    placeholder = { Text("Ex: Super 20W50, Orgânica, Titanium") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Style, contentDescription = null, tint = OrangeNeon)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.DarkGray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Vida Útil Padrão (KM) *
                OutlinedTextField(
                    value = uiState.defaultLifeKm,
                    onValueChange = { viewModel.onDefaultLifeKmChanged(it) },
                    label = { Text("Vida Útil Padrão (KM) *") },
                    placeholder = { Text("Ex: 5000, 15000") },
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveProduct() },
                    enabled = !uiState.isSaving && uiState.brand.isNotBlank() && uiState.defaultLifeKm.isNotBlank() && uiState.selectedFormTypeId != null,
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
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Produto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmDialog && uiState.editingProductId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Produto?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir '${uiState.brand}'? Os lançamentos de manutenção vinculados manterão seus registros.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteProduct(uiState.editingProductId!!)
                            viewModel.closeProductDialog()
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

    // Diálogo Simples de 1 linha: "Novo Tipo de Peça"
    if (uiState.isAddTypeDialogOpen) {
        var typeNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeAddTypeDialog() },
            title = { Text("Novo Tipo de Peça", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = typeNameInput,
                    onValueChange = { typeNameInput = it },
                    label = { Text("Nome do Tipo (ex: Óleo do Motor)") },
                    placeholder = { Text("Ex: Pastilha de Freio, Vela de Ignição") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Category, contentDescription = null, tint = OrangeNeon)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createQuickPartType(typeNameInput)
                    },
                    enabled = typeNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddTypeDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Diálogo de Contexto: Opções do Tipo de Peça (Renomear / Excluir)
    if (showTypeOptionsMenu && selectedTypeForOptions != null) {
        val currentType = selectedTypeForOptions!!
        AlertDialog(
            onDismissRequest = { showTypeOptionsMenu = false },
            title = { Text("Tipo: ${currentType.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja renomear ou excluir este tipo de peça?", color = Color.LightGray)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTypeOptionsMenu = false
                        viewModel.openRenameTypeDialog(currentType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Renomear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTypeOptionsMenu = false
                        viewModel.openDeleteTypeDialog(currentType)
                    }
                ) {
                    Text("Excluir Tipo", color = RedAlert)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Diálogo de Renomear Tipo de Peça
    if (uiState.editingTypeForRename != null) {
        val targetType = uiState.editingTypeForRename!!
        var renameInput by remember(targetType) { mutableStateOf(targetType.name) }

        AlertDialog(
            onDismissRequest = { viewModel.closeRenameTypeDialog() },
            title = { Text("Renomear Tipo de Peça", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Novo Nome") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renamePartType(targetType, renameInput)
                    },
                    enabled = renameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeRenameTypeDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Diálogo de Confirmar Exclusão de Tipo de Peça
    if (uiState.typeToDelete != null) {
        val targetType = uiState.typeToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteTypeDialog() },
            title = { Text("Excluir Tipo de Peça?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir o tipo '${targetType.name}'?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePartType(targetType.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteTypeDialog() }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
