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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onNavigateToLancarManutencao: () -> Unit,
    onNavigateToPartProducts: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                onClick = {
                    viewModel.openAddDialog()
                    onNavigateToLancarManutencao()
                },
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
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Banner com Hodômetro Atual do Veículo
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "HODÔMETRO ATUAL",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${uiState.currentOdometerKm.toPlainString()} KM",
                                color = OrangeNeon,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 3. Indicador de Carregamento
            if (uiState.isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = OrangeNeon,
                        trackColor = SurfaceDark
                    )
                }
            }

            // 4. Estado Vazio / Sugestões
            if (!uiState.isLoading && filteredParts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (uiState.searchQuery.isBlank()) "Nenhuma peça em monitoramento." else "Nenhuma peça encontrada.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Acompanhe a vida útil das peças para saber exatamente quando realizar a próxima troca preventiva.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.searchQuery.isBlank()) {
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
                                            onClick = {
                                                viewModel.openAddDialog(partName)
                                                onNavigateToLancarManutencao()
                                            },
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
                    onEdit = {
                        viewModel.startEditing(part)
                        onNavigateToLancarManutencao()
                    },
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
