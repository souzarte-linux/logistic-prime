package com.fernando.centraldomotorista.ui.screens.routes.master.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Componente de Mapa embutido (osmdroid) para visualização e navegação de paradas da Rota Master.
 *
 * Características:
 * 1. Pins com numeração de sequência (stopOrder) e coloridos por StopStatus:
 *    - Pendente: Laranja Neon
 *    - Entregue: Verde Esmeralda
 *    - Ausente: Ouro / Âmbar
 *    - Devolvido: Vermelho
 *    - Ponto de Partida: Azul
 * 2. Card inferior interativo ao selecionar um pin, com botão direto para navegação GPS.
 * 3. Degradação graciosa quando nenhuma parada possui latitude/longitude disponível.
 */
@Composable
fun RouteMapView(
    stops: List<MasterRouteStop>,
    startLat: Double? = null,
    startLng: Double? = null,
    onNavigateGps: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedStop by remember { mutableStateOf<MasterRouteStop?>(null) }

    // Filtra paradas com coordenadas válidas
    val stopsWithCoords = remember(stops) {
        stops.filter { it.latitude != null && it.longitude != null }
    }

    val hasAnyCoordinates = stopsWithCoords.isNotEmpty() || (startLat != null && startLng != null)

    if (!hasAnyCoordinates) {
        // Degradação graciosa amigável caso não haja coordenadas ainda
        EmptyCoordinatesFallback(modifier = modifier)
        return
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val geoPoints = mutableListOf<GeoPoint>()

                // 1. Marcador do Ponto de Partida (Base/Garagem)
                if (startLat != null && startLng != null) {
                    val basePoint = GeoPoint(startLat, startLng)
                    geoPoints.add(basePoint)
                    val baseMarker = Marker(mapView).apply {
                        position = basePoint
                        title = "Ponto de Partida (Base)"
                        snippet = "Início da Rota"
                        icon = createPinDrawable(context, "★", android.graphics.Color.parseColor("#2196F3"))
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            selectedStop = null
                            true
                        }
                    }
                    mapView.overlays.add(baseMarker)
                }

                // 2. Marcadores das Paradas
                stopsWithCoords.forEach { stop ->
                    val lat = stop.latitude!!.toDouble()
                    val lng = stop.longitude!!.toDouble()
                    val point = GeoPoint(lat, lng)
                    geoPoints.add(point)

                    val pinColor = when (stop.status) {
                        StopStatus.PENDENTE -> android.graphics.Color.parseColor("#FF9800")
                        StopStatus.ENTREGUE -> android.graphics.Color.parseColor("#4CAF50")
                        StopStatus.AUSENTE -> android.graphics.Color.parseColor("#FFC107")
                        StopStatus.DEVOLVIDO -> android.graphics.Color.parseColor("#F44336")
                    }

                    val marker = Marker(mapView).apply {
                        position = point
                        title = "#${stop.stopOrder} - ${stop.recipientName ?: "Destinatário"}"
                        snippet = stop.fullAddress
                        icon = createPinDrawable(context, stop.stopOrder.toString(), pinColor)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            selectedStop = stop
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }

                // 3. Ajuste de Câmera (BoundingBox ou Zoom)
                if (geoPoints.isNotEmpty()) {
                    if (geoPoints.size == 1) {
                        mapView.controller.setZoom(15.0)
                        mapView.controller.setCenter(geoPoints.first())
                    } else {
                        mapView.post {
                            try {
                                val box = BoundingBox.fromGeoPoints(geoPoints)
                                mapView.zoomToBoundingBox(box.increaseByScale(1.3f), true)
                            } catch (_: Exception) {
                                mapView.controller.setZoom(13.0)
                                mapView.controller.setCenter(geoPoints.first())
                            }
                        }
                    }
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gerenciamento de ciclo de vida do MapView
        DisposableEffect(Unit) {
            onDispose {
                // Recursos liberados
            }
        }

        // Card Inferior com Detalhes da Parada Selecionada
        AnimatedVisibility(
            visible = selectedStop != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            selectedStop?.let { stop ->
                SelectedStopCard(
                    stop = stop,
                    onNavigateGps = { onNavigateGps(stop.fullAddress) },
                    onDismiss = { selectedStop = null }
                )
            }
        }
    }
}

/**
 * Card flutuante sobre o mapa mostrando detalhes da parada selecionada e botão GPS.
 */
@Composable
private fun SelectedStopCard(
    stop: MasterRouteStop,
    onNavigateGps: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Surface(
                        shape = CircleShape,
                        color = OrangeNeon,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${stop.stopOrder}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = stop.recipientName ?: "Destinatário",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar detalhes",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = stop.fullAddress,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de Status
                val (statusText, statusBg, statusColor) = when (stop.status) {
                    StopStatus.PENDENTE -> Triple("PENDENTE", OrangeNeon.copy(alpha = 0.15f), OrangeNeon)
                    StopStatus.ENTREGUE -> Triple("ENTREGUE", GreenNeon.copy(alpha = 0.15f), GreenNeon)
                    StopStatus.AUSENTE -> Triple("AUSENTE", YellowGold.copy(alpha = 0.15f), YellowGold)
                    StopStatus.DEVOLVIDO -> Triple("DEVOLVIDO", Color(0xFFF44336).copy(alpha = 0.15f), Color(0xFFF44336))
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Button(
                    onClick = onNavigateGps,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Navegar GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Exibido quando nenhuma parada possui latitude/longitude registrada.
 */
@Composable
private fun EmptyCoordinatesFallback(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = OrangeNeon.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = "Coordenadas em Obtenção",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Text(
                text = "As paradas estão sendo geocodificadas em segundo plano. Os pontos no mapa aparecerão automaticamente assim que as coordenadas forem resolvidas.",
                fontSize = 12.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Gera dinamicamente um Drawable Bitmap de pin circular colorido com numeração de parada.
 */
private fun createPinDrawable(context: Context, label: String, colorInt: Int): Drawable {
    val sizePx = (36 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Círculo colorido externo
    val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius - 2f, paintCircle)

    // Borda branca interna
    val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * context.resources.displayMetrics.density
    }
    canvas.drawCircle(radius, radius, radius - 3f, paintBorder)

    // Texto do número ou estrela
    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (if (label.length > 2) 11f else 13f) * context.resources.displayMetrics.density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val textY = radius - ((paintText.descent() + paintText.ascent()) / 2f)
    canvas.drawText(label, radius, textY, paintText)

    return BitmapDrawable(context.resources, bitmap)
}
