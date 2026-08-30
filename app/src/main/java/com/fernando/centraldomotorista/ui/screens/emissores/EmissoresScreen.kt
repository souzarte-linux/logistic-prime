package com.fernando.centraldomotorista.ui.screens.emissores

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
import androidx.compose.runtime.*
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
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmissoresScreen(
    viewModel: EmissoresViewModel = viewModel(),
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
                        text = "EMISSORES DE CARTÃO",
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
                    IconButton(onClick = { viewModel.loadOperators() }) {
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Emissor")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
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
                    placeholder = { Text("Buscar emissor por nome...", color = Color.Gray, fontSize = 14.sp) },
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

            // 2. Contador de Emissores
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTITUIÇÕES CADASTRADAS",
                        color = Color.Gray,
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
            if (!uiState.isLoading && filteredOperators.isEmpty()) {
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
                                color = Color.White
                            )
                            Text(
                                text = if (uiState.operators.isEmpty())
                                    "Cadastre os bancos e operadoras para vincular aos seus cartões de crédito e débito."
                                else "Tente buscar por outro termo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.operators.isEmpty()) {
                                Text(
                                    text = "SUGESTÕES DE BANCOS:",
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
                                    items(POPULAR_EMISSORES) { bankName ->
                                        SuggestionChip(
                                            onClick = { viewModel.openAddDialog(bankName) },
                                            label = { Text(bankName, fontSize = 12.sp, color = Color.White) },
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

            // 5. Lista de Emissores
            items(filteredOperators, key = { it.id }) { operator ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startEditing(operator) },
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
                        // Ícone do emissor
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Informações do Emissor
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = operator.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Instituição Emissora de Cartões",
                                fontSize = 11.sp,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
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

    // Modal de Formulário (Adicionar / Editar)
    if (uiState.isFormOpen) {
        val isEditing = uiState.editingOperatorId != null
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            containerColor = SurfaceDark,
            contentColor = Color.White
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

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Nome da Instituição (ex: Nubank, Itaú)") },
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

                Button(
                    onClick = { viewModel.saveOperator() },
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
                            text = if (isEditing) "Salvar Alterações" else "Cadastrar Emissor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showDeleteConfirmDialog && uiState.editingOperatorId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Emissor?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir '${uiState.name}'? Cartões existentes não serão apagados.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteOperator(uiState.editingOperatorId!!)
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
