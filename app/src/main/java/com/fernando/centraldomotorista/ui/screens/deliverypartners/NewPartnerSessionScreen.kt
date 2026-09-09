package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.BarcodeScannerScreen
import com.fernando.centraldomotorista.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPartnerSessionScreen(
    partnerId: String,
    viewModel: NewPartnerSessionViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSessionCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(partnerId) {
        viewModel.loadData(partnerId)
    }

    LaunchedEffect(uiState.sessionCreated) {
        if (uiState.sessionCreated) {
            Toast.makeText(context, "Sessão iniciada com sucesso!", Toast.LENGTH_SHORT).show()
            onSessionCreated()
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
            scannedCount = uiState.scannedCount,
            expectedCount = uiState.expectedPackageCount,
            scannedBarcodes = uiState.scannedBarcodes,
            onBarcodeScanned = { code -> viewModel.onBarcodeScanned(code) },
            onCloseScanner = { viewModel.closeScanner() }
        )
        return
    }

    // Divergence Confirmation Dialog
    if (uiState.showDivergenceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDivergenceDialog() },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Divergência na Bipagem", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    text = "Você expediu ${uiState.expectedPackageCount} pacote(s) mas bipou ${uiState.scannedCount}.\n\nDeseja continuar mesmo assim ou revisar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSaveAnyway() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Continuar Mesmo Assim", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDivergenceDialog() }) {
                    Text("Revisar Bipagem")
                }
            }
        )
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var routeDropdownExpanded by remember { mutableStateOf(false) }

    val selectedRouteName = remember(uiState.selectedRouteId, uiState.routes) {
        uiState.routes.firstOrNull { it.id == uiState.selectedRouteId }?.name ?: "Selecione uma rota..."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NOVA SESSÃO DE ENTREGA",
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
                        onClick = { viewModel.onSaveClick() },
                        enabled = !uiState.isSaving && uiState.partner != null,
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
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Salvar e Iniciar Sessão",
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
                // 1. Card do Entregador Parceiro (Preenchido / Readonly)
                item {
                    val partner = uiState.partner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ENTREGADOR PARCEIRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = OrangeNeon
                                )
                                Text(
                                    text = partner?.fullName ?: "Carregando...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (partner != null) {
                                    Text(
                                        text = "Taxa: R$ ${String.format(Locale("pt", "BR"), "%.2f", partner.packageRate)}/pct | Bônus: R$ ${String.format(Locale("pt", "BR"), "%.2f", partner.defaultBonus)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Rota Realizada (Dropdown editável alimentado por delivery_routes)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ROTA REALIZADA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { routeDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon)
                                        Text(
                                            text = selectedRouteName,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            DropdownMenu(
                                expanded = routeDropdownExpanded,
                                onDismissRequest = { routeDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                uiState.routes.forEach { route ->
                                    DropdownMenuItem(
                                        text = { Text(route.name, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon) },
                                        onClick = {
                                            viewModel.onRouteSelected(route.id)
                                            routeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Quantidade de Pacotes Expedidos & Hora de Início
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pacotes Expedidos
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PACOTES EXPEDIDOS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.expectedPackageCountText,
                                onValueChange = { viewModel.onExpectedPackageCountChanged(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = OrangeNeon)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Hora de Início
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "HORA DE INÍCIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val curTime = uiState.startTime
                                        TimePickerDialog(
                                            context,
                                            { _, hour, min ->
                                                viewModel.onStartTimeChanged(hour, min)
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
                                        .padding(horizontal = 14.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = OrangeNeon)
                                    Text(
                                        text = uiState.startTime.format(timeFormatter),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Seção "Bipagem de Pacotes"
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (uiState.hasDivergence) RedAlert.copy(alpha = 0.5f) else OrangeNeon.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BIPAGEM DE PACOTES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = OrangeNeon
                                )

                                if (uiState.hasDivergence && uiState.expectedPackageCount > 0) {
                                    Surface(
                                        color = RedAlert.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Divergência",
                                            color = RedAlert,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Placar Grande: "X de Y pacotes bipados"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${uiState.scannedCount}",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (!uiState.hasDivergence && uiState.scannedCount > 0) GreenNeon else OrangeNeon
                                    )
                                    Text(
                                        text = " de ${uiState.expectedPackageCount} pacotes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 6.dp)
                                    )
                                }
                                Text(
                                    text = "bipados até o momento",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Botão para abrir câmera contínua
                            Button(
                                onClick = { viewModel.openScanner() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangeNeon,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.scannedCount == 0) "Abrir Câmera para Bipar" else "Continuar Bipagem (${uiState.scannedCount})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            // Lista com os códigos já bipados
                            if (uiState.scannedBarcodes.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Códigos Bipados (${uiState.scannedCount}):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(uiState.scannedBarcodes.toList()) { code ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = code,
                                                        fontSize = 11.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remover",
                                                        tint = RedAlert,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable { viewModel.removeBarcode(code) }
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
    }
}
