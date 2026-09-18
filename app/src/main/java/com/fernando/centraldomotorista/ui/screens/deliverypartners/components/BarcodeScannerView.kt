package com.fernando.centraldomotorista.ui.screens.deliverypartners.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    scannedCount: Int,
    expectedCount: Int,
    scannedBarcodes: Set<String>,
    onBarcodeScanned: (String) -> Unit,
    onRemoveBarcode: (String) -> Unit = {},
    onCloseScanner: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                showPermissionRationale = false
                onCloseScanner()
            },
            title = {
                Text("Permissão de Câmera Necessária", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("O aplicativo precisa de acesso à câmera para realizar a leitura e bipagem dos códigos de barras e QR Codes dos pacotes.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    onCloseScanner()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (hasCameraPermission) {
        ContinuousBarcodeScanner(
            scannedCount = scannedCount,
            expectedCount = expectedCount,
            scannedBarcodes = scannedBarcodes,
            onBarcodeScanned = onBarcodeScanned,
            onRemoveBarcode = onRemoveBarcode,
            onCloseScanner = onCloseScanner
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Permissão de câmera necessária para bipagem",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Conceder Permissão", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onCloseScanner) {
                    Text("Voltar ao Formulário", color = Color.LightGray)
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ContinuousBarcodeScanner(
    scannedCount: Int,
    expectedCount: Int,
    scannedBarcodes: Set<String>,
    onBarcodeScanned: (String) -> Unit,
    onRemoveBarcode: (String) -> Unit = {},
    onCloseScanner: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentScannedBarcodes by rememberUpdatedState(scannedBarcodes)
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableStateOf(0L) }
    var barcodeToDelete by remember { mutableStateOf<String?>(null) }
    var showScannedListDialog by remember { mutableStateOf(false) }

    // Alerta de código duplicado
    var duplicateAlertBarcode by remember { mutableStateOf<String?>(null) }
    var lastDuplicateAlertTimestamp by remember { mutableStateOf(0L) }

    // Fechamento automático do modal de código duplicado após tempo suficiente para leitura (~2.2s)
    LaunchedEffect(duplicateAlertBarcode) {
        if (duplicateAlertBarcode != null) {
            delay(2200L)
            duplicateAlertBarcode = null
        }
    }

    // Feedback helpers: Vibration + Audio Beep
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun triggerFeedback() {
        // 1. Vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerView", "Erro ao acionar vibração: ${e.message}")
        }

        // 2. Audio Beep (tom positivo de confirmação)
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("ScannerView", "Erro ao emitir beep: ${e.message}")
        }
    }

    fun triggerNegativeFeedback() {
        // 1. Double buzz vibration para aviso de erro
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 70, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 70, 150), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 100, 70, 150), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerView", "Erro ao acionar vibração de erro: ${e.message}")
        }

        // 2. Audio Beep: tom negativo (TONE_PROP_NACK)
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 280)
        } catch (e: Exception) {
            Log.e("ScannerView", "Erro ao emitir som de erro: ${e.message}")
        }
    }

    // Laser line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()
                    )

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    if (showScannedListDialog || barcodeToDelete != null) return@addOnSuccessListener
                                    val now = System.currentTimeMillis()

                                    // 1. Se houver algum código ainda NÃO escaneado no frame, prioriza o novo pacote
                                    val validNewBarcode = barcodes.mapNotNull { it.rawValue?.trim() }
                                        .firstOrNull { it.isNotBlank() && !currentScannedBarcodes.contains(it) }

                                    if (validNewBarcode != null) {
                                        lastScannedCode = validNewBarcode
                                        lastScannedTimestamp = now
                                        duplicateAlertBarcode = null
                                        triggerFeedback()
                                        currentOnBarcodeScanned(validNewBarcode)
                                    } else {
                                        // 2. Se todos os códigos do frame já foram escaneados, verificar duplicata
                                        val duplicateBarcode = barcodes.mapNotNull { it.rawValue?.trim() }
                                            .firstOrNull { it.isNotBlank() && currentScannedBarcodes.contains(it) }

                                        if (duplicateBarcode != null) {
                                            // Ignora se for o mesmo código recém-bipado (< 1.5s) enquanto o usuário afasta a câmera
                                            val isRecentScanMovingAway = (duplicateBarcode == lastScannedCode && (now - lastScannedTimestamp) < 1500L)
                                            // Evita repetir alerta sonoro para o mesmo código durante a exibição (< 2.5s)
                                            val isRecentDuplicateAlert = (duplicateBarcode == duplicateAlertBarcode && (now - lastDuplicateAlertTimestamp) < 2500L)

                                            if (!isRecentScanMovingAway && !isRecentDuplicateAlert) {
                                                lastDuplicateAlertTimestamp = now
                                                duplicateAlertBarcode = duplicateBarcode
                                                triggerNegativeFeedback()
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ScannerView", "Erro ao escanear: ${e.message}")
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("ScannerView", "Falha ao vincular CameraX: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay with Viewfinder Frame & Animated Laser
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = canvasWidth * 0.8f
            val frameHeight = frameWidth * 0.7f
            val left = (canvasWidth - frameWidth) / 2f
            val top = (canvasHeight - frameHeight) / 2.5f

            // Frame outline
            drawRoundRect(
                color = OrangeNeon,
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 4f)
            )

            // Animated Laser Line
            val laserY = top + (frameHeight * laserOffset)
            drawLine(
                color = GreenNeon,
                start = Offset(left + 16f, laserY),
                end = Offset(left + frameWidth - 16f, laserY),
                strokeWidth = 4f
            )
        }

        // Top Header: Package Counter & Controls
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCloseScanner) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar Câmera", tint = Color.White)
                    }

                    // Counter: X de Y pacotes bipados
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BIPAGEM CONTÍNUA",
                            color = OrangeNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$scannedCount",
                                color = if (expectedCount > 0 && scannedCount == expectedCount) GreenNeon else Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " / $expectedCount pacotes",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scannedCount > 0) {
                            IconButton(onClick = { showScannedListDialog = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Ver Códigos Bipados",
                                    tint = OrangeNeon
                                )
                            }
                        }

                        // Torch Toggle
                        IconButton(
                            onClick = {
                                isTorchOn = !isTorchOn
                                cameraControl?.enableTorch(isTorchOn)
                            }
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Lanterna",
                                tint = if (isTorchOn) Color.Yellow else Color.White
                            )
                        }
                    }
                }

                // Status chip / feedback
                if (lastScannedCode != null) {
                    val codeTarget = lastScannedCode!!
                    Surface(
                        color = GreenNeon.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenNeon)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Lido: $codeTarget",
                                color = GreenNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { barcodeToDelete = codeTarget },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Excluir código lido",
                                    tint = RedAlert,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de Confirmação de Exclusão de Código durante a Bipagem
        if (barcodeToDelete != null) {
            val codeToDelete = barcodeToDelete!!
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
                    Text("Deseja realmente remover o código \"$codeToDelete\" da lista de pacotes bipados?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRemoveBarcode(codeToDelete)
                            if (lastScannedCode == codeToDelete) {
                                lastScannedCode = null
                            }
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

        // Modal / Diálogo para Consultar e Gerenciar Códigos durante a Bipagem
        if (showScannedListDialog) {
            AlertDialog(
                onDismissRequest = { showScannedListDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Códigos Bipados ($scannedCount)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showScannedListDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                text = {
                    if (scannedBarcodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum código bipado até o momento.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(scannedBarcodes.toList()) { code ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = code,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { barcodeToDelete = code },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Excluir código",
                                                tint = RedAlert,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { showScannedListDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                    ) {
                        Text("Continuar Bipando", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Pequeno Modal de Código Duplicado (Auto-dismiss sem interação necessária)
        AnimatedVisibility(
            visible = duplicateAlertBarcode != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.88f, animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.88f, animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E1414).copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(2.dp, RedAlert),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { duplicateAlertBarcode = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(RedAlert.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Código Duplicado",
                            tint = RedAlert,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "CÓDIGO JÁ ESCANEADO!",
                        color = RedAlert,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Este pacote já foi bipado anteriormente nesta sessão e não pode ser adicionado novamente.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    duplicateAlertBarcode?.let { code ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = code,
                                color = OrangeNeon,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls: Complete Scanning button
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aponte a câmera para os códigos de barras ou QR Codes dos pacotes em sequência.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onCloseScanner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Concluir Bipagem ($scannedCount pacotes)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
