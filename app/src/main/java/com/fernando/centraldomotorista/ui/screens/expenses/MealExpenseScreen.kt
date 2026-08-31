package com.fernando.centraldomotorista.ui.screens.expenses

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FieldBackground = Color(0xFF242427)
private val FieldBorder = Color(0xFF333338)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealExpenseScreen(
    viewModel: MealExpenseViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCompanies: () -> Unit = {},
    onNavigateToManageCards: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var companyMenuExpanded by remember { mutableStateOf(false) }
    var companySearchQuery by remember { mutableStateOf("") }
    var showCardModal by remember { mutableStateOf(false) }
    var newCompanyNameInput by remember { mutableStateOf("") }

    // Re-fetch data on screen display / return from child screens
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadInitialData()
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

    val selectedCompany = remember(uiState.companies, uiState.selectedCompanyId) {
        uiState.companies.firstOrNull { it.id == uiState.selectedCompanyId }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(BackgroundDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, OrangeNeon.copy(alpha = 0.6f), CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Voltar",
                            tint = OrangeNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "LANÇAMENTO DE ALIMENTAÇÃO",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 0.8.sp,
                        color = OrangeNeon
                    )
                }

                // Notification Bell with Badge
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificações",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(15.dp)
                            .background(OrangeNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. EMPRESA (alimentado pela tabela Company)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "EMPRESA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(FieldBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp))
                            .clickable { companyMenuExpanded = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedCompany?.name ?: "Ex: Restaurante do Silva",
                            color = if (selectedCompany != null) Color.White else Color.Gray,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = companyMenuExpanded,
                        onDismissRequest = { companyMenuExpanded = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .fillMaxWidth(0.9f)
                    ) {
                        OutlinedTextField(
                            value = companySearchQuery,
                            onValueChange = { companySearchQuery = it },
                            placeholder = { Text("Buscar empresa...", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = OrangeNeon)
                                    Text("+ Nova Empresa", color = OrangeNeon, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                companyMenuExpanded = false
                                viewModel.openAddCompanyDialog()
                            }
                        )

                        val filteredCompanies = if (companySearchQuery.isBlank()) {
                            uiState.companies
                        } else {
                            uiState.companies.filter { it.name.contains(companySearchQuery, ignoreCase = true) }
                        }

                        filteredCompanies.forEach { company ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = company.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        val address = listOfNotNull(company.neighborhood, company.city).joinToString(" - ")
                                        if (address.isNotBlank()) {
                                            Text(text = address, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.onCompanySelected(company.id)
                                    companyMenuExpanded = false
                                }
                            )
                        }

                        if (filteredCompanies.isEmpty() && companySearchQuery.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Nenhuma empresa encontrada com '$companySearchQuery'", color = Color.Gray, fontSize = 12.sp) },
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // 2. O QUE FOI COMPRADO
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "O QUE FOI COMPRADO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    placeholder = { Text("Ex: Almoço, lanche...", color = Color.Gray, fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldBackground,
                        unfocusedContainerColor = FieldBackground,
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = FieldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }

            // 3. VALOR TOTAL PAGO
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "VALOR TOTAL PAGO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    placeholder = { Text("R$ 0,00", color = Color.Gray, fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    visualTransformation = CurrencyVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldBackground,
                        unfocusedContainerColor = FieldBackground,
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = FieldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }

            // 4. DATA E HORA
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DATA E HORA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                val formattedDateTime = remember(uiState.dateTime) {
                    uiState.dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(FieldBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, FieldBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val current = uiState.dateTime
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            viewModel.onDateTimeChanged(
                                                LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                                            )
                                        },
                                        current.hour,
                                        current.minute,
                                        true
                                    ).show()
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedDateTime,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Selecionar Data e Hora",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 5. OBSERVAÇÃO (OPCIONAL)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "OBSERVAÇÃO (OPCIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    placeholder = { Text("Descreva os itens consumidos...", color = Color.Gray, fontSize = 14.sp) },
                    minLines = 3,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldBackground,
                        unfocusedContainerColor = FieldBackground,
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = FieldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            // 6. FORMA DE PAGAMENTO
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FORMA DE PAGAMENTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFC5C5C5)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val methods = listOf(
                        Triple("pix", "PIX", Icons.Default.QrCode),
                        Triple("cartao", "Cartão", Icons.Default.CreditCard),
                        Triple("dinheiro", "Dinheiro", Icons.Default.Payments)
                    )

                    methods.forEach { (key, label, icon) ->
                        val isSelected = uiState.paymentMethod == key

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(FieldBackground, RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) OrangeNeon else FieldBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.onPaymentMethodChanged(key)
                                    if (key == "cartao") {
                                        showCardModal = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) OrangeNeon else Color(0xFF9E9E9E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OrangeNeon else Color(0xFFC0C0C0)
                                )
                            }
                        }
                    }
                }

                if (uiState.paymentMethod == "cartao" && uiState.cardPaymentData != null) {
                    val card = uiState.creditCards.firstOrNull { it.id == uiState.cardPaymentData?.cardId }
                    val cardName = card?.let { "${it.nickname} (•• ${it.lastFour})" } ?: "Cartão selecionado"
                    val parcelasInfo = if (uiState.cardPaymentData?.isInstallment == true) {
                        "(${uiState.cardPaymentData?.installmentTotal}x)"
                    } else "à vista"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkAlt, RoundedCornerShape(8.dp))
                            .clickable { showCardModal = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💳 $cardName $parcelasInfo",
                            color = OrangeNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Alterar",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7. BOTÃO SALVAR DESPESA
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveMealExpense {
                        onNavigateBack()
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.Black,
                    disabledContainerColor = OrangeNeon.copy(alpha = 0.5f),
                    disabledContentColor = Color.DarkGray
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "SALVAR DESPESA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    // Modal de Cartão de Crédito
    if (showCardModal) {
        CardPaymentModal(
            availableCards = uiState.creditCards,
            initialData = uiState.cardPaymentData,
            purchaseDate = uiState.dateTime,
            onNavigateToManageCards = onNavigateToManageCards,
            onConfirm = { cardData ->
                viewModel.onCardPaymentDataChanged(cardData)
                showCardModal = false
            },
            onDismiss = {
                showCardModal = false
            }
        )
    }

    // Dialog para cadastro rápido de nova Empresa
    if (uiState.isAddCompanyDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAddCompanyDialog() },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Nova Empresa",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cadastre o nome do restaurante, lanchonete ou empresa fornecedora:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newCompanyNameInput,
                        onValueChange = { newCompanyNameInput = it },
                        label = { Text("Nome da Empresa") },
                        placeholder = { Text("Ex: Restaurante do Silva") },
                        singleLine = true,
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addQuickCompany(newCompanyNameInput)
                        newCompanyNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddCompanyDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}
