package com.fernando.centraldomotorista.ui.screens.pecas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.ui.screens.expenses.CardPaymentModal
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val WhatsAppGreen = Color(0xFF25D366)
private val GreenNeon = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartMaintenanceScreen(
    viewModel: PartMaintenanceViewModel = viewModel(),
    onNavigateToPartProducts: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCardModal by remember { mutableStateOf(false) }

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
                    // Botão para navegar para a tela de Produtos & Marcas de Peças
                    IconButton(onClick = onNavigateToPartProducts) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Gerenciar Produtos e Marcas",
                            tint = OrangeNeon
                        )
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color.White
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
                Icon(Icons.Default.Add, contentDescription = "Lançar Peça / Manutenção")
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

            // 2. Atalho para Produtos & Marcas + Contador
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PEÇAS EM MONITORAMENTO",
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
                                    "Cadastre peças e componentes do seu veículo para acompanhar a vida útil, receber alertas e controlar o custo financeiro."
                                else "Tente buscar por outro termo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.parts.isEmpty()) {
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
                val linkedProduct = remember(uiState.partProducts, part.partProductId) {
                    uiState.partProducts.firstOrNull { it.id == part.partProductId }
                }

                PartMaintenanceCard(
                    part = part,
                    company = linkedCompany,
                    product = linkedProduct,
                    currentOdometerKm = uiState.currentOdometerKm,
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

    // Modal de Formulário de Lançamento / Edição de Manutenção
    if (uiState.isFormOpen) {
        val isEditing = uiState.editingPartId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
        var productDropdownExpanded by remember { mutableStateOf(false) }
        var companyDropdownExpanded by remember { mutableStateOf(false) }
        var productSearchQuery by remember { mutableStateOf("") }

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
                            text = if (isEditing) "EDITAR PEÇA / MANUTENÇÃO" else "LANÇAR MANUTENÇÃO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Controle de durabilidade e despesa financeira",
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

                // SEÇÃO 1: DADOS DA PEÇA & CATÁLOGO
                Text(
                    text = "DADOS DA PEÇA & CATÁLOGO",
                    color = OrangeNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                // 1. Seletor de Produto Cadastrado (Tipo — Marca Modelo) com botão "+"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentSelectedProduct = uiState.partProducts.firstOrNull { it.id == uiState.selectedPartProductId }
                    val currentProductLabel = if (currentSelectedProduct != null) {
                        val typeName = uiState.partTypes.firstOrNull { it.id == currentSelectedProduct.partTypeId }?.name ?: "Peça"
                        val modelText = if (!currentSelectedProduct.model.isNullOrBlank()) " ${currentSelectedProduct.model}" else ""
                        "$typeName — ${currentSelectedProduct.brand}$modelText"
                    } else {
                        "Escolher produto cadastrado (opcional)"
                    }

                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentProductLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produto / Marca (Catálogo)") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = OrangeNeon)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded)
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
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false },
                            modifier = Modifier
                                .background(SurfaceDark)
                                .heightIn(max = 300.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("(Preenchimento manual / Sem catálogo)", color = Color.Gray) },
                                onClick = {
                                    viewModel.clearSelectedProduct()
                                    productDropdownExpanded = false
                                }
                            )

                            val availableProducts = uiState.partProducts.filter { prod ->
                                val typeName = uiState.partTypes.firstOrNull { it.id == prod.partTypeId }?.name ?: ""
                                productSearchQuery.isBlank() ||
                                        typeName.contains(productSearchQuery, ignoreCase = true) ||
                                        prod.brand.contains(productSearchQuery, ignoreCase = true) ||
                                        (prod.model?.contains(productSearchQuery, ignoreCase = true) == true)
                            }

                            if (availableProducts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum produto encontrado. Clique em '+' para cadastrar.", color = Color.LightGray, fontSize = 12.sp) },
                                    onClick = {
                                        productDropdownExpanded = false
                                        viewModel.openAddProductDialog()
                                    }
                                )
                            } else {
                                availableProducts.forEach { prod ->
                                    val typeName = uiState.partTypes.firstOrNull { it.id == prod.partTypeId }?.name ?: "Peça"
                                    val modelText = if (!prod.model.isNullOrBlank()) " ${prod.model}" else ""
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "$typeName — ${prod.brand}$modelText",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Vida útil padrão: ${prod.defaultLifeKm} KM",
                                                    color = OrangeNeon,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onSelectProduct(prod)
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Botão "+" para abrir o formulário completo de Adicionar Produto
                    IconButton(
                        onClick = { viewModel.openAddProductDialog() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Adicionar Novo Produto",
                            tint = OrangeNeon,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // 2. Nome da Peça / Descrição * (editável)
                OutlinedTextField(
                    value = uiState.partName,
                    onValueChange = { viewModel.onPartNameChanged(it) },
                    label = { Text("Nome da Peça / Descrição do Serviço *") },
                    placeholder = { Text("Ex: Óleo do Motor — Mobil Super 20W50") },
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Marca e Modelo lado a lado (opcionais, auto-preenchidos ou editáveis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.partBrand,
                        onValueChange = { viewModel.onPartBrandChanged(it) },
                        label = { Text("Marca da Peça") },
                        placeholder = { Text("Ex: Mobil, Cobreq") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.partModel,
                        onValueChange = { viewModel.onPartModelChanged(it) },
                        label = { Text("Modelo Peça") },
                        placeholder = { Text("Ex: Super 20W50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Vida Útil em KM * e KM da Última Troca *
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.lastChangeKm,
                        onValueChange = { viewModel.onLastChangeKmChanged(it) },
                        label = { Text("KM Troca*") },
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. Empresa / Oficina (opcional) — Dropdown + Botão "+"
                Text(
                    text = "EMPRESA / OFICINA ONDE FOI REALIZADA",
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
                            shape = RoundedCornerShape(12.dp),
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
                                text = { Text("(Nenhuma empresa vinculada)", color = Color.Gray) },
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

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                // SEÇÃO 2: IMPACTO FINANCEIRO & PAGAMENTO (EXPENSES)
                Text(
                    text = "IMPACTO FINANCEIRO & PAGAMENTO",
                    color = GreenNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                // 6. Valor Total Pago (R$)
                OutlinedTextField(
                    value = uiState.totalAmountText,
                    onValueChange = { viewModel.onTotalAmountChanged(it) },
                    label = { Text("Valor Total Pago (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GreenNeon)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenNeon,
                        focusedLabelColor = GreenNeon,
                        unfocusedBorderColor = Color.DarkGray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = GreenNeon,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Data e Hora da Troca
                val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                OutlinedTextField(
                    value = uiState.lastChangeDateTime.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data e Hora da Troca") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangeNeon)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDateTimePicker(context, uiState.lastChangeDateTime) {
                                viewModel.onLastChangeDateTimeChanged(it)
                            }
                        }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Alterar Data", tint = OrangeNeon)
                        }
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
                        .fillMaxWidth()
                        .clickable {
                            showDateTimePicker(context, uiState.lastChangeDateTime) {
                                viewModel.onLastChangeDateTimeChanged(it)
                            }
                        }
                )

                // 8. Nota Fiscal / Cupom / Recibo (opcional)
                OutlinedTextField(
                    value = uiState.receiptNumber,
                    onValueChange = { viewModel.onReceiptNumberChanged(it) },
                    label = { Text("Nº Nota Fiscal / Cupom / Recibo (opcional)") },
                    placeholder = { Text("Ex: NF-e 123456") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = OrangeNeon)
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

                // 9. Observação (opcional, multilinha)
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Observação / Detalhes do Serviço (opcional)") },
                    placeholder = { Text("Ex: Mão de obra inclusa, garantia de 3 meses, etc.") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = OrangeNeon)
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

                // 10. Forma de Pagamento (Segmented Buttons: PIX / Cartão / Dinheiro)
                Text(
                    text = "FORMA DE PAGAMENTO",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PIX
                    PaymentOptionButton(
                        title = "PIX",
                        icon = Icons.Default.QrCode,
                        isSelected = uiState.paymentMethod == "pix",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onPaymentMethodSelected("pix") }
                    )

                    // Cartão (Abre Modal de Cartão)
                    PaymentOptionButton(
                        title = "Cartão",
                        icon = Icons.Default.CreditCard,
                        isSelected = uiState.paymentMethod == "cartao",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.onPaymentMethodSelected("cartao")
                            showCardModal = true
                        }
                    )

                    // Dinheiro
                    PaymentOptionButton(
                        title = "Dinheiro",
                        icon = Icons.Default.AttachMoney,
                        isSelected = uiState.paymentMethod == "dinheiro",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onPaymentMethodSelected("dinheiro") }
                    )
                }

                // Resumo do Cartão se selecionado
                if (uiState.paymentMethod == "cartao" && uiState.cardPaymentData != null) {
                    val cardData = uiState.cardPaymentData!!
                    Surface(
                        color = SurfaceDarkAlt,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCardModal = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (cardData.isInstallment) "Parcelado: ${cardData.installmentTotal}x" else "Crédito / Débito à Vista",
                                    color = OrangeNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${cardData.cardBrand ?: "Cartão"} • ${cardData.cardOperator ?: ""}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { showCardModal = true }) {
                                Text("Alterar", color = OrangeNeon, fontSize = 12.sp)
                            }
                        }
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
                            text = if (isEditing) "Salvar Alterações" else "Confirmar Lançamento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Modal de Pagamento com Cartão
        if (showCardModal) {
            CardPaymentModal(
                availableCards = uiState.availableCards,
                availableBrands = uiState.availableBrands,
                availableOperators = uiState.availableOperators,
                initialData = uiState.cardPaymentData,
                purchaseDate = uiState.lastChangeDateTime,
                onAddBrand = { viewModel.addCardBrand(it) },
                onAddOperator = { viewModel.addCardOperator(it) },
                onNavigateToManageCards = {},
                onConfirm = { cardData ->
                    viewModel.onCardPaymentConfirmed(cardData)
                    showCardModal = false
                },
                onDismiss = { showCardModal = false }
            )
        }

        // Confirmação de exclusão
        if (showDeleteConfirmDialog && uiState.editingPartId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Peça?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir o monitoramento de '${uiState.partName}' e a despesa associada?", color = Color.LightGray) },
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

    // Modal de Formulário Completo: "Adicionar Produto ao Catálogo"
    if (uiState.isAddProductDialogOpen) {
        var quickTypeDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddProductDialog() },
            title = { Text("Adicionar Produto / Marca", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dropdown de Tipo de Peça + Botão "+"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val selectedTypeName = uiState.partTypes.firstOrNull { it.id == uiState.quickProductTypeId }?.name
                            ?: "Selecione o Tipo"

                        ExposedDropdownMenuBox(
                            expanded = quickTypeDropdownExpanded,
                            onExpandedChange = { quickTypeDropdownExpanded = !quickTypeDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedTypeName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Peça *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quickTypeDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = quickTypeDropdownExpanded,
                                onDismissRequest = { quickTypeDropdownExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                uiState.partTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name, color = Color.White) },
                                        onClick = {
                                            viewModel.onQuickProductTypeChanged(type.id)
                                            quickTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.openAddTypeDialog() },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Novo Tipo",
                                tint = OrangeNeon,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Marca
                    OutlinedTextField(
                        value = uiState.quickProductBrand,
                        onValueChange = { viewModel.onQuickProductBrandChanged(it) },
                        label = { Text("Marca * (ex: Mobil, Cobreq)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Modelo
                    OutlinedTextField(
                        value = uiState.quickProductModel,
                        onValueChange = { viewModel.onQuickProductModelChanged(it) },
                        label = { Text("Modelo (opcional, ex: Super 20W50)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Vida Útil Padrão
                    OutlinedTextField(
                        value = uiState.quickProductLifeKm,
                        onValueChange = { viewModel.onQuickProductLifeKmChanged(it) },
                        label = { Text("Vida Útil Padrão em KM *") },
                        placeholder = { Text("Ex: 5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveQuickProduct() },
                    enabled = uiState.quickProductBrand.isNotBlank() && uiState.quickProductLifeKm.isNotBlank() && uiState.quickProductTypeId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar e Selecionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddProductDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Diálogo Simples de 1 linha: "Novo Tipo de Peça"
    if (uiState.isAddTypeDialogOpen) {
        var quickTypeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeAddTypeDialog() },
            title = { Text("Novo Tipo de Peça", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = quickTypeName,
                    onValueChange = { quickTypeName = it },
                    label = { Text("Nome da Categoria (ex: Óleo do Motor)") },
                    placeholder = { Text("Ex: Pastilha de Freio, Vela de Ignição") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createQuickPartType(quickTypeName) },
                    enabled = quickTypeName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar Tipo")
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addQuickCompany(quickCompanyName) },
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
    product: PartProduct?,
    currentOdometerKm: BigDecimal,
    onEdit: () -> Unit,
    onContactPhone: (String, Boolean) -> Unit,
    onOpenMap: (Company) -> Unit
) {
    val nextDueKm = part.lastChangeKm + part.lifeKm

    // 1. Cálculo de Quilometragem e Progresso de Vida Útil
    val usedKm = if (currentOdometerKm > part.lastChangeKm) {
        currentOdometerKm - part.lastChangeKm
    } else {
        BigDecimal.ZERO
    }

    val progressRatio = if (part.lifeKm > BigDecimal.ZERO) {
        (usedKm.toDouble() / part.lifeKm.toDouble()).coerceAtLeast(0.0)
    } else {
        0.0
    }

    val progressPercent = (progressRatio * 100.0).toInt()
    val progressFraction = progressRatio.toFloat().coerceIn(0f, 1f)

    // 2. Cores da linha e status conforme a regra:
    // - Até 50% -> Verde
    // - 51% até 85% -> Amarelo-Laranjado
    // - Superior a 85% -> Vermelho
    val (statusColor, statusBgColor, statusText) = when {
        progressPercent <= 50 -> Triple(
            Color(0xFF22C55E), // Verde
            Color(0xFF22C55E).copy(alpha = 0.15f),
            "Em dia (${progressPercent}% de uso)"
        )
        progressPercent <= 85 -> Triple(
            Color(0xFFFF9800), // Amarelo-Laranjado
            Color(0xFFFF9800).copy(alpha = 0.15f),
            "Atenção (${progressPercent}% de uso)"
        )
        else -> Triple(
            Color(0xFFEF4444), // Vermelho
            Color(0xFFEF4444).copy(alpha = 0.15f),
            if (progressPercent >= 100) "Vencida (${progressPercent}% de uso)!" else "Troca Iminente (${progressPercent}% de uso)!"
        )
    }

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
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    if (product != null) {
                        val modelText = if (!product.model.isNullOrBlank()) " • ${product.model}" else ""
                        Text(
                            text = "Produto: ${product.brand}$modelText",
                            fontSize = 12.sp,
                            color = OrangeNeon
                        )
                    }

                    Text(
                        text = "Vida útil total: ${part.lifeKm} KM",
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

                if (currentOdometerKm > BigDecimal.ZERO) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ODÔMETRO ATUAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "$currentOdometerKm KM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PRÓXIMA TROCA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = "$nextDueKm KM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // 3. Barra Graduada de Vida Útil (Linha Grossa com Transição de Cores)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDarkAlt.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Linha de Status e Quilometragem Restante
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (currentOdometerKm > BigDecimal.ZERO) {
                        if (currentOdometerKm <= nextDueKm) {
                            val remainingKm = nextDueKm - currentOdometerKm
                            Text(
                                text = "Restam $remainingKm KM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.LightGray
                            )
                        } else {
                            val overdueKm = currentOdometerKm - nextDueKm
                            Text(
                                text = "Vencida há $overdueKm KM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    } else {
                        Text(
                            text = "0% de uso",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // A Linha Grossa de Progresso (Altura 8dp com cantos arredondados)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                            .background(statusColor, RoundedCornerShape(4.dp))
                    )
                }

                // Legenda de Escala da Linha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0% (${part.lastChangeKm} KM)",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "100% (${nextDueKm} KM)",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }

            // Empresa Vinculada + Ações Rápidas
            if (company != null) {
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                                company.number?.takeIf { it.isNotBlank() },
                                company.neighborhood?.takeIf { it.isNotBlank() },
                                company.city?.takeIf { it.isNotBlank() }
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        val hasAddress = !company.street.isNullOrBlank() || !company.cep.isNullOrBlank() || !company.city.isNullOrBlank()
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

@Composable
private fun PaymentOptionButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else SurfaceDarkAlt,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) OrangeNeon else Color.DarkGray
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) OrangeNeon else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) OrangeNeon else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

fun showDateTimePicker(
    context: Context,
    currentDateTime: LocalDateTime,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selected = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                    onDateTimeSelected(selected)
                },
                currentDateTime.hour,
                currentDateTime.minute,
                true
            ).show()
        },
        currentDateTime.year,
        currentDateTime.monthValue - 1,
        currentDateTime.dayOfMonth
    ).show()
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
        company.neighborhood?.takeIf { it.isNotBlank() },
        company.city?.takeIf { it.isNotBlank() },
        company.state?.takeIf { it.isNotBlank() },
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
