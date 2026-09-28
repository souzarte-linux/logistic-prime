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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon

/**
 * Overlay visual com retículo duplo sobre o visor CameraX:
 * 1. Mira Superior: Código de Barras / QR Code com laser verde animado.
 * 2. Mira Inferior: OCR de Nome, Endereço e CEP com moldura de cantos em L (brackets).
 */
@Composable
fun DualScannerOverlay(
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false
) {
    val density = LocalDensity.current

    // Animação contínua da linha laser no retículo de código de barras
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

    val barcodeBoxHeightPx = with(density) { 110.dp.toPx() }
    val ocrBoxHeightPx = with(density) { 140.dp.toPx() }
    val gapBetweenPx = with(density) { 68.dp.toPx() }
    val bracketLengthPx = with(density) { 24.dp.toPx() }
    val strokeWidthPx = with(density) { 3.5.dp.toPx() }
    val bracketStrokePx = with(density) { 3.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val barcodeWidth = canvasWidth * 0.82f
            val barcodeLeft = (canvasWidth - barcodeWidth) / 2f
            val barcodeTop = canvasHeight * 0.16f

            val ocrWidth = canvasWidth * 0.88f
            val ocrLeft = (canvasWidth - ocrWidth) / 2f
            val ocrTop = barcodeTop + barcodeBoxHeightPx + gapBetweenPx

            // Fundo escurecido semi-transparente fora dos retículos
            val scrimPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                addRoundRect(
                    RoundRect(
                        left = barcodeLeft,
                        top = barcodeTop,
                        right = barcodeLeft + barcodeWidth,
                        bottom = barcodeTop + barcodeBoxHeightPx,
                        cornerRadius = CornerRadius(with(density) { 16.dp.toPx() })
                    )
                )
                addRoundRect(
                    RoundRect(
                        left = ocrLeft,
                        top = ocrTop,
                        right = ocrLeft + ocrWidth,
                        bottom = ocrTop + ocrBoxHeightPx,
                        cornerRadius = CornerRadius(with(density) { 16.dp.toPx() })
                    )
                )
            }

            // Desenha scrim com transparência suave
            drawPath(
                path = scrimPath,
                color = Color.Black.copy(alpha = 0.48f)
            )

            // --- 1. Retículo Superior: Barcode ---
            drawRoundRect(
                color = OrangeNeon,
                topLeft = Offset(barcodeLeft, barcodeTop),
                size = Size(barcodeWidth, barcodeBoxHeightPx),
                cornerRadius = CornerRadius(with(density) { 16.dp.toPx() }),
                style = Stroke(width = strokeWidthPx)
            )

            // Linha laser verde animada dentro do retículo superior
            val laserY = barcodeTop + (barcodeBoxHeightPx * laserProgress)
            val laserPadding = with(density) { 12.dp.toPx() }
            drawLine(
                color = GreenNeon,
                start = Offset(barcodeLeft + laserPadding, laserY),
                end = Offset(barcodeLeft + barcodeWidth - laserPadding, laserY),
                strokeWidth = with(density) { 3.dp.toPx() },
                cap = StrokeCap.Round
            )

            // --- 2. Retículo Inferior: OCR de Texto (Cantos tipo bracket) ---
            val ocrRight = ocrLeft + ocrWidth
            val ocrBottom = ocrTop + ocrBoxHeightPx
            val bracketColor = Color.White.copy(alpha = 0.85f)

            // Fundo sutil do OCR
            drawRoundRect(
                color = OrangeNeon.copy(alpha = 0.08f),
                topLeft = Offset(ocrLeft, ocrTop),
                size = Size(ocrWidth, ocrBoxHeightPx),
                cornerRadius = CornerRadius(with(density) { 12.dp.toPx() })
            )

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

        // Rótulos informativos alinhados acima de cada retículo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Posicione o Código de Barras / QR Code",
                color = OrangeNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Spacer(modifier = Modifier.height(115.dp))

            Text(
                text = "Enquadre a Etiqueta (Nome, Endereço e CEP)",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
