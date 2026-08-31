package com.fernando.centraldomotorista.ui.screens.cards

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.ui.screens.bandeiras.BandeirasUiState
import com.fernando.centraldomotorista.ui.screens.bandeiras.BandeirasViewModel
import com.fernando.centraldomotorista.ui.screens.bandeiras.POPULAR_BRANDS
import com.fernando.centraldomotorista.ui.screens.emissores.EmissoresUiState
import com.fernando.centraldomotorista.ui.screens.emissores.EmissoresViewModel
import com.fernando.centraldomotorista.ui.screens.emissores.POPULAR_EMISSORES
import com.fernando.centraldomotorista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardManagementScreen(
    creditCardsViewModel: CreditCardsViewModel = viewModel(),
    emissoresViewModel: EmissoresViewModel = viewModel(),
    bandeirasViewModel: BandeirasViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Meus Cartões", "Emissores", "Bandeiras")

    val cardsUiState by creditCardsViewModel.uiState.collectAsStateWithLifecycle()
    val emissoresUiState by emissoresViewModel.uiState.collectAsStateWithLifecycle()
    val bandeirasUiState by bandeirasViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Feedback do ViewModel de Cartões
    LaunchedEffect(cardsUiState.message) {
        cardsUiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            creditCardsViewModel.clearMessages()
        }
    }
    LaunchedEffect(cardsUiState.error) {
        cardsUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            creditCardsViewModel.clearMessages()
        }
    }

    // Feedback do ViewModel de Emissores
    LaunchedEffect(emissoresUiState.message) {
        emissoresUiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            emissoresViewModel.clearMessages()
            creditCardsViewModel.loadData()
        }
    }
    LaunchedEffect(emissoresUiState.error) {
        emissoresUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            emissoresViewModel.clearMessages()
        }
    }

    // Feedback do ViewModel de Bandeiras
    LaunchedEffect(bandeirasUiState.message) {
        bandeirasUiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            bandeirasViewModel.clearMessages()
            creditCardsViewModel.loadData()
        }
    }
    LaunchedEffect(bandeirasUiState.error) {
        bandeirasUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            bandeirasViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "GERENCIAMENTO DE CARTÕES",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
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
                        IconButton(onClick = {
                            when (selectedTabIndex) {
                                0 -> creditCardsViewModel.loadData()
                                1 -> emissoresViewModel.loadOperators()
                                2 -> bandeirasViewModel.loadBrands()
                            }
                        }) {
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

                // Barra de Abas (TabRow)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = OrangeNeon,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = OrangeNeon,
                            height = 3.dp
                        )
                    },
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.CreditCard
                                            1 -> Icons.AutoMirrored.Filled.ReceiptLong
                                            else -> Icons.Default.Style
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> creditCardsViewModel.openAddDialog()
                        1 -> emissoresViewModel.openAddDialog()
                        2 -> bandeirasViewModel.openAddDialog()
                    }
                },
                containerColor = OrangeNeon,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = when (selectedTabIndex) {
                        0 -> "Adicionar Cartão"
                        1 -> "Adicionar Emissor"
                        else -> "Adicionar Bandeira"
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // ABA 1: MEUS CARTÕES
                    CardsTabContent(
                        uiState = cardsUiState,
                        viewModel = creditCardsViewModel
                    )
                }
                1 -> {
                    // ABA 2: EMISSORES
                    EmissoresTabContent(
                        uiState = emissoresUiState,
                        viewModel = emissoresViewModel
                    )
                }
                2 -> {
                    // ABA 3: BANDEIRAS
                    BandeirasTabContent(
                        uiState = bandeirasUiState,
                        viewModel = bandeirasViewModel
                    )
                }
            }
        }
    }

    // Modal de Cartões
    if (cardsUiState.isAddDialogOpen) {
        AddCreditCardDialog(
            uiState = cardsUiState,
            viewModel = creditCardsViewModel,
            onDismiss = { creditCardsViewModel.closeAddDialog() },
            onSave = { creditCardsViewModel.saveCard() }
        )
    }

    // Modal de Emissores
    if (emissoresUiState.isFormOpen) {
        val isEditing = emissoresUiState.editingOperatorId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { emissoresViewModel.closeForm() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Editar Emissor" else "Novo Emissor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = RedAlert
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = emissoresUiState.name,
                    onValueChange = { emissoresViewModel.onNameChanged(it) },
                    label = { Text("Nome da Instituição Emissora") },
                    placeholder = { Text("Ex: Nubank, Itaú, Banco Inter...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (emissoresUiState.error != null) {
                    Text(
                        text = emissoresUiState.error ?: "",
                        color = RedAlert,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { emissoresViewModel.saveOperator() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !emissoresUiState.isSaving && emissoresUiState.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (emissoresUiState.isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Emissor",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Emissor?", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Essa ação removerá o emissor cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            emissoresUiState.editingOperatorId?.let { emissoresViewModel.deleteOperator(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                    ) {
                        Text("Excluir", color = Color.White)
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

    // Modal de Bandeiras
    if (bandeirasUiState.isFormOpen) {
        val isEditing = bandeirasUiState.editingBrandId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { bandeirasViewModel.closeForm() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Editar Bandeira" else "Nova Bandeira",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = RedAlert
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bandeirasUiState.name,
                    onValueChange = { bandeirasViewModel.onNameChanged(it) },
                    label = { Text("Nome da Bandeira") },
                    placeholder = { Text("Ex: Mastercard, Visa, Elo...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (bandeirasUiState.error != null) {
                    Text(
                        text = bandeirasUiState.error ?: "",
                        color = RedAlert,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { bandeirasViewModel.saveBrand() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !bandeirasUiState.isSaving && bandeirasUiState.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (bandeirasUiState.isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Bandeira",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Bandeira?", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Essa ação removerá a bandeira cadastrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            bandeirasUiState.editingBrandId?.let { bandeirasViewModel.deleteBrand(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                    ) {
                        Text("Excluir", color = Color.White)
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
}

/**
 * Conteúdo da Aba Meus Cartões
 */
@Composable
private fun CardsTabContent(
    uiState: CreditCardsUiState,
    viewModel: CreditCardsViewModel
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangeNeon)
        }
        return
    }

    if (uiState.cards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Nenhum cartão cadastrado.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Cadastre seus cartões para controlar vencimentos e parcelamentos de combustível e manutenção.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.openAddDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cadastrar Primeiro Cartão", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CARTÕES CADASTRADOS (${uiState.cards.size})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        items(uiState.cards, key = { it.id }) { card ->
            val brand = uiState.brands.firstOrNull { it.id == card.brandId }?.name ?: "Bandeira"
            val operator = uiState.operators.firstOrNull { it.id == card.issuerId }?.name ?: "Emissor"
            CreditCardItem(
                card = card,
                brandName = brand,
                operatorName = operator,
                onEdit = { viewModel.openEditDialog(card) },
                onToggleActive = { active -> viewModel.toggleCardActive(card, active) },
                onDelete = { viewModel.deleteCard(card.id) }
            )
        }
    }
}

/**
 * Conteúdo da Aba Emissores
 */
@Composable
private fun EmissoresTabContent(
    uiState: EmissoresUiState,
    viewModel: EmissoresViewModel
) {
    val filteredOperators = remember(uiState.operators, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.operators
        } else {
            uiState.operators.filter {
                it.name.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Barra de Busca
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Buscar emissor por nome...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeNeon,
                    focusedLabelColor = OrangeNeon
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTITUIÇÕES CADASTRADAS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${filteredOperators.size} encontrado(s)",
                    color = OrangeNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

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
        } else if (filteredOperators.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = if (uiState.operators.isEmpty()) "Nenhum emissor cadastrado" else "Nenhum resultado encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.operators.isEmpty())
                                "Cadastre os bancos e operadoras para vincular aos seus cartões."
                            else "Tente buscar por outro termo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.operators.isEmpty()) {
                            Text(
                                text = "SUGESTÕES DE BANCOS:",
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
                                items(POPULAR_EMISSORES) { bankName ->
                                    SuggestionChip(
                                        onClick = { viewModel.openAddDialog(bankName) },
                                        label = { Text(bankName, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredOperators, key = { it.id }) { operator ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startEditing(operator) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, OrangeNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = operator.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Instituição Emissora de Cartões",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Editar",
                            tint = OrangeNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Conteúdo da Aba Bandeiras
 */
@Composable
private fun BandeirasTabContent(
    uiState: BandeirasUiState,
    viewModel: BandeirasViewModel
) {
    val filteredBrands = remember(uiState.brands, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.brands
        } else {
            uiState.brands.filter {
                it.name.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Barra de Busca
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Buscar bandeira por nome...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeNeon,
                    focusedLabelColor = OrangeNeon
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BANDEIRAS CADASTRADAS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${filteredBrands.size} encontrada(s)",
                    color = OrangeNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

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
        } else if (filteredBrands.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = if (uiState.brands.isEmpty()) "Nenhuma bandeira cadastrada" else "Nenhum resultado encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.brands.isEmpty())
                                "Cadastre as bandeiras de cartão (Visa, Mastercard, Elo, etc.) para vincular aos seus cartões."
                            else "Tente buscar por outro termo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.brands.isEmpty()) {
                            Text(
                                text = "SUGESTÕES DE BANDEIRAS:",
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
                                items(POPULAR_BRANDS) { brandName ->
                                    SuggestionChip(
                                        onClick = { viewModel.openAddDialog(brandName) },
                                        label = { Text(brandName, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredBrands, key = { it.id }) { brand ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startEditing(brand) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OrangeNeon.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, OrangeNeon.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = brand.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bandeira de Cartão",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Editar",
                            tint = OrangeNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
