package com.fernando.centraldomotorista.ui.screens.empresas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CepVisualTransformation
import com.fernando.centraldomotorista.ui.utils.CnpjVisualTransformation
import com.fernando.centraldomotorista.ui.utils.PhoneVisualTransformation

private val WhatsAppGreen = Color(0xFF25D366)

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

    when (uiState.mode) {
        EmpresaScreenMode.LIST -> {
            EmpresasListView(
                uiState = uiState,
                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                onRefresh = { viewModel.loadCompanies() },
                onAddNew = { viewModel.openCreateForm() },
                onSelectSuggestion = { viewModel.openCreateForm(it) },
                onSelectCompany = { viewModel.openViewDetails(it) },
                onNavigateBack = onNavigateBack
            )
        }
        EmpresaScreenMode.VIEW -> {
            uiState.selectedCompany?.let { company ->
                CompanyDetailsView(
                    company = company,
                    onEdit = { viewModel.openEditFormFromDetails() },
                    onDelete = { viewModel.deleteCompany(company.id) },
                    onNavigateBack = { viewModel.navigateBackFromView() }
                )
            } ?: run {
                viewModel.navigateBackFromView()
            }
        }
        EmpresaScreenMode.FORM -> {
            CompanyFormView(
                uiState = uiState,
                onNameChanged = { viewModel.onNameChanged(it) },
                onCepChanged = { viewModel.onCepChanged(it) },
                onStreetChanged = { viewModel.onStreetChanged(it) },
                onNumberChanged = { viewModel.onNumberChanged(it) },
                onComplementChanged = { viewModel.onComplementChanged(it) },
                onNeighborhoodChanged = { viewModel.onNeighborhoodChanged(it) },
                onCityChanged = { viewModel.onCityChanged(it) },
                onStateChanged = { viewModel.onStateChanged(it) },
                onCnpjChanged = { viewModel.onCnpjChanged(it) },
                onPhoneChanged = { viewModel.onPhoneChanged(it) },
                onIsWhatsappChanged = { viewModel.onIsWhatsappChanged(it) },
                onSocialMediaChanged = { viewModel.onSocialMediaChanged(it) },
                onWebsiteChanged = { viewModel.onWebsiteChanged(it) },
                onSave = { viewModel.saveCompany() },
                onCancel = { viewModel.navigateBackFromForm() },
                onConfirmDiscard = { viewModel.navigateBackFromForm(force = true) },
                onDismissDiscard = { viewModel.dismissDiscardAlert() }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 1. TELA DE LISTAGEM DE EMPRESAS
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmpresasListView(
    uiState: EmpresasUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddNew: () -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onSelectCompany: (Company) -> Unit,
    onNavigateBack: () -> Unit
) {
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
                    IconButton(onClick = onRefresh) {
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
                onClick = onAddNew,
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
                            (it.street?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
                            (it.neighborhood?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
                            (it.city?.contains(uiState.searchQuery, ignoreCase = true) == true)
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
            // Barra de Busca
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Buscar por nome, bairro, cidade...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChanged("") }) {
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

            // Contador de Empresas
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
                                            onClick = { onSelectSuggestion(companyName) },
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

            // Lista de Empresas
            items(filteredCompanies, key = { it.id }) { company ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectCompany(company) },
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
                                .size(46.dp)
                                .background(
                                    color = OrangeNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = OrangeNeon.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
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

                        // Informações
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

                            // Endereço e Cidade (se houver)
                            val addressStreet = listOfNotNull(
                                company.street?.takeIf { it.isNotBlank() },
                                company.number?.takeIf { it.isNotBlank() }?.let { "nº $it" }
                            ).joinToString(", ")

                            val cityState = listOfNotNull(
                                company.neighborhood?.takeIf { it.isNotBlank() },
                                company.city?.takeIf { it.isNotBlank() }?.let { city ->
                                    if (!company.state.isNullOrBlank()) "$city - ${company.state}" else city
                                }
                            ).joinToString(" • ")

                            val fullDisplayAddress = listOfNotNull(
                                addressStreet.takeIf { it.isNotBlank() },
                                cityState.takeIf { it.isNotBlank() }
                            ).joinToString(" - ")

                            if (fullDisplayAddress.isNotBlank()) {
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
                                        text = fullDisplayAddress,
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
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver Detalhes",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. TELA DE VISUALIZAÇÃO (MODO DETALHES - READ-ONLY)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyDetailsView(
    company: Company,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DETALHES DA EMPRESA",
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
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Empresa",
                            tint = OrangeNeon
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Empresa",
                            tint = RedAlert
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cartão de Apresentação da Empresa
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .border(1.dp, OrangeNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = company.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Empresa / Prestadora Parceira",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Seção 1: Localização & Endereço
            val hasAddress = !company.street.isNullOrBlank() || !company.cep.isNullOrBlank() || !company.number.isNullOrBlank() || !company.city.isNullOrBlank()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "LOCALIZAÇÃO & ENDEREÇO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )
                    }

                    if (hasAddress) {
                        if (!company.street.isNullOrBlank()) {
                            DetailItem(label = "Endereço / Rua", value = company.street)
                        }
                        if (!company.number.isNullOrBlank()) {
                            DetailItem(label = "Número", value = company.number)
                        }
                        if (!company.complement.isNullOrBlank()) {
                            DetailItem(label = "Complemento", value = company.complement)
                        }
                        if (!company.neighborhood.isNullOrBlank()) {
                            DetailItem(label = "Bairro", value = company.neighborhood)
                        }
                        if (!company.city.isNullOrBlank() || !company.state.isNullOrBlank()) {
                            val cityStateText = listOfNotNull(
                                company.city?.takeIf { it.isNotBlank() },
                                company.state?.takeIf { it.isNotBlank() }
                            ).joinToString(" - ")
                            DetailItem(label = "Cidade / UF", value = cityStateText)
                        }
                        if (!company.cep.isNullOrBlank()) {
                            DetailItem(label = "CEP", value = EmpresasViewModel.formatCep(company.cep))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { openCompanyAddress(context, company) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceDarkAlt,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir no Google Maps", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Nenhum endereço cadastrado.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Seção 2: Contato & Atendimento
            val hasContact = !company.phone.isNullOrBlank() || !company.socialMedia.isNullOrBlank() || !company.website.isNullOrBlank()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "CONTATO & REDES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )
                    }

                    if (hasContact) {
                        if (!company.phone.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Telefone / Celular", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = EmpresasViewModel.formatPhone(company.phone),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                if (company.isWhatsapp) {
                                    Surface(
                                        color = WhatsAppGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(13.dp))
                                            Text("WhatsApp", color = WhatsAppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { openCompanyContact(context, company.phone, company.isWhatsapp) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (company.isWhatsapp) WhatsAppGreen else OrangeNeon,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (company.isWhatsapp) Icons.Default.ChatBubble else Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (company.isWhatsapp) "Conversar no WhatsApp" else "Ligar para Empresa",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!company.socialMedia.isNullOrBlank()) {
                            DetailItem(label = "Rede Social", value = company.socialMedia)
                        }

                        if (!company.website.isNullOrBlank()) {
                            DetailItem(label = "Site Oficial", value = company.website)
                            OutlinedButton(
                                onClick = { openBrowserUrl(context, company.website) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Acessar Site no Navegador")
                            }
                        }
                    } else {
                        Text("Nenhum contato cadastrado.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Seção 3: Identificação Fiscal (CNPJ)
            if (!company.cnpj.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                            Text(
                                text = "DADOS CADASTRAIS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = OrangeNeon
                            )
                        }
                        DetailItem(label = "CNPJ", value = EmpresasViewModel.formatCnpj(company.cnpj))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Inferior de Editar Empresa
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Dados da Empresa", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Empresa?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir '${company.name}'? O histórico de manutenções anteriores não será apagado.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
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

// -----------------------------------------------------------------------------
// 3. TELA DE FORMULÁRIO (CADASTRO / EDIÇÃO COMPLETA)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyFormView(
    uiState: EmpresasUiState,
    onNameChanged: (String) -> Unit,
    onCepChanged: (String) -> Unit,
    onStreetChanged: (String) -> Unit,
    onNumberChanged: (String) -> Unit,
    onComplementChanged: (String) -> Unit,
    onNeighborhoodChanged: (String) -> Unit,
    onCityChanged: (String) -> Unit,
    onStateChanged: (String) -> Unit,
    onCnpjChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onIsWhatsappChanged: (Boolean) -> Unit,
    onSocialMediaChanged: (String) -> Unit,
    onWebsiteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit
) {
    val isEditing = uiState.isEditing

    BackHandler {
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDITAR EMPRESA" else "NOVA EMPRESA",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Seção 1: Informações Principais
            Text(
                text = "DADOS DA EMPRESA",
                color = OrangeNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 1. Nome da Empresa * (obrigatório)
            OutlinedTextField(
                value = uiState.formData.name,
                onValueChange = onNameChanged,
                label = { Text("Nome da Empresa *") },
                placeholder = { Text("Ex: Dinho Motos, Posto Shell, Auto Peças...") },
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

            // 2. CNPJ (opcional, máscara 00.000.000/0001-00)
            OutlinedTextField(
                value = uiState.formData.cnpj,
                onValueChange = onCnpjChanged,
                label = { Text("CNPJ (opcional)") },
                placeholder = { Text("00.000.000/0001-00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CnpjVisualTransformation(),
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Seção 2: Localização & Endereço
            Text(
                text = "LOCALIZAÇÃO & ENDEREÇO",
                color = OrangeNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            // 3. CEP (opcional — preenchimento automático ViaCEP)
            OutlinedTextField(
                value = uiState.formData.cep,
                onValueChange = onCepChanged,
                label = { Text("CEP (opcional - busca automática)") },
                placeholder = { Text("00000-000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CepVisualTransformation(),
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Endereço e Número lado a lado (0.68f / 0.32f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.formData.street,
                    onValueChange = onStreetChanged,
                    label = { Text("Endereço / Logradouro") },
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.68f)
                )

                OutlinedTextField(
                    value = uiState.formData.number,
                    onValueChange = onNumberChanged,
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.32f)
                )
            }

            // 5. Complemento (opcional, até 500 caracteres, multilinha com contador)
            OutlinedTextField(
                value = uiState.formData.complement,
                onValueChange = onComplementChanged,
                label = { Text("Complemento (opcional)") },
                placeholder = { Text("Ex: Galpão B, Próximo ao viaduto...") },
                minLines = 2,
                maxLines = 4,
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = OrangeNeon)
                },
                supportingText = {
                    Text(
                        text = "${uiState.formData.complement.length}/500",
                        color = if (uiState.formData.complement.length >= 500) RedAlert else Color.Gray,
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Bairro
            OutlinedTextField(
                value = uiState.formData.neighborhood,
                onValueChange = onNeighborhoodChanged,
                label = { Text("Bairro") },
                placeholder = { Text("Ex: Centro, Pinheiros, Vila Nova...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.HomeWork, contentDescription = null, tint = OrangeNeon)
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

            // 7. Cidade e Estado lado a lado (0.72f / 0.28f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.formData.city,
                    onValueChange = onCityChanged,
                    label = { Text("Cidade") },
                    placeholder = { Text("Ex: São Paulo, Campinas...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = OrangeNeon)
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
                    modifier = Modifier.weight(0.72f)
                )

                OutlinedTextField(
                    value = uiState.formData.state,
                    onValueChange = onStateChanged,
                    label = { Text("UF") },
                    placeholder = { Text("SP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    leadingIcon = {
                        Icon(Icons.Default.Map, contentDescription = null, tint = OrangeNeon)
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
                    modifier = Modifier.weight(0.28f)
                )
            }

            // Seção 3: Contato & Canais
            Text(
                text = "CONTATO & REDES",
                color = OrangeNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            // 8. Contato/Celular + Checkbox WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.formData.phone,
                    onValueChange = onPhoneChanged,
                    label = { Text("Contato / Celular") },
                    placeholder = { Text("(00) 00000-0000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = PhoneVisualTransformation(),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                // Checkbox WhatsApp
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onIsWhatsappChanged(!uiState.formData.isWhatsapp) }
                        .padding(top = 6.dp),
                    color = if (uiState.formData.isWhatsapp) WhatsAppGreen.copy(alpha = 0.15f) else SurfaceDarkAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.formData.isWhatsapp) WhatsAppGreen else Color.DarkGray
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "WhatsApp",
                            tint = if (uiState.formData.isWhatsapp) WhatsAppGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "WhatsApp",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.formData.isWhatsapp) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.formData.isWhatsapp) WhatsAppGreen else Color.LightGray
                        )
                        Checkbox(
                            checked = uiState.formData.isWhatsapp,
                            onCheckedChange = { onIsWhatsappChanged(it) },
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

            // 9. Rede Social (opcional)
            OutlinedTextField(
                value = uiState.formData.socialMedia,
                onValueChange = onSocialMediaChanged,
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 10. Site (opcional)
            OutlinedTextField(
                value = uiState.formData.website,
                onValueChange = onWebsiteChanged,
                label = { Text("Site Oficial (opcional)") },
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botões de Ação no final da tela: Salvar e Cancelar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving && uiState.formData.name.isNotBlank(),
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
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Empresa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Alerta de Descarte de Alterações
    if (uiState.showDiscardAlert) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = {
                Text(
                    text = "Descartar alterações?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Você possui dados alterados que ainda não foram salvos. Deseja realmente sair e perder as modificações?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDiscard,
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Descartar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) {
                    Text("Continuar Editando", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// -----------------------------------------------------------------------------
// Componentes Auxiliares
// -----------------------------------------------------------------------------
@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
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

private fun openBrowserUrl(context: Context, url: String) {
    try {
        val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
    }
}
