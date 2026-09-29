package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.screens.routes.master.ScannerStep
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon

/**
 * Overlay visual com fluxo fracionado em 2 etapas (TASK-DES-08):
 * - ScannerStep.BARCODE_SEARCH: Mira centralizada ampla exclusiva de código de barras com laser verde animado.
 *   Visor 100% desobstruído de seletores superiores.
 * - ScannerStep.OCR_CONFIRMATION: Mira de cantos em L (brackets) no terço superior para enquadramento da etiqueta.
 */
@Composable
fun DualScannerOverlay(
    modifier: Modifier = Modifier,
    currentStep: ScannerStep = ScannerStep.BARCODE_SEARCH,
    isProcessing: Boolean = false
) {
    val density = LocalDensity.current

    // Animação contínua da linha laser no retículo de código de barras (Etapa 1)
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val strokeWidthPx = with(density) { 3.5.dp.toPx() }
    val bracketLengthPx = with(density) { 24.dp.toPx() }
    val bracketStrokePx = with(density) { 3.dp.toPx() }
    val laserPaddingPx = with(density) { 12.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            when (currentStep) {
                ScannerStep.BARCODE_SEARCH -> {
                    // --- ETAPA 1: Mira Centralizada Ampla de Código de Barras ---
                    val barcodeWidth = canvasWidth * 0.84f
                    val barcodeHeight = with(density) { 130.dp.toPx() }
                    val barcodeLeft = (canvasWidth - barcodeWidth) / 2f
                    val barcodeTop = canvasHeight * 0.36f

                    // Fundo escurecido suave com recorte na mira central
                    val scrimPath = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                        addRoundRect(
                            RoundRect(
                                left = barcodeLeft,
                                top = barcodeTop,
                                right = barcodeLeft + barcodeWidth,
                                bottom = barcodeTop + barcodeHeight,
                                cornerRadius = CornerRadius(cornerRadiusPx)
                            )
                        )
                    }
                    drawPath(
                        path = scrimPath,
                        color = Color.Black.copy(alpha = 0.45f)
                    )

                    // Borda do Retículo de Barcode
                    drawRoundRect(
                        color = OrangeNeon,
                        topLeft = Offset(barcodeLeft, barcodeTop),
                        size = Size(barcodeWidth, barcodeHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Linha laser verde animada
                    val laserY = barcodeTop + (barcodeHeight * laserProgress)
                    drawLine(
                        color = GreenNeon,
                        start = Offset(barcodeLeft + laserPaddingPx, laserY),
                        end = Offset(barcodeLeft + barcodeWidth - laserPaddingPx, laserY),
                        strokeWidth = with(density) { 3.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }

                ScannerStep.OCR_CONFIRMATION -> {
                    // --- ETAPA 2: Retículo Superior de OCR de Texto ---
                    val ocrWidth = canvasWidth * 0.88f
                    val ocrHeight = with(density) { 140.dp.toPx() }
                    val ocrLeft = (canvasWidth - ocrWidth) / 2f
                    val ocrTop = with(density) { 80.dp.toPx() }
                    val ocrRight = ocrLeft + ocrWidth
                    val ocrBottom = ocrTop + ocrHeight
                    val bracketColor = Color.White.copy(alpha = 0.85f)

                    // Scrim escurecido fora da área de OCR
                    val scrimPath = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                        addRoundRect(
                            RoundRect(
                                left = ocrLeft,
                                top = ocrTop,
                                right = ocrRight,
                                bottom = ocrBottom,
                                cornerRadius = CornerRadius(cornerRadiusPx)
                            )
                        )
                    }
                    drawPath(
                        path = scrimPath,
                        color = Color.Black.copy(alpha = 0.45f)
                    )

                    // Fundo sutil do OCR
                    drawRoundRect(
                        color = OrangeNeon.copy(alpha = 0.08f),
                        topLeft = Offset(ocrLeft, ocrTop),
                        size = Size(ocrWidth, ocrHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )

                    // Brackets (cantos em L)
                    // Canto superior esquerdo
                    drawLine(bracketColor, Offset(ocrLeft, ocrTop), Offset(ocrLeft + bracketLengthPx, ocrTop), bracketStrokePx, StrokeCap.Round)
                    drawLine(bracketColor, Offset(ocrLeft, ocrTop), Offset(ocrLeft, ocrTop + bracketLengthPx), bracketStrokePx, StrokeCap.Round)

                    // Canto superior direito
                    drawLine(bracketColor, Offset(ocrRight, ocrTop), Offset(ocrRight - bracketLengthPx, ocrTop), bracketStrokePx, StrokeCap.Round)
                    drawLine(bracketColor, Offset(ocrRight, ocrTop), Offset(ocrRight, ocrTop + bracketLengthPx), bracketStrokePx, StrokeCap.Round)

                    // Canto inferior esquerdo
                    drawLine(bracketColor, Offset(ocrLeft, ocrBottom), Offset(ocrLeft + bracketLengthPx, ocrBottom), bracketStrokePx, StrokeCap.Round)
                    drawLine(bracketColor, Offset(ocrLeft, ocrBottom), Offset(ocrLeft, ocrBottom - bracketLengthPx), bracketStrokePx, StrokeCap.Round)

                    // Canto inferior direito
                    drawLine(bracketColor, Offset(ocrRight, ocrBottom), Offset(ocrRight - bracketLengthPx, ocrBottom), bracketStrokePx, StrokeCap.Round)
                    drawLine(bracketColor, Offset(ocrRight, ocrBottom), Offset(ocrRight, ocrBottom - bracketLengthPx), bracketStrokePx, StrokeCap.Round)
                }
            }
        }

        // Rótulos informativos alinhados de acordo com a etapa ativa
        when (currentStep) {
            ScannerStep.BARCODE_SEARCH -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(bottom = 170.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Centralize o Código de Barras ou QR Code",
                        color = OrangeNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            ScannerStep.OCR_CONFIRMATION -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enquadre a Etiqueta (Nome, Endereço e CEP)",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
