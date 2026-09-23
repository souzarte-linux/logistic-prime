package com.fernando.centraldomotorista.ui.screens.deliverypartners

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
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.BarcodeScannerScreen
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosePartnerSessionScreen(
    sessionId: String,
    viewModel: ClosePartnerSessionViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSessionClosed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }
    var isEditingStartTime by remember { mutableStateOf(false) }
    var barcodeToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        viewModel.loadData(sessionId)
    }

    LaunchedEffect(uiState.sessionFinalized) {
        if (uiState.sessionFinalized) {
            Toast.makeText(context, "Sessão finalizada e pagamento lançado com sucesso!", Toast.LENGTH_LONG).show()
            onSessionClosed()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Intercept back handler when scanner is open
    BackHandler(enabled = uiState.isScannerOpen) {
        viewModel.closeScanner()
    }

    // If scanner is open, display full-screen CameraX + ML Kit scanner
    if (uiState.isScannerOpen) {
        BarcodeScannerScreen(
            scannedCount = uiState.returnedBarcodes.size,
            expectedCount = uiState.basePackageCount,
            scannedBarcodes = uiState.returnedBarcodes,
            onBarcodeScanned = { code -> viewModel.onReturnedBarcodeScanned(code) },
            onRemoveBarcode = { code -> viewModel.removeReturnedBarcode(code) },
            onCloseScanner = { viewModel.closeScanner() }
        )
        return
    }

    val session = uiState.session
    val partner = uiState.partner
    val route = uiState.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FECHAR SESSÃO E PAGAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.finalizeAndPay() },
                        enabled = !uiState.isFinalizing && session != null && partner != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (uiState.isFinalizing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finalizar e Pagar (R$ ${String.format(Locale("pt", "BR"), "%.2f", uiState.amountPaid)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeNeon)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // 1. Resumo da Sessão em Andamento
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = partner?.fullName ?: "Entregador",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = route?.name ?: "Sem Rota",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = OrangeNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "EM ANDAMENTO",
                                        color = OrangeNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Início da Sessão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val formattedStartTime = remember(uiState.startTime) {
                                        uiState.startTime.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatter)
                                    }
                                    Text(
                                        text = formattedStartTime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pacotes Expedidos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session?.expectedPackageCount ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pacotes Bipados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session?.scannedCount ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OrangeNeon
                                    )
                                }
                            }
                        }
                    }
                }

                // 1.1 Data e Hora de Início (Visualização inicial com Edição de Data e Hora)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATA E HORA DE INÍCIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isEditingStartTime) {
                                TextButton(
                                    onClick = { isEditingStartTime = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val startZoned = uiState.startTime.atZoneSameInstant(ZoneId.systemDefault())

                        if (!isEditingStartTime) {
                            // Modo Visualização Apenas
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isEditingStartTime = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangeNeon)
                                        Text(
                                            text = "${startZoned.format(dateFormatter)} às ${startZoned.format(timeFormatter)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text("Alterar", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            // Modo Edição
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, OrangeNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Toque nos campos abaixo para alterar a data ou a hora de início:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Botão Seletor de Data
                                        OutlinedCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, dayOfMonth ->
                                                            viewModel.onStartDateChanged(year, month + 1, dayOfMonth)
                                                        },
                                                        startZoned.year,
                                                        startZoned.monthValue - 1,
                                                        startZoned.dayOfMonth
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                                Text(startZoned.format(dateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Botão Seletor de Hora
                                        OutlinedCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, hour, min ->
                                                            viewModel.onStartTimeChanged(hour, min)
                                                        },
                                                        startZoned.hour,
                                                        startZoned.minute,
                                                        true
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                                Text(startZoned.format(timeFormatter), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { isEditingStartTime = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Concluir Edição", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Hora de Fim
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "HORA DE TÉRMINO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val curTime = uiState.endTime.atZoneSameInstant(ZoneId.systemDefault())
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            viewModel.onEndTimeChanged(hour, min)
                                        },
                                        curTime.hour,
                                        curTime.minute,
                                        true
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = OrangeNeon)
                                    Text(
                                        text = uiState.endTime.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatter),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("Alterar", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 3. Pacotes Entregues e Pacotes Devolvidos
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pacotes Entregues
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PACOTES ENTREGUES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.deliveredCountText,
                                onValueChange = { viewModel.onDeliveredCountChanged(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GreenNeon)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Pacotes Devolvidos
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PACOTES DEVOLVIDOS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.returnedCountText,
                                onValueChange = { viewModel.onReturnedCountChanged(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null, tint = RedAlert)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.openScanner() }) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Bipar Pacotes Devolvidos",
                                            tint = OrangeNeon
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedAlert,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3.1 Seção Consolidada "ITENS BIPADOS" (Entregues em verde, Devolvidos com traço vermelho)
                item {
                    val allScannedItems = remember(session?.scannedBarcodes, uiState.returnedBarcodes) {
                        val allCodes = (session?.scannedBarcodes.orEmpty() + uiState.returnedBarcodes).distinct()
                        // Organizado em ordem de sucesso de entrega (0) para insucesso (1)
                        allCodes.sortedWith(
                            compareBy<String> { code -> if (uiState.returnedBarcodes.contains(code)) 1 else 0 }
                                .thenBy { it }
                        )
                    }
                    val deliveredInListCount = allScannedItems.count { !uiState.returnedBarcodes.contains(it) }
                    val returnedInListCount = allScannedItems.count { uiState.returnedBarcodes.contains(it) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cabeçalho da Seção
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "ITENS BIPADOS (${allScannedItems.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = GreenNeon.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$deliveredInListCount Entregues",
                                            color = GreenNeon,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (returnedInListCount > 0) {
                                        Surface(
                                            color = RedAlert.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$returnedInListCount Devolvidos",
                                                color = RedAlert,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Botão para abrir câmera / bipar pacotes devolvidos
                            Button(
                                onClick = { viewModel.openScanner() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangeNeon,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (returnedInListCount > 0) "Bipar Mais Devolvidos ($returnedInListCount)" else "Bipar Pacotes Devolvidos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Botões de Ação: "Copiar Todos" e "Exportar Imagem"
                            if (allScannedItems.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val allCodesText = allScannedItems.joinToString("\n")
                                            clipboardManager.setText(AnnotatedString(allCodesText))
                                            Toast.makeText(context, "${allScannedItems.size} códigos copiados!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = OrangeNeon)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiar Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Button(
                                        onClick = {
                                            val dateStr = uiState.startTime.atZoneSameInstant(ZoneId.systemDefault()).format(dateFormatter)
                                            SessionShareHelper.shareScannedBarcodesImage(
                                                context = context,
                                                partnerName = partner?.fullName ?: "Entregador",
                                                routeName = route?.name ?: "Sem Rota",
                                                sessionDateStr = dateStr,
                                                items = allScannedItems.map { code ->
                                                    ScannedItemExport(
                                                        barcode = code,
                                                        isReturned = uiState.returnedBarcodes.contains(code)
                                                    )
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = OrangeNeon.copy(alpha = 0.2f),
                                            contentColor = OrangeNeon
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp), tint = OrangeNeon)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Exportar Imagem", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Listagem de Itens
                            if (allScannedItems.isEmpty()) {
                                Text(
                                    text = "Nenhum pacote bipado registrado nesta sessão.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    allScannedItems.forEach { code ->
                                        val isReturned = uiState.returnedBarcodes.contains(code)
                                        Surface(
                                            color = if (isReturned) RedAlert.copy(alpha = 0.08f) else GreenNeon.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isReturned) RedAlert.copy(alpha = 0.35f) else GreenNeon.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isReturned) Icons.AutoMirrored.Filled.KeyboardReturn else Icons.Default.CheckCircle,
                                                        contentDescription = if (isReturned) "Devolvido" else "Entregue",
                                                        tint = if (isReturned) RedAlert else GreenNeon,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = code,
                                                        fontSize = 13.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isReturned) RedAlert else GreenNeon,
                                                        style = TextStyle(
                                                            textDecoration = if (isReturned) TextDecoration.LineThrough else null
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Badge de Status
                                                    Surface(
                                                        color = if (isReturned) RedAlert.copy(alpha = 0.18f) else GreenNeon.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isReturned) "DEVOLVIDO" else "ENTREGUE",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isReturned) RedAlert else GreenNeon,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    // Copiar registro individual
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(AnnotatedString(code))
                                                            Toast.makeText(context, "Código copiado: $code", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copiar Código",
                                                            tint = if (isReturned) RedAlert else GreenNeon,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }

                                                    // Desfazer devolução se marcado como devolvido
                                                    if (isReturned) {
                                                        IconButton(
                                                            onClick = { barcodeToDelete = code },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Desfazer Devolução",
                                                                tint = RedAlert,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Alerta de Divergência se Entregues + Devolvidos != ScannedCount
                if (uiState.hasDivergence) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RedAlert.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Atenção: Contagem Divergente",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RedAlert
                                    )
                                    Text(
                                        text = "Total informado (${uiState.totalAccounted}) ≠ Expedidos (${uiState.basePackageCount}). Entregues + Devolvidos deve ser igual aos pacotes expedidos.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Cálculo Automático do Valor a Pagar
                item {
                    val rate = if ((session?.packageRate ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) session!!.packageRate else (partner?.packageRate ?: java.math.BigDecimal.ZERO)
                    val bonus = if ((session?.defaultBonus ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) session!!.defaultBonus else (partner?.defaultBonus ?: java.math.BigDecimal.ZERO)
                    val delivered = uiState.deliveredCount
                    val subtotal = java.math.BigDecimal(delivered).multiply(rate)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GreenNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CÁLCULO DO PAGAMENTO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = GreenNeon
                                )
                                Text(
                                    text = "Automático",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Detalhamento do cálculo
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$delivered pct × R$ ${String.format(Locale("pt", "BR"), "%.2f", rate)}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", subtotal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (bonus > java.math.BigDecimal.ZERO) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "+ Bônus padrão da rota",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", bonus)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GreenNeon
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sugestão Calculada:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", uiState.suggestedAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GreenNeon
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Campo editável: Valor Total a Pagar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VALOR TOTAL A PAGAR (EDITÁVEL)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = uiState.amountPaidText,
                                    onValueChange = { input ->
                                        val clean = cleanCurrencyInput(input)
                                        val decimal = parseCurrency(clean)
                                        viewModel.onAmountPaidChanged(clean, decimal)
                                    },
                                    prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = GreenNeon) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = CurrencyVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Este valor será lançado automaticamente como despesa na categoria 'equipe' e abaterá o Lucro Líquido de hoje.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmação de Exclusão de Código Devolvido
    if (barcodeToDelete != null) {
        val codeTarget = barcodeToDelete!!
        AlertDialog(
            onDismissRequest = { barcodeToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Excluir Código Bipado", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text("Deseja realmente remover o código \"$codeTarget\" da lista de pacotes devolvidos?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeReturnedBarcode(codeTarget)
                        barcodeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { barcodeToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
