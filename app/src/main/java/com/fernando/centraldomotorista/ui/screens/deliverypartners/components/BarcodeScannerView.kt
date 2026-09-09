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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    scannedCount: Int,
    expectedCount: Int,
    scannedBarcodes: Set<String>,
    onBarcodeScanned: (String) -> Unit,
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
    onCloseScanner: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

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

        // 2. Audio Beep
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("ScannerView", "Erro ao emitir beep: ${e.message}")
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
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue?.trim()
                                        if (!rawValue.isNullOrBlank()) {
                                            // Ignore if already scanned in this session
                                            if (!scannedBarcodes.contains(rawValue)) {
                                                lastScannedCode = rawValue
                                                triggerFeedback()
                                                onBarcodeScanned(rawValue)
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

                // Status chip / feedback
                if (lastScannedCode != null) {
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
                                text = "Lido: $lastScannedCode",
                                color = GreenNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
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
