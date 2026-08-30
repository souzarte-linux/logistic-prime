package com.fernando.centraldomotorista.ui.screens.empresas

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
import com.fernando.centraldomotorista.ui.theme.*

private val WhatsAppGreen = Color(0xFF25D366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpresasScreen(
    viewModel: EmpresasViewModel = viewModel(),
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
                        text = "EMPRESAS & PRESTADORAS",
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
                    IconButton(onClick = { viewModel.loadCompanies() }) {
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Empresa")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        val filteredCompanies = remember(uiState.companies, uiState.searchQuery) {
            if (uiState.searchQuery.isBlank()) {
                uiState.companies
            } else {
                uiState.companies.filter {
                    it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            (it.cnpj?.contains(uiState.searchQuery) == true) ||
                            (it.street?.contains(uiState.searchQuery, ignoreCase = true) == true)
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
                    placeholder = { Text("Buscar empresa por nome ou endereço...", color = Color.Gray, fontSize = 14.sp) },
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

            // 2. Contador de Empresas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EMPRESAS CADASTRADAS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${filteredCompanies.size} cadastrada(s)",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
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
            if (!uiState.isLoading && filteredCompanies.isEmpty()) {
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
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (uiState.companies.isEmpty()) "Nenhuma empresa cadastrada" else "Nenhum resultado encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.companies.isEmpty())
                                    "Cadastre as oficinas, auto peças, parceiros e prestadoras de serviço onde você realiza manutenções ou serviços."
                                else "Tente buscar por outro nome ou endereço.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.companies.isEmpty()) {
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
                                    items(POPULAR_COMPANIES) { companyName ->
                                        SuggestionChip(
                                            onClick = { viewModel.openAddDialog(companyName) },
                                            label = { Text(companyName, fontSize = 12.sp, color = Color.White) },
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

            // 5. Lista de Empresas
            items(filteredCompanies, key = { it.id }) { company ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startEditing(company) },
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
                        // Ícone da empresa
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = OrangeNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = OrangeNeon.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Informações da Empresa
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = company.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Endereço (se houver)
                            val address = listOfNotNull(
                                company.street?.takeIf { it.isNotBlank() },
                                company.number?.takeIf { it.isNotBlank() }?.let { "nº $it" }
                            ).joinToString(", ")

                            if (address.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = address,
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Telefone e WhatsApp (se houver)
                            if (!company.phone.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (company.isWhatsapp) {
                                        Surface(
                                            color = WhatsAppGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ChatBubble,
                                                    contentDescription = "WhatsApp",
                                                    tint = WhatsAppGreen,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "WhatsApp",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WhatsAppGreen
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = EmpresasViewModel.formatPhone(company.phone),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // Seta indicativa
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Editar",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal de Formulário Completo (Adicionar / Editar Empresa)
    if (uiState.isFormOpen) {
        val isEditing = uiState.editingCompanyId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                // Cabeçalho do Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "EDITAR EMPRESA" else "CADASTRAR NOVA EMPRESA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Preencha os dados da empresa ou prestadora",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Empresa", tint = RedAlert)
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                // 1. Nome da Empresa * (obrigatório)
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Nome da Empresa *") },
                    placeholder = { Text("Ex: Dinho Motos, Posto Shell, Auto Peças Silva") },
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
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. CEP (opcional — preenchimento automático ViaCEP)
                OutlinedTextField(
                    value = uiState.cep,
                    onValueChange = { viewModel.onCepChanged(it) },
                    label = { Text("CEP (opcional - busca automática)") },
                    placeholder = { Text("00000-000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeNeon)
                    },
                    trailingIcon = {
                        if (uiState.isSearchingCep) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OrangeNeon,
                                strokeWidth = 2.dp
                            )
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
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Endereço e Número lado a lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Endereço (maior)
                    OutlinedTextField(
                        value = uiState.street,
                        onValueChange = { viewModel.onStreetChanged(it) },
                        label = { Text("Endereço / Rua") },
                        placeholder = { Text("Rua, Av...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = OrangeNeon)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(0.68f)
                    )

                    // Número (menor)
                    OutlinedTextField(
                        value = uiState.number,
                        onValueChange = { viewModel.onNumberChanged(it) },
                        label = { Text("Número") },
                        placeholder = { Text("123") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = OrangeNeon)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(0.32f)
                    )
                }

                // 4. Complemento (opcional, até 500 caracteres, multilinha com contador 0/500)
                OutlinedTextField(
                    value = uiState.complement,
                    onValueChange = { viewModel.onComplementChanged(it) },
                    label = { Text("Complemento (opcional)") },
                    placeholder = { Text("Ex: Galpão B, Próximo ao viaduto...") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = OrangeNeon)
                    },
                    supportingText = {
                        Text(
                            text = "${uiState.complement.length}/500",
                            color = if (uiState.complement.length >= 500) RedAlert else Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
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

                // 5. CNPJ (opcional, máscara 00.000.000/0001-00)
                OutlinedTextField(
                    value = uiState.cnpj,
                    onValueChange = { viewModel.onCnpjChanged(it) },
                    label = { Text("CNPJ (opcional)") },
                    placeholder = { Text("00.000.000/0001-00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = OrangeNeon)
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

                // 6. Contato/Celular + Checkbox WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label = { Text("Contato / Celular") },
                        placeholder = { Text("(00) 00000-0000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = OrangeNeon)
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

                    // Checkbox WhatsApp com ícone verde
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.onIsWhatsappChanged(!uiState.isWhatsapp) }
                            .padding(top = 6.dp),
                        color = if (uiState.isWhatsapp) WhatsAppGreen.copy(alpha = 0.15f) else SurfaceDarkAlt,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.isWhatsapp) WhatsAppGreen else Color.DarkGray
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "WhatsApp",
                                tint = if (uiState.isWhatsapp) WhatsAppGreen else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = if (uiState.isWhatsapp) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.isWhatsapp) WhatsAppGreen else Color.LightGray
                            )
                            Checkbox(
                                checked = uiState.isWhatsapp,
                                onCheckedChange = { viewModel.onIsWhatsappChanged(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = WhatsAppGreen,
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 7. Rede Social (opcional)
                OutlinedTextField(
                    value = uiState.socialMedia,
                    onValueChange = { viewModel.onSocialMediaChanged(it) },
                    label = { Text("Rede Social (opcional)") },
                    placeholder = { Text("Ex: @suaempresa, instagram.com/...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = OrangeNeon)
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

                // 8. Site (opcional)
                OutlinedTextField(
                    value = uiState.website,
                    onValueChange = { viewModel.onWebsiteChanged(it) },
                    label = { Text("Site (opcional)") },
                    placeholder = { Text("https://www.empresa.com.br") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, tint = OrangeNeon)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Botão Salvar
                Button(
                    onClick = { viewModel.saveCompany() },
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
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Empresa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Diálogo de confirmação de exclusão
        if (showDeleteConfirmDialog && uiState.editingCompanyId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Empresa?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir '${uiState.name}'? Peças e manutenções já vinculadas manterão o histórico.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteCompany(uiState.editingCompanyId!!)
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
}
