package com.fernando.centraldomotorista.ui.screens.routes.master.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Diálogo modal para criação da Rota do Dia do Usuário Master.
 * Permite capturar ponto de partida via GPS ou digitação manual e vincular a uma plataforma.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartRouteDialog(
    platforms: List<Platform>,
    onDismiss: () -> Unit,
    onConfirmStart: (platformId: String?, startLocation: String, lat: Double?, lng: Double?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPlatformId by remember { mutableStateOf<String?>(platforms.firstOrNull()?.id) }
    var startLocationText by remember { mutableStateOf("") }
    var startLatitude by remember { mutableStateOf<Double?>(null) }
    var startLongitude by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var isManualLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                isLocating = true
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    startLatitude = loc.latitude
                    startLongitude = loc.longitude
                    val addr = LocationHelper.reverseGeocode(context, loc.latitude, loc.longitude)
                    val formatted = if (addr != null) {
                        "${addr.thoroughfare ?: addr.subLocality ?: "Localização GPS"}, ${addr.subAdminArea ?: addr.adminArea ?: ""}".trimEnd(',', ' ')
                    } else {
                        "GPS (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})"
                    }
                    startLocationText = formatted
                    isManualLocation = false
                } else {
                    isManualLocation = true
                }
                isLocating = false
            }
        } else {
            isManualLocation = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Iniciar Rota do Dia",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure as informações iniciais para abrir a bipagem de pacotes.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )

                // 1. Seleção de Plataforma
                Text(
                    text = "Plataforma de Entrega:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                if (platforms.isEmpty()) {
                    Text(
                        text = "Nenhuma plataforma cadastrada. A rota será criada como Geral.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        platforms.forEach { platform ->
                            val isSelected = selectedPlatformId == platform.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlatformId = platform.id },
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
                                    selectedContainerColor = OrangeNeon.copy(alpha = 0.15f),
                                    selectedLabelColor = OrangeNeon,
                                    containerColor = SurfaceDarkAlt,
                                    labelColor = TextSecondaryDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) OrangeNeon else Color.Transparent,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Ponto de Partida (GPS vs Manual)
                Text(
                    text = "Ponto de Partida / Galpão:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão GPS
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isManualLocation && startLocationText.isNotBlank()) GreenNeon.copy(alpha = 0.2f) else SurfaceDarkAlt,
                            contentColor = if (!isManualLocation && startLocationText.isNotBlank()) GreenNeon else TextPrimaryDark
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (!isManualLocation && startLocationText.isNotBlank()) GreenNeon else Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = GreenNeon
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GreenNeon
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Usar GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Botão Digitar Manual
                    Button(
                        onClick = { isManualLocation = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isManualLocation) OrangeNeon.copy(alpha = 0.15f) else SurfaceDarkAlt,
                            contentColor = if (isManualLocation) OrangeNeon else TextPrimaryDark
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isManualLocation) OrangeNeon else Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Digitar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Campo de texto de localização
                OutlinedTextField(
                    value = startLocationText,
                    onValueChange = {
                        startLocationText = it
                        isManualLocation = true
                    },
                    label = { Text("Nome da Origem ou Galpão", fontSize = 12.sp) },
                    placeholder = { Text("Ex: Galpão Cajamar - Mercado Livre", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = SurfaceDarkAlt,
                        unfocusedContainerColor = SurfaceDarkAlt,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLocation = startLocationText.trim().ifBlank { "Galpão Base" }
                    onConfirmStart(selectedPlatformId, finalLocation, startLatitude, startLongitude)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ABRIR SCANNER DE PACOTES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = TextSecondaryDark)
            }
        }
    )
}
