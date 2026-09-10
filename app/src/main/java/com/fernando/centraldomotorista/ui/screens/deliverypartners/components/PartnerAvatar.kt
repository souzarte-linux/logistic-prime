package com.fernando.centraldomotorista.ui.screens.deliverypartners.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.abs

/**
 * Avatar circular do entregador parceiro:
 * - Renderiza a imagem do photo_url se disponível.
 * - Caso contrário (ou se o download falhar), exibe as iniciais do primeiro nome + sobrenome
 *   num círculo com gradiente colorido vibrante.
 */
@Composable
fun PartnerAvatar(
    photoUrl: String?,
    name: String?,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(photoUrl) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(photoUrl) { mutableStateOf(false) }

    LaunchedEffect(photoUrl) {
        if (!photoUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(photoUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    val stream = connection.getInputStream()
                    val decoded = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        bitmap = decoded
                        loadFailed = decoded == null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        loadFailed = true
                    }
                }
            }
        } else {
            bitmap = null
            loadFailed = false
        }
    }

    val initials = remember(name) {
        getPartnerInitials(name)
    }

    val gradientColors = remember(name) {
        getAvatarGradient(name)
    }

    val fontSize = (size.value * 0.36f).sp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = Brush.linearGradient(gradientColors))
            .border(1.5.dp, OrangeNeon.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !loadFailed) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = name ?: "Foto do parceiro",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Retorna as iniciais do primeiro nome + sobrenome (ex: "João Silva" -> "JS").
 */
fun getPartnerInitials(fullName: String?): String {
    if (fullName.isNullOrBlank()) return "EP"
    val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "EP"
    }
}

/**
 * Formata o nome para "Nome + Segundo Nome" (ex: "João da Silva Santos" -> "João Silva" ou "João Santos").
 * Trata preposições como 'de', 'da', 'do', 'dos', 'das' para pegar o sobrenome real.
 */
fun formatShortName(fullName: String?): String {
    if (fullName.isNullOrBlank()) return ""
    val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (parts.size <= 1) return parts.firstOrNull() ?: ""

    val prepositions = setOf("de", "da", "do", "das", "dos", "e")
    val first = parts[0]
    val second = parts.drop(1).firstOrNull { it.lowercase() !in prepositions } ?: parts[1]
    return "$first $second"
}

/**
 * Retorna o ícone do Material Icons correspondente ao tipo de entrega.
 */
fun getDeliveryTypeIcon(deliveryType: String?): ImageVector {
    return when (deliveryType?.lowercase()) {
        "a_pe" -> Icons.Default.DirectionsWalk
        "bike" -> Icons.Default.DirectionsBike
        "moto" -> Icons.Default.TwoWheeler
        "carro" -> Icons.Default.DirectionsCar
        "utilitario" -> Icons.Default.LocalShipping
        else -> Icons.Default.TwoWheeler
    }
}

/**
 * Retorna o emoji correspondente ao tipo de entrega.
 */
fun getDeliveryTypeEmoji(deliveryType: String?): String {
    return when (deliveryType?.lowercase()) {
        "a_pe" -> "🚶"
        "bike" -> "🚲"
        "moto" -> "🛵"
        "carro" -> "🚗"
        "utilitario" -> "🚚"
        else -> "🛵"
    }
}

/**
 * Retorna o rótulo legível do tipo de entrega.
 */
fun getDeliveryTypeLabel(deliveryType: String?): String {
    return when (deliveryType?.lowercase()) {
        "a_pe" -> "A pé"
        "bike" -> "Bike"
        "moto" -> "Moto"
        "carro" -> "Carro"
        "utilitario" -> "Utilitário"
        else -> "Moto"
    }
}

private fun getAvatarGradient(name: String?): List<Color> {
    val hash = abs((name ?: "").hashCode())
    val palettes = listOf(
        listOf(OrangeNeon, Color(0xFFFF5722)),
        listOf(Color(0xFF3F51B5), Color(0xFF2196F3)),
        listOf(Color(0xFF009688), Color(0xFF4CAF50)),
        listOf(Color(0xFF9C27B0), Color(0xFFE91E63)),
        listOf(Color(0xFF673AB7), Color(0xFF3F51B5)),
        listOf(Color(0xFFFF9800), Color(0xFFFF5722))
    )
    return palettes[hash % palettes.size]
}
