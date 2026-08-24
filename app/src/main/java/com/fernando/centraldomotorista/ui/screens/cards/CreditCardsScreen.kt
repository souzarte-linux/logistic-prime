package com.fernando.centraldomotorista.ui.screens.cards

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.ui.theme.*

val CARD_TYPES = listOf(
    "credito" to "Crédito",
    "debito" to "Débito",
    "multiplo" to "Múltiplo",
    "voucher" to "Voucher"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    viewModel: CreditCardsViewModel = viewModel(),
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
                        text = "CARTÕES DE CRÉDITO",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = OrangeNeon,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cartão")
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEUS CARTÕES (${uiState.cards.size})",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    TextButton(onClick = { viewModel.loadData() }) {
                        Text("Atualizar", color = OrangeNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.cards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
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
                                text = if (uiState.isLoading) "Carregando cartões..." else "Nenhum cartão cadastrado.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Cadastre seus cartões para controlar vencimentos e parcelamentos de combustível e manutenção.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            } else {
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
    }

    if (uiState.isAddDialogOpen) {
        AddCreditCardDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.closeAddDialog() },
            onSave = { viewModel.saveCard() }
        )
    }
}

@Composable
fun CreditCardItem(
    card: CreditCard,
    brandName: String,
    operatorName: String,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (card.active) SurfaceDark else SurfaceDark.copy(alpha = 0.5f)),
        border = if (card.active) androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = if (card.active) OrangeNeon else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = card.nickname,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (card.active) Color.White else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = OrangeNeon.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "$brandName • $operatorName",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Badge Tipo
                val typeLabel = CARD_TYPES.firstOrNull { it.first == card.cardType }?.second ?: card.cardType.replaceFirstChar { it.uppercase() }
                Surface(
                    color = if (card.active) GreenNeon.copy(alpha = 0.15f) else Color.DarkGray,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = typeLabel.uppercase(),
                        color = if (card.active) GreenNeon else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Número mascarado
            val firstFourDisplay = card.firstFour?.let { "$it " } ?: "•••• "
            Text(
                text = "$firstFourDisplay•••• •••• ${card.lastFour}",
                color = Color.LightGray,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )

            // Vencimento e Fechamento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("FECHAMENTO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Dia ${card.closingDay}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("VENCIMENTO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Dia ${card.dueDay}", fontSize = 13.sp, color = OrangeNeon, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TITULAR", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(card.holderName.uppercase(), fontSize = 12.sp, color = Color.LightGray, maxLines = 1)
                }
            }

            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f))

            // Switch Ativo / Inativo e Botão Excluir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = card.active,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrangeNeon,
                            checkedTrackColor = OrangeNeon.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = SurfaceDarkAlt
                        )
                    )
                    Text(
                        text = if (card.active) "Cartão Ativo" else "Cartão Inativo",
                        fontSize = 12.sp,
                        color = if (card.active) GreenNeon else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = RedAlert)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Cartão", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover o cartão \"${card.nickname}\"?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditCardDialog(
    uiState: CreditCardsUiState,
    viewModel: CreditCardsViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showNewBrandDialog by remember { mutableStateOf(false) }
    var showNewOperatorDialog by remember { mutableStateOf(false) }
    var brandMenuExpanded by remember { mutableStateOf(false) }
    var issuerMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (uiState.editingCardId != null) "Editar Cartão de Crédito" else "Novo Cartão de Crédito",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Apelido
                    OutlinedTextField(
                        value = uiState.nickname,
                        onValueChange = { viewModel.onNicknameChanged(it) },
                        label = { Text("Apelido do Cartão *") },
                        placeholder = { Text("Ex: Nubank Principal") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Nome do Titular
                    OutlinedTextField(
                        value = uiState.holderName,
                        onValueChange = { viewModel.onHolderNameChanged(it) },
                        label = { Text("Nome do Titular (como no cartão) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // 4 Primeiros e 4 Últimos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.firstFour,
                            onValueChange = { viewModel.onFirstFourChanged(it) },
                            label = { Text("4 Primeiros") },
                            placeholder = { Text("1234") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.lastFour,
                            onValueChange = { viewModel.onLastFourChanged(it) },
                            label = { Text("4 Últimos *") },
                            placeholder = { Text("5678") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Bandeira com botão "+"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val selectedBrand = uiState.brands.firstOrNull { it.id == uiState.selectedBrandId }?.name ?: "Selecione a Bandeira"
                        ExposedDropdownMenuBox(
                            expanded = brandMenuExpanded,
                            onExpandedChange = { brandMenuExpanded = !brandMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedBrand,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bandeira") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = brandMenuExpanded,
                                onDismissRequest = { brandMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                uiState.brands.forEach { brand ->
                                    DropdownMenuItem(
                                        text = { Text(brand.name, color = Color.White) },
                                        onClick = {
                                            viewModel.onBrandSelected(brand.id)
                                            brandMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showNewBrandDialog = true },
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Nova Bandeira", tint = OrangeNeon)
                        }
                    }
                }

                item {
                    // Emissor com botão "+"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val selectedIssuer = uiState.operators.firstOrNull { it.id == uiState.selectedIssuerId }?.name ?: "Selecione o Emissor"
                        ExposedDropdownMenuBox(
                            expanded = issuerMenuExpanded,
                            onExpandedChange = { issuerMenuExpanded = !issuerMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedIssuer,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Emissor / Banco") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issuerMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = issuerMenuExpanded,
                                onDismissRequest = { issuerMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                uiState.operators.forEach { op ->
                                    DropdownMenuItem(
                                        text = { Text(op.name, color = Color.White) },
                                        onClick = {
                                            viewModel.onIssuerSelected(op.id)
                                            issuerMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showNewOperatorDialog = true },
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Novo Emissor", tint = OrangeNeon)
                        }
                    }
                }

                item {
                    // Tipo do Cartão (Crédito, Débito, Múltiplo, Voucher)
                    val selectedTypeLabel = CARD_TYPES.firstOrNull { it.first == uiState.cardType }?.second ?: "Crédito"
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTypeLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo do Cartão") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            CARD_TYPES.forEach { (typeKey, typeName) ->
                                DropdownMenuItem(
                                    text = { Text(typeName, color = Color.White) },
                                    onClick = {
                                        viewModel.onCardTypeChanged(typeKey)
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Vencimento e Fechamento
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.closingDay,
                            onValueChange = { viewModel.onClosingDayChanged(it) },
                            label = { Text("Dia Fechamento") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.dueDay,
                            onValueChange = { viewModel.onDueDayChanged(it) },
                            label = { Text("Dia Vencimento") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text(
                        text = if (uiState.editingCardId != null) "Salvar Alterações" else "Salvar Cartão",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = SurfaceDark
    )

    // Dialog Simples para Nova Bandeira
    if (showNewBrandDialog) {
        var brandName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBrandDialog = false },
            title = { Text("Cadastrar Nova Bandeira", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Nome da Bandeira (ex: Elo, Hipercard)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBrand(brandName)
                        showNewBrandDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBrandDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog Simples para Novo Emissor
    if (showNewOperatorDialog) {
        var operatorName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewOperatorDialog = false },
            title = { Text("Cadastrar Novo Emissor", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = operatorName,
                    onValueChange = { operatorName = it },
                    label = { Text("Nome do Emissor / Banco (ex: Inter, Itaú)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addOperator(operatorName)
                        showNewOperatorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewOperatorDialog = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = SurfaceDark
        )
    }
}
