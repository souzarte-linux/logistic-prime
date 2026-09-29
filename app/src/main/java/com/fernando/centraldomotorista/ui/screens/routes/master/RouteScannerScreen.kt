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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.screens.routes.master.components.DualScannerOverlay
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

                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val detectedBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                                if (detectedBarcode != null) {
                                    if (viewModel.isBarcodeAlreadyScanned(detectedBarcode)) {
                                        viewModel.onDuplicateBarcodeDetected(ctx, detectedBarcode)
                                        viewModel.isAnalyzingFrame.set(false)
                                        imageProxy.close()
                                    } else {
                                        // Código novo! Processa OCR para capturar endereço
                                        textRecognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val parsed = BrazilianLabelParser.parse(visionText.text)
                                                viewModel.onPackageScanned(ctx, detectedBarcode, parsed)
                                            }
                                            .addOnFailureListener {
                                                val fallback = BrazilianLabelParser.parse("")
                                                viewModel.onPackageScanned(ctx, detectedBarcode, fallback)
                                            }
                                            .addOnCompleteListener {
                                                viewModel.isAnalyzingFrame.set(false)
                                                imageProxy.close()
                                            }
                                    }
                                } else {
                                    viewModel.isAnalyzingFrame.set(false)
                                    imageProxy.close()
                                }
                            }
                            .addOnFailureListener {
                                viewModel.isAnalyzingFrame.set(false)
                                imageProxy.close()
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

        // --- 2. Overlay do Retículo Duplo com Laser Animado ---
        DualScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            isProcessing = uiState.isSaving
        )

        // --- 3. Barra Superior e Controles do Scanner ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "📦 ${uiState.totalScannedCount} Bipados",
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

            Spacer(modifier = Modifier.height(10.dp))

            // --- Prompt 2: Indicador Grande e Destacado de Plataforma Ativa (Sticky) ---
            val activePlatName = uiState.activePlatform?.name ?: "Plataforma Geral"
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, OrangeNeon),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPlatformSelectorDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PLATAFORMA ATIVA (STICKY):",
                                fontSize = 9.sp,
                                color = OrangeNeon,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activePlatName.uppercase(),
                                fontSize = 13.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Trocar",
                            fontSize = 11.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Prompt 2: Seletor de Tipo de Pacote (Pacotinho vs Volumoso) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isPacotinho = uiState.currentPackageType == PackageType.PACOTINHO
                Surface(
                    color = if (isPacotinho) OrangeNeon else Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isPacotinho) OrangeNeon else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.onPackageTypeSelected(PackageType.PACOTINHO) }
                ) {
                    Text(
                        text = "📦 Pacotinho",
                        color = if (isPacotinho) Color.White else TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }

                val isVolumoso = uiState.currentPackageType == PackageType.VOLUMOSO
                Surface(
                    color = if (isVolumoso) YellowGold else Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isVolumoso) YellowGold else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.onPackageTypeSelected(PackageType.VOLUMOSO) }
                ) {
                    Text(
                        text = "🏋️ Volumoso",
                        color = if (isVolumoso) Color.Black else TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // --- 4. Alerta de Pacote Duplicado ---
        AnimatedVisibility(
            visible = uiState.duplicateAlertBarcode != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 180.dp)
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

        // --- 5. Botão de Finalizar Carga e Ir para Cockpit (Fixo no Rodapé se não houver mini-card ativo) ---
        if (uiState.lastScannedStop == null) {
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

        // --- 6. Mini-Card Inferior com Dados do Pacote Bipado + Extensões dos Prompts 3 e 6 ---
        AnimatedVisibility(
            visible = uiState.lastScannedStop != null,
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
                    onUpdateManualData = { name, addr, cep ->
                        viewModel.updateStopManualData(name, addr, cep)
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
 * Mini-card inferior com feedback de confirmação imediata do pacote lido
 * e suporte a:
 * - Gate de confiança (bloqueio do countdown e modo edição manual)
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
    onUpdateManualData: (String?, String, String?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(stop.recipientName ?: "") }
    var editAddress by remember { mutableStateOf(stop.fullAddress) }
    var editCep by remember { mutableStateOf(stop.cep ?: "") }

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

            // Modo Edição Manual ou Exibição
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Destinatário", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Endereço Completo", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCep,
                        onValueChange = { editCep = it },
                        label = { Text("CEP", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onUpdateManualData(editName, editAddress, editCep)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar Correção", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de Ações Rápidas do Mini-Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão de Editar Dados
                if (!isEditing) {
                    OutlinedButton(
                        onClick = { isEditing = true },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondaryDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }

                // Botão Bipar Próximo (Auto 2s se confiável, manual se incompleto)
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
