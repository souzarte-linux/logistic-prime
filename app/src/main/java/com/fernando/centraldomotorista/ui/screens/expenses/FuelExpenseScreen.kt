package com.fernando.centraldomotorista.ui.screens.expenses

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import com.fernando.centraldomotorista.ui.utils.KmVisualTransformation
import com.fernando.centraldomotorista.ui.utils.SuffixVisualTransformation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelExpenseScreen(
    viewModel: FuelExpenseViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGasStations: () -> Unit,
    onNavigateToManageCards: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var stationMenuExpanded by remember { mutableStateOf(false) }
    var fuelMenuExpanded by remember { mutableStateOf(false) }
    var showCardModal by remember { mutableStateOf(false) }

    // Re-fetch data on screen display / return from child screens (ON_RESUME)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NOVO ABASTECIMENTO",
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
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Posto de Abastecimento (Dropdown + Botão "+")
                        Text(
                            text = "POSTO DE ABASTECIMENTO",
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
                            val selectedStation = uiState.gasStations.firstOrNull { it.id == uiState.selectedStationId }
                            val stationLabel = selectedStation?.let { "${it.name} (${it.brand})" }
                                ?: if (uiState.gasStations.isEmpty()) "Nenhum posto cadastrado" else "Selecione o Posto"

                            ExposedDropdownMenuBox(
                                expanded = stationMenuExpanded,
                                onExpandedChange = { if (uiState.gasStations.isNotEmpty()) stationMenuExpanded = !stationMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = stationLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = OrangeNeon) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationMenuExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        unfocusedBorderColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = stationMenuExpanded,
                                    onDismissRequest = { stationMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    uiState.gasStations.forEach { station ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(station.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("${station.brand}${station.address?.let { " • $it" } ?: ""}", color = Color.Gray, fontSize = 11.sp)
                                                }
                                            },
                                            onClick = {
                                                viewModel.onStationSelected(station.id)
                                                stationMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            FilledIconButton(
                                onClick = onNavigateToGasStations,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Cadastrar Posto")
                            }
                        }

                        // 2. Tipo de Combustível
                        val currentStation = uiState.gasStations.firstOrNull { it.id == uiState.selectedStationId }
                        val availableFuels = if (currentStation != null && currentStation.fuelTypes.isNotEmpty()) {
                            currentStation.fuelTypes
                        } else {
                            listOf("Gasolina Comum", "Gasolina Aditivada", "Etanol", "GNV", "Diesel")
                        }

                        ExposedDropdownMenuBox(
                            expanded = fuelMenuExpanded,
                            onExpandedChange = { fuelMenuExpanded = !fuelMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedFuelType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Combustível") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = fuelMenuExpanded,
                                onDismissRequest = { fuelMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                availableFuels.forEach { fuel ->
                                    DropdownMenuItem(
                                        text = { Text(fuel, color = Color.White) },
                                        onClick = {
                                            viewModel.onFuelTypeSelected(fuel)
                                            fuelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Preço/Litro e Litros (Lado a lado com Máscaras)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.pricePerLiterText,
                                onValueChange = { viewModel.onPricePerLiterChanged(it) },
                                label = { Text("Preço / Litro") },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                visualTransformation = CurrencyVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                value = uiState.litersText,
                                onValueChange = { viewModel.onLitersChanged(it) },
                                label = { Text("Litros") },
                                placeholder = { Text("0,0") },
                                singleLine = true,
                                visualTransformation = SuffixVisualTransformation(" L"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                        // 4. Odômetro (KM com Máscara)
                        OutlinedTextField(
                            value = uiState.odometerKmText,
                            onValueChange = { viewModel.onOdometerChanged(it) },
                            label = { Text("Odômetro Atual (KM)") },
                            placeholder = { Text("Ex: 125000") },
                            singleLine = true,
                            visualTransformation = KmVisualTransformation(" KM"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 5. Completou o Tanque? (Toggle)
                        Text(
                            text = "COMPLETOU O TANQUE?",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onFullTankChanged(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isFullTank) OrangeNeon else SurfaceDarkAlt,
                                    contentColor = if (uiState.isFullTank) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Sim, tanque cheio", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }

                            Button(
                                onClick = { viewModel.onFullTankChanged(false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.isFullTank) OrangeNeon else SurfaceDarkAlt,
                                    contentColor = if (!uiState.isFullTank) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Não, parcial", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                        }

                        // 6. Valor Total Pago (Calculado ou Editável)
                        OutlinedTextField(
                            value = uiState.totalAmountText,
                            onValueChange = { viewModel.onTotalAmountChanged(it) },
                            label = { Text("Valor Total Pago (R$) *") },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            visualTransformation = CurrencyVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenNeon,
                                focusedLabelColor = GreenNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = GreenNeon,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 7. Data e Hora Picker
                        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        OutlinedTextField(
                            value = uiState.dateTime.format(dateFormatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Data e Hora do Abastecimento") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    showDateTimePicker(context, uiState.dateTime) { newDateTime ->
                                        viewModel.onDateTimeChanged(newDateTime)
                                    }
                                }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar Data", tint = OrangeNeon)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDateTimePicker(context, uiState.dateTime) { newDateTime ->
                                        viewModel.onDateTimeChanged(newDateTime)
                                    }
                                }
                        )

                        // 8. Cupom Fiscal (Opcional)
                        OutlinedTextField(
                            value = uiState.receiptNumber,
                            onValueChange = { viewModel.onReceiptNumberChanged(it) },
                            label = { Text("Nº Cupom Fiscal / Recibo (Opcional)") },
                            placeholder = { Text("Ex: 123456") },
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

                        // 9. Observação (Opcional, multilinha)
                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = { viewModel.onNotesChanged(it) },
                            label = { Text("Observações (Opcional)") },
                            placeholder = { Text("Ex: Calibragem de pneus inclusa, etc.") },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 10. Forma de Pagamento (PIX / Cartão / Dinheiro)
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

                            // Cartão (Abre Modal)
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
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCardModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Cartão Vinculado",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = GreenNeon
                                        )
                                        Text(
                                            text = "${cardData.cardBrand ?: "Bandeira"} • ${cardData.cardOperator ?: "Emissor"} • ${if (cardData.isInstallment) "${cardData.installmentTotal}x" else "À vista"}",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Text("Alterar", color = OrangeNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 11. Botão "Salvar Despesa"
                        Button(
                            onClick = {
                                viewModel.saveFuelExpense {
                                    onNavigateBack()
                                }
                            },
                            enabled = !uiState.isSaving && uiState.totalAmountText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Salvar Abastecimento",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCardModal) {
        CardPaymentModal(
            availableCards = uiState.creditCards,
            initialData = uiState.cardPaymentData,
            purchaseDate = uiState.dateTime,
            onNavigateToManageCards = {
                showCardModal = false
                onNavigateToManageCards()
            },
            onConfirm = { result ->
                viewModel.onCardPaymentDataConfirmed(result)
                showCardModal = false
            },
            onDismiss = { showCardModal = false }
        )
    }
}

@Composable
fun PaymentOptionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) OrangeNeon else SurfaceDarkAlt,
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun showDateTimePicker(
    context: android.content.Context,
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
