package com.fernando.centraldomotorista.ui.screens.pecas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.ui.screens.expenses.CardPaymentModal
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val GreenNeon = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancarManutencaoScreen(
    viewModel: PartMaintenanceViewModel = viewModel(),
    onNavigateToManageCards: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEditing = uiState.editingPartId != null
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var companyDropdownExpanded by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var showCardModal by remember { mutableStateOf(false) }

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
                        text = if (isEditing) "EDITAR MANUTENÇÃO" else "LANÇAR MANUTENÇÃO",
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
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Manutenção",
                                tint = RedAlert
                            )
                        }
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SEÇÃO 1: PEÇA & VIDA ÚTIL
            Text(
                text = "DADOS DA PEÇA & TROCA",
                color = OrangeNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )

            // 1. Seletor de Produto do Catálogo (opcional) + Botão "+"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selectedProduct = uiState.partProducts.firstOrNull { it.id == uiState.selectedPartProductId }
                val selectedProductName = if (selectedProduct != null) {
                    val typeName = uiState.partTypes.firstOrNull { it.id == selectedProduct.partTypeId }?.name ?: "Peça"
                    "$typeName — ${selectedProduct.brand}${if (!selectedProduct.model.isNullOrBlank()) " ${selectedProduct.model}" else ""}"
                } else {
                    "Selecionar do Catálogo (opcional)"
                }

                ExposedDropdownMenuBox(
                    expanded = productDropdownExpanded,
                    onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedProductName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produto do Catálogo") },
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
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = { productSearchQuery = it },
                            placeholder = { Text("Buscar peça ou produto...", fontSize = 12.sp) },
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
                            text = { Text("(Nenhum / Digitação Manual)", color = Color.Gray) },
                            onClick = {
                                viewModel.clearSelectedProduct()
                                productDropdownExpanded = false
                            }
                        )

                        val filteredProds = if (productSearchQuery.isBlank()) {
                            uiState.partProducts
                        } else {
                            uiState.partProducts.filter { p ->
                                val tName = uiState.partTypes.firstOrNull { it.id == p.partTypeId }?.name ?: ""
                                p.brand.contains(productSearchQuery, ignoreCase = true) ||
                                        (p.model?.contains(productSearchQuery, ignoreCase = true) == true) ||
                                        tName.contains(productSearchQuery, ignoreCase = true)
                            }
                        }

                        filteredProds.forEach { product ->
                            val typeName = uiState.partTypes.firstOrNull { it.id == product.partTypeId }?.name ?: "Peça"
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "$typeName — ${product.brand}${if (!product.model.isNullOrBlank()) " ${product.model}" else ""}",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Vida útil padrão: ${product.defaultLifeKm} KM",
                                            color = OrangeNeon,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onSelectProduct(product)
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Botão "+" para cadastro rápido de novo produto
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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

            // 3. Marca e Modelo lado a lado
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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

            // 4. Vida Útil em KM * e KM da Troca *
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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
                        showDateTimePicker(context, uiState.lastChangeDateTime) { dt ->
                            viewModel.onLastChangeDateTimeChanged(dt)
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
                        showDateTimePicker(context, uiState.lastChangeDateTime) { dt ->
                            viewModel.onLastChangeDateTimeChanged(dt)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
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
                PaymentOptionButton(
                    title = "PIX",
                    icon = Icons.Default.QrCode,
                    isSelected = uiState.paymentMethod == "pix",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onPaymentMethodSelected("pix") }
                )
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
                PaymentOptionButton(
                    title = "Dinheiro",
                    icon = Icons.Default.Payments,
                    isSelected = uiState.paymentMethod == "dinheiro",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onPaymentMethodSelected("dinheiro") }
                )
            }

            // Resumo do Cartão Selecionado se a forma de pagamento for "cartao"
            if (uiState.paymentMethod == "cartao" && uiState.cardPaymentData != null) {
                val cardData = uiState.cardPaymentData!!
                val cardObj = uiState.availableCards.firstOrNull { it.id == cardData.cardId }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDarkAlt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCardModal = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "💳 ${cardObj?.nickname ?: cardData.cardBrand ?: "Cartão de Crédito"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val parcelasText = if (cardData.isInstallment) "${cardData.installmentTotal}x parcelado" else "À vista"
                            Text(
                                text = "$parcelasText • Venc: Dia ${cardData.cardDueDay}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Alterar",
                            color = OrangeNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BOTÕES DE AÇÃO INFERIORES: Salvar e Cancelar
            Button(
                onClick = {
                    viewModel.savePartMaintenance {
                        onNavigateBack()
                    }
                },
                enabled = !uiState.isSaving,
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
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "SALVAR ALTERAÇÕES" else "SALVAR MANUTENÇÃO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("CANCELAR", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal de Pagamento com Cartão
    if (showCardModal) {
        CardPaymentModal(
            availableCards = uiState.availableCards,
            initialData = uiState.cardPaymentData,
            purchaseDate = uiState.lastChangeDateTime,
            onNavigateToManageCards = {
                showCardModal = false
                onNavigateToManageCards()
            },
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
            title = { Text("Excluir Monitoramento", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir o monitoramento de '${uiState.partName}' e a despesa associada?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePartMaintenance(uiState.editingPartId!!) {
                            onNavigateBack()
                        }
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

    // Modal de Formulário Rápido: "Adicionar Produto ao Catálogo"
    if (uiState.isAddProductDialogOpen) {
        var quickTypeDropdownExpanded by remember { mutableStateOf(false) }
        val dialogFocusManager = LocalFocusManager.current
        val dialogKeyboardController = LocalSoftwareKeyboardController.current

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { dialogFocusManager.moveFocus(FocusDirection.Down) }),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { dialogFocusManager.moveFocus(FocusDirection.Down) }),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            dialogKeyboardController?.hide()
                            dialogFocusManager.clearFocus()
                        }),
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
        val dialogKeyboardController = LocalSoftwareKeyboardController.current
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dialogKeyboardController?.hide() }),
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
        val dialogKeyboardController = LocalSoftwareKeyboardController.current
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { dialogKeyboardController?.hide() }),
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
private fun PaymentOptionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OrangeNeon else SurfaceDarkAlt,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private fun showDateTimePicker(
    context: Context,
    initialDateTime: LocalDateTime,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    val year = initialDateTime.year
    val month = initialDateTime.monthValue - 1
    val day = initialDateTime.dayOfMonth
    val hour = initialDateTime.hour
    val minute = initialDateTime.minute

    DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            val result = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
            onDateTimeSelected(result)
        }, hour, minute, true).show()
    }, year, month, day).show()
}

