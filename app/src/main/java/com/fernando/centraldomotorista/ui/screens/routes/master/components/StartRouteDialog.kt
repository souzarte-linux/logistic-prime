package com.fernando.centraldomotorista.ui.screens.routes.master.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.preferences.RoutePreferences
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Destino inicial após a criação da Rota Master.
 */
enum class StartRouteDestination {
    SCANNER,
    COCKPIT
}

/**
 * Diálogo modal para criação da Rota do Dia do Usuário Master.
 * Permite capturar ponto de partida via GPS ou digitação manual e vincular a uma plataforma.
 */
@Composable
fun StartRouteDialog(
    platforms: List<Platform>,
    isCreating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmStart: (platformId: String?, startLocation: String, lat: Double?, lng: Double?, destination: StartRouteDestination) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routePreferences = remember { RoutePreferences(context) }

    // Lembra e pré-seleciona a última plataforma utilizada pelo motorista
    val lastSavedPlatformId = remember { routePreferences.getLastPlatformIdSync() }
    var selectedPlatformId by remember {
        val initialId = if (lastSavedPlatformId != null && platforms.any { it.id == lastSavedPlatformId }) {
            lastSavedPlatformId
        } else {
            platforms.firstOrNull()?.id
        }
        mutableStateOf<String?>(initialId)
    }
    var showPlatformDropdown by remember { mutableStateOf(false) }

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
        onDismissRequest = { if (!isCreating) onDismiss() },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure as informações iniciais para abrir a bipagem de pacotes.",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                // 1. Seleção de Plataforma (Combobox)
                Text(
                    text = "Plataforma de Entrega:",
                    fontSize = 12.sp,
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
                    val activePlatform = platforms.firstOrNull { it.id == selectedPlatformId }
                    val platformDisplayName = activePlatform?.name ?: "Plataforma Geral / Nenhuma"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = SurfaceDarkAlt,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selectedPlatformId != null) OrangeNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPlatformDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = platformDisplayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar Plataforma",
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPlatformDropdown,
                            onDismissRequest = { showPlatformDropdown = false },
                            containerColor = SurfaceDarkAlt,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.background(SurfaceDarkAlt)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "🌐 Plataforma Geral / Nenhuma",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedPlatformId == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPlatformId == null) OrangeNeon else TextPrimaryDark
                                    )
                                },
                                colors = MenuDefaults.itemColors(textColor = TextPrimaryDark),
                                onClick = {
                                    selectedPlatformId = null
                                    routePreferences.setLastPlatformIdSync(null)
                                    showPlatformDropdown = false
                                }
                            )

                            platforms.forEach { platform ->
                                val isSelected = platform.id == selectedPlatformId
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "📦 ${platform.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) OrangeNeon else TextPrimaryDark
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = OrangeNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = MenuDefaults.itemColors(textColor = TextPrimaryDark),
                                    onClick = {
                                        selectedPlatformId = platform.id
                                        routePreferences.setLastPlatformIdSync(platform.id)
                                        showPlatformDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Ponto de Partida (GPS vs Manual)
                Text(
                    text = "Ponto de Partida / Galpão:",
                    fontSize = 12.sp,
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
                            .height(38.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = GreenNeon
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = GreenNeon
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Usar GPS",
                            fontSize = 11.sp,
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
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Digitar",
                            fontSize = 11.sp,
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
                    label = { Text("Nome da Origem ou Galpão", fontSize = 11.sp) },
                    placeholder = { Text("Ex: Galpão Cajamar - Mercado Livre", fontSize = 11.sp) },
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

                // Feedback de erro caso ocorra falha na criação
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFF3E1A1A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Opção 1: Abrir Scanner de Pacotes (Ação Primária)
                Button(
                    onClick = {
                        if (!isCreating) {
                            val finalLocation = startLocationText.trim().ifBlank { "Galpão Base" }
                            routePreferences.setLastPlatformIdSync(selectedPlatformId)
                            onConfirmStart(
                                selectedPlatformId,
                                finalLocation,
                                startLatitude,
                                startLongitude,
                                StartRouteDestination.SCANNER
                            )
                        }
                    },
                    enabled = !isCreating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.White,
                        disabledContainerColor = OrangeNeon.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CRIANDO ROTA...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ABRIR SCANNER DE PACOTES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Opção 2: Ir direto para o Cockpit de Bordo (Ação Secundária)
                OutlinedButton(
                    onClick = {
                        if (!isCreating) {
                            val finalLocation = startLocationText.trim().ifBlank { "Galpão Base" }
                            routePreferences.setLastPlatformIdSync(selectedPlatformId)
                            onConfirmStart(
                                selectedPlatformId,
                                finalLocation,
                                startLatitude,
                                startLongitude,
                                StartRouteDestination.COCKPIT
                            )
                        }
                    },
                    enabled = !isCreating,
                    border = BorderStroke(1.dp, OrangeNeon),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangeNeon,
                        disabledContentColor = OrangeNeon.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IR PARA O COCKPIT DE BORDO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Opção 3: Cancelar (Alinhado verticalmente para não sobrepor botões)
                TextButton(
                    onClick = onDismiss,
                    enabled = !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = null
    )
}
