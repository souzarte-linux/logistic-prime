package com.fernando.centraldomotorista.ui.screens.routes.master

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.Marketplace
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.screens.routes.master.components.DualScannerOverlay
import com.fernando.centraldomotorista.ui.screens.routes.master.components.EditStopDialog
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold
import com.fernando.centraldomotorista.util.BrazilianLabelParser
import com.fernando.centraldomotorista.util.ParsedAddress
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * Tela de Scanner de Câmera contínuo com leitura dupla e extensões ADR-003:
 * - Mira Superior: Barcode Scanning (ML Kit)
 * - Mira Inferior: Text Recognition OCR (ML Kit)
 * - Indicador sticky de plataforma ativa
 * - Seletor de Pacotinho vs Volumoso
 * - Gate de confiança no auto-avanço
 * - Atribuição cruzada a parceiros
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun RouteScannerScreen(
    routeId: String,
    viewModel: RouteScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCockpit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPlatformSelectorDialog by remember { mutableStateOf(false) }
    var showMarketplaceSelectorDialog by remember { mutableStateOf(false) }
    var stopToEdit by remember { mutableStateOf<MasterRouteStop?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(routeId) {
        viewModel.initRoute(routeId)
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedState(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
    }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                barcodeScanner.close()
                textRecognizer.close()
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    if (showPlatformSelectorDialog) {
        PlatformSelectorDialog(
            platforms = uiState.platforms,
            selectedPlatformId = uiState.currentPlatformId,
            onSelect = { pId ->
                viewModel.onPlatformSelected(pId)
                showPlatformSelectorDialog = false
            },
            onDismiss = { showPlatformSelectorDialog = false }
        )
    }

    if (showMarketplaceSelectorDialog) {
        MarketplaceSelectorDialog(
            marketplaces = uiState.marketplaces,
            currentMarketplaceName = uiState.currentMarketplaceName,
            onSelect = { marketplaceName ->
                viewModel.onMarketplaceSelected(marketplaceName)
                showMarketplaceSelectorDialog = false
            },
            onDismiss = { showMarketplaceSelectorDialog = false }
        )
    }

    if (stopToEdit != null) {
        val stop = stopToEdit!!
        EditStopDialog(
            stop = stop,
            platforms = uiState.platforms,
            marketplaces = uiState.marketplaces,
            onDismiss = { stopToEdit = null },
            onSave = { name, addr, cep, pkgType, platId, mktName, notes ->
                viewModel.updateScannedStop(
                    stop = stop,
                    recipientName = name,
                    fullAddress = addr,
                    cep = cep,
                    packageType = pkgType,
                    platformId = platId,
                    marketplaceName = mktName,
                    notes = notes,
                    onSuccess = {
                        stopToEdit = null
                        Toast.makeText(context, "Pacote #${stop.stopOrder} atualizado!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- 1. CameraX PreviewView ---
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null || viewModel.isAnalyzingFrame.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                        viewModel.isAnalyzingFrame.set(true)

                        val currentStep = viewModel.uiState.value.currentStep

                        if (currentStep == ScannerStep.BARCODE_SEARCH) {
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val detectedBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                                    if (detectedBarcode != null) {
                                        if (viewModel.isBarcodeAlreadyScanned(detectedBarcode)) {
                                            viewModel.onDuplicateBarcodeDetected(ctx, detectedBarcode)
                                        } else {
                                            viewModel.onBarcodeDetected(ctx, detectedBarcode)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    viewModel.isAnalyzingFrame.set(false)
                                    imageProxy.close()
                                }
                        } else {
                            // ScannerStep.OCR_CONFIRMATION: Foco contínuo no OCR da etiqueta
                            textRecognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    val parsed = BrazilianLabelParser.parse(visionText.text)
                                    if (!parsed.cep.isNullOrBlank() || !parsed.recipientName.isNullOrBlank() || parsed.street != null) {
                                        viewModel.onOcrAddressDetected(parsed)
                                    }
                                }
                                .addOnCompleteListener {
                                    viewModel.isAnalyzingFrame.set(false)
                                    imageProxy.close()
                                }
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera: Camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        Log.e("RouteScannerScreen", "Falha ao vincular CameraX: ${exc.message}", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Overlay do Retículo Fracionado com Laser Animado (TASK-DES-08) ---
        DualScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            currentStep = uiState.currentStep,
            isProcessing = uiState.isSaving
        )

        // --- 3. Barra Superior Minimalista (Visor 100% Desobstruído) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }

            // Contador de Pacotes Bipados
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val counterText = if (uiState.currentStep == ScannerStep.OCR_CONFIRMATION) {
                        "📦 Pacote #${uiState.totalScannedCount + 1}"
                    } else {
                        "📦 ${uiState.totalScannedCount} Bipados"
                    }
                    Text(
                        text = counterText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botão Lanterna / Flash
            IconButton(
                onClick = { viewModel.toggleTorch(cameraControl) },
                modifier = Modifier
                    .background(
                        if (uiState.isTorchOn) OrangeNeon else Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Lanterna",
                    tint = if (uiState.isTorchOn) Color.Black else Color.White
                )
            }
        }

        // --- 4. Alerta de Pacote Duplicado ---
        AnimatedVisibility(
            visible = uiState.duplicateAlertBarcode != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            Surface(
                color = RedAlert,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PACOTE JÁ BIPADO NESTA ROTA!",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 5. Etapa 2: Card Inferior Deslizante de OCR & Confirmação (TASK-DES-08) ---
        AnimatedVisibility(
            visible = uiState.currentStep == ScannerStep.OCR_CONFIRMATION && uiState.pendingBarcode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OcrConfirmationCard(
                barcode = uiState.pendingBarcode ?: "",
                parsedAddress = uiState.pendingParsedAddress,
                isOcrScanning = uiState.isOcrScanning,
                activePlatform = uiState.activePlatform,
                currentMarketplaceName = uiState.currentMarketplaceName,
                currentPackageType = uiState.currentPackageType,
                selectedPartnerId = uiState.selectedPartnerIdForNextScan,
                partners = uiState.deliveryPartners,
                onChangePlatform = { showPlatformSelectorDialog = true },
                onChangeMarketplace = { showMarketplaceSelectorDialog = true },
                onSelectPackageType = { viewModel.onPackageTypeSelected(it) },
                onSelectPartner = { viewModel.onAssignedPartnerSelected(it) },
                onConfirmPackage = { viewModel.confirmPendingPackage(context) },
                onSkipOcr = { viewModel.skipOcrAndConfirm(context) },
                onRetryBarcode = { viewModel.retryScanningCurrentPackage() },
                isSaving = uiState.isSaving
            )
        }

        // --- 6. Etapa 1: Botão de Finalizar Carga e Ir para Cockpit (Fixo no Rodapé se não houver mini-card ativo) ---
        if (uiState.currentStep == ScannerStep.BARCODE_SEARCH && uiState.lastScannedStop == null) {
            Button(
                onClick = { onNavigateToCockpit(routeId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark.copy(alpha = 0.92f),
                    contentColor = OrangeNeon
                ),
                border = BorderStroke(1.dp, OrangeNeon),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IR PARA COCKPIT (${uiState.totalScannedCount} PACOTES)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // --- 7. Etapa 1: Mini-Card Inferior com Dados do Pacote Bipado + Extensões dos Prompts 3 e 6 ---
        AnimatedVisibility(
            visible = uiState.currentStep == ScannerStep.BARCODE_SEARCH && uiState.lastScannedStop != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            uiState.lastScannedStop?.let { stop ->
                ScannedPackageMiniCard(
                    stop = stop,
                    countdown = uiState.autoAdvanceCountdown,
                    isConfidenceWarning = uiState.isConfidenceWarning,
                    partners = uiState.deliveryPartners,
                    onDismiss = { viewModel.dismissLastScannedCard() },
                    onFinishToCockpit = { onNavigateToCockpit(routeId) },
                    onAttachPhoto = { photoUri -> viewModel.attachPhotoToLastStop(photoUri) },
                    onEditClick = {
                        viewModel.cancelAutoAdvance()
                        stopToEdit = stop
                    }
                )
            }
        }
    }
}

/**
 * Diálogo para troca da plataforma ativa sticky (Prompt 2).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformSelectorDialog(
    platforms: List<Platform>,
    selectedPlatformId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Selecionar Plataforma Ativa",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Os próximos pacotes bipados serão associados a esta plataforma até você trocá-la novamente.",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    platforms.forEach { platform ->
                        val isSelected = platform.id == selectedPlatformId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelect(platform.id) },
                            label = { Text(platform.name, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = OrangeNeon
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                                selectedLabelColor = OrangeNeon,
                                containerColor = SurfaceDarkAlt,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * Diálogo para seleção do Tomador / Marketplace ativo sticky.
 * Permite selecionar entre os tomadores conhecidos (TikTok Shop, Kwai, Mercado Livre, Shopee, C&A, Riachuelo, Shein, Amazon, Magalu, Loja Virtual)
 * ou cadastrar/digitar um novo tomador sob demanda.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketplaceSelectorDialog(
    marketplaces: List<Marketplace>,
    currentMarketplaceName: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🏬 Selecionar Tomador / Origem",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Defina o e-commerce ou cliente gerador da carga. Os próximos pacotes bipados carregarão esta origem até nova alteração.",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                // Campo para digitar tomador customizado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Outro marketplace...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                onSelect(customName.trim())
                            }
                        },
                        enabled = customName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Usar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Principais Tomadores:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryDark
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    marketplaces.forEach { mkt ->
                        val isSelected = mkt.name.equals(currentMarketplaceName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelect(mkt.name) },
                            label = { Text(mkt.name, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = OrangeNeon
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                                selectedLabelColor = OrangeNeon,
                                containerColor = SurfaceDarkAlt,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * Mini-card inferior com feedback de confirmação imediata do pacote lido
 * e suporte a:
 * - Edição completa sem fechar via EditStopDialog (Prompt 4)
 * - Foto de backup da etiqueta
 * - Atribuição a parceiro
 */
@Composable
private fun ScannedPackageMiniCard(
    stop: MasterRouteStop,
    countdown: Int?,
    isConfidenceWarning: Boolean,
    partners: List<DeliveryPartner>,
    onDismiss: () -> Unit,
    onFinishToCockpit: () -> Unit,
    onAttachPhoto: (String) -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.96f)),
        border = BorderStroke(1.5.dp, if (isConfidenceWarning) YellowGold else GreenNeon),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isConfidenceWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isConfidenceWarning) YellowGold else GreenNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConfidenceWarning) "LEITURA REQUER REVISÃO" else "PACOTE #${stop.stopOrder} BIPADO!",
                        color = if (isConfidenceWarning) YellowGold else GreenNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Pacotinho / Volumoso
                    Surface(
                        color = if (stop.packageType == PackageType.VOLUMOSO) YellowGold.copy(alpha = 0.2f) else SurfaceDarkAlt,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (stop.packageType == PackageType.VOLUMOSO) "🏋️ Volumoso" else "📦 Pacotinho",
                            color = if (stop.packageType == PackageType.VOLUMOSO) YellowGold else TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Badge Tomador / Marketplace (se houver)
                    if (!stop.marketplaceName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = OrangeNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🏬 ${stop.marketplaceName}",
                                color = OrangeNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = SurfaceDarkAlt,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stop.barcode.takeLast(10),
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exibição dos Dados Capturados
            if (!stop.recipientName.isNullOrBlank()) {
                Text(
                    text = "👤 ${stop.recipientName.uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = stop.fullAddress.ifBlank { "Endereço incompleto no OCR (toque em Editar)" },
                fontSize = 12.sp,
                color = if (stop.fullAddress.isBlank()) YellowGold else TextSecondaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de Ações Rápidas do Mini-Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão de Editar Dados - abre o diálogo completo e cancela o timer
                OutlinedButton(
                    onClick = onEditClick,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondaryDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 11.sp, color = TextSecondaryDark)
                }

                // Botão Bipar Próximo (Auto 5s se confiável, manual se incompleto)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConfidenceWarning) YellowGold else GreenNeon,
                        contentColor = if (isConfidenceWarning) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(
                        text = if (countdown != null) "Bipar Próximo (${countdown}s)" else "Bipar Próximo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botão Cockpit
                OutlinedButton(
                    onClick = onFinishToCockpit,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Cockpit", color = TextSecondaryDark, fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Tela de solicitação de permissão de câmera.
 */
@Composable
private fun CameraPermissionDeniedState(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = OrangeNeon.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "Acesso à Câmera Necessário",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Para realizar a leitura contínua de códigos de barras e o reconhecimento automático de endereço das etiquetas no galpão, o app precisa de permissão de acesso à câmera.",
                fontSize = 13.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Permitir Uso da Câmera", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voltar", color = TextSecondaryDark)
            }
        }
    }
}

/**
 * Card inferior deslizado na Etapa 2 (OCR_CONFIRMATION) conforme TASK-DES-08:
 * - Exibe código lido em destaque com opção de descarte/re-bipar;
 * - Exibe endereço e destinatário em tempo real lidos pelo OCR ML Kit;
 * - Permite alternar a plataforma ativa e o tipo de pacote (Pacotinho vs Volumoso);
 * - Botões de ação direta: CONFIRMAR PACOTE ou Pular OCR.
 */
@Composable
private fun OcrConfirmationCard(
    barcode: String,
    parsedAddress: ParsedAddress?,
    isOcrScanning: Boolean,
    activePlatform: Platform?,
    currentMarketplaceName: String?,
    currentPackageType: PackageType,
    selectedPartnerId: String?,
    partners: List<DeliveryPartner>,
    onChangePlatform: () -> Unit,
    onChangeMarketplace: () -> Unit,
    onSelectPackageType: (PackageType) -> Unit,
    onSelectPartner: (String?) -> Unit,
    onConfirmPackage: () -> Unit,
    onSkipOcr: () -> Unit,
    onRetryBarcode: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.98f)),
        border = BorderStroke(1.5.dp, OrangeNeon),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha 1: Código lido e Botão Bipar Novamente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OrangeNeon.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷️ ",
                            fontSize = 12.sp
                        )
                        Text(
                            text = barcode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )
                    }
                }

                TextButton(
                    onClick = onRetryBarcode,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Bipar Novamente",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bipar Novamente",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Linha 2: Dados do OCR
            Surface(
                color = SurfaceDarkAlt,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val hasRecipient = !parsedAddress?.recipientName.isNullOrBlank()
                    val hasAddress = !parsedAddress?.fullFormattedAddress.isNullOrBlank()
                    val hasCep = !parsedAddress?.cep.isNullOrBlank()

                    if (hasRecipient) {
                        Text(
                            text = "👤 ${parsedAddress?.recipientName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasAddress) {
                        Text(
                            text = "📍 ${parsedAddress?.fullFormattedAddress}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }

                    if (hasCep) {
                        Text(
                            text = "CEP: ${parsedAddress?.cep}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                    }

                    if (!hasRecipient && !hasAddress && !hasCep) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (isOcrScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = OrangeNeon
                                )
                                Text(
                                    text = "Enquadre a etiqueta para capturar endereço...",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            } else {
                                Text(
                                    text = "Etiqueta não identificada. Salve ou pule o OCR.",
                                    fontSize = 11.sp,
                                    color = YellowGold
                                )
                            }
                        }
                    }
                }
            }

            // Linha 3: Parametrização Operacional - Transportadora / Plataforma Ativa
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangePlatform)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "🚚 Transportadora:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = activePlatform?.name ?: "Plataforma Geral",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Trocar",
                        fontSize = 11.sp,
                        color = OrangeNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Linha 3.1: Parametrização Operacional - Tomador / Marketplace
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeMarketplace)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "🏬 Tomador / Origem:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = currentMarketplaceName ?: "Geral",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Trocar",
                        fontSize = 11.sp,
                        color = OrangeNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Linha 4: Tipo de Pacote (50% / 50%)
            val isPacotinho = currentPackageType == PackageType.PACOTINHO
            val isVolumoso = currentPackageType == PackageType.VOLUMOSO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isPacotinho) OrangeNeon else SurfaceDarkAlt,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isPacotinho) OrangeNeon else Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPackageType(PackageType.PACOTINHO) }
                ) {
                    Text(
                        text = "📦 Pacote",
                        color = if (isPacotinho) Color.White else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    color = if (isVolumoso) YellowGold else SurfaceDarkAlt,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isVolumoso) YellowGold else Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPackageType(PackageType.VOLUMOSO) }
                ) {
                    Text(
                        text = "🏋️ Volumoso",
                        color = if (isVolumoso) Color.Black else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Se houver parceiros cadastrados (Prompt 6), oferece atalho de atribuição
            if (partners.isNotEmpty()) {
                val assignedPartnerName = partners.firstOrNull { it.id == selectedPartnerId }?.fullName
                Surface(
                    color = if (selectedPartnerId != null) OrangeNeon.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selectedPartnerId != null) OrangeNeon else Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextPartner = when {
                                selectedPartnerId == null -> partners.firstOrNull()?.id
                                else -> {
                                    val idx = partners.indexOfFirst { it.id == selectedPartnerId }
                                    if (idx in 0 until partners.size - 1) partners[idx + 1].id else null
                                }
                            }
                            onSelectPartner(nextPartner)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedPartnerId != null) "👤 Atribuir a: ${assignedPartnerName ?: "Parceiro"}" else "👤 Destinado a: Master (Você)",
                            fontSize = 11.sp,
                            color = if (selectedPartnerId != null) OrangeNeon else TextSecondaryDark,
                            fontWeight = if (selectedPartnerId != null) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "Alternar",
                            fontSize = 10.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Linha 4: Botões de Ação
            Button(
                onClick = onConfirmPackage,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "CONFIRMAR PACOTE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            TextButton(
                onClick = onSkipOcr,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text(
                    text = "Pular OCR (Salvar Apenas Código)",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
        }
    }
}
